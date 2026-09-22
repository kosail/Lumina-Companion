package com.korealm.lumina.data

/**
 * Android default: the Lúmina hotspot gateway, `10.42.0.1` (FE-INV-034). This is the product
 * default, so a phone that joins the hotspot connects with no configuration.
 *
 * Development overrides live in Ajustes: the emulator uses `10.0.2.2` (its alias for the host
 * loopback) and a physical device on the mock uses the dev host's LAN IP.
 */
actual val defaultEndpointHost: String = "10.42.0.1"
