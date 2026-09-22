package com.korealm.lumina.ui.picker

import androidx.compose.runtime.Composable

/**
 * Maximum number of photos requested per enrollment.
 *
 * The contract recommends 3–5 photos and hard-caps at 12 (contract §4.10); asking for 5 keeps a
 * batch comfortably inside the caps and matches FE-INV-033.
 */
const val MAX_ENROLL_PHOTOS: Int = 5

/**
 * Remembers a platform image picker and returns a launch lambda.
 *
 * Responsibility: hide the platform difference (FE-INV-025 documented fallback — the system Photo
 * Picker is Android-only) so the people screen is written once. When the user picks images, [onPicked]
 * receives their raw bytes; resizing/encoding happens later in `ImagePreparer`.
 *
 * Android uses the system Photo Picker (`PickMultipleVisualMedia`, no storage permission); desktop
 * uses a Swing file chooser for the dev loop. The returned lambda is stable across recompositions.
 *
 * @param onPicked called with the selected images' bytes (empty if the user cancelled).
 */
@Composable
expect fun rememberPhotoPicker(onPicked: (List<ByteArray>) -> Unit): () -> Unit
