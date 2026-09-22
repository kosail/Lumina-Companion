package com.korealm.lumina

import android.app.Application
import com.korealm.lumina.di.startLuminaKoin

/**
 * Android [Application]: starts Koin once per process.
 *
 * Responsibility: call [startLuminaKoin] before any Activity composes [App]. Using the Application
 * (rather than `MainActivity.onCreate`) guarantees a single `startKoin` call even when the Activity
 * is recreated on a configuration change, which would otherwise throw.
 */
class LuminaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startLuminaKoin()
    }
}
