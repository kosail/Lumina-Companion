package com.korealm.lumina.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.korealm.lumina.protocol.DayNight
import com.korealm.lumina.protocol.SinkState
import com.korealm.lumina.ui.ConnectionState
import kotlin.math.roundToInt
import lumina.shared.generated.resources.Res
import lumina.shared.generated.resources.action_start
import lumina.shared.generated.resources.action_stop
import lumina.shared.generated.resources.audio_not_ready
import lumina.shared.generated.resources.conn_connecting
import lumina.shared.generated.resources.conn_incompatible
import lumina.shared.generated.resources.conn_offline
import lumina.shared.generated.resources.conn_online
import lumina.shared.generated.resources.day_day
import lumina.shared.generated.resources.day_night
import lumina.shared.generated.resources.day_unknown
import lumina.shared.generated.resources.label_audio
import lumina.shared.generated.resources.label_daynight
import lumina.shared.generated.resources.label_faces
import lumina.shared.generated.resources.label_load
import lumina.shared.generated.resources.label_memory
import lumina.shared.generated.resources.label_muted
import lumina.shared.generated.resources.label_running
import lumina.shared.generated.resources.label_speed
import lumina.shared.generated.resources.label_temperature
import lumina.shared.generated.resources.label_uptime
import lumina.shared.generated.resources.label_volume
import lumina.shared.generated.resources.section_control
import lumina.shared.generated.resources.section_device
import lumina.shared.generated.resources.section_performance
import lumina.shared.generated.resources.section_sensors
import lumina.shared.generated.resources.sink_absent
import lumina.shared.generated.resources.sink_ready
import lumina.shared.generated.resources.sink_waiting
import lumina.shared.generated.resources.token_warning_body
import lumina.shared.generated.resources.token_warning_title
import lumina.shared.generated.resources.value_no
import lumina.shared.generated.resources.value_unknown
import lumina.shared.generated.resources.value_yes
import org.jetbrains.compose.resources.stringResource
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * The dashboard tab: a color-coded connection banner, read-only status cards, and the volume/mute
 * and runtime controls.
 *
 * Responsibility: render [DashboardUiState] and emit [DashboardActions] only — no business logic
 * (FE-INV-051). Designed to FE-INV-026: generous spacing, large type, visible labels, color-coded
 * sections and an announced connection state (FE-INV-010).
 *
 * Controls are disabled while offline ([DashboardUiState.status] is `null`), because every command
 * needs the agent; a successful `volume.set` is still allowed when the mixer value is unknown, per
 * the resilience matrix (contract §9).
 *
 * Unit symbols (`%`, `°C`, `MB`, `s`, `FPS`) are treated as data formatting, not translatable text;
 * every label/state/message comes from `strings.xml` (FE-INV-041).
 *
 * @param state the current dashboard state.
 * @param actions the user intents.
 * @param modifier layout modifier from the caller (the scaffold content padding).
 */
@Composable
fun DashboardScreen(
    state: DashboardUiState,
    actions: DashboardActions,
    modifier: Modifier = Modifier,
) {
    val online = state.status != null
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ConnectionBanner(state)
        if (state.needsToken) TokenWarning()

        val status = state.status ?: return@Column

        SmallTitle(text = stringResource(Res.string.section_device))
        Card(modifier = Modifier.fillMaxWidth()) {
            InfoRow(
                label = stringResource(Res.string.label_running),
                value = if (status.runtime.running) {
                    stringResource(Res.string.value_yes)
                } else {
                    stringResource(Res.string.value_no)
                },
            )
            HorizontalDivider()
            InfoRow(label = stringResource(Res.string.label_audio), value = sinkLabel(status.runtime.sink))
            HorizontalDivider()
            InfoRow(
                label = stringResource(Res.string.label_faces),
                value = status.runtime.faceCount.toString(),
            )
            HorizontalDivider()
            InfoRow(
                label = stringResource(Res.string.label_uptime),
                value = "${status.runtime.uptimeSeconds} s",
            )
        }

        SmallTitle(text = stringResource(Res.string.section_control))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Button(
                    onClick = actions.onRuntimeToggle,
                    enabled = online && state.pending == null,
                    colors = ButtonDefaults.buttonColorsPrimary(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(
                            if (status.runtime.running) Res.string.action_stop else Res.string.action_start,
                        ),
                    )
                }
                if (status.runtime.sink != SinkState.Ready) {
                    Text(
                        text = stringResource(Res.string.audio_not_ready),
                        style = MiuixTheme.textStyles.body1,
                        color = MiuixTheme.colorScheme.error,
                    )
                }
            }
        }

        SmallTitle(text = stringResource(Res.string.section_performance))
        Card(modifier = Modifier.fillMaxWidth()) {
            InfoRow(
                label = stringResource(Res.string.label_speed),
                value = "${oneDecimal(status.core.fps)} FPS",
            )
            HorizontalDivider()
            InfoRow(
                label = stringResource(Res.string.label_memory),
                value = "${oneDecimal(status.core.rssMb)} MB",
            )
            HorizontalDivider()
            InfoRow(
                label = stringResource(Res.string.label_temperature),
                value = status.core.tempC?.let { "${oneDecimal(it)} °C" }
                    ?: stringResource(Res.string.value_unknown),
            )
            HorizontalDivider()
            InfoRow(
                label = stringResource(Res.string.label_load),
                value = status.core.load1?.let { twoDecimals(it) }
                    ?: stringResource(Res.string.value_unknown),
            )
        }

        SmallTitle(text = stringResource(Res.string.section_sensors))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(Res.string.label_volume),
                        style = MiuixTheme.textStyles.body1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                    Text(
                        text = state.volume?.percent?.let { "$it %" }
                            ?: stringResource(Res.string.value_unknown),
                        style = MiuixTheme.textStyles.title4,
                        color = MiuixTheme.colorScheme.onSurfaceContainer,
                    )
                }
                // `steps = 99` gives the 101 discrete values 0..100; `onValueChange` echoes into state
                // immediately (see the view model) so the thumb tracks the gesture.
                Slider(
                    value = (state.volume?.percent ?: 0) / 100f,
                    onValueChange = { actions.onVolumeChange((it * 100).roundToInt()) },
                    onValueChangeFinished = actions.onVolumeChangeFinished,
                    valueRange = 0f..1f,
                    steps = 99,
                    enabled = online,
                )
            }
            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.label_muted),
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                Switch(
                    checked = state.volume?.muted ?: false,
                    onCheckedChange = actions.onMuteToggle,
                    enabled = online && state.pending == null,
                )
            }
            HorizontalDivider()
            InfoRow(
                label = stringResource(Res.string.label_daynight),
                value = dayNightLabel(status.sensors.dayNight),
            )
        }
    }
}

/**
 * The connection banner. Color-coded (green-ish for online, red for offline) and marked as a polite
 * `liveRegion` so TalkBack announces connection changes without the user focusing it (FE-INV-010).
 */
@Composable
private fun ConnectionBanner(state: DashboardUiState) {
    val colors = MiuixTheme.colorScheme
    val label = when (state.connection) {
        ConnectionState.Connecting -> stringResource(Res.string.conn_connecting)
        ConnectionState.Online -> stringResource(Res.string.conn_online)
        ConnectionState.Offline -> stringResource(Res.string.conn_offline)
        ConnectionState.Incompatible -> stringResource(Res.string.conn_incompatible)
    }
    val container = when (state.connection) {
        ConnectionState.Connecting -> colors.secondaryContainer
        ConnectionState.Online -> colors.tertiaryContainer
        ConnectionState.Offline, ConnectionState.Incompatible -> colors.errorContainer
    }
    val onContainer = when (state.connection) {
        ConnectionState.Connecting -> colors.onSecondaryContainer
        ConnectionState.Online -> colors.onTertiaryContainer
        ConnectionState.Offline, ConnectionState.Incompatible -> colors.onErrorContainer
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        colors = CardDefaults.defaultColors(color = container, contentColor = onContainer),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
            style = MiuixTheme.textStyles.title3,
            color = onContainer,
        )
    }
}

/**
 * Persistent warning shown after an `unauthorized` reply: the token is wrong and no control command
 * will work until it is fixed in Ajustes (contract §4.9, §9). Kept visible (not a transient snackbar)
 * because it requires a user action; announced via `liveRegion`.
 */
@Composable
private fun TokenWarning() {
    val colors = MiuixTheme.colorScheme
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        colors = CardDefaults.defaultColors(
            color = colors.errorContainer,
            contentColor = colors.onErrorContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(Res.string.token_warning_title),
                style = MiuixTheme.textStyles.title4,
                color = colors.onErrorContainer,
            )
            Text(
                text = stringResource(Res.string.token_warning_body),
                style = MiuixTheme.textStyles.body1,
                color = colors.onErrorContainer,
            )
        }
    }
}

/** One label/value row inside a card. Large value text per FE-INV-026. */
@Composable
private fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MiuixTheme.textStyles.body1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        if (value.isNotEmpty()) {
            Text(
                text = value,
                style = MiuixTheme.textStyles.title4,
                color = MiuixTheme.colorScheme.onSurfaceContainer,
            )
        }
    }
}

/** Maps the audio sink state to a Spanish label. */
@Composable
private fun sinkLabel(sink: SinkState): String = when (sink) {
    SinkState.Ready -> stringResource(Res.string.sink_ready)
    SinkState.Waiting -> stringResource(Res.string.sink_waiting)
    SinkState.Absent -> stringResource(Res.string.sink_absent)
    SinkState.Unknown -> stringResource(Res.string.value_unknown)
}

/** Maps the day/night state to a Spanish label. */
@Composable
private fun dayNightLabel(dayNight: DayNight): String = when (dayNight) {
    DayNight.Day -> stringResource(Res.string.day_day)
    DayNight.Night -> stringResource(Res.string.day_night)
    DayNight.Unknown -> stringResource(Res.string.day_unknown)
}

/** Formats a double with one decimal without JVM-only APIs (common code). */
private fun oneDecimal(value: Double): String = ((value * 10).roundToInt() / 10.0).toString()

/** Formats a double with two decimals without JVM-only APIs (common code). */
private fun twoDecimals(value: Double): String = ((value * 100).roundToInt() / 100.0).toString()
