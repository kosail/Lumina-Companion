package com.korealm.lumina.di

import com.korealm.lumina.data.DeviceRepository
import com.korealm.lumina.data.DeviceRepositoryImpl
import com.korealm.lumina.data.EnrollmentRepository
import com.korealm.lumina.data.EnrollmentRepositoryImpl
import com.korealm.lumina.data.ImagePreparer
import com.korealm.lumina.data.PeopleRepository
import com.korealm.lumina.data.PeopleRepositoryImpl
import com.korealm.lumina.data.SettingsStore
import com.korealm.lumina.data.SettingsStoreImpl
import com.korealm.lumina.data.createImagePreparer
import com.korealm.lumina.data.defaultEndpointHost
import com.korealm.lumina.transport.ControlClient
import com.korealm.lumina.transport.ControlConnectionFactory
import com.korealm.lumina.transport.KtorControlClient
import com.korealm.lumina.transport.KtorControlConnectionFactory
import com.korealm.lumina.transport.KtorTelemetrySocketFactory
import com.korealm.lumina.transport.KtorTelemetrySource
import com.korealm.lumina.transport.TelemetrySocketFactory
import com.korealm.lumina.transport.TelemetrySource
import com.korealm.lumina.ui.dashboard.DashboardViewModel
import com.korealm.lumina.ui.people.PeopleViewModel
import com.korealm.lumina.ui.settings.SettingsViewModel
import com.russhwolf.settings.Settings
import io.ktor.network.selector.SelectorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.module

/**
 * The app's single Koin module (common to Android and desktop).
 *
 * Responsibility: wire interfaces to implementations so no layer constructs its own dependencies
 * (FE-INV-050). Concrete transport/repository types appear only here; the UI receives
 * [TelemetrySource]/[ControlClient] through the graph.
 *
 * Kotlin note (Koin): `single { }` creates one instance for the app lifetime; `get()` inside a
 * definition resolves another dependency from the same graph. The nested `{ get<SettingsStore>()... }`
 * lambdas are resolved lazily at call time, so changing settings later is picked up automatically.
 */
val appModule = module {

    // Ktor selector multiplexes all socket I/O; one instance for the app (closed on process exit).
    single<SelectorManager> { SelectorManager() }

    // App-lifetime scope that owns the shared telemetry session (`DeviceRepositoryImpl.status()`).
    // Injected instead of `GlobalScope` so it is a normal dependency (FE-INV-051/052); it lives until
    // the process ends, which matches the app's two permanent screens.
    single { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    // Key-value persistence: the no-arg factory self-initializes per platform (verification §3.5).
    single<Settings> { Settings() }

    single<SettingsStore> { SettingsStoreImpl(settings = get(), defaultHost = defaultEndpointHost) }

    // Transport seams (interfaces) -> Ktor implementations. Only the *Factory implementations import
    // io.ktor.network (FE-INV-022).
    single<TelemetrySocketFactory> { KtorTelemetrySocketFactory(get()) }
    single<ControlConnectionFactory> { KtorControlConnectionFactory(get()) }

    single<TelemetrySource> {
        KtorTelemetrySource(
            endpointProvider = { get<SettingsStore>().endpoint },
            sockets = get(),
        )
    }

    single<ControlClient> {
        KtorControlClient(
            endpointProvider = { get<SettingsStore>().endpoint },
            tokenProvider = { get<SettingsStore>().token },
            connections = get(),
        )
    }

    // Image pipeline for photo enrollment (platform expect/actual).
    single<ImagePreparer> { createImagePreparer() }

    // The data-layer facades the UI talks to, so it never imports transport/ directly (FE-INV-050).
    single<DeviceRepository> { DeviceRepositoryImpl(telemetry = get(), control = get(), scope = get()) }
    single<PeopleRepository> { PeopleRepositoryImpl(device = get()) }
    single<EnrollmentRepository> {
        EnrollmentRepositoryImpl(control = get(), imagePreparer = get())
    }

    // View models are `single`s rather than `viewModel {}` definitions: Compose Multiplatform desktop
    // does not guarantee a host ViewModelStoreOwner, so `koinViewModel()` could fail there. A single
    // keeps one instance for the app lifetime (fine for a persistent dashboard, people screen and
    // settings form). TODO(di): switch to viewModel {} + koinViewModel once desktop store-owner
    // support is confirmed.
    single { DashboardViewModel(get()) }
    single { PeopleViewModel(get(), get()) }
    single { SettingsViewModel(get()) }
}
