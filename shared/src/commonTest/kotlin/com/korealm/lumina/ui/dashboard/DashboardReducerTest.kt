package com.korealm.lumina.ui.dashboard

import com.korealm.lumina.protocol.ControlResult
import com.korealm.lumina.protocol.RuntimeState
import com.korealm.lumina.protocol.SinkState
import com.korealm.lumina.protocol.VolumeState
import com.korealm.lumina.testing.sampleStatus
import com.korealm.lumina.transport.TelemetryEvent
import com.korealm.lumina.ui.ConnectionState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pure tests for the dashboard reductions (no coroutines, no Android, no device).
 *
 * The transport → state mapping and the control-result → state mapping are the dashboard's only
 * logic, so they are covered exhaustively here.
 */
class DashboardReducerTest {

    @Test
    fun startsInConnectingWithNoStatus() {
        val state = DashboardUiState()
        assertEquals(ConnectionState.Connecting, state.connection)
        assertNull(state.status)
        assertNull(state.volume)
    }

    @Test
    fun onlineCarriesTheStatusAndSeedsVolume() {
        val state = reduceDashboardState(DashboardUiState(), TelemetryEvent.Online(sampleStatus()))
        assertEquals(ConnectionState.Online, state.connection)
        assertEquals(sampleStatus(), state.status)
        assertNull(state.incompatibleProto)
        assertEquals(VolumeState(percent = 70, muted = false, ok = true), state.volume)
    }

    @Test
    fun offlineClearsTheStatusAndVolume() {
        val online = reduceDashboardState(DashboardUiState(), TelemetryEvent.Online(sampleStatus()))

        val offline = reduceDashboardState(online, TelemetryEvent.Offline)

        assertEquals(ConnectionState.Offline, offline.connection)
        assertNull(offline.status)
        assertNull(offline.volume)
    }

    @Test
    fun incompatibleClearsTheStatusAndRecordsTheProto() {
        val online = reduceDashboardState(DashboardUiState(), TelemetryEvent.Online(sampleStatus()))

        val incompatible = reduceDashboardState(online, TelemetryEvent.Incompatible(2))

        assertEquals(ConnectionState.Incompatible, incompatible.connection)
        assertNull(incompatible.status)
        assertEquals(2, incompatible.incompatibleProto)
        assertNull(incompatible.volume)
    }

    @Test
    fun onlineAfterIncompatibleClearsTheProto() {
        val incompatible = reduceDashboardState(DashboardUiState(), TelemetryEvent.Incompatible(2))

        val online = reduceDashboardState(incompatible, TelemetryEvent.Online(sampleStatus()))

        assertEquals(ConnectionState.Online, online.connection)
        assertNull(online.incompatibleProto)
    }

    @Test
    fun aControlVolumeSurvivesOneTelemetryFrameThenTelemetryWins() {
        val online = reduceDashboardState(DashboardUiState(), TelemetryEvent.Online(sampleStatus()))

        val afterSet = reduceVolumeResult(online, ControlResult.Ok(VolumeState(percent = 30, muted = false, ok = true)))
        assertEquals(30, afterSet.volume?.percent)
        assertTrue(afterSet.volumeFromControl)

        // The first frame after the command could have been generated before it: it is ignored.
        val firstFrame = reduceDashboardState(afterSet, TelemetryEvent.Online(sampleStatus()))
        assertEquals(30, firstFrame.volume?.percent)
        assertFalse(firstFrame.volumeFromControl)

        // The next frame is authoritative.
        val secondFrame = reduceDashboardState(firstFrame, TelemetryEvent.Online(sampleStatus()))
        assertEquals(70, secondFrame.volume?.percent)
    }

    @Test
    fun beginActionSetsPendingAndClearsTheMessage() {
        val withMessage = DashboardUiState(message = UiMessage(5, ControlMessage.Io), messageSeq = 5)

        val pending = beginAction(withMessage, PendingAction.Volume)

        assertEquals(PendingAction.Volume, pending.pending)
        assertNull(pending.message)
    }

    @Test
    fun volumeSuccessClearsPendingAndUsesTheReply() {
        val pending = beginAction(DashboardUiState(), PendingAction.Volume)

        val state = reduceVolumeResult(pending, ControlResult.Ok(VolumeState(percent = 25, muted = false, ok = true)))

        assertNull(state.pending)
        assertEquals(25, state.volume?.percent)
        assertTrue(state.volumeFromControl)
        assertEquals(ControlMessage.VolumeChanged, state.message?.message)
    }

    @Test
    fun volumeFailureSetsTheMessageAndClearsPending() {
        val pending = beginAction(DashboardUiState(), PendingAction.Volume)

        val state = reduceVolumeResult(pending, ControlResult.Io("timeout"))

        assertNull(state.pending)
        assertEquals(ControlMessage.Io, state.message?.message)
    }

    @Test
    fun unauthorizedRaisesNeedsToken() {
        val state = reduceVolumeResult(DashboardUiState(), ControlResult.Unauthorized)

        assertTrue(state.needsToken)
        assertEquals(ControlMessage.Unauthorized, state.message?.message)
        assertNull(state.pending)
    }

    @Test
    fun aLaterSuccessClearsNeedsToken() {
        val flagged = DashboardUiState(needsToken = true)

        val state = reduceRuntimeResult(
            flagged,
            ControlResult.Ok(RuntimeState(running = true, sink = SinkState.Ready)),
        )

        assertFalse(state.needsToken)
        assertEquals(ControlMessage.RuntimeStarted, state.message?.message)
    }

    @Test
    fun muteMessageReflectsTheResultingState() {
        val muted = reduceMuteResult(
            DashboardUiState(),
            ControlResult.Ok(VolumeState(percent = 50, muted = true, ok = true)),
        )
        assertEquals(ControlMessage.Muted, muted.message?.message)

        val unmuted = reduceMuteResult(
            DashboardUiState(),
            ControlResult.Ok(VolumeState(percent = 50, muted = false, ok = true)),
        )
        assertEquals(ControlMessage.Unmuted, unmuted.message?.message)
    }

    @Test
    fun runtimeMessageReflectsTheResultingState() {
        val stopped = reduceRuntimeResult(
            DashboardUiState(),
            ControlResult.Ok(RuntimeState(running = false, sink = SinkState.Absent)),
        )
        assertEquals(ControlMessage.RuntimeStopped, stopped.message?.message)
    }

    @Test
    fun messageIdsIncreaseSoTheUiShowsEachOne() {
        val first = reduceVolumeResult(
            DashboardUiState(),
            ControlResult.Ok(VolumeState(percent = 10, muted = false, ok = true)),
        )
        val second = reduceVolumeResult(
            first,
            ControlResult.Ok(VolumeState(percent = 20, muted = false, ok = true)),
        )

        val firstId = first.message?.id ?: 0
        val secondId = second.message?.id ?: 0
        assertTrue(secondId > firstId)
    }
}
