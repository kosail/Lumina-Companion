package com.korealm.lumina.data

import com.korealm.lumina.transport.Endpoint

/**
 * Persistent, user-settable configuration.
 *
 * Responsibility: the one place the app reads/writes the gateway address and the control token, so
 * no other layer touches preferences and nothing is hardcoded (FE-INV-053/050). Implementations back
 * this with `multiplatform-settings`; tests use an in-memory implementation.
 *
 * The token is a secret: it is read only to build control requests and must never be logged or shown
 * in full (FE-INV-053).
 */
interface SettingsStore {

    /** Gateway address and ports. Defaults come from the platform (desktop loopback / emulator). */
    var endpoint: Endpoint

    /** Shared control token; empty until the operator enters it (Phase 3 settings screen). */
    var token: String
}
