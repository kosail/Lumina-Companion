package com.korealm.lumina.data

import com.korealm.lumina.transport.Endpoint

/**
 * In-memory [SettingsStore] for tests.
 *
 * Responsibility: let `SettingsViewModel` be tested without `multiplatform-settings` or platform
 * initialization (FE-INV-050). The defaults mirror the desktop loopback so assertions are explicit.
 */
class FakeSettingsStore(
    override var endpoint: Endpoint = Endpoint(host = "127.0.0.1"),
    override var token: String = "",
) : SettingsStore
