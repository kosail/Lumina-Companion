package com.korealm.lumina.ui.dashboard

import com.korealm.lumina.data.FakeDeviceRepository
import com.korealm.lumina.protocol.ControlResult
import com.korealm.lumina.protocol.RuntimeState
import com.korealm.lumina.protocol.SinkState
import com.korealm.lumina.testing.sampleStatus
import com.korealm.lumina.transport.TelemetryEvent
import com.korealm.lumina.ui.ConnectionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Wiring tests for [DashboardViewModel]: telemetry collection, the volume debounce, and command
 * dispatch — all against a [FakeDeviceRepository], with virtual time.
 *
 * Lives in `jvmTest` because `Dispatchers.setMain` (needed for `viewModelScope`) is a JVM/Android
 * test utility; the pure reductions are covered in `commonTest` by `DashboardReducerTest`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun collectsEventsIntoUiState() = runTest(dispatcher) {
        val events = MutableSharedFlow<TelemetryEvent>(extraBufferCapacity = 8)
        val viewModel = DashboardViewModel(FakeDeviceRepository(events))
        runCurrent()

        assertEquals(ConnectionState.Connecting, viewModel.uiState.value.connection)

        events.emit(TelemetryEvent.Online(sampleStatus()))
        runCurrent()
        assertEquals(ConnectionState.Online, viewModel.uiState.value.connection)
        assertEquals(sampleStatus(), viewModel.uiState.value.status)
        assertEquals(70, viewModel.uiState.value.volume?.percent)

        events.emit(TelemetryEvent.Offline)
        runCurrent()
        assertEquals(ConnectionState.Offline, viewModel.uiState.value.connection)
        assertNull(viewModel.uiState.value.status)
    }

    @Test
    fun volumeChangeIsEchoedThenSentAfterTheDebounce() = runTest(dispatcher) {
        val repository = FakeDeviceRepository()
        val viewModel = DashboardViewModel(repository)
        runCurrent()

        viewModel.onVolumeChange(30)
        // Optimistic echo: the UI tracks the gesture without waiting for the command.
        assertEquals(30, viewModel.uiState.value.volume?.percent)

        runCurrent()
        assertTrue(repository.volumeSetCalls.isEmpty(), "debounce must not fire immediately")

        advanceTimeBy(VOLUME_DEBOUNCE_MS + 1)
        runCurrent()
        assertEquals(listOf(30), repository.volumeSetCalls)
    }

    @Test
    fun volumeChangeFinishedSendsImmediately() = runTest(dispatcher) {
        val repository = FakeDeviceRepository()
        val viewModel = DashboardViewModel(repository)
        runCurrent()

        viewModel.onVolumeChange(40)
        viewModel.onVolumeChangeFinished()
        runCurrent()

        assertEquals(listOf(40), repository.volumeSetCalls)
    }

    @Test
    fun unauthorizedRaisesNeedsToken() = runTest(dispatcher) {
        val repository = FakeDeviceRepository()
        repository.volumeResult = ControlResult.Unauthorized
        val viewModel = DashboardViewModel(repository)
        runCurrent()

        viewModel.onVolumeChange(20)
        viewModel.onVolumeChangeFinished()
        runCurrent()

        assertTrue(viewModel.uiState.value.needsToken)
        assertEquals(ControlMessage.Unauthorized, viewModel.uiState.value.message?.message)
    }

    @Test
    fun runtimeToggleStopsARunningDevice() = runTest(dispatcher) {
        val events = MutableSharedFlow<TelemetryEvent>(extraBufferCapacity = 8)
        val repository = FakeDeviceRepository(events)
        val viewModel = DashboardViewModel(repository)
        runCurrent()

        events.emit(TelemetryEvent.Online(sampleStatus())) // running = true
        runCurrent()

        viewModel.onRuntimeToggle()
        runCurrent()

        assertEquals(1, repository.runtimeStopCalls)
        assertEquals(0, repository.runtimeStartCalls)
    }

    @Test
    fun runtimeToggleStartsAStoppedDevice() = runTest(dispatcher) {
        val events = MutableSharedFlow<TelemetryEvent>(extraBufferCapacity = 8)
        val repository = FakeDeviceRepository(events)
        val viewModel = DashboardViewModel(repository)
        runCurrent()

        val stopped = sampleStatus().let { it.copy(runtime = it.runtime.copy(running = false)) }
        events.emit(TelemetryEvent.Online(stopped))
        runCurrent()

        viewModel.onRuntimeToggle()
        runCurrent()

        assertEquals(1, repository.runtimeStartCalls)
        assertEquals(0, repository.runtimeStopCalls)
    }

    @Test
    fun startingBlocksASecondToggleUntilTelemetryConfirms() = runTest(dispatcher) {
        val events = MutableSharedFlow<TelemetryEvent>(extraBufferCapacity = 8)
        val repository = FakeDeviceRepository(events)
        repository.runtimeResult =
            ControlResult.Ok(RuntimeState(running = true, sink = SinkState.Absent, initializing = true))
        val viewModel = DashboardViewModel(repository)
        runCurrent()

        val stopped = sampleStatus().let { it.copy(runtime = it.runtime.copy(running = false)) }
        events.emit(TelemetryEvent.Online(stopped))
        runCurrent()

        viewModel.onRuntimeToggle()
        runCurrent()
        assertEquals(1, repository.runtimeStartCalls)
        assertEquals(RuntimeTransition.Starting, viewModel.uiState.value.transition)

        // A second tap while the start settles must be ignored (no re-send during the ~18-60 s boot).
        viewModel.onRuntimeToggle()
        runCurrent()
        assertEquals(1, repository.runtimeStartCalls)

        // The device confirms with `initializing`: the local transition clears; the button still
        // reads "starting" from the status.
        val initializing = sampleStatus().let {
            it.copy(runtime = it.runtime.copy(running = false, initializing = true))
        }
        events.emit(TelemetryEvent.Online(initializing))
        runCurrent()
        assertNull(viewModel.uiState.value.transition)
        assertEquals(RuntimeButtonState.Starting, runtimeButtonState(viewModel.uiState.value))

        // Ready: the button becomes "stop".
        events.emit(TelemetryEvent.Online(sampleStatus()))
        runCurrent()
        assertEquals(RuntimeButtonState.Stop, runtimeButtonState(viewModel.uiState.value))
    }

    @Test
    fun anUnconfirmedStartClearsAfterTheGraceWindow() = runTest(dispatcher) {
        val events = MutableSharedFlow<TelemetryEvent>(extraBufferCapacity = 8)
        val repository = FakeDeviceRepository(events)
        repository.runtimeResult =
            ControlResult.Ok(RuntimeState(running = true, sink = SinkState.Absent, initializing = true))
        val viewModel = DashboardViewModel(repository)
        runCurrent()

        val stopped = sampleStatus().let { it.copy(runtime = it.runtime.copy(running = false)) }
        events.emit(TelemetryEvent.Online(stopped))
        runCurrent()

        viewModel.onRuntimeToggle()
        runCurrent()
        assertEquals(RuntimeTransition.Starting, viewModel.uiState.value.transition)

        advanceTimeBy(RUNTIME_START_GRACE_MS + 1)
        runCurrent()

        assertNull(viewModel.uiState.value.transition)
        assertEquals(ControlMessage.RuntimeStartUnconfirmed, viewModel.uiState.value.message?.message)
    }

    @Test
    fun anOldAgentStartHoldsUntilTelemetryConfirms() = runTest(dispatcher) {
        val events = MutableSharedFlow<TelemetryEvent>(extraBufferCapacity = 8)
        val repository = FakeDeviceRepository(events)
        // An agent that predates the additive `initializing` flag replies `running=true` (is-active)
        // but cannot say whether the runtime is ready.
        repository.runtimeResult = ControlResult.Ok(RuntimeState(running = true, sink = SinkState.Absent))
        val viewModel = DashboardViewModel(repository)
        runCurrent()

        val stopped = sampleStatus().let { it.copy(runtime = it.runtime.copy(running = false)) }
        events.emit(TelemetryEvent.Online(stopped))
        runCurrent()

        viewModel.onRuntimeToggle()
        runCurrent()
        assertEquals(RuntimeTransition.Starting, viewModel.uiState.value.transition)

        // The short grace must not fire: an old agent can take up to ~201 s to become ready.
        advanceTimeBy(RUNTIME_START_GRACE_MS + 1)
        runCurrent()
        assertEquals(RuntimeTransition.Starting, viewModel.uiState.value.transition)

        // Telemetry reports the runtime up: the transition clears.
        events.emit(TelemetryEvent.Online(sampleStatus()))
        runCurrent()
        assertNull(viewModel.uiState.value.transition)
    }

    @Test
    fun anOldAgentStartFallsBackAfterTheLongWindow() = runTest(dispatcher) {
        val events = MutableSharedFlow<TelemetryEvent>(extraBufferCapacity = 8)
        val repository = FakeDeviceRepository(events)
        repository.runtimeResult = ControlResult.Ok(RuntimeState(running = true, sink = SinkState.Absent))
        val viewModel = DashboardViewModel(repository)
        runCurrent()

        val stopped = sampleStatus().let { it.copy(runtime = it.runtime.copy(running = false)) }
        events.emit(TelemetryEvent.Online(stopped))
        runCurrent()

        viewModel.onRuntimeToggle()
        runCurrent()
        assertEquals(RuntimeTransition.Starting, viewModel.uiState.value.transition)

        // No confirmation ever arrives: the long fallback finally clears it with a neutral message.
        advanceTimeBy(RUNTIME_START_FALLBACK_MS + 1)
        runCurrent()
        assertNull(viewModel.uiState.value.transition)
        assertEquals(ControlMessage.RuntimeStartUnconfirmed, viewModel.uiState.value.message?.message)
    }

    @Test
    fun muteToggleEchoesAndSends() = runTest(dispatcher) {
        val repository = FakeDeviceRepository()
        val viewModel = DashboardViewModel(repository)
        runCurrent()

        viewModel.onMuteToggle(true)
        runCurrent()

        assertTrue(viewModel.uiState.value.volume?.muted == true)
        assertEquals(listOf(true), repository.muteCalls)
    }
}
