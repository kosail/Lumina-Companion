package com.korealm.lumina.testing

import com.korealm.lumina.protocol.CoreInfo
import com.korealm.lumina.protocol.DayNight
import com.korealm.lumina.protocol.DeviceStatus
import com.korealm.lumina.protocol.EnrollInfo
import com.korealm.lumina.protocol.RuntimeInfo
import com.korealm.lumina.protocol.SensorInfo
import com.korealm.lumina.protocol.SinkState

/** A representative healthy [DeviceStatus] for tests that need one. */
internal fun sampleStatus(): DeviceStatus = DeviceStatus(
    proto = 1,
    runtime = RuntimeInfo(
        reachable = true,
        running = true,
        uptimeSeconds = 62,
        sink = SinkState.Ready,
        faceCount = 2,
    ),
    core = CoreInfo(
        fps = 4.3,
        rssMb = 218.0,
        memAvailableKb = 65536L,
        tempC = 46.2,
        load1 = 1.80,
    ),
    sensors = SensorInfo(
        volumePercent = 70,
        muted = false,
        luma = 0.42,
        dayNight = DayNight.Day,
    ),
    people = listOf("David Solís"),
    enroll = EnrollInfo(active = false, phase = null, captured = null, total = null),
    receivedAtMs = 0L,
)
