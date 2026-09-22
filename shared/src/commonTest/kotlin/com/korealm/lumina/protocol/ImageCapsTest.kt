package com.korealm.lumina.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Tests for the image-enrollment caps (FE-INV-033, `API_CONTRACT.md` §4.10/§5.9).
 *
 * Covers the hard caps (`<= 12` images, `<= 8 MiB` decoded), the base64 math, and the 16 MiB line
 * guard.
 */
class ImageCapsTest {

    @Test
    fun anEmptyBatchIsRejected() {
        assertIs<ImageBatchValidation.Empty>(validateImageBatchSizes(count = 0, totalDecodedBytes = 0))
        assertIs<ImageBatchValidation.Empty>(validateImageBatch(emptyList()))
    }

    @Test
    fun moreThanTheMaxCountIsRejected() {
        val result = assertIs<ImageBatchValidation.TooManyImages>(
            validateImageBatchSizes(count = 13, totalDecodedBytes = 13),
        )
        assertEquals(13, result.count)
        assertEquals(MAX_ENROLL_IMAGES, result.max)
    }

    @Test
    fun exactlyTheMaxCountIsAllowed() {
        assertIs<ImageBatchValidation.Ok>(
            validateImageBatchSizes(count = MAX_ENROLL_IMAGES, totalDecodedBytes = 12),
        )
    }

    @Test
    fun aboveTheByteCapIsRejected() {
        val result = assertIs<ImageBatchValidation.TooLarge>(
            validateImageBatchSizes(count = 1, totalDecodedBytes = MAX_ENROLL_IMAGE_BYTES + 1),
        )
        assertEquals(MAX_ENROLL_IMAGE_BYTES + 1, result.totalDecodedBytes)
        assertEquals(MAX_ENROLL_IMAGE_BYTES, result.max)
    }

    @Test
    fun exactlyTheByteCapIsAllowed() {
        assertIs<ImageBatchValidation.Ok>(
            validateImageBatchSizes(count = 5, totalDecodedBytes = MAX_ENROLL_IMAGE_BYTES),
        )
    }

    @Test
    fun computesBase64Length() {
        // 4 * ceil(n / 3)
        assertEquals(0L, base64EncodedSize(0))
        assertEquals(4L, base64EncodedSize(1))
        assertEquals(4L, base64EncodedSize(3))
        assertEquals(8L, base64EncodedSize(4))
        assertEquals(8L, base64EncodedSize(6))
        assertEquals(12L, base64EncodedSize(7))
    }

    @Test
    fun theLineGuardMatchesTheSixteenMebibyteLimit() {
        // The 8 MiB decoded cap always fits the 16 MiB line; a line-sized decode does not.
        assertTrue(fitsControlLine(MAX_ENROLL_IMAGE_BYTES))
        assertTrue(fitsControlLine(0))
        assertFalse(fitsControlLine(MAX_LINE_BYTES))
    }

    @Test
    fun validatesAConcreteBatch() {
        assertIs<ImageBatchValidation.Ok>(validateImageBatch(listOf(ByteArray(100), ByteArray(200))))
    }

    @Test
    fun rejectsABatchWithNoDecodableBytes() {
        // Images were supplied but none has bytes: the device would answer "no decodable images".
        assertIs<ImageBatchValidation.Undecodable>(validateImageBatchSizes(count = 5, totalDecodedBytes = 0))
        assertIs<ImageBatchValidation.Undecodable>(validateImageBatch(listOf(ByteArray(0))))
    }

    @Test
    fun base64LengthSaturatesInsteadOfOverflowing() {
        // A wrapped value could make the line guard wrongly return true; saturation prevents that.
        assertEquals(Long.MAX_VALUE, base64EncodedSize(Long.MAX_VALUE))
        assertEquals(0L, base64EncodedSize(-1))
    }

    @Test
    fun theLineGuardRejectsAbsurdSizes() {
        assertFalse(fitsControlLine(Long.MAX_VALUE))
        assertFalse(fitsControlLine(MAX_LINE_BYTES + 1))
    }
}
