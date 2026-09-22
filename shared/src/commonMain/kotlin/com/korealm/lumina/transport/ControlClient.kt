package com.korealm.lumina.transport

import com.korealm.lumina.protocol.ControlResult
import com.korealm.lumina.protocol.EnrollEvent
import com.korealm.lumina.protocol.ENROLL_FRAMES_DEFAULT
import com.korealm.lumina.protocol.RuntimeState
import com.korealm.lumina.protocol.VolumeState
import kotlinx.coroutines.flow.Flow

/**
 * Token-gated control channel (TCP `47601`).
 *
 * Responsibility: expose each contract command as a typed function so callers never build JSON,
 * manage sockets, or interpret wire replies (FE-INV-022/030/050). Request/response commands return a
 * [ControlResult] value and streaming enrollment commands return a cold `Flow<EnrollEvent>`; expected
 * failures are never thrown (AGENTS §7).
 *
 * Enrollment (contract §5.8/§6.3): `enroll.camera.start` **blocks its own connection** until the
 * enrollment ends, so it is modelled as a cold [Flow]. [cancelEnrollment] must be sent on a **second**
 * connection while the first is streaming.
 *
 * Threading: request/response methods are `suspend`; enrollment flows do their work when collected.
 * All socket work must be safe on `Dispatchers.IO`. Cancellation closes the connection (FE-INV-052).
 */
interface ControlClient {

    /** `volume.get` (contract §5.1). `percent` is `null` when the mixer is unknown. */
    suspend fun volumeGet(): ControlResult<VolumeState>

    /** `volume.set` with a `0..100` percent (contract §5.2). */
    suspend fun volumeSet(percent: Int): ControlResult<VolumeState>

    /** `volume.mute` (contract §5.3). */
    suspend fun setMuted(muted: Boolean): ControlResult<VolumeState>

    /** `people.list` (contract §5.4); may return last-known names when the runtime is down. */
    suspend fun people(): ControlResult<List<String>>

    /** `runtime.state` (contract §5.5). */
    suspend fun runtimeState(): ControlResult<RuntimeState>

    /** `runtime.start` — recovery action (contract §5.6). */
    suspend fun runtimeStart(): ControlResult<RuntimeState>

    /** `runtime.stop` (contract §5.7). */
    suspend fun runtimeStop(): ControlResult<RuntimeState>

    /**
     * `enroll.camera.start` (contract §5.8): streams zero or more [EnrollEvent.Progress] and ends
     * with exactly one terminal event ([EnrollEvent.Done]/[EnrollEvent.Error]/
     * [EnrollEvent.Failure]). This call holds its connection until the stream ends (~60 s worst
     * case); cancel it from a second connection via [cancelEnrollment].
     *
     * @param name the person's name; non-empty (the device rejects an empty name).
     * @param frames requested frames, clamped to `1..10` (contract §2).
     */
    fun enrollFromCamera(name: String, frames: Int = ENROLL_FRAMES_DEFAULT): Flow<EnrollEvent>

    /**
     * `enroll.images` (contract §5.9): sends one line with the images base64-encoded and streams the
     * resulting `enroll.progress`/`enroll.done`/`enroll.error`.
     *
     * @param images **prepared** JPEG bytes (already resized/encoded; see `ImagePreparer`). Caps are
     *   validated by the caller before this is invoked.
     */
    fun enrollFromImages(name: String, images: List<ByteArray>): Flow<EnrollEvent>

    /**
     * `enroll.camera.cancel` (contract §5.8): must use a **second** connection while the enrollment
     * connection is blocked. Returns `Ok(Unit)` on the `enroll.cancelled` acknowledgement.
     */
    suspend fun cancelEnrollment(): ControlResult<Unit>
}
