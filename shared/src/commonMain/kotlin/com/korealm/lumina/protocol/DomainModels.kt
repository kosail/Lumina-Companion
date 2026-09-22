package com.korealm.lumina.protocol

/**
 * Domain models for telemetry and control replies.
 *
 * Responsibility: the app's own, *interpreted* view of the device. Sentinels from the wire
 * (`volume == -1`, `luma == -1`, `null`) become `null`, and enum-like strings become Kotlin enums
 * with an `Unknown` member so an unrecognized value never crashes the app (FE-INV-031).
 *
 * Why two layers (wire vs domain): the wire models must follow the contract byte-for-byte, while the
 * UI should never see a magic number like `-1`. Keeping them apart means a contract change touches
 * only the mapper.
 */

/** State of the audio sink (`runtime.sink`), tolerant of unknown values. */
enum class SinkState {
    Ready,
    Waiting,
    Absent,
    Unknown;

    companion object {
        /** Maps a wire `sink` string; any unrecognized value becomes [Unknown]. */
        fun fromWire(raw: String): SinkState = when (raw) {
            "ready" -> Ready
            "waiting" -> Waiting
            "absent" -> Absent
            else -> Unknown
        }
    }
}

/** Derived day/night state (`sensors.dayNight`). */
enum class DayNight {
    Day,
    Night,
    Unknown;

    companion object {
        /** Maps a wire `dayNight` string; any unrecognized value becomes [Unknown]. */
        fun fromWire(raw: String): DayNight = when (raw) {
            "day" -> Day
            "night" -> Night
            else -> Unknown
        }
    }
}

/** Phase of an in-progress enrollment (`enroll.phase` / `enroll.progress.phase`). */
enum class EnrollPhase {
    StoppingRuntime,
    Capturing,
    StartingRuntime,
    Unknown;

    companion object {
        /** Maps a wire `phase` string; any unrecognized value becomes [Unknown]. */
        fun fromWire(raw: String): EnrollPhase = when (raw) {
            "stopping_runtime" -> StoppingRuntime
            "capturing" -> Capturing
            "starting_runtime" -> StartingRuntime
            else -> Unknown
        }
    }
}

/** Post-enrollment runtime state (`enroll.error.runtime`), used by the "Start Lúmina" recovery. */
enum class RuntimeAfterEnroll {
    Started,
    Absent,
    Unchanged,
    Unknown;

    companion object {
        /** Maps a wire `runtime` string; any unrecognized value becomes [Unknown]. */
        fun fromWire(raw: String): RuntimeAfterEnroll = when (raw) {
            "started" -> Started
            "absent" -> Absent
            "unchanged" -> Unchanged
            else -> Unknown
        }
    }
}

/** Client-side liveness of the device, derived from telemetry arrival times (contract §2, §9). */
enum class Liveness {
    Online,
    Offline,
}

/** Interpreted `status.runtime`. */
data class RuntimeInfo(
    val reachable: Boolean,
    val running: Boolean,
    val uptimeSeconds: Int,
    val sink: SinkState,
    val faceCount: Int,
)

/** Interpreted `status.core`; `null` means "unknown" (never `0`). */
data class CoreInfo(
    val fps: Double,
    val rssMb: Double,
    val memAvailableKb: Long?,
    val tempC: Double?,
    val load1: Double?,
)

/** Interpreted `status.sensors`; `volumePercent`/`luma` are `null` when unknown (never `-1`). */
data class SensorInfo(
    val volumePercent: Int?,
    val muted: Boolean,
    val luma: Double?,
    val dayNight: DayNight,
)

/** Interpreted `status.enroll`. */
data class EnrollInfo(
    val active: Boolean,
    val phase: EnrollPhase?,
    val captured: Int?,
    val total: Int?,
)

/**
 * A fully interpreted telemetry frame.
 *
 * [receivedAtMs] is the local arrival time, used for ordering and liveness because the device clock
 * (`ts`) may jump after boot (contract §4.1).
 *
 * [proto] is carried through from the wire so the app can detect an incompatible device. Frames are
 * already gated to [SUPPORTED_PROTO] by `decodeStatus`, so this is normally `1`; keeping it on the
 * domain object lets the UI/diagnostics show the negotiated version.
 */
data class DeviceStatus(
    val proto: Int,
    val runtime: RuntimeInfo,
    val core: CoreInfo,
    val sensors: SensorInfo,
    val people: List<String>,
    val enroll: EnrollInfo,
    val receivedAtMs: Long,
)

/** Interpreted `volume.state`; `percent` is `null` when the mixer is unknown. */
data class VolumeState(
    val percent: Int?,
    val muted: Boolean,
    /** `false` when the mixer command failed. */
    val ok: Boolean,
)

/** Interpreted `runtime.state`. */
data class RuntimeState(
    val running: Boolean,
    val sink: SinkState,
)
