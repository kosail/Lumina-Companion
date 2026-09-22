package com.korealm.lumina.data

import com.korealm.lumina.protocol.ControlResult
import com.korealm.lumina.protocol.EnrollEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Test double for [EnrollmentRepository].
 *
 * Responsibility: let the people view model drive an enrollment without sockets (FE-INV-050). Each
 * enrollment method returns the stream registered with [onCamera]/[onImages] and records the call.
 */
class FakeEnrollmentRepository : EnrollmentRepository {

    /** How many times [enrollFromCamera] was called. */
    var cameraCalls = 0
        private set

    /** How many times [enrollFromImages] was called. */
    var imagesCalls = 0
        private set

    /** How many times [cancel] was called. */
    var cancelCalls = 0
        private set

    /** Names passed to the enrollment methods, in order. */
    val names = mutableListOf<String>()

    /** Raw image batches passed to [enrollFromImages], in order. */
    val imageBatches = mutableListOf<List<ByteArray>>()

    /** Result returned by [cancel] (settable per test). */
    var cancelResult: ControlResult<Unit> = ControlResult.Ok(Unit)

    private var cameraStream: Flow<EnrollEvent> = emptyFlow()
    private var imagesStream: Flow<EnrollEvent> = emptyFlow()

    /** Sets the stream returned by the next [enrollFromCamera] call. */
    fun onCamera(stream: Flow<EnrollEvent>) {
        cameraStream = stream
    }

    /** Sets the stream returned by the next [enrollFromImages] call. */
    fun onImages(stream: Flow<EnrollEvent>) {
        imagesStream = stream
    }

    override fun enrollFromCamera(name: String, frames: Int): Flow<EnrollEvent> {
        cameraCalls += 1
        names += name
        return cameraStream
    }

    override fun enrollFromImages(name: String, rawImages: List<ByteArray>): Flow<EnrollEvent> {
        imagesCalls += 1
        names += name
        imageBatches += rawImages
        return imagesStream
    }

    override suspend fun cancel(): ControlResult<Unit> {
        cancelCalls += 1
        return cancelResult
    }
}
