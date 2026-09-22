package com.korealm.lumina.ui.dashboard

import com.korealm.lumina.protocol.DeviceStatus
import com.korealm.lumina.protocol.VolumeState
import com.korealm.lumina.ui.ConnectionState

/** A control command that is currently in flight, used to disable the matching control. */
enum class PendingAction {
    Volume,
    Mute,
    Runtime,
}

/**
 * Immutable UI state for the dashboard (FE-INV-051).
 *
 * Responsibility: a single snapshot the dashboard composable renders. [status] is the last valid
 * frame, or `null` when offline/connecting so stale values are never shown as live (FE-INV-031).
 *
 * @param connection current connection state.
 * @param status last valid device status, or `null`.
 * @param incompatibleProto the device's `proto` when [connection] is [ConnectionState.Incompatible].
 * @param volume the volume/mute the UI shows. It is normally reseeded from telemetry, but a
 *   successful control reply takes precedence for one telemetry frame so a just-commanded value is
 *   not overwritten by an older in-flight frame ([volumeFromControl]).
 * @param volumeFromControl true when [volume] came from a control reply (or an optimistic echo) and
 *   the next telemetry frame should be ignored; see [reduceDashboardState].
 * @param pending the control command in flight, or `null`.
 * @param message the last action outcome to announce once, or `null`.
 * @param messageSeq monotonic counter feeding [UiMessage.id]; never shown.
 * @param needsToken true after an `unauthorized` reply until a later command succeeds; drives the
 *   persistent "fix the token" card.
 */
data class DashboardUiState(
    val connection: ConnectionState = ConnectionState.Connecting,
    val status: DeviceStatus? = null,
    val incompatibleProto: Int? = null,
    val volume: VolumeState? = null,
    val volumeFromControl: Boolean = false,
    val pending: PendingAction? = null,
    val message: UiMessage? = null,
    val messageSeq: Long = 0L,
    val needsToken: Boolean = false,
)
