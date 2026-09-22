package com.korealm.lumina.data

import com.korealm.lumina.protocol.ControlResult
import com.korealm.lumina.protocol.RuntimeState
import com.korealm.lumina.protocol.VolumeState
import com.korealm.lumina.transport.TelemetryEvent
import kotlinx.coroutines.flow.Flow

/**
 * The app's view of the paired Lúmina device.
 *
 * Responsibility: be the single `data/` entry point for device state and device commands, so the UI
 * depends on `data/` and never on `transport/` directly (FE-INV-050, PLAN §4). It exposes the
 * telemetry stream and the token-gated control commands; expected failures stay values
 * ([ControlResult]), never thrown (AGENTS §7).
 *
 * Scope: request/response control only. Enrollment (camera/images) lives on
 * `EnrollmentRepository` and the people screen wiring on `PeopleRepository` (Phase 4).
 *
 * Threading: [status] is a shared flow (one UDP session for the whole app, see
 * [DeviceRepositoryImpl]); the command functions are `suspend` and must be called from a background
 * dispatcher (they open sockets).
 */
interface DeviceRepository {

    /** Live telemetry: `Online(DeviceStatus)` / `Offline` / `Incompatible` (5 s liveness owned by the transport). */
    fun status(): Flow<TelemetryEvent>

    /** `volume.get` (contract §5.1). */
    suspend fun volumeGet(): ControlResult<VolumeState>

    /** `volume.set` with a `0..100` percent (contract §5.2). */
    suspend fun volumeSet(percent: Int): ControlResult<VolumeState>

    /** `volume.mute` (contract §5.3). */
    suspend fun setMuted(muted: Boolean): ControlResult<VolumeState>

    /**
     * `people.list` (contract §5.4). Unlike the telemetry `people` array, this may return the
     * last-known names even when the runtime is down; used for an explicit refresh after enrolling.
     */
    suspend fun requestPeople(): ControlResult<List<String>>

    /** `runtime.start` — the recovery action (contract §5.6). */
    suspend fun runtimeStart(): ControlResult<RuntimeState>

    /** `runtime.stop` (contract §5.7). */
    suspend fun runtimeStop(): ControlResult<RuntimeState>
}
