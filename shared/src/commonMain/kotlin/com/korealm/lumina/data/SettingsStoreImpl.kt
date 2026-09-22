package com.korealm.lumina.data

import com.korealm.lumina.transport.Endpoint
import com.korealm.lumina.transport.TCP_CONTROL_PORT
import com.korealm.lumina.transport.UDP_TELEMETRY_PORT
import com.russhwolf.settings.Settings

/**
 * [SettingsStore] backed by `multiplatform-settings`.
 *
 * Responsibility: map the [Endpoint]/token to string/int preference keys, applying platform defaults
 * when a value is absent. Constructed once in Koin with the no-arg `Settings()` (which on Android
 * self-initializes via androidx-startup and on the JVM uses `Preferences`), per `docs/API_VERIFICATION.md`
 * §3.5.
 *
 * Kotlin note: `getStringOrNull`/`getIntOrNull` return `null` when unset, which is how the defaults
 * below are selected; the `get`/`set` blocks are Kotlin property accessors (like Java getters/setters
 * but declared together).
 *
 * @param settings the backing store (injected so tests can use `MapSettings`).
 * @param defaultHost platform-appropriate host used until the user sets one.
 */
class SettingsStoreImpl(
    private val settings: Settings,
    private val defaultHost: String,
) : SettingsStore {

    override var endpoint: Endpoint
        get() = Endpoint(
            host = settings.getStringOrNull(KEY_HOST) ?: defaultHost,
            udpPort = settings.getIntOrNull(KEY_UDP_PORT) ?: UDP_TELEMETRY_PORT,
            tcpPort = settings.getIntOrNull(KEY_TCP_PORT) ?: TCP_CONTROL_PORT,
        )
        set(value) {
            settings.putString(KEY_HOST, value.host)
            settings.putInt(KEY_UDP_PORT, value.udpPort)
            settings.putInt(KEY_TCP_PORT, value.tcpPort)
        }

    override var token: String
        get() = settings.getStringOrNull(KEY_TOKEN) ?: ""
        set(value) {
            settings.putString(KEY_TOKEN, value)
        }

    private companion object {
        const val KEY_HOST = "endpoint.host"
        const val KEY_UDP_PORT = "endpoint.udpPort"
        const val KEY_TCP_PORT = "endpoint.tcpPort"
        const val KEY_TOKEN = "auth.token"
    }
}
