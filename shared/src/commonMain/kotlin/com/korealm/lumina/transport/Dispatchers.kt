package com.korealm.lumina.transport

import kotlinx.coroutines.CoroutineDispatcher

/**
 * Background dispatcher for socket I/O (FE-INV-052).
 *
 * `Dispatchers.IO` is not part of the Kotlin Multiplatform **common** API, so it is exposed here as an
 * `expect`/`actual` value that both targets (JVM, Android) satisfy. Socket work runs on it rather than
 * on the caller's context — which is Android's main thread when a control command is launched from
 * `viewModelScope`. Resolving the gateway address or opening a socket on the main thread throws
 * `NetworkOnMainThreadException`, the Phase 6 physical-device bug (CHG-FE-0027).
 *
 * Kotlin note: `expect val` is declared once here and `actual`-ized in every target source set
 * (`jvmMain`, `androidMain`); this is KMP's equivalent of a platform-specific implementation.
 */
expect val ioDispatcher: CoroutineDispatcher
