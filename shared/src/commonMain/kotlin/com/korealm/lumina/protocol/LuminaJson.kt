package com.korealm.lumina.protocol

import kotlinx.serialization.json.Json

/**
 * The single, configured JSON instance for the whole protocol layer.
 *
 * Responsibility: own the one `Json` configuration used to encode control requests and decode
 * telemetry/replies. It lives in `protocol/` so that every wire concern is in one place and the
 * transport layer never constructs its own parser.
 *
 * Configuration and *why* (contract `API_CONTRACT.md` §4, §10):
 * - [Json.ignoreUnknownKeys] = `true` — the contract says clients MUST ignore unknown keys so a
 *   newer device can add fields without breaking this app.
 * - [Json.explicitNulls] = `false` — a nullable field that is absent (or `null`) decodes to `null`,
 *   and `null` values are omitted when encoding. This keeps requests free of `"x":null`.
 * - [Json.coerceInputValues] = `true` — a `null` (or otherwise invalid) value for a field that has a
 *   default falls back to that default instead of throwing. This is a deliberate resilience choice
 *   (user-approved, CHG-FE-0006): a garbled telemetry frame degrades to "unknown" rather than
 *   killing the stream.
 * - [Json.encodeDefaults] = `true` — request objects rely on this: the discriminator `t` and the
 *   default `frames` are declared with defaults, and MUST still appear on the wire.
 *
 * Kotlin note (for the Java maintainer): `Json { ... }` is a builder function whose lambda has
 * `JsonBuilder` as receiver, so `ignoreUnknownKeys = true` sets a property on that builder. `val` is
 * an immutable binding (like a `final` field).
 *
 * Verified: kotlinx-serialization-json 1.11.0 — `JsonBuilder.ignoreUnknownKeys/explicitNulls/
 * coerceInputValues/encodeDefaults`; see `docs/API_VERIFICATION.md` §3.2 (accessed 2026-09-21).
 */
internal val LuminaJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    coerceInputValues = true
    encodeDefaults = true
}
