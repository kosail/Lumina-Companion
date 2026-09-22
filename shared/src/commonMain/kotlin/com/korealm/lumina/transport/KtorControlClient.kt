package com.korealm.lumina.transport

import com.korealm.lumina.protocol.ControlResult
import com.korealm.lumina.protocol.EnrollEvent
import com.korealm.lumina.protocol.EnrollmentFailure
import com.korealm.lumina.protocol.ErrorCode
import com.korealm.lumina.protocol.Reply
import com.korealm.lumina.protocol.ReplyDecodeResult
import com.korealm.lumina.protocol.RuntimeState
import com.korealm.lumina.protocol.VolumeState
import com.korealm.lumina.protocol.decodeReply
import com.korealm.lumina.protocol.enrollCameraCancelLine
import com.korealm.lumina.protocol.enrollCameraStartLine
import com.korealm.lumina.protocol.enrollImagesLine
import com.korealm.lumina.protocol.peopleListLine
import com.korealm.lumina.protocol.runtimeStartLine
import com.korealm.lumina.protocol.runtimeStateLine
import com.korealm.lumina.protocol.runtimeStopLine
import com.korealm.lumina.protocol.toControlResult
import com.korealm.lumina.protocol.volumeGetLine
import com.korealm.lumina.protocol.volumeMuteLine
import com.korealm.lumina.protocol.volumeSetLine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.io.encoding.Base64

/** Default per-request timeout for a control command. */
const val DEFAULT_CONTROL_TIMEOUT_MS: Long = 10_000L

/**
 * Maximum wait for one line during an enrollment. An enrollment is long-lived (up to ~60 s) so the
 * normal request timeout does not apply, but a completely stalled stream must not hang the UI
 * forever; every progress line resets this watchdog.
 */
const val ENROLL_READ_TIMEOUT_MS: Long = 30_000L

/**
 * Production [ControlClient] over the injected [ControlConnectionFactory].
 *
 * Responsibility: implement the "connection per request" pattern (PLAN §5.1) — open, write one
 * token-gated line built by `protocol/CommandBuilders`, read one line, decode it with
 * `protocol/decodeReply`, map it to a [ControlResult], close. Enrollment commands stream on a
 * dedicated, long-lived connection (contract §5.8/§6.3). Expected failures are values, never
 * thrown (AGENTS §7).
 *
 * There is no request `id`; replies are correlated by order on a connection (contract §3.2).
 *
 * All socket work runs on [ioDispatcher] (FE-INV-052): commands are launched from `viewModelScope`,
 * which is Android's main thread, and resolving the gateway / connecting there throws
 * `NetworkOnMainThreadException` (CHG-FE-0027).
 *
 * @param endpointProvider supplies the current gateway at call time.
 * @param tokenProvider supplies the shared token; never logged or stored here (FE-INV-053).
 * @param connections opens the short-lived TCP connection.
 * @param requestTimeoutMs maximum time to wait for the reply before returning `ControlResult.Io`.
 */
class KtorControlClient(
    private val endpointProvider: () -> Endpoint,
    private val tokenProvider: () -> String,
    private val connections: ControlConnectionFactory,
    private val requestTimeoutMs: Long = DEFAULT_CONTROL_TIMEOUT_MS,
) : ControlClient {

    override suspend fun volumeGet(): ControlResult<VolumeState> =
        request({ volumeGetLine(tokenProvider()) }, Reply::toVolumeResult)

    override suspend fun volumeSet(percent: Int): ControlResult<VolumeState> =
        request({ volumeSetLine(tokenProvider(), percent) }, Reply::toVolumeResult)

    override suspend fun setMuted(muted: Boolean): ControlResult<VolumeState> =
        request({ volumeMuteLine(tokenProvider(), muted) }, Reply::toVolumeResult)

    override suspend fun people(): ControlResult<List<String>> =
        request({ peopleListLine(tokenProvider()) }) { reply ->
            when (reply) {
                is Reply.People -> ControlResult.Ok(reply.names)
                is Reply.Error -> reply.toControlResult()
                else -> ControlResult.Io("unexpected reply: $reply")
            }
        }

    override suspend fun runtimeState(): ControlResult<RuntimeState> =
        request({ runtimeStateLine(tokenProvider()) }, Reply::toRuntimeResult)

    override suspend fun runtimeStart(): ControlResult<RuntimeState> =
        request({ runtimeStartLine(tokenProvider()) }, Reply::toRuntimeResult)

    override suspend fun runtimeStop(): ControlResult<RuntimeState> =
        request({ runtimeStopLine(tokenProvider()) }, Reply::toRuntimeResult)

    override fun enrollFromCamera(name: String, frames: Int): Flow<EnrollEvent> =
        enrollmentStream { enrollCameraStartLine(tokenProvider(), name, frames) }

    override fun enrollFromImages(name: String, images: List<ByteArray>): Flow<EnrollEvent> =
        enrollmentStream {
            // The wire carries raw base64 (no `data:` prefix, contract §4.10). `Base64.Default` is
            // the RFC 4648 alphabet the device expects (Kotlin stdlib, stable since 2.2).
            enrollImagesLine(tokenProvider(), name, images.map { Base64.Default.encode(it) })
        }

    override suspend fun cancelEnrollment(): ControlResult<Unit> {
        val endpoint = endpointProvider()
        val connection = try {
            // See [request]: connect off the caller's (Android main) thread (FE-INV-052, CHG-FE-0027).
            withContext(ioDispatcher) { connections.open(endpoint) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            return ControlResult.Io("connect failed: ${e.message}")
        }
        return try {
            withTimeoutOrNull(requestTimeoutMs) {
                connection.writeLine(enrollCameraCancelLine(tokenProvider()))
                val line = connection.readLine()
                    ?: return@withTimeoutOrNull ControlResult.Io("connection closed before a reply")
                when (val decoded = decodeReply(line)) {
                    is ReplyDecodeResult.Malformed -> ControlResult.Io(decoded.message)
                    is ReplyDecodeResult.Decoded -> when (val reply = decoded.reply) {
                        // The cancelling connection receives `enroll.cancelled`; the enrolling
                        // connection ends separately with `enroll.error` (contract §6.3).
                        is Reply.Enroll -> if (reply.event is EnrollEvent.Cancelled) {
                            ControlResult.Ok(Unit)
                        } else {
                            ControlResult.Io("unexpected enrollment reply: $reply")
                        }

                        is Reply.Error -> reply.toControlResult()
                        else -> ControlResult.Io("unexpected reply: $reply")
                    }
                }
            } ?: ControlResult.Io("timed out after ${requestTimeoutMs}ms")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            ControlResult.Io("i/o error: ${e.message}")
        } finally {
            connection.close()
        }
    }

    /**
     * Runs one streaming enrollment on a dedicated connection.
     *
     * Unlike [request], the connection is long-lived and a per-line [ENROLL_READ_TIMEOUT_MS] watchdog
     * (not an overall timeout) protects against a silently stalled stream. Each `enroll.*` line is
     * emitted as it arrives; the flow completes at the first terminal event or maps any
     * build/write/`error`/malformed/EOF failure to [EnrollEvent.Failure] — it **never throws**
     * (FE-INV-052 / AGENTS §7). Cancelling the collector closes the connection, but the enrollment
     * keeps running on the device until the UI sends [cancelEnrollment] (contract §6.3).
     */
    private fun enrollmentStream(buildLine: () -> String): Flow<EnrollEvent> = flow {
        // Build the request before opening a socket, so an encoding failure cannot leak a connection.
        val requestLine = try {
            buildLine()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            emit(EnrollEvent.Failure(EnrollmentFailure.Io))
            return@flow
        }

        val endpoint = endpointProvider()
        val connection = try {
            // Connect off the collector's thread (Android's main thread when collected from
            // viewModelScope) so address resolution cannot throw NetworkOnMainThreadException
            // (FE-INV-052, CHG-FE-0027). The channel reads below dispatch to the socket's own I/O
            // context, so the per-line watchdog keeps running on the collector's context.
            withContext(ioDispatcher) { connections.open(endpoint) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            emit(EnrollEvent.Failure(EnrollmentFailure.Io))
            return@flow
        }
        try {
            try {
                connection.writeLine(requestLine)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                emit(EnrollEvent.Failure(EnrollmentFailure.Io))
                return@flow
            }
            while (true) {
                val line = try {
                    withTimeoutOrNull(ENROLL_READ_TIMEOUT_MS) { connection.readLine() }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    emit(EnrollEvent.Failure(EnrollmentFailure.Io))
                    return@flow
                }
                if (line == null) {
                    // null means the read watcher timed out or the device closed the stream; a
                    // collector cancellation surfaces as CancellationException above instead.
                    emit(EnrollEvent.Failure(EnrollmentFailure.Io))
                    return@flow
                }
                when (val decoded = decodeReply(line)) {
                    is ReplyDecodeResult.Malformed -> {
                        emit(EnrollEvent.Failure(EnrollmentFailure.Io))
                        return@flow
                    }

                    is ReplyDecodeResult.Decoded -> when (val reply = decoded.reply) {
                        is Reply.Enroll -> {
                            emit(reply.event)
                            if (reply.event !is EnrollEvent.Progress) return@flow
                        }

                        // Contract §10: ignore a message type this client does not recognize and
                        // keep reading, so a newer device's informational line cannot abort the
                        // enrollment with a false I/O failure.
                        is Reply.Unknown -> Unit

                        is Reply.Error -> {
                            emit(EnrollEvent.Failure(reply.code.toEnrollmentFailure()))
                            return@flow
                        }

                        else -> {
                            emit(EnrollEvent.Failure(EnrollmentFailure.Io))
                            return@flow
                        }
                    }
                }
            }
        } finally {
            connection.close()
        }
    }

    /**
     * Runs one request/reply exchange on a fresh connection.
     *
     * `withTimeoutOrNull` (not `withTimeout`) is used so a timeout becomes the value
     * `ControlResult.Io("timed out ...")` instead of a `TimeoutCancellationException`, which would be
     * indistinguishable from an external cancellation.
     */
    private suspend fun <T> request(
        buildLine: () -> String,
        map: (Reply) -> ControlResult<T>,
    ): ControlResult<T> {
        val endpoint = endpointProvider()
        val connection = try {
            // Resolve the gateway and connect off the caller's thread: a command launched from
            // viewModelScope runs on Android's main thread, where address resolution throws
            // NetworkOnMainThreadException (FE-INV-052, CHG-FE-0027).
            withContext(ioDispatcher) { connections.open(endpoint) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            return ControlResult.Io("connect failed: ${e.message}")
        }
        return try {
            withTimeoutOrNull(requestTimeoutMs) {
                connection.writeLine(buildLine())
                val line = connection.readLine()
                    ?: return@withTimeoutOrNull ControlResult.Io("connection closed before a reply")
                when (val decoded = decodeReply(line)) {
                    is ReplyDecodeResult.Malformed -> ControlResult.Io(decoded.message)
                    is ReplyDecodeResult.Decoded -> map(decoded.reply)
                }
            } ?: ControlResult.Io("timed out after ${requestTimeoutMs}ms")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            ControlResult.Io("i/o error: ${e.message}")
        } finally {
            connection.close()
        }
    }
}

/** Maps a reply expected to be `volume.state`. */
private fun Reply.toVolumeResult(): ControlResult<VolumeState> = when (this) {
    is Reply.Volume -> ControlResult.Ok(state)
    is Reply.Error -> toControlResult()
    else -> ControlResult.Io("unexpected reply: $this")
}

/** Maps a reply expected to be `runtime.state`. */
private fun Reply.toRuntimeResult(): ControlResult<RuntimeState> = when (this) {
    is Reply.Runtime -> ControlResult.Ok(state)
    is Reply.Error -> toControlResult()
    else -> ControlResult.Io("unexpected reply: $this")
}

/**
 * Maps an `error.code` to the enrollment-failure kind, so an enrollment rejection is rendered with
 * the same Spanish copy as a control failure. An unknown code is treated as a generic device
 * failure, mirroring [com.korealm.lumina.protocol.toControlResult].
 */
private fun ErrorCode.toEnrollmentFailure(): EnrollmentFailure = when (this) {
    ErrorCode.Unauthorized -> EnrollmentFailure.Unauthorized
    ErrorCode.Busy -> EnrollmentFailure.Busy
    ErrorCode.BadRequest -> EnrollmentFailure.BadRequest
    ErrorCode.Internal -> EnrollmentFailure.Internal
    ErrorCode.Unknown -> EnrollmentFailure.Internal
}
