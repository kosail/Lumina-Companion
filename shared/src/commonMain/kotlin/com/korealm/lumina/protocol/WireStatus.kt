package com.korealm.lumina.protocol

import kotlinx.serialization.Serializable

/**
 * Wire models for the UDP `status` telemetry object (`API_CONTRACT.md` §4.1).
 *
 * Responsibility: mirror the contract's JSON exactly. These types are dumb data carriers — they do
 * NOT interpret sentinels or enums. The interpretation happens in [toDomain] (StatusMapper.kt) so
 * that the wire shape and the app's domain shape stay independent.
 *
 * Rules encoded here:
 * - Every field the contract marks `required` has **no default** (a missing required field is a
 *   malformed frame and must fail decoding).
 * - Fields the contract does not require have a default, so a device that omits them still decodes.
 * - Enum-like fields (`sink`, `dayNight`, `phase`) are kept as [String] on purpose: the contract
 *   requires clients to tolerate unknown enum values, which a Kotlin `enum` would reject.
 *
 * Kotlin note: `@Serializable` is processed at compile time by the kotlinx-serialization plugin to
 * generate a serializer; there is no reflection. Property declaration order is also the JSON key
 * order on encode.
 */
@Serializable
data class WireStatus(
    /** Discriminator; always `"status"`. Defaulted so it never blocks decoding. */
    val t: String = "status",
    /** Protocol version; must be `1` (`API_CONTRACT.md` §10). */
    val proto: Int = 1,
    /** Device epoch seconds — BEST EFFORT and may jump after NTP. Use arrival time for ordering. */
    val ts: Long = 0L,
    val runtime: WireRuntime,
    val core: WireCore = WireCore(),
    val sensors: WireSensors,
    /** Enrolled names in insertion order; `[]` when the runtime is unreachable. */
    val people: List<String> = emptyList(),
    val enroll: WireEnroll = WireEnroll(),
)

/** `status.runtime` (`API_CONTRACT.md` §4.1). All fields are required by the contract. */
@Serializable
data class WireRuntime(
    /** `false` when the runtime status file is older than 5 s (stopped/crashed). */
    val reachable: Boolean,
    val running: Boolean,
    val uptimeS: Int,
    /** `ready` | `waiting` | `absent` (kept as [String] for forward compatibility). */
    val sink: String,
    /** Number of enrolled people. */
    val faceCount: Int,
)

/**
 * `status.core` (`API_CONTRACT.md` §4.1). The contract lists no `required` fields here, so every
 * field has a default. `memAvailableKb`, `tempC`, and `load1` are nullable (unknown when `null`).
 */
@Serializable
data class WireCore(
    /** Inference FPS, 1 decimal; `0.0` when unreachable. */
    val fps: Double = 0.0,
    /** Runtime resident memory MiB, 1 decimal; `0.0` when unreachable. */
    val rssMb: Double = 0.0,
    /** System `MemAvailable` KiB; `null` when unknown. */
    val memAvailableKb: Long? = null,
    /** SoC temperature °C, 1 decimal; `null` when unknown. */
    val tempC: Double? = null,
    /** 1-minute load average, 2 decimals; `null` when unknown. */
    val load1: Double? = null,
)

/**
 * `status.sensors` (`API_CONTRACT.md` §4.1). All four fields are required.
 * Sentinels: `volume == -1` and `luma < 0` mean "unknown" and are normalized in [toDomain].
 */
@Serializable
data class WireSensors(
    /** `0..100`, or `-1` when the mixer is unknown. */
    val volume: Int,
    val muted: Boolean,
    /** Mean frame luminance `0..1`, or `-1` when unknown. */
    val luma: Double,
    /** `day` | `night` | `unknown` (kept as [String] for forward compatibility). */
    val dayNight: String,
)

/**
 * `status.enroll` (`API_CONTRACT.md` §4.1). The contract defines a `oneOf`: either `{active:false}`
 * or `{active:true, phase, captured, total}`. A single tolerant model with defaults covers both.
 */
@Serializable
data class WireEnroll(
    val active: Boolean = false,
    /** `stopping_runtime` | `capturing` | `starting_runtime`; present only when active. */
    val phase: String? = null,
    val captured: Int? = null,
    val total: Int? = null,
)
