package com.korealm.lumina.data

import com.korealm.lumina.transport.Endpoint
import com.korealm.lumina.transport.TCP_CONTROL_PORT
import com.korealm.lumina.transport.UDP_TELEMETRY_PORT
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for [SettingsStoreImpl] over an in-memory [MapSettings].
 *
 * Responsibility: cover the persistence gap left open in Phases 2-4, where the store was only
 * exercised through `SettingsViewModel` with a fake and its real mapping (defaults + round-trip) was
 * never asserted. Runs on the JVM with no file store and no Android (FE-INV-050), per the
 * user-approved `multiplatform-settings-test` test-only dependency (CHG-FE-0022).
 */
class SettingsStoreImplTest {

    private fun store(settings: MapSettings = MapSettings()): SettingsStoreImpl =
        SettingsStoreImpl(settings = settings, defaultHost = "127.0.0.1")

    @Test
    fun anEmptyStoreUsesThePlatformDefaults() {
        val store = store()

        assertEquals(Endpoint("127.0.0.1", UDP_TELEMETRY_PORT, TCP_CONTROL_PORT), store.endpoint)
        assertEquals("", store.token)
    }

    @Test
    fun roundTripsTheEndpointAndToken() {
        val store = store()

        store.endpoint = Endpoint("10.42.0.1", 50000, 50001)
        store.token = "abc123"

        assertEquals(Endpoint("10.42.0.1", 50000, 50001), store.endpoint)
        assertEquals("abc123", store.token)
    }

    @Test
    fun aPartiallySeededStoreKeepsTheMissingDefaults() {
        // Only the host was ever persisted; the ports must fall back to the contract constants.
        val store = store(MapSettings("endpoint.host" to "192.168.1.10"))

        assertEquals(Endpoint("192.168.1.10", UDP_TELEMETRY_PORT, TCP_CONTROL_PORT), store.endpoint)
        assertEquals("", store.token)
    }
}
