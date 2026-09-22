package com.korealm.lumina.data

/**
 * The default gateway host for the current platform, used until the user configures one.
 *
 * Responsibility: supply a sensible development default without hardcoding it in the transport
 * (FE-INV-034/053). `expect`/`actual` is used because the right value differs per target:
 * - Desktop (JVM): `127.0.0.1` — the mock agent runs on the same machine.
 * - Android: `10.0.2.2` — the emulator's alias for the host loopback.
 *
 * A physical device must point at the Pi's LAN IP or the hotspot gateway `10.42.0.1`; the Phase 3
 * settings screen lets the operator change it.
 *
 * Kotlin note: `expect val` is declared once here and must be `actual`-ized in every target source
 * set (`jvmMain`, `androidMain`); this is KMP's equivalent of a platform-specific implementation.
 */
expect val defaultEndpointHost: String
