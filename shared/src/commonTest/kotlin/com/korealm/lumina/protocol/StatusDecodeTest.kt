package com.korealm.lumina.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Decoding tests for the UDP `status` frame (`API_CONTRACT.md` §4.1).
 *
 * Covers: the contract's own example, unknown-key tolerance (forward compatibility), missing
 * optional fields, and `coerceInputValues` behaviour.
 */
class StatusDecodeTest {

    /** The exact `APP RECEIVES` example from `API_CONTRACT.md` §4.1. */
    private val contractExample = """{"t":"status","proto":1,"ts":1690000000,"runtime":{"reachable":true,"running":true,"uptimeS":62,"sink":"ready","faceCount":2},"core":{"fps":4.3,"rssMb":218.0,"memAvailableKb":65536,"tempC":46.2,"load1":1.80},"sensors":{"volume":70,"muted":false,"luma":0.420,"dayNight":"day"},"people":["David Solís"],"enroll":{"active":false}}"""

    @Test
    fun decodesTheContractExample() {
        val status = LuminaJson.decodeFromString(WireStatus.serializer(), contractExample)

        assertEquals("status", status.t)
        assertEquals(1, status.proto)
        assertEquals(1690000000L, status.ts)

        assertEquals(true, status.runtime.reachable)
        assertEquals(true, status.runtime.running)
        assertEquals(62, status.runtime.uptimeS)
        assertEquals("ready", status.runtime.sink)
        assertEquals(2, status.runtime.faceCount)

        assertEquals(4.3, status.core.fps)
        assertEquals(218.0, status.core.rssMb)
        assertEquals(65536L, status.core.memAvailableKb)
        assertEquals(46.2, status.core.tempC)
        assertEquals(1.80, status.core.load1)

        assertEquals(70, status.sensors.volume)
        assertEquals(false, status.sensors.muted)
        assertEquals(0.420, status.sensors.luma)
        assertEquals("day", status.sensors.dayNight)

        assertEquals(listOf("David Solís"), status.people)
        assertEquals(false, status.enroll.active)
        assertNull(status.enroll.phase)
    }

    @Test
    fun ignoresUnknownKeysAtEveryLevel() {
        // A newer device may add fields; they must not break decoding (contract §10).
        val json = """{"t":"status","proto":1,"futureTop":1,"ts":1,"runtime":{"reachable":true,"running":true,"uptimeS":1,"sink":"ready","faceCount":0,"futureRuntime":"x"},"core":{"fps":1.0,"rssMb":1.0,"futureCore":true},"sensors":{"volume":50,"muted":false,"luma":0.5,"dayNight":"day"},"people":[],"enroll":{"active":false,"futureEnroll":[]}}"""
        val status = LuminaJson.decodeFromString(WireStatus.serializer(), json)
        assertEquals(50, status.sensors.volume)
    }

    @Test
    fun toleratesMissingOptionalFields() {
        // `core`, `people`, and `enroll` are not required by the contract; omitting them uses defaults.
        val json = """{"t":"status","proto":1,"ts":5,"runtime":{"reachable":false,"running":false,"uptimeS":0,"sink":"absent","faceCount":0},"sensors":{"volume":-1,"muted":false,"luma":-1.0,"dayNight":"unknown"}}"""
        val status = LuminaJson.decodeFromString(WireStatus.serializer(), json)
        assertEquals(0.0, status.core.fps)
        assertNull(status.core.tempC)
        assertEquals(emptyList(), status.people)
        assertEquals(false, status.enroll.active)
    }

    @Test
    fun coercesNullValuesToDefaults() {
        // `coerceInputValues = true`: null for a non-null field that has a default uses the default.
        val json = """{"t":"status","proto":1,"ts":5,"runtime":{"reachable":true,"running":true,"uptimeS":1,"sink":"ready","faceCount":0},"core":{"fps":null,"rssMb":null},"sensors":{"volume":-1,"muted":false,"luma":-1.0,"dayNight":"unknown"},"enroll":null}"""
        val status = LuminaJson.decodeFromString(WireStatus.serializer(), json)
        assertEquals(0.0, status.core.fps)
        assertEquals(0.0, status.core.rssMb)
        assertEquals(false, status.enroll.active)
    }
}
