package com.korealm.lumina.data

import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for the desktop [ImagePreparer] (actual `createImagePreparer`): it must produce JPEG bytes
 * no taller than 1080 px and return `null` for undecodable input (FE-INV-033).
 */
class JvmImagePreparerTest {

    private val preparer = createImagePreparer()

    @Test
    fun scalesDownTo1080AndEncodesJpeg() {
        val png = png(2000, 3000)

        val prepared = assertNotNull(preparer.prepare(png))

        // JPEG SOI marker 0xFFD8.
        assertEquals(0xFF, prepared[0].toInt() and 0xFF)
        assertEquals(0xD8, prepared[1].toInt() and 0xFF)
        val decoded = ImageIO.read(ByteArrayInputStream(prepared))
        assertEquals(1080, decoded.height)
        assertTrue(decoded.width < 2000)
    }

    @Test
    fun aSmallImageKeepsItsDimensions() {
        val png = png(320, 240)

        val decoded = ImageIO.read(ByteArrayInputStream(assertNotNull(preparer.prepare(png))))

        assertEquals(320, decoded.width)
        assertEquals(240, decoded.height)
    }

    @Test
    fun undecodableInputReturnsNull() {
        assertNull(preparer.prepare(byteArrayOf(1, 2, 3)))
    }

    private fun png(width: Int, height: Int): ByteArray {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        return ByteArrayOutputStream().use { out ->
            ImageIO.write(image, "png", out)
            out.toByteArray()
        }
    }
}
