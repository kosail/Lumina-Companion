package com.korealm.lumina.protocol

import kotlinx.serialization.Serializable

/**
 * Builders for every `APP SENDS` control line (`API_CONTRACT.md` §4.10, §5).
 *
 * Responsibility: produce the exact JSON text for a request, with the shared `token` included. The
 * builders return a string **without** the trailing `\n`; the transport appends it (one object per
 * line, contract §3.2).
 *
 * Why build from typed objects instead of raw strings: the serializer guarantees valid JSON escaping
 * (important for UTF-8 names like `Solís`), and [LuminaJson] is configured with `encodeDefaults` so
 * the discriminator `t` and the default `frames` always reach the wire.
 *
 * Every command except [subscribeLine] is token-gated (FE-INV-032). The token is passed in by the
 * caller and is never logged or stored here (FE-INV-053).
 */

/** Default camera-enrollment frame target (`ENROLL_FRAMES_DEFAULT`, contract §2). */
const val ENROLL_FRAMES_DEFAULT: Int = 10

/** Upper bound for requested camera frames; the device clamps to `[1, 10]` (contract §2). */
const val ENROLL_FRAMES_MAX: Int = 10

/** UDP `subscribe`; unauthenticated and read-only (contract §3.1). */
fun subscribeLine(): String =
    LuminaJson.encodeToString(SubscribeRequest.serializer(), SubscribeRequest())

/** `volume.get` (contract §5.1). */
fun volumeGetLine(token: String): String =
    LuminaJson.encodeToString(VolumeGetRequest.serializer(), VolumeGetRequest(token = token))

/** `volume.set` with a `0..100` percent (contract §5.2). Out-of-range is rejected by the device. */
fun volumeSetLine(token: String, percent: Int): String =
    LuminaJson.encodeToString(VolumeSetRequest.serializer(), VolumeSetRequest(token = token, value = percent))

/** `volume.mute` (contract §5.3). */
fun volumeMuteLine(token: String, muted: Boolean): String =
    LuminaJson.encodeToString(VolumeMuteRequest.serializer(), VolumeMuteRequest(token = token, value = muted))

/** `people.list` (contract §5.4). May return last-known names when the runtime is down. */
fun peopleListLine(token: String): String =
    LuminaJson.encodeToString(PeopleListRequest.serializer(), PeopleListRequest(token = token))

/** `runtime.state` (contract §5.5). */
fun runtimeStateLine(token: String): String =
    LuminaJson.encodeToString(RuntimeStateRequest.serializer(), RuntimeStateRequest(token = token))

/** `runtime.start` — recovery action (contract §5.6). */
fun runtimeStartLine(token: String): String =
    LuminaJson.encodeToString(RuntimeStartRequest.serializer(), RuntimeStartRequest(token = token))

/** `runtime.stop` (contract §5.7). */
fun runtimeStopLine(token: String): String =
    LuminaJson.encodeToString(RuntimeStopRequest.serializer(), RuntimeStopRequest(token = token))

/**
 * `enroll.camera.start` (contract §5.8). Blocks its connection until the enrollment ends; cancel it
 * from a **second** connection via [enrollCameraCancelLine].
 *
 * @param frames requested frame count; clamped to `1..`[ENROLL_FRAMES_MAX] here as well as on the
 *   device, so a caller mistake cannot produce an out-of-range request.
 */
fun enrollCameraStartLine(token: String, name: String, frames: Int = ENROLL_FRAMES_DEFAULT): String =
    LuminaJson.encodeToString(
        EnrollCameraStartRequest.serializer(),
        EnrollCameraStartRequest(token = token, name = name, frames = frames.coerceIn(1, ENROLL_FRAMES_MAX)),
    )

/** `enroll.camera.cancel` — must be sent on a second connection (contract §5.8, §6.3). */
fun enrollCameraCancelLine(token: String): String =
    LuminaJson.encodeToString(EnrollCameraCancelRequest.serializer(), EnrollCameraCancelRequest(token = token))

/**
 * `enroll.images` (contract §5.9).
 *
 * @param images raw base64 strings (no `data:` prefix). Caps are enforced by [validateImageBatch];
 *   this builder does not re-check them.
 */
fun enrollImagesLine(token: String, name: String, images: List<String>): String =
    LuminaJson.encodeToString(
        EnrollImagesRequest.serializer(),
        EnrollImagesRequest(token = token, name = name, images = images),
    )

// ---------------------------------------------------------------------------
// Request payloads. Kept `private`: callers use the builder functions above, so the wire shape
// cannot leak. Field order matters — it is the JSON key order (t, token, then command fields).
// ---------------------------------------------------------------------------

@Serializable
private data class SubscribeRequest(val t: String = "subscribe")

@Serializable
private data class VolumeGetRequest(val t: String = "volume.get", val token: String)

@Serializable
private data class VolumeSetRequest(val t: String = "volume.set", val token: String, val value: Int)

@Serializable
private data class VolumeMuteRequest(val t: String = "volume.mute", val token: String, val value: Boolean)

@Serializable
private data class PeopleListRequest(val t: String = "people.list", val token: String)

@Serializable
private data class RuntimeStateRequest(val t: String = "runtime.state", val token: String)

@Serializable
private data class RuntimeStartRequest(val t: String = "runtime.start", val token: String)

@Serializable
private data class RuntimeStopRequest(val t: String = "runtime.stop", val token: String)

@Serializable
private data class EnrollCameraStartRequest(
    val t: String = "enroll.camera.start",
    val token: String,
    val name: String,
    val frames: Int = ENROLL_FRAMES_DEFAULT,
)

@Serializable
private data class EnrollCameraCancelRequest(val t: String = "enroll.camera.cancel", val token: String)

@Serializable
private data class EnrollImagesRequest(
    val t: String = "enroll.images",
    val token: String,
    val name: String,
    val images: List<String>,
)
