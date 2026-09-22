package com.korealm.lumina.ui.picker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * Desktop [rememberPhotoPicker]: a Swing multi-file chooser for the dev loop.
 *
 * The system Photo Picker is Android-only, so desktop (where enrollment is exercised against the
 * mock agent) uses `JFileChooser` and reads the files directly. `showOpenDialog` is modal and blocks
 * the calling thread, which is acceptable for a developer-only path.
 */
@Composable
actual fun rememberPhotoPicker(onPicked: (List<ByteArray>) -> Unit): () -> Unit =
    remember(onPicked) {
        {
            val chooser = JFileChooser().apply {
                isMultiSelectionEnabled = true
                fileFilter = FileNameExtensionFilter("Imágenes (jpg, png)", "jpg", "jpeg", "png")
            }
            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                val images = chooser.selectedFiles.mapNotNull { file ->
                    runCatching { file.readBytes() }.getOrNull()
                }
                onPicked(images)
            }
        }
    }
