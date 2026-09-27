package com.korealm.lumina.protocol

import kotlinx.serialization.Serializable

/**
 * Wire models for every device → client control reply (`API_CONTRACT.md` §4.2–§4.9).
 *
 * Responsibility: mirror the reply JSON exactly. Like the status models, these are data carriers;
 * interpretation (sentinel/`enum` mapping) is done by [decodeReply] / [toDomain] helpers.
 *
 * Each reply is discriminated by its `t` field; there is no request `id` and replies arrive in
 * request order on a connection (contract §3.2).
 */

/** Reply to `volume.get`/`volume.set`/`volume.mute` (`API_CONTRACT.md` §4.2). */
@Serializable
data class WireVolumeState(
    val t: String = "volume.state",
    /** `true` when the mixer command succeeded; always `true` for `volume.get`. */
    val ok: Boolean,
    /** `0..100`, or `-1` when unknown. */
    val value: Int,
    val muted: Boolean,
)

/** Reply to `people.list` (`API_CONTRACT.md` §4.3). May be last-known when the runtime is down. */
@Serializable
data class WirePeople(
    val t: String = "people",
    val names: List<String>,
)

/** Reply to `runtime.state`/`runtime.start`/`runtime.stop` (`API_CONTRACT.md` §4.4). */
@Serializable
data class WireRuntimeState(
    val t: String = "runtime.state",
    /** `systemctl is-active`: true as soon as the unit is active, before it is ready. */
    val running: Boolean,
    /**
     * Additive/optional (contract §4.4): the unit is active but a fresh running status has not
     * arrived yet. Defaults to `false` so an older agent's reply still decodes.
     */
    val initializing: Boolean = false,
    /** `ready` | `waiting` | `absent` (kept as [String] for forward compatibility). */
    val sink: String,
)

/** A streamed `enroll.progress` line (`API_CONTRACT.md` §4.5). */
@Serializable
data class WireEnrollProgress(
    val t: String = "enroll.progress",
    /** `stopping_runtime` | `capturing` | `starting_runtime`. */
    val phase: String,
    /** Present when `phase == capturing`. */
    val captured: Int? = null,
    /** Present when `phase == capturing`. */
    val total: Int? = null,
    /** Present for a transient "no face detected" notice. */
    val message: String? = null,
)

/** Terminal success of an enrollment (`API_CONTRACT.md` §4.6). */
@Serializable
data class WireEnrollDone(
    val t: String = "enroll.done",
    val ok: Boolean = true,
    /** May lag by ~1 s; refresh `people.list` afterwards. */
    val personCount: Int,
    val embeddingsAdded: Int,
)

/** Terminal failure of an enrollment (`API_CONTRACT.md` §4.7). */
@Serializable
data class WireEnrollError(
    val t: String = "enroll.error",
    /** `-1` when cancelled/killed; `1`/`2` for tool errors. */
    val exitCode: Int,
    val message: String,
    /** `started` | `absent` | `unchanged` — the authoritative post-enrollment runtime state. */
    val runtime: String,
)

/** Acknowledgement of `enroll.camera.cancel` on the cancelling connection (`API_CONTRACT.md` §4.8). */
@Serializable
data class WireEnrollCancelled(
    val t: String = "enroll.cancelled",
)

/** A protocol error (`API_CONTRACT.md` §4.9). Do not string-match [message]; use [code]. */
@Serializable
data class WireError(
    val t: String = "error",
    /** `unauthorized` | `bad_request` | `busy` | `internal`. */
    val code: String,
    val message: String,
)
