package com.korealm.lumina.di

import org.koin.core.context.startKoin

/**
 * Starts the global Koin context with [appModule].
 *
 * Responsibility: the single entry point both platforms call once at process start — Android from
 * `LuminaApplication.onCreate`, desktop from `main`. It must run before any composable resolves a
 * dependency.
 *
 * Kotlin note: `startKoin { modules(...) }` configures the global `Koin` instance; calling it twice
 * in one process throws, which is why it is invoked from the Application/`main` (once per process)
 * rather than from an Activity that can be recreated.
 */
fun startLuminaKoin() {
    startKoin {
        modules(appModule)
    }
}
