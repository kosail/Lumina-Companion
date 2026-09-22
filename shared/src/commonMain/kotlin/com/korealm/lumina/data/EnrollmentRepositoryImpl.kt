package com.korealm.lumina.data

import com.korealm.lumina.protocol.ControlResult
import com.korealm.lumina.protocol.EnrollEvent
import com.korealm.lumina.protocol.EnrollmentFailure
import com.korealm.lumina.transport.ControlClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

/**
 * Default [EnrollmentRepository]: forwards camera enrollment, and for photos prepares the batch
 * before delegating to the transport.
 *
 * Responsibility: keep the image pipeline (resize/JPEG + caps) in the data layer so the view model
 * only deals with raw picked bytes (FE-INV-033/050). Preparation runs on `Dispatchers.Default`
 * because decoding/encoding images is CPU-heavy (FE-INV-052); the transport is not entered at all
 * when preparation fails.
 *
 * Kotlin note: `withContext` returns to the flow's context before `emitAll`, because a flow may only
 * emit in its own context (emitting inside `withContext` would throw).
 *
 * @param control the TCP control client (enrollment streams + cancel).
 * @param imagePreparer the platform image resizer/encoder.
 */
class EnrollmentRepositoryImpl(
    private val control: ControlClient,
    private val imagePreparer: ImagePreparer,
) : EnrollmentRepository {

    override fun enrollFromCamera(name: String, frames: Int): Flow<EnrollEvent> =
        control.enrollFromCamera(name, frames)

    override fun enrollFromImages(name: String, rawImages: List<ByteArray>): Flow<EnrollEvent> = flow {
        val batch = withContext(Dispatchers.Default) { prepareImageBatch(imagePreparer, rawImages) }
        when (batch) {
            is PreparedImageBatch.Ok -> emitAll(control.enrollFromImages(name, batch.images))
            PreparedImageBatch.NotPrepared ->
                emit(EnrollEvent.Failure(EnrollmentFailure.ImagePreparation))

            is PreparedImageBatch.Rejected ->
                emit(EnrollEvent.Failure(EnrollmentFailure.BadRequest))
        }
    }

    override suspend fun cancel(): ControlResult<Unit> = control.cancelEnrollment()
}
