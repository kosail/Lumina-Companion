package com.korealm.lumina.data

import com.korealm.lumina.protocol.ControlResult
import com.korealm.lumina.protocol.RuntimeState
import com.korealm.lumina.transport.TelemetryEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Default [PeopleRepository]: derives the live list from [DeviceRepository] telemetry and forwards
 * the refresh/recovery commands.
 *
 * Responsibility: keep the `transport/` types (and the mapping of [TelemetryEvent] to a people
 * snapshot) out of the UI layer (FE-INV-050). It adds no retry logic — that lives in the transport.
 *
 * @param device the shared device facade (status + control commands).
 */
class PeopleRepositoryImpl(
    private val device: DeviceRepository,
) : PeopleRepository {

    override fun people(): Flow<PeopleSnapshot> = device.status().map { event ->
        when (event) {
            is TelemetryEvent.Online -> PeopleSnapshot(online = true, names = event.status.people)
            // Offline and incompatible both mean "no live data"; the view model keeps the last list.
            TelemetryEvent.Offline -> PeopleSnapshot(online = false, names = emptyList())
            is TelemetryEvent.Incompatible -> PeopleSnapshot(online = false, names = emptyList())
        }
    }

    override suspend fun refresh(): ControlResult<List<String>> = device.requestPeople()

    override suspend fun startRuntime(): ControlResult<RuntimeState> = device.runtimeStart()
}
