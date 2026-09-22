package com.korealm.lumina.data

/**
 * The default gateway host for the current platform, used until the user configures one.
 *
 * Responsibility: supply a sensible default without hardcoding it in the transport (FE-INV-034/053).
 * `expect`/`actual` is used because the right value differs per target:
 * - Android: `10.42.0.1` — the Lúmina hotspot gateway, the product default (FE-INV-034). A phone on
 *   the hotspot connects with no configuration.
 * - Desktop (JVM): `127.0.0.1` — the app there is the developer loop against the mock agent.
 *
 * Development overrides are entered in Ajustes: the Android emulator uses `10.0.2.2` (its alias for
 * the host loopback) and a physical device on the mock uses the dev host's LAN IP. Desktop is
 * dev-only, so its `127.0.0.1` default is already the mock host.
 *
 * Kotlin note: `expect val` is declared once here and must be `actual`-ized in every target source
 * set (`jvmMain`, `androidMain`); this is KMP's equivalent of a platform-specific implementation.
 */
expect val defaultEndpointHost: String
