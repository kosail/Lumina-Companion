package com.korealm.lumina.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests for [WireStatus.toDomain] — the sentinel handling required by FE-INV-031.
 *
 * Every "unknown" sentinel must become `null` (or [DayNight.Unknown]), never `0` or `-1`.
 */
class StatusMapperTest {

    /** Builds a wire frame with sensible defaults so each test overrides only what it checks. */
    private fun wire(
        proto: Int = 1,
        reachable: Boolean = true,
        people: List<String> = listOf("Ana"),
        volume: Int = 70,
        muted: Boolean = false,
        luma: Double = 0.4,
        dayNight: String = "day",
        memAvailableKb: Long? = 65536L,
        tempC: Double? = 46.2,
        load1: Double? = 1.8,
        sink: String = "ready",
        initializing: Boolean = false,
        enrollActive: Boolean = false,
        enrollPhase: String? = null,
        captured: Int? = null,
        total: Int? = null,
    ): WireStatus = WireStatus(
        proto = proto,
        runtime = WireRuntime(
            reachable = reachable,
            running = reachable,
            initializing = initializing,
            uptimeS = 10,
            sink = sink,
            faceCount = people.size,
        ),
        core = WireCore(fps = 4.3, rssMb = 218.0, memAvailableKb = memAvailableKb, tempC = tempC, load1 = load1),
        sensors = WireSensors(volume = volume, muted = muted, luma = luma, dayNight = dayNight),
        people = people,
        enroll = WireEnroll(active = enrollActive, phase = enrollPhase, captured = captured, total = total),
    )

    @Test
    fun mapsAHealthyFrame() {
        val status = wire().toDomain(receivedAtMs = 1234L)

        assertEquals(70, status.sensors.volumePercent)
        assertEquals(0.4, status.sensors.luma)
        assertEquals(DayNight.Day, status.sensors.dayNight)
        assertEquals(SinkState.Ready, status.runtime.sink)
        assertEquals(listOf("Ana"), status.people)
        assertEquals(1234L, status.receivedAtMs)
    }

    @Test
    fun turnsVolumeSentinelIntoNull() {
        // -1 means "mixer unknown"; 0 is a real volume and must survive.
        assertNull(wire(volume = -1).toDomain(0L).sensors.volumePercent)
        assertEquals(0, wire(volume = 0).toDomain(0L).sensors.volumePercent)
    }

    @Test
    fun turnsLumaSentinelIntoNull() {
        // luma < 0 means "unknown"; 0.0 is a real (dark) value and must survive.
        assertNull(wire(luma = -1.0).toDomain(0L).sensors.luma)
        assertEquals(0.0, wire(luma = 0.0).toDomain(0L).sensors.luma)
    }

    @Test
    fun mapsUnknownEnumsToUnknown() {
        val status = wire(dayNight = "dawn", sink = "splendid").toDomain(0L)
        assertEquals(DayNight.Unknown, status.sensors.dayNight)
        assertEquals(SinkState.Unknown, status.runtime.sink)
    }

    @Test
    fun keepsNullOsFieldsNull() {
        val status = wire(memAvailableKb = null, tempC = null, load1 = null).toDomain(0L)
        assertNull(status.core.memAvailableKb)
        assertNull(status.core.tempC)
        assertNull(status.core.load1)
    }

    @Test
    fun keepsPeopleWhenUnreachable() {
        // The device reads names from its persisted store, so they stay valid while the runtime is
        // stopped (contract §4.1, CHG-FE-0035).
        val status = wire(reachable = false, people = listOf("Ana"), sink = "absent").toDomain(0L)
        assertEquals(listOf("Ana"), status.people)
        assertEquals(false, status.runtime.reachable)
        assertEquals(SinkState.Absent, status.runtime.sink)
    }

    @Test
    fun mapsTheInitializingFlag() {
        assertEquals(true, wire(initializing = true).toDomain(0L).runtime.initializing)
        assertEquals(false, wire().toDomain(0L).runtime.initializing)
    }

    @Test
    fun carriesTheProtocolVersion() {
        assertEquals(1, wire(proto = 1).toDomain(0L).proto)
    }

    @Test
    fun treatsOutOfRangeVolumeAsUnknown() {
        // Contract is 0..100; -1 is the unknown sentinel and anything outside the range is unknown.
        assertNull(wire(volume = -1).toDomain(0L).sensors.volumePercent)
        assertNull(wire(volume = 101).toDomain(0L).sensors.volumePercent)
        assertNull(wire(volume = 150).toDomain(0L).sensors.volumePercent)
        assertEquals(0, wire(volume = 0).toDomain(0L).sensors.volumePercent)
        assertEquals(100, wire(volume = 100).toDomain(0L).sensors.volumePercent)
    }

    @Test
    fun forcesUnknownDayNightWhenLumaIsUnknown() {
        // A non-conforming device could send dayNight:"day" while luma is the -1 sentinel; the
        // unknown luma wins (FE-INV-031).
        val status = wire(luma = -1.0, dayNight = "day").toDomain(0L)
        assertNull(status.sensors.luma)
        assertEquals(DayNight.Unknown, status.sensors.dayNight)
    }

    @Test
    fun keepsDayNightWhenLumaIsKnown() {
        assertEquals(DayNight.Night, wire(luma = 0.1, dayNight = "night").toDomain(0L).sensors.dayNight)
    }

    @Test
    fun normalizesInvalidEnrollmentCounts() {
        // total must be >= 1 and captured >= 0; anything else becomes null so the UI cannot divide
        // by zero or render a negative count.
        val bad = wire(enrollActive = true, enrollPhase = "capturing", captured = -3, total = 0).toDomain(0L)
        assertNull(bad.enroll.captured)
        assertNull(bad.enroll.total)

        val good = wire(enrollActive = true, enrollPhase = "capturing", captured = 3, total = 10).toDomain(0L)
        assertEquals(3, good.enroll.captured)
        assertEquals(10, good.enroll.total)
    }

    @Test
    fun mapsEnrollmentPhaseAndUnknownPhase() {
        assertEquals(
            EnrollPhase.StoppingRuntime,
            wire(enrollActive = true, enrollPhase = "stopping_runtime").toDomain(0L).enroll.phase,
        )
        assertEquals(
            EnrollPhase.Unknown,
            wire(enrollActive = true, enrollPhase = "teleporting").toDomain(0L).enroll.phase,
        )
    }
}
