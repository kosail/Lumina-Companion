package com.korealm.lumina.ui.settings

import com.korealm.lumina.data.FakeSettingsStore
import com.korealm.lumina.transport.Endpoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for [SettingsViewModel]: load, validate, persist — against a [FakeSettingsStore]. No
 * coroutines or Android are involved.
 */
class SettingsViewModelTest {

    @Test
    fun loadsThePersistedValues() {
        val store = FakeSettingsStore(Endpoint("10.42.0.1", 47600, 47601), "secret")

        val viewModel = SettingsViewModel(store)

        assertEquals("10.42.0.1", viewModel.uiState.value.host)
        assertEquals("47600", viewModel.uiState.value.udpPort)
        assertEquals("47601", viewModel.uiState.value.tcpPort)
        assertEquals("secret", viewModel.uiState.value.token)
        assertFalse(viewModel.uiState.value.saved)
    }

    @Test
    fun savePersistsAValidForm() {
        val store = FakeSettingsStore()
        val viewModel = SettingsViewModel(store)

        viewModel.onHostChange("192.168.1.5")
        viewModel.onUdpPortChange("50000")
        viewModel.onTcpPortChange("50001")
        viewModel.onTokenChange("newtok")
        viewModel.onSave()

        assertEquals(Endpoint("192.168.1.5", 50000, 50001), store.endpoint)
        assertEquals("newtok", store.token)
        assertTrue(viewModel.uiState.value.saved)
        assertFalse(viewModel.uiState.value.errors.hasAny)
    }

    @Test
    fun anInvalidFormDoesNotPersist() {
        val store = FakeSettingsStore()
        val viewModel = SettingsViewModel(store)

        viewModel.onHostChange("   ")
        viewModel.onSave()

        assertTrue(viewModel.uiState.value.errors.host)
        assertFalse(viewModel.uiState.value.saved)
        assertEquals(Endpoint("127.0.0.1"), store.endpoint)
    }

    @Test
    fun editingClearsTheSavedConfirmation() {
        val store = FakeSettingsStore()
        val viewModel = SettingsViewModel(store)

        viewModel.onSave()
        assertTrue(viewModel.uiState.value.saved)

        viewModel.onHostChange("x")
        assertFalse(viewModel.uiState.value.saved)
    }

    @Test
    fun togglingRevealsTheToken() {
        val viewModel = SettingsViewModel(FakeSettingsStore())

        assertFalse(viewModel.uiState.value.tokenRevealed)

        viewModel.onToggleTokenVisibility()

        assertTrue(viewModel.uiState.value.tokenRevealed)
    }
}
