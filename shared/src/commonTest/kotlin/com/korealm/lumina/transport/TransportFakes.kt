package com.korealm.lumina.transport

import com.korealm.lumina.protocol.ControlResult
import com.korealm.lumina.protocol.EnrollEvent
import com.korealm.lumina.protocol.RuntimeState
import com.korealm.lumina.protocol.SinkState
import com.korealm.lumina.protocol.VolumeState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Test doubles for the transport seams.
 *
 * Responsibility: let the transport be tested with no sockets, no clock and no real time
 * (FE-INV-050). These are hand-written fakes (not mocks) so tests read as plain code.
 */

/** An in-memory [TelemetrySocket]: records sent payloads and delivers scripted datagrams. */
class FakeTelemetrySocket : TelemetrySocket {
    val sent = mutableListOf<ByteArray>()
    private val incoming = Channel<ByteArray>(Channel.UNLIMITED)
    var closed = false

    override suspend fun send(payload: ByteArray) {
        sent += payload
    }

    override suspend fun receive(): ByteArray = incoming.receive()

    override fun close() {
        closed = true
        incoming.close()
    }

    /** Pushes one datagram to the next [receive] call. */
    fun deliver(bytes: ByteArray) {
        incoming.trySend(bytes)
    }

    /** Makes [receive] throw [cause], simulating a socket failure. */
    fun fail(cause: Throwable) {
        incoming.close(cause)
    }
}

/** A [TelemetrySocketFactory] returning a single pre-built [FakeTelemetrySocket]. */
class FakeTelemetrySocketFactory(private val socket: TelemetrySocket) : TelemetrySocketFactory {
    var opened = 0
    override suspend fun open(endpoint: Endpoint): TelemetrySocket {
        opened += 1
        return socket
    }
}

/** A [ControlConnection] that records written lines and returns a fixed reply. */
class FakeControlConnection(private val reply: String?) : ControlConnection {
    val written = mutableListOf<String>()
    var closed = false

    override suspend fun writeLine(line: String) {
        // Mirrors KtorControlConnection: per the ControlConnection contract, writeLine owns the
        // "\n" framing, so a faithful double records the resulting wire bytes. Keeping this here
        // (not appending in KtorControlClient) avoids a double newline on the real socket.
        written += line + "\n"
    }

    override suspend fun readLine(): String? = reply

    override fun close() {
        closed = true
    }
}

/** A [ControlConnectionFactory] returning a single pre-built [FakeControlConnection]. */
class FakeControlConnectionFactory(private val connection: ControlConnection) : ControlConnectionFactory {
    override suspend fun open(endpoint: Endpoint): ControlConnection = connection
}

/** A [Clock] whose value the test sets directly. */
class FakeClock(var now: Long = 0L) : Clock {
    override fun nowMs(): Long = now
}

/** A [Ticker] driven manually by [tick] instead of by time. */
class FakeTicker : Ticker {
    private val ticks = MutableSharedFlow<Unit>(extraBufferCapacity = 64)
    override fun ticks(periodMs: Long): Flow<Unit> = ticks
    fun tick() {
        ticks.tryEmit(Unit)
    }
}

/** A [TelemetrySource] backed by a test-controlled flow. */
class FakeTelemetrySource(private val events: Flow<TelemetryEvent>) : TelemetrySource {
    override fun status(): Flow<TelemetryEvent> = events
}

/**
 * A [ControlConnection] that returns a scripted sequence of lines, one per [readLine] call, then
 * `null` (clean EOF). Used for the streaming enrollment tests, which need several replies.
 */
class ScriptedControlConnection(private val lines: List<String?>) : ControlConnection {
    val written = mutableListOf<String>()
    var closed = false
    private var index = 0

    override suspend fun writeLine(line: String) {
        written += line + "\n"
    }

    override suspend fun readLine(): String? = if (index < lines.size) lines[index++] else null

    override fun close() {
        closed = true
    }
}

/** A [ControlConnectionFactory] that returns a different connection on each [open] (in order). */
class SequentialControlConnectionFactory(
    private val connections: List<ControlConnection>,
) : ControlConnectionFactory {
    var opened = 0
        private set

    override suspend fun open(endpoint: Endpoint): ControlConnection = connections[opened++]
}

/**
 * A [ControlConnection] whose [writeLine] always throws, to verify the streaming enrollment maps a
 * write failure to a value instead of throwing (CHG-FE-0017).
 */
class ThrowingWriteControlConnection(private val error: Throwable) : ControlConnection {
    var closed = false
        private set

    override suspend fun writeLine(line: String) {
        throw error
    }

    override suspend fun readLine(): String? = null

    override fun close() {
        closed = true
    }
}

/**
 * Test double for [ControlClient] where every command returns a settable value.
 *
 * Responsibility: let tests that only need a collaborator (e.g. `DeviceRepositoryImpl`) avoid a
 * socket without hand-implementing every method at each call site.
 */
class FakeControlClient : ControlClient {

    var volumeResult: ControlResult<VolumeState> =
        ControlResult.Ok(VolumeState(percent = null, muted = false, ok = true))

    var peopleResult: ControlResult<List<String>> = ControlResult.Ok(emptyList())

    var runtimeResult: ControlResult<RuntimeState> =
        ControlResult.Ok(RuntimeState(running = true, sink = SinkState.Ready))

    var cancelResult: ControlResult<Unit> = ControlResult.Ok(Unit)

    var cameraStream: Flow<EnrollEvent> = emptyFlow()

    var imagesStream: Flow<EnrollEvent> = emptyFlow()

    override suspend fun volumeGet(): ControlResult<VolumeState> = volumeResult

    override suspend fun volumeSet(percent: Int): ControlResult<VolumeState> = volumeResult

    override suspend fun setMuted(muted: Boolean): ControlResult<VolumeState> = volumeResult

    override suspend fun people(): ControlResult<List<String>> = peopleResult

    override suspend fun runtimeState(): ControlResult<RuntimeState> = runtimeResult

    override suspend fun runtimeStart(): ControlResult<RuntimeState> = runtimeResult

    override suspend fun runtimeStop(): ControlResult<RuntimeState> = runtimeResult

    override fun enrollFromCamera(name: String, frames: Int): Flow<EnrollEvent> = cameraStream

    override fun enrollFromImages(name: String, images: List<ByteArray>): Flow<EnrollEvent> = imagesStream

    override suspend fun cancelEnrollment(): ControlResult<Unit> = cancelResult
}
