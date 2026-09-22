package com.korealm.lumina.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Tests for [decodeStatus] — the telemetry entry point that enforces the `t` discriminator and the
 * `proto` version gate (`API_CONTRACT.md` §3.1, §10; FE-INV-030).
 */
class DecodeStatusTest {

    private val valid = """{"t":"status","proto":1,"ts":1690000000,"runtime":{"reachable":true,"running":true,"uptimeS":62,"sink":"ready","faceCount":2},"core":{"fps":4.3,"rssMb":218.0,"memAvailableKb":65536,"tempC":46.2,"load1":1.80},"sensors":{"volume":70,"muted":false,"luma":0.420,"dayNight":"day"},"people":["David Solís"],"enroll":{"active":false}}"""

    @Test
    fun decodesAValidFrame() {
        val result = assertIs<StatusDecodeResult.Decoded>(decodeStatus(valid, receivedAtMs = 999L))
        assertEquals(1, result.status.proto)
        assertEquals(999L, result.status.receivedAtMs)
        assertEquals(70, result.status.sensors.volumePercent)
    }

    @Test
    fun rejectsANonStatusDiscriminator() {
        val volumeState = """{"t":"volume.state","ok":true,"value":1,"muted":false}"""
        assertIs<StatusDecodeResult.Malformed>(decodeStatus(volumeState, 0L))
        assertIs<StatusDecodeResult.Malformed>(decodeStatus("""{"no":"discriminator"}""", 0L))
    }

    @Test
    fun rejectsMalformedJson() {
        assertIs<StatusDecodeResult.Malformed>(decodeStatus("not json at all", 0L))
        assertIs<StatusDecodeResult.Malformed>(decodeStatus("[1,2,3]", 0L))
    }

    @Test
    fun rejectsAMissingProto() {
        // A version gate must not be bypassable by omitting `proto`.
        val noProto = valid.replace(""""proto":1,""", "")
        assertIs<StatusDecodeResult.Malformed>(decodeStatus(noProto, 0L))
    }

    @Test
    fun rejectsANonIntegerProto() {
        val floatProto = valid.replace(""""proto":1""", """"proto":1.5""")
        assertIs<StatusDecodeResult.Malformed>(decodeStatus(floatProto, 0L))
    }

    @Test
    fun reportsAnUnsupportedProto() {
        val proto2 = valid.replace(""""proto":1""", """"proto":2""")
        val result = assertIs<StatusDecodeResult.UnsupportedProto>(decodeStatus(proto2, 0L))
        assertEquals(2, result.proto)
    }

    @Test
    fun decodesTheActiveEnrollmentBranch() {
        val active = """{"t":"status","proto":1,"ts":1,"runtime":{"reachable":true,"running":true,"uptimeS":1,"sink":"ready","faceCount":1},"core":{},"sensors":{"volume":50,"muted":false,"luma":0.5,"dayNight":"day"},"people":["Ana"],"enroll":{"active":true,"phase":"capturing","captured":3,"total":10}}"""
        val result = assertIs<StatusDecodeResult.Decoded>(decodeStatus(active, 0L))
        assertEquals(true, result.status.enroll.active)
        assertEquals(EnrollPhase.Capturing, result.status.enroll.phase)
        assertEquals(3, result.status.enroll.captured)
        assertEquals(10, result.status.enroll.total)
    }
}
