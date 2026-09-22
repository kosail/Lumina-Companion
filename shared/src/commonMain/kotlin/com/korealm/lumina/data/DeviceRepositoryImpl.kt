package com.korealm.lumina.data

import com.korealm.lumina.protocol.ControlResult
import com.korealm.lumina.protocol.RuntimeState
import com.korealm.lumina.protocol.VolumeState
import com.korealm.lumina.transport.ControlClient
import com.korealm.lumina.transport.TelemetryEvent
import com.korealm.lumina.transport.TelemetrySource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn

/**
 * Default [DeviceRepository]: a thin facade that forwards to the transport interfaces.
 *
 * Responsibility: keep the `transport/` types out of the UI layer (FE-INV-050) and share **one**
 * telemetry session across the whole app. It deliberately adds no command logic: liveness, decoding
 * and retry live in the transport, and command interpretation lives in the view models.
 *
 * Why [status] is shared: `TelemetrySource.status()` is cold, so each collector would otherwise open
 * its own UDP socket. With the dashboard and the people screen both observing, that would double the
 * subscription and let the two screens disagree on liveness. `shareIn` collapses them into a single
 * upstream session owned by [scope] (the DI app scope). `replay = 0` preserves the transport's
 * behaviour that nothing is emitted until the first datagram (the UI starts at "Conectando").
 *
 * Kotlin note: `shareIn` needs a `CoroutineScope`; it is injected from Koin (the app-lifetime scope)
 * rather than taken from a `GlobalScope` (FE-INV-051/052). Both collaborators are interfaces, so
 * this class is unit-tested with fakes and never touches a socket itself (FE-INV-022).
 *
 * @param telemetry the UDP telemetry stream (cold upstream, shared here).
 * @param control the TCP control client.
 * @param scope app-lifetime scope that owns the shared upstream (cancelled when the app dies).
 */
class DeviceRepositoryImpl(
    private val telemetry: TelemetrySource,
    private val control: ControlClient,
    scope: CoroutineScope,
) : DeviceRepository {

    // One upstream session for every collector (dashboard + people). `WhileSubscribed` keeps the
    // socket open only while at least one screen observes it, matching the original cold behaviour
    // while avoiding a second subscription. Note: `shareIn` does not restart an upstream that
    // *completes*; this is safe only because `KtorTelemetrySource.status()` is an infinite retry
    // loop. If that ever changes, revisit this sharing (CHG-FE-0017).
    private val sharedStatus: Flow<TelemetryEvent> = telemetry.status()
        .shareIn(scope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000), replay = 0)

    override fun status(): Flow<TelemetryEvent> = sharedStatus

    override suspend fun volumeGet(): ControlResult<VolumeState> = control.volumeGet()

    override suspend fun volumeSet(percent: Int): ControlResult<VolumeState> = control.volumeSet(percent)

    override suspend fun setMuted(muted: Boolean): ControlResult<VolumeState> = control.setMuted(muted)

    override suspend fun requestPeople(): ControlResult<List<String>> = control.people()

    override suspend fun runtimeStart(): ControlResult<RuntimeState> = control.runtimeStart()

    override suspend fun runtimeStop(): ControlResult<RuntimeState> = control.runtimeStop()
}
