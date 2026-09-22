package com.korealm.lumina.data

import com.korealm.lumina.protocol.ControlResult
import com.korealm.lumina.protocol.RuntimeState
import com.korealm.lumina.protocol.SinkState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Test double for [PeopleRepository].
 *
 * Responsibility: let the people view model be tested with no sockets (FE-INV-050). The live
 * snapshot flow is whatever the test passes; refresh/startRuntime return scripted values and count
 * their calls.
 */
class FakePeopleRepository(
    private val snapshots: Flow<PeopleSnapshot> = flowOf(),
) : PeopleRepository {

    /** How many times [refresh] was called. */
    var refreshCalls = 0
        private set

    /** How many times [startRuntime] was called. */
    var startRuntimeCalls = 0
        private set

    /** Result returned by [refresh] (settable per test). */
    var refreshResult: ControlResult<List<String>> = ControlResult.Ok(emptyList())

    /** Result returned by [startRuntime] (settable per test). */
    var runtimeResult: ControlResult<RuntimeState> =
        ControlResult.Ok(RuntimeState(running = true, sink = SinkState.Ready))

    override fun people(): Flow<PeopleSnapshot> = snapshots

    override suspend fun refresh(): ControlResult<List<String>> {
        refreshCalls += 1
        return refreshResult
    }

    override suspend fun startRuntime(): ControlResult<RuntimeState> {
        startRuntimeCalls += 1
        return runtimeResult
    }
}
