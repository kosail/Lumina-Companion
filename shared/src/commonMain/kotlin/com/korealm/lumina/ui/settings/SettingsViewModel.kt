package com.korealm.lumina.ui.settings

import androidx.lifecycle.ViewModel
import com.korealm.lumina.data.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * View model for the Ajustes tab.
 *
 * Responsibility: load the persisted [SettingsStore] into an editable [SettingsUiState], validate it
 * on save, and write it back (FE-INV-051/053). It holds no sockets; changing the endpoint is picked
 * up by the transport, which reads the store lazily (and reconnects when the host changes).
 *
 * Kotlin note: unlike the dashboard, this view model does no asynchronous work, so it needs no
 * `viewModelScope`; the state is a plain `MutableStateFlow`.
 *
 * @param settingsStore the persistent configuration store (interface; faked in tests).
 */
class SettingsViewModel(
    private val settingsStore: SettingsStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())

    /** Read-only state for the settings composable. */
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        val endpoint = settingsStore.endpoint
        _uiState.value = SettingsUiState(
            host = endpoint.host,
            udpPort = endpoint.udpPort.toString(),
            tcpPort = endpoint.tcpPort.toString(),
            token = settingsStore.token,
        )
    }

    /** The host field changed; clears its error and any previous "saved" confirmation. */
    fun onHostChange(value: String) = edit {
        it.copy(host = value, saved = false, errors = it.errors.copy(host = false))
    }

    /** The UDP port field changed. */
    fun onUdpPortChange(value: String) = edit {
        it.copy(udpPort = value, saved = false, errors = it.errors.copy(udpPort = false))
    }

    /** The TCP port field changed. */
    fun onTcpPortChange(value: String) = edit {
        it.copy(tcpPort = value, saved = false, errors = it.errors.copy(tcpPort = false))
    }

    /** The token field changed. */
    fun onTokenChange(value: String) = edit {
        it.copy(token = value, saved = false)
    }

    /** Toggles the token between masked and clear text. */
    fun onToggleTokenVisibility() = edit {
        it.copy(tokenRevealed = !it.tokenRevealed)
    }

    /**
     * Validates and persists the form. On invalid input it sets the error flags and writes nothing;
     * on success it updates the store and shows the confirmation.
     */
    fun onSave() {
        val current = _uiState.value
        val validation = validateSettings(current.host, current.udpPort, current.tcpPort, current.token)
        val endpoint = validation.endpoint
        if (endpoint == null) {
            _uiState.value = current.copy(errors = validation.errors, saved = false)
            return
        }
        settingsStore.endpoint = endpoint
        settingsStore.token = validation.token
        _uiState.value = current.copy(errors = SettingsErrors(), saved = true)
    }

    /** Applies a pure state edit; the only mutation point, so state stays immutable (FE-INV-051). */
    private fun edit(block: (SettingsUiState) -> SettingsUiState) {
        _uiState.value = block(_uiState.value)
    }
}
