package com.korealm.lumina.ui.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pure tests for [validateSettings] — the settings form's only logic (no Compose, no Android).
 */
class SettingsValidationTest {

    @Test
    fun aCompleteFormProducesAnEndpoint() {
        val result = validateSettings(
            host = "10.42.0.1",
            udpPort = "47600",
            tcpPort = "47601",
            token = "abc123",
        )

        assertFalse(result.errors.hasAny)
        assertEquals("10.42.0.1", result.endpoint?.host)
        assertEquals(47600, result.endpoint?.udpPort)
        assertEquals(47601, result.endpoint?.tcpPort)
        assertEquals("abc123", result.token)
    }

    @Test
    fun hostAndTokenAreTrimmed() {
        val result = validateSettings("  pi.local \n", "47600", "47601", " tok \n")

        assertEquals("pi.local", result.endpoint?.host)
        assertEquals("tok", result.token)
    }

    @Test
    fun aBlankHostIsRejected() {
        val result = validateSettings("   ", "47600", "47601", "tok")

        assertTrue(result.errors.host)
        assertNull(result.endpoint)
    }

    @Test
    fun portsMustBeInRange() {
        assertTrue(validateSettings("h", "0", "47601", "t").errors.udpPort)
        assertTrue(validateSettings("h", "65536", "47601", "t").errors.udpPort)
        assertTrue(validateSettings("h", "not-a-number", "47601", "t").errors.udpPort)
        assertTrue(validateSettings("h", "47600", "0", "t").errors.tcpPort)
        assertNull(validateSettings("h", "0", "47601", "t").endpoint)
    }

    @Test
    fun theBoundaryPortsAreAccepted() {
        val low = validateSettings("h", "1", "1", "t")
        val high = validateSettings("h", "65535", "65535", "t")

        assertEquals(1, low.endpoint?.udpPort)
        assertEquals(65535, high.endpoint?.tcpPort)
    }

    @Test
    fun anEmptyTokenIsAllowed() {
        val result = validateSettings("h", "47600", "47601", "")

        assertFalse(result.errors.hasAny)
        assertEquals("", result.token)
        assertEquals("h", result.endpoint?.host)
    }
}
