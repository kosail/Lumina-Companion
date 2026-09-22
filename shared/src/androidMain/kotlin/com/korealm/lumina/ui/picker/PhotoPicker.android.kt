package com.korealm.lumina.ui.picker

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Android [rememberPhotoPicker] over the system Photo Picker (activity 1.13.0).
 *
 * Verified against the androidx-main source (activity 1.13.0 has no git tag; see
 * `docs/API_VERIFICATION.md` §6/§8): `PickMultipleVisualMedia(maxItems)` requires `maxItems > 1`,
 * takes a `PickVisualMediaRequest` and returns a (possibly empty) `List<Uri>`; the request is built
 * with `PickVisualMediaRequest.Builder()` (the top-level helper is hidden-deprecated). No storage
 * permission is needed.
 *
 * The picked `content://` URIs are read immediately via the `ContentResolver`; they are not persisted
 * (the bytes are prepared and sent right away). Reading several full-resolution photos is multi-MB
 * I/O, so it runs on [Dispatchers.Default] and the bytes are delivered back on the calling (Main)
 * dispatcher (FE-INV-052).
 */
@Composable
actual fun rememberPhotoPicker(onPicked: (List<ByteArray>) -> Unit): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = MAX_ENROLL_PHOTOS),
    ) { uris ->
        // A URI that cannot be opened is skipped; the repository turns a short batch into a value.
        scope.launch {
            val images = withContext(Dispatchers.Default) {
                val resolver = context.contentResolver
                uris.mapNotNull { uri ->
                    runCatching { resolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
                }
            }
            onPicked(images)
        }
    }
    return remember(launcher) {
        {
            launcher.launch(
                PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    .build(),
            )
        }
    }
}
