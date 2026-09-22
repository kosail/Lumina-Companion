package com.korealm.lumina.transport

import com.korealm.lumina.protocol.subscribeLine
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** A valid `status` frame from `API_CONTRACT.md` §4.1. */
private const val VALID_STATUS =
    """{"t":"status","proto":1,"ts":1690000000,"runtime":{"reachable":true,"running":true,"uptimeS":62,"sink":"ready","faceCount":2},"core":{"fps":4.3,"rssMb":218.0,"memAvailableKb":65536,"tempC":46.2,"load1":1.80},"sensors":{"volume":70,"muted":false,"luma":0.420,"dayNight":"day"},"people":["David Solís"],"enroll":{"active":false}}"""

/** The same frame with an unsupported protocol version. */
private val VALID_STATUS_PROTO_2: String = VALID_STATUS.replace(""""proto":1""", """"proto":2""")

/**
 * Tests for [KtorTelemetrySource]: subscribe handshake, frame decoding, liveness, re-subscribe and
 * retry — all driven by fakes, no network and no real time (PLAN Phase 2).
 */
class TelemetrySourceTest {

    private fun source(
        socket: TelemetrySocket,
        clock: Clock,
        ticker: Ticker,
    ): TelemetrySource = KtorTelemetrySource(
        endpointProvider = { Endpoint("gateway") },
        sockets = FakeTelemetrySocketFactory(socket),
        clock = clock,
        ticker = ticker,
    )

    @Test
    fun sendsSubscribeOnStart() = runTest {
        val socket = FakeTelemetrySocket()
        val job = launch { source(socket, FakeClock(), FakeTicker()).status().collect {} }
        runCurrent()

        assertEquals(subscribeLine(), socket.sent.single().decodeToString())
        job.cancelAndJoin()
    }

    @Test
    fun emitsOnlineForAValidFrame() = runTest {
        val socket = FakeTelemetrySocket()
        val clock = FakeClock()
        val events = mutableListOf<TelemetryEvent>()
        val job = launch { source(socket, clock, FakeTicker()).status().collect { events += it } }
        runCurrent()

        clock.now = 1_000
        socket.deliver(VALID_STATUS.encodeToByteArray())
        runCurrent()

        val online = assertIs<TelemetryEvent.Online>(events.last())
        assertEquals(1_000L, online.status.receivedAtMs)
        assertEquals(70, online.status.sensors.volumePercent)
        job.cancelAndJoin()
    }

    @Test
    fun ignoresMalformedDatagrams() = runTest {
        val socket = FakeTelemetrySocket()
        val events = mutableListOf<TelemetryEvent>()
        val job = launch { source(socket, FakeClock(), FakeTicker()).status().collect { events += it } }
        runCurrent()

        socket.deliver("not json at all".encodeToByteArray())
        runCurrent()

        assertTrue(events.isEmpty())
        job.cancelAndJoin()
    }

    @Test
    fun emitsIncompatibleForAnUnsupportedProto() = runTest {
        val socket = FakeTelemetrySocket()
        val events = mutableListOf<TelemetryEvent>()
        val job = launch { source(socket, FakeClock(), FakeTicker()).status().collect { events += it } }
        runCurrent()

        socket.deliver(VALID_STATUS_PROTO_2.encodeToByteArray())
        runCurrent()

        val incompatible = assertIs<TelemetryEvent.Incompatible>(events.last())
        assertEquals(2, incompatible.proto)
        job.cancelAndJoin()
    }

    @Test
    fun emitsOfflineOnceLivenessExpires() = runTest {
        val socket = FakeTelemetrySocket()
        val clock = FakeClock()
        val ticker = FakeTicker()
        val events = mutableListOf<TelemetryEvent>()
        val job = launch { source(socket, clock, ticker).status().collect { events += it } }
        runCurrent()

        clock.now = 1_000
        socket.deliver(VALID_STATUS.encodeToByteArray())
        runCurrent()

        // 4999 ms since the last status: still online.
        clock.now = 5_999
        ticker.tick()
        runCurrent()
        assertTrue(events.none { it is TelemetryEvent.Offline })

        // Exactly 5000 ms: offline (inclusive boundary, FE-INV-031).
        clock.now = 6_000
        ticker.tick()
        runCurrent()
        assertIs<TelemetryEvent.Offline>(events.last())
        job.cancelAndJoin()
    }

    @Test
    fun resubscribesAfterFiveSeconds() = runTest {
        val socket = FakeTelemetrySocket()
        val clock = FakeClock()
        val ticker = FakeTicker()
        val job = launch { source(socket, clock, ticker).status().collect {} }
        runCurrent()
        assertEquals(1, socket.sent.size)

        clock.now = 5_000
        ticker.tick()
        runCurrent()

        assertEquals(2, socket.sent.size)
        job.cancelAndJoin()
    }

    @Test
    fun emitsOfflineAndKeepsRetryingOnSocketFailure() = runTest {
        val socket = FakeTelemetrySocket()
        val events = mutableListOf<TelemetryEvent>()
        val job = launch { source(socket, FakeClock(), FakeTicker()).status().collect { events += it } }
        runCurrent()

        socket.fail(RuntimeException("connection reset"))
        runCurrent()

        assertIs<TelemetryEvent.Offline>(events.last())
        job.cancelAndJoin()
    }

    @Test
    fun restartsTheSessionWhenTheEndpointChanges() = runTest {
        // A factory that records every endpoint it is asked to open and returns a fresh socket, so the
        // restart can be observed (the shared FakeTelemetrySocketFactory returns one reused socket).
        val endpoints = mutableListOf<Endpoint>()
        val sockets = mutableListOf<FakeTelemetrySocket>()
        val factory = object : TelemetrySocketFactory {
            override suspend fun open(endpoint: Endpoint): TelemetrySocket {
                endpoints += endpoint
                return FakeTelemetrySocket().also { sockets += it }
            }
        }
        var endpoint = Endpoint("first")
        val ticker = FakeTicker()
        val telemetry = KtorTelemetrySource(
            endpointProvider = { endpoint },
            sockets = factory,
            clock = FakeClock(),
            ticker = ticker,
        )
        val job = launch { telemetry.status().collect {} }
        runCurrent()

        assertEquals(listOf(Endpoint("first")), endpoints)
        assertEquals(subscribeLine(), sockets.single().sent.single().decodeToString())

        // Ajustes changed the gateway: the next tick must end the session and reconnect to the new host.
        endpoint = Endpoint("second")
        ticker.tick()
        runCurrent()

        assertEquals(listOf(Endpoint("first"), Endpoint("second")), endpoints)
        assertEquals(subscribeLine(), sockets.last().sent.single().decodeToString())
        job.cancelAndJoin()
    }
}
