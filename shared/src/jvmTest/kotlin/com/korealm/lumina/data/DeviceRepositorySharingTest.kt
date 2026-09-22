package com.korealm.lumina.data

import com.korealm.lumina.testing.sampleStatus
import com.korealm.lumina.transport.FakeControlClient
import com.korealm.lumina.transport.TelemetryEvent
import com.korealm.lumina.transport.TelemetrySource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests that [DeviceRepositoryImpl] shares one telemetry session across collectors (Phase 4), so the
 * dashboard and the people screen do not open two UDP sockets.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DeviceRepositorySharingTest {

    @Test
    fun twoCollectorsShareOneTelemetrySession() = runTest {
        var opened = 0
        val upstream = MutableSharedFlow<TelemetryEvent>(extraBufferCapacity = 1)
        val telemetry = object : TelemetrySource {
            override fun status(): Flow<TelemetryEvent> = flow {
                opened += 1
                emit(TelemetryEvent.Online(sampleStatus()))
                emitAll(upstream)
            }
        }
        val repository = DeviceRepositoryImpl(telemetry, FakeControlClient(), backgroundScope)

        val first = mutableListOf<TelemetryEvent>()
        val second = mutableListOf<TelemetryEvent>()
        // The collectors are long-lived (they follow the shared upstream), so they belong on
        // `backgroundScope`, which `runTest` cancels when the test body ends.
        backgroundScope.launch { repository.status().collect { first += it } }
        backgroundScope.launch { repository.status().collect { second += it } }
        runCurrent()

        upstream.tryEmit(TelemetryEvent.Offline)
        runCurrent()

        assertEquals(1, opened, "both collectors must share one upstream session")
        assertEquals(TelemetryEvent.Offline, first.last())
        assertEquals(TelemetryEvent.Offline, second.last())
    }

    @Test
    fun resubscribesAfterTheStopTimeoutWhenCollectorsReturn() = runTest {
        var opened = 0
        val upstream = MutableSharedFlow<TelemetryEvent>()
        val telemetry = object : TelemetrySource {
            override fun status(): Flow<TelemetryEvent> = flow {
                opened += 1
                emitAll(upstream)
            }
        }
        val repository = DeviceRepositoryImpl(telemetry, FakeControlClient(), backgroundScope)

        val first = backgroundScope.launch { repository.status().collect {} }
        runCurrent()
        assertEquals(1, opened)

        // With no collectors, `WhileSubscribed(5000)` keeps the upstream for the grace period...
        first.cancel()
        runCurrent()
        advanceTimeBy(5_000)
        runCurrent()
        assertEquals(1, opened, "the stop timeout must not re-open the upstream on its own")

        // ...then a returning collector resubscribes, so a fresh upstream session is opened.
        val second = backgroundScope.launch { repository.status().collect {} }
        runCurrent()
        assertEquals(2, opened, "a returning collector must resubscribe")
        second.cancel()
    }
}
