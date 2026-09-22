package com.korealm.lumina.data

import com.korealm.lumina.protocol.ControlResult
import com.korealm.lumina.protocol.RuntimeState
import com.korealm.lumina.protocol.SinkState
import com.korealm.lumina.protocol.VolumeState
import com.korealm.lumina.transport.TelemetryEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Test double for [DeviceRepository].
 *
 * Responsibility: let the view models be tested with no sockets, no clock and no device
 * (FE-INV-050). The telemetry stream is whatever flow the test passes; each command returns a
 * scripted [ControlResult] and records its arguments.
 */
class FakeDeviceRepository(
    private val events: Flow<TelemetryEvent> = flowOf(),
) : DeviceRepository {

    /** Percents passed to [volumeSet], in order. */
    val volumeSetCalls = mutableListOf<Int>()

    /** Values passed to [setMuted], in order. */
    val muteCalls = mutableListOf<Boolean>()

    /** How many times [runtimeStart] was called. */
    var runtimeStartCalls = 0
        private set

    /** How many times [runtimeStop] was called. */
    var runtimeStopCalls = 0
        private set

    /** How many times [requestPeople] was called. */
    var requestPeopleCalls = 0
        private set

    /** Result returned by [requestPeople] (settable per test). */
    var peopleResult: ControlResult<List<String>> = ControlResult.Ok(emptyList())

    /** Result returned by the volume commands (settable per test). */
    var volumeResult: ControlResult<VolumeState> =
        ControlResult.Ok(VolumeState(percent = 50, muted = false, ok = true))

    /** Result returned by [setMuted] (settable per test). */
    var muteResult: ControlResult<VolumeState> =
        ControlResult.Ok(VolumeState(percent = 50, muted = true, ok = true))

    /** Result returned by the runtime commands (settable per test). */
    var runtimeResult: ControlResult<RuntimeState> =
        ControlResult.Ok(RuntimeState(running = true, sink = SinkState.Ready))

    override fun status(): Flow<TelemetryEvent> = events

    override suspend fun volumeGet(): ControlResult<VolumeState> = volumeResult

    override suspend fun volumeSet(percent: Int): ControlResult<VolumeState> {
        volumeSetCalls += percent
        return volumeResult
    }

    override suspend fun setMuted(muted: Boolean): ControlResult<VolumeState> {
        muteCalls += muted
        return muteResult
    }

    override suspend fun requestPeople(): ControlResult<List<String>> {
        requestPeopleCalls += 1
        return peopleResult
    }

    override suspend fun runtimeStart(): ControlResult<RuntimeState> {
        runtimeStartCalls += 1
        return runtimeResult
    }

    override suspend fun runtimeStop(): ControlResult<RuntimeState> {
        runtimeStopCalls += 1
        return runtimeResult
    }
}
