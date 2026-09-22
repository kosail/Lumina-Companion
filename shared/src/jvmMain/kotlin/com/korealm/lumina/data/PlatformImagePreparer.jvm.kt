package com.korealm.lumina.data

import com.korealm.lumina.protocol.RECOMMENDED_MAX_IMAGE_HEIGHT_PX
import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

/**
 * Desktop (JVM) [ImagePreparer]: `ImageIO` decode + downscale + JPEG encode.
 *
 * Kotlin note: `actual` provides the platform implementation of the common `expect` factory; this is
 * only used by the desktop dev loop, but it keeps photo enrollment testable without a device.
 */
actual fun createImagePreparer(): ImagePreparer = JvmImagePreparer

private object JvmImagePreparer : ImagePreparer {

    override fun prepare(raw: ByteArray): ByteArray? {
        val source = try {
            ImageIO.read(ByteArrayInputStream(raw))
        } catch (e: Exception) {
            // A corrupt/unsupported picked file must become a value, not a crash (AGENTS §7).
            null
        } ?: return null

        val target = toRgb(scaleDown(source, RECOMMENDED_MAX_IMAGE_HEIGHT_PX))
        return ByteArrayOutputStream().use { out ->
            if (!ImageIO.write(target, "jpg", out)) null else out.toByteArray()
        }
    }
}

/** Scales [image] down so its height is at most [maxHeight], preserving the aspect ratio. */
private fun scaleDown(image: BufferedImage, maxHeight: Int): BufferedImage {
    if (image.height <= maxHeight) return image
    val width = (image.width.toLong() * maxHeight / image.height).toInt().coerceAtLeast(1)
    val scaled = BufferedImage(width, maxHeight, BufferedImage.TYPE_INT_RGB)
    val graphics = scaled.createGraphics()
    try {
        graphics.setRenderingHint(
            RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BILINEAR,
        )
        graphics.drawImage(image, 0, 0, width, maxHeight, null)
    } finally {
        graphics.dispose()
    }
    return scaled
}

/**
 * Normalizes [image] to `TYPE_INT_RGB`. JPEG has no alpha channel, so drawing onto an RGB surface
 * with a white background avoids the corrupt-colour output `ImageIO` produces for some ARGB images.
 */
private fun toRgb(image: BufferedImage): BufferedImage {
    if (image.type == BufferedImage.TYPE_INT_RGB) return image
    val rgb = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_RGB)
    val graphics = rgb.createGraphics()
    try {
        graphics.drawImage(image, 0, 0, Color.WHITE, null)
    } finally {
        graphics.dispose()
    }
    return rgb
}
