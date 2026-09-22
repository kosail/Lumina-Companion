package com.korealm.lumina.data

import com.korealm.lumina.protocol.ControlResult
import com.korealm.lumina.protocol.ENROLL_FRAMES_DEFAULT
import com.korealm.lumina.protocol.EnrollEvent
import kotlinx.coroutines.flow.Flow

/**
 * The enrollment data entry point (camera and photos).
 *
 * Responsibility: give the people view model the enrollment streams without exposing the transport,
 * and run the image pipeline (prepare → validate) before an `enroll.images` request (FE-INV-033/
 * 050). Expected failures stay values inside the [Flow] ([EnrollEvent.Failure]), never thrown.
 */
interface EnrollmentRepository {

    /**
     * Camera enrollment stream (contract §5.8): progress then one terminal event. Cancel it via
     * [cancel] on a second connection while this flow is active.
     *
     * @param frames requested frames; clamped to `1..10` by the transport.
     */
    fun enrollFromCamera(name: String, frames: Int = ENROLL_FRAMES_DEFAULT): Flow<EnrollEvent>

    /**
     * Photo enrollment stream (contract §5.9). Each raw image is resized/JPEG-encoded by the
     * `ImagePreparer` and the batch validated against the caps before anything is sent; a preparation
     * failure becomes [EnrollEvent.Failure] rather than a bad request.
     */
    fun enrollFromImages(name: String, rawImages: List<ByteArray>): Flow<EnrollEvent>

    /**
     * `enroll.camera.cancel` (contract §5.8/§6.3): sent on a **second** connection. Returns
     * `Ok(Unit)` on the `enroll.cancelled` acknowledgement.
     */
    suspend fun cancel(): ControlResult<Unit>
}
