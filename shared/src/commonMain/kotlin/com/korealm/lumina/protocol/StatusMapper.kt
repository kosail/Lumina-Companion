package com.korealm.lumina.protocol

/**
 * Converts a decoded telemetry frame ([WireStatus]) into the app's [DeviceStatus].
 *
 * Responsibility: apply every "unknown" sentinel exactly once, in one place:
 * - `sensors.volume == -1`  -> [SensorInfo.volumePercent] `= null`
 * - `sensors.luma < 0`      -> [SensorInfo.luma] `= null`, which also forces [DayNight.Unknown]
 * - out-of-range values (`volume` outside `0..100`) are treated as unknown, not shown raw
 * - `tempC`/`load1`/`memAvailableKb` are already nullable on the wire
 * - `enroll.total <= 0` and `enroll.captured < 0` are non-conforming and normalized to `null`
 *
 * `people` is passed through unchanged: the device reads it from its persisted enrolled store, so the
 * list stays valid even when the runtime is stopped (contract §4.1). The screen keeps the last-known
 * list across an offline blip (see `PeopleViewModel`).
 *
 * The mapper is pure: no clock, no I/O. [receivedAtMs] is passed in by the transport so the function
 * stays deterministic and unit-testable (the device `ts` may jump; ordering uses arrival time).
 *
 * @param receivedAtMs local monotonic arrival time of the datagram, in milliseconds.
 */
fun WireStatus.toDomain(receivedAtMs: Long): DeviceStatus {
    val reachable = runtime.reachable
    // `luma` unknown is the authoritative signal for day/night: if luma is the `-1` sentinel, the
    // derived `dayNight` must be "unknown" too, even if a non-conforming device still sent "day".
    val luma = sensors.luma.takeIf { it >= 0.0 }
    return DeviceStatus(
        proto = proto,
        runtime = RuntimeInfo(
            reachable = reachable,
            running = runtime.running,
            uptimeSeconds = runtime.uptimeS,
            sink = SinkState.fromWire(runtime.sink),
            faceCount = runtime.faceCount,
            initializing = runtime.initializing,
        ),
        core = CoreInfo(
            fps = core.fps,
            rssMb = core.rssMb,
            memAvailableKb = core.memAvailableKb,
            tempC = core.tempC,
            load1 = core.load1,
        ),
        sensors = SensorInfo(
            // `takeIf` keeps 0..100 (real values) but turns the `-1` sentinel and any out-of-range
            // value into null, so the UI never shows e.g. 150%.
            volumePercent = sensors.volume.takeIf { it in 0..100 },
            muted = sensors.muted,
            luma = luma,
            dayNight = if (luma == null) DayNight.Unknown else DayNight.fromWire(sensors.dayNight),
        ),
        // The device sources `people` from its persisted store, so keep it as-is: the list is valid
        // even while the runtime is stopped (contract §4.1, CHG-FE-0035).
        people = people,
        enroll = EnrollInfo(
            active = enroll.active,
            phase = enroll.phase?.let { EnrollPhase.fromWire(it) },
            // Contract minimums: captured >= 0, total >= 1. Normalize here so the progress UI can
            // never divide by a zero/negative total.
            captured = enroll.captured?.takeIf { it >= 0 },
            total = enroll.total?.takeIf { it > 0 },
        ),
        receivedAtMs = receivedAtMs,
    )
}
