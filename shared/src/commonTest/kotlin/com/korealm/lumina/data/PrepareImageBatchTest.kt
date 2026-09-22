package com.korealm.lumina.data

import com.korealm.lumina.protocol.MAX_ENROLL_IMAGES
import com.korealm.lumina.protocol.MAX_ENROLL_IMAGE_BYTES
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Pure tests for [prepareImageBatch] (FE-INV-033): empty batches, an undecodable image, and the
 * count/size caps, all with a fake [ImagePreparer] so no real decoding happens.
 */
class PrepareImageBatchTest {

    private val identity = ImagePreparer { it }

    @Test
    fun emptyBatchIsRejected() {
        assertIs<PreparedImageBatch.Rejected>(prepareImageBatch(identity, emptyList()))
    }

    @Test
    fun anUndecodableImageIsNotPrepared() {
        val onlyEvenLengths = ImagePreparer { bytes -> if (bytes.size % 2 == 0) bytes else null }

        val result = prepareImageBatch(
            onlyEvenLengths,
            listOf(byteArrayOf(1, 2), byteArrayOf(3)),
        )

        assertEquals(PreparedImageBatch.NotPrepared, result)
    }

    @Test
    fun aThrowingPreparerIsNotPreparedNotThrown() {
        val throwing = ImagePreparer { throw IllegalStateException("decoder blew up") }

        val result = prepareImageBatch(throwing, listOf(byteArrayOf(1)))

        assertEquals(PreparedImageBatch.NotPrepared, result)
    }

    @Test
    fun okBatchKeepsThePreparedBytesInOrder() {
        val marker = ImagePreparer { bytes -> bytes + 9 }

        val result = prepareImageBatch(marker, listOf(byteArrayOf(1), byteArrayOf(2)))

        val ok = assertIs<PreparedImageBatch.Ok>(result)
        assertEquals(listOf<Byte>(1, 9), ok.images[0].toList())
        assertEquals(listOf<Byte>(2, 9), ok.images[1].toList())
    }

    @Test
    fun tooManyImagesIsRejected() {
        val many = List(MAX_ENROLL_IMAGES + 1) { byteArrayOf(1) }

        val result = prepareImageBatch(identity, many)

        assertIs<PreparedImageBatch.Rejected>(result)
    }

    @Test
    fun tooManyBytesIsRejected() {
        val huge = byteArrayOf(0).copyOf((MAX_ENROLL_IMAGE_BYTES + 1).toInt())

        val result = prepareImageBatch(identity, listOf(huge))

        assertIs<PreparedImageBatch.Rejected>(result)
    }
}
