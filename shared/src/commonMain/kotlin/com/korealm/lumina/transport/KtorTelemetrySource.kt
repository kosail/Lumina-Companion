package com.korealm.lumina.transport

import com.korealm.lumina.protocol.Liveness
import com.korealm.lumina.protocol.StatusDecodeResult
import com.korealm.lumina.protocol.decodeStatus
import com.korealm.lumina.protocol.reduceLiveness
import com.korealm.lumina.protocol.subscribeLine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** How often the maintenance loop wakes to re-subscribe and to evaluate liveness. */
private const val TICK_PERIOD_MS: Long = 250L

/** Re-send `subscribe` this often to stay within `SUBSCRIBER_TTL` (contract §2/§3.1). */
private const val SUBSCRIBE_INTERVAL_MS: Long = 5_000L

/**
 * Ends the current session because the gateway changed (the user edited Ajustes).
 *
 * Kotlin note: this is control flow via an exception. A child coroutine (the ticker collector)
 * cannot "return" the parent `channelFlow`, so it throws; the exception cancels the session and is
 * rethrown to the outer loop, which reconnects immediately with the new endpoint. A dedicated type
 * (not a generic `Throwable`) keeps it out of the failure/backoff path.
 */
private class EndpointChangedException : RuntimeException("gateway changed")

/**
 * Production [TelemetrySource] over the injected [TelemetrySocketFactory].
 *
 * Responsibility: run one UDP session — send `subscribe`, decode each `status`, re-subscribe every
 * 5 s, and report offline after 5 s of silence — and retry the session with [backoffDelayMs] when
 * the socket fails (contract §3.1/§3.3). All timing is injected ([Clock]/[Ticker]) so tests are
 * deterministic (FE-INV-031/052).
 *
 * The socket itself is created through [TelemetrySocketFactory], so this class contains no Ktor
 * types and is unit-tested with a fake socket.
 *
 * @param endpointProvider supplies the current gateway (read from settings at collection time).
 * @param sockets opens the UDP socket.
 * @param clock monotonic clock for liveness/re-subscribe decisions.
 * @param ticker periodic source that drives the maintenance loop.
 */
class KtorTelemetrySource(
    private val endpointProvider: () -> Endpoint,
    private val sockets: TelemetrySocketFactory,
    private val clock: Clock = MonotonicClock,
    private val ticker: Ticker = DelayTicker,
) : TelemetrySource {

    override fun status(): Flow<TelemetryEvent> = flow {
        var attempt = 0
        while (true) {
            try {
                // `emitAll` keeps the flow alive for the whole session; it returns only if the
                // session ends, which in practice means an error (the receive loop is infinite).
                emitAll(session(endpointProvider()))
                attempt = 0
            } catch (e: CancellationException) {
                // Collector cancelled: propagate without emitting or backing off (FE-INV-052).
                throw e
            } catch (e: EndpointChangedException) {
                // The gateway changed (Ajustes): reconnect immediately to the new endpoint, with no
                // backoff and without reporting offline (the old socket was healthy, not failed).
                attempt = 0
            } catch (e: Throwable) {
                // Any transport failure: report offline, then retry after backoff.
                attempt += 1
                emit(TelemetryEvent.Offline)
                delay(backoffDelayMs(attempt))
            }
        }
    }

    /** One connection lifetime: subscribe, receive, and maintain until the socket fails. */
    private fun session(endpoint: Endpoint): Flow<TelemetryEvent> = channelFlow {
        val socket = sockets.open(endpoint)
        // Seed liveness at connect time rather than `null`: the 5 s offline window must run from the
        // moment the session starts, so the first ticker pass (~250 ms) does not report Offline
        // before a datagram is even expected. `null` would read as immediately Offline
        // (FE-INV-031; see R1 in CHG-FE-0020).
        var lastArrivalMs: Long = clock.nowMs()
        var lastSubscribeMs = clock.nowMs()
        try {
            socket.send(subscribeLine().encodeToByteArray())
            launch {
                ticker.ticks(TICK_PERIOD_MS).collect {
                    // Settings may have changed the gateway; end this session so the outer loop
                    // reconnects to the new host at once (otherwise Ajustes appears to do nothing).
                    if (endpointProvider() != endpoint) {
                        throw EndpointChangedException()
                    }
                    val now = clock.nowMs()
                    if (now - lastSubscribeMs >= SUBSCRIBE_INTERVAL_MS) {
                        socket.send(subscribeLine().encodeToByteArray())
                        lastSubscribeMs = now
                    }
                    if (reduceLiveness(lastArrivalMs, now) == Liveness.Offline) {
                        send(TelemetryEvent.Offline)
                    }
                }
            }
            while (isActive) {
                val bytes = socket.receive()
                val now = clock.nowMs()
                when (val result = decodeStatus(bytes.decodeToString(), now)) {
                    is StatusDecodeResult.Decoded -> {
                        // Only a decodable frame refreshes liveness (FE-INV-031): a garbage datagram
                        // must not keep the dashboard looking "online".
                        lastArrivalMs = now
                        send(TelemetryEvent.Online(result.status))
                    }

                    is StatusDecodeResult.UnsupportedProto -> {
                        // The device is present — it is sending frames we merely cannot speak — so an
                        // unsupported `proto` must still refresh liveness. Otherwise the ticker would
                        // overwrite the "incompatible" banner with Offline within one tick (R2 in
                        // CHG-FE-0020). Only undecodable garbage leaves liveness untouched.
                        lastArrivalMs = now
                        send(TelemetryEvent.Incompatible(result.proto))
                    }

                    is StatusDecodeResult.Malformed -> Unit
                }
            }
        } finally {
            socket.close()
        }
    }
}
