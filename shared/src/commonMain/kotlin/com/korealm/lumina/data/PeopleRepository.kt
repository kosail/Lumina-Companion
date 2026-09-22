package com.korealm.lumina.data

import com.korealm.lumina.protocol.ControlResult
import com.korealm.lumina.protocol.RuntimeState
import kotlinx.coroutines.flow.Flow

/**
 * A point-in-time view of the people list plus whether the device is currently reachable.
 *
 * Responsibility: give the people screen one small, UI-agnostic value. [names] is empty while
 * offline; the view model keeps the last-known list so a temporary drop does not erase what the
 * screen already showed.
 */
data class PeopleSnapshot(val online: Boolean, val names: List<String>)

/**
 * The people screen's data entry point.
 *
 * Responsibility: expose the live people list (from telemetry) and the explicit `people.list`
 * refresh, so the view model depends only on `data/` (FE-INV-050). The `runtime.start` recovery
 * action lives here too, because it is the follow-up to a failed enrollment (contract §6.5).
 */
interface PeopleRepository {

    /** Live people/connection state, derived from the shared telemetry stream. */
    fun people(): Flow<PeopleSnapshot>

    /**
     * `people.list` (contract §5.4). May return last-known names even when the runtime is down, so
     * it is used to refresh after an enrollment rather than as the live source.
     */
    suspend fun refresh(): ControlResult<List<String>>

    /** `runtime.start` — "Iniciar Lúmina" recovery after `enroll.error.runtime != "started"` (contract §6.5). */
    suspend fun startRuntime(): ControlResult<RuntimeState>
}
