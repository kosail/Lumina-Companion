package com.korealm.lumina.ui.dashboard

import com.korealm.lumina.protocol.ControlResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pure tests for [failureMessage] and [ControlMessage.isError] — the control-result → message mapping
 * (no coroutines, no Android).
 */
class DashboardMessagesTest {

    @Test
    fun successHasNoFailureMessage() {
        assertNull(failureMessage(ControlResult.Ok(Unit)))
    }

    @Test
    fun mapsEveryErrorCodeToItsMessage() {
        assertEquals(ControlMessage.Unauthorized, failureMessage(ControlResult.Unauthorized))
        assertEquals(ControlMessage.Busy, failureMessage(ControlResult.Busy))
        assertEquals(ControlMessage.BadRequest, failureMessage(ControlResult.BadRequest("bad")))
        assertEquals(ControlMessage.Internal, failureMessage(ControlResult.Internal("boom")))
        assertEquals(ControlMessage.Io, failureMessage(ControlResult.Io("timeout")))
    }

    @Test
    fun failureMessagesAreFlaggedAsErrors() {
        assertTrue(ControlMessage.Unauthorized.isError)
        assertTrue(ControlMessage.Busy.isError)
        assertTrue(ControlMessage.BadRequest.isError)
        assertTrue(ControlMessage.Internal.isError)
        assertTrue(ControlMessage.Io.isError)
        assertTrue(ControlMessage.RuntimeStartUnconfirmed.isError)
        assertTrue(ControlMessage.RuntimeStartFailed.isError)
        assertTrue(ControlMessage.RuntimeStopFailed.isError)
    }

    @Test
    fun confirmationsAreNotErrors() {
        assertFalse(ControlMessage.VolumeChanged.isError)
        assertFalse(ControlMessage.Muted.isError)
        assertFalse(ControlMessage.Unmuted.isError)
        assertFalse(ControlMessage.RuntimeStarted.isError)
        assertFalse(ControlMessage.RuntimeStopped.isError)
    }
}
