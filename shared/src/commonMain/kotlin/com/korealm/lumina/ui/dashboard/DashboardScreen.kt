package com.korealm.lumina.ui.dashboard

import androidx.compose.foundation.layout.*
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
import com.korealm.lumina.ui.components.InfoRow
import com.korealm.lumina.ui.components.SectionTitle
import com.korealm.lumina.ui.components.TokenWarning
import lumina.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Check
import top.yukonga.miuix.kmp.icon.extended.*
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.roundToInt

/**
 * The dashboard tab: a color-coded connection banner, read-only status cards, and the volume/mute
 * and runtime controls.
 *
 * Responsibility: render [DashboardUiState] and emit [DashboardActions] only — no business logic
 * (FE-INV-051). Designed to FE-INV-026: generous spacing, large type, visible labels and a
 * decorative MiuiX icon on every section, row and control so low-vision users can navigate by
 * shape/icon, while TalkBack users rely on the labels (icons are `contentDescription = null`).
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

        SectionTitle(icon = MiuixIcons.Phone, text = stringResource(Res.string.section_device))
        Card(modifier = Modifier.fillMaxWidth()) {
            InfoRow(
                label = stringResource(Res.string.label_running),
                value = if (status.runtime.running) {
                    stringResource(Res.string.value_yes)
                } else {
                    stringResource(Res.string.value_no)
                },
                icon = MiuixIcons.Play,
            )
            HorizontalDivider()
            InfoRow(
                label = stringResource(Res.string.label_audio),
                value = sinkLabel(status.runtime.sink),
                icon = MiuixIcons.VolumeUp,
            )
            HorizontalDivider()
            InfoRow(
                label = stringResource(Res.string.label_faces),
                value = status.runtime.faceCount.toString(),
                icon = MiuixIcons.ContactsCircle,
            )
            HorizontalDivider()
            InfoRow(
                label = stringResource(Res.string.label_uptime),
                value = "${status.runtime.uptimeSeconds} s",
                icon = MiuixIcons.Timer,
            )
        }

        SectionTitle(icon = MiuixIcons.Tune, text = stringResource(Res.string.section_control))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // The button reflects the device's initializing/running state plus any settling
                // transition, and is disabled while starting/stopping or during an enrollment
                // (which stops the runtime itself) — CHG-FE-0034/0035.
                val buttonState = runtimeButtonState(state)
                val transitioning = buttonState == RuntimeButtonState.Starting ||
                    buttonState == RuntimeButtonState.Stopping
                Button(
                    onClick = actions.onRuntimeToggle,
                    enabled = online && state.pending == null && !transitioning && !status.enroll.active,
                    colors = ButtonDefaults.buttonColorsPrimary(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = when (buttonState) {
                            RuntimeButtonState.Stop, RuntimeButtonState.Stopping -> MiuixIcons.Pause
                            RuntimeButtonState.Start, RuntimeButtonState.Starting -> MiuixIcons.Play
                        },
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(
                            when (buttonState) {
                                RuntimeButtonState.Starting -> Res.string.action_starting
                                RuntimeButtonState.Stopping -> Res.string.action_stopping
                                RuntimeButtonState.Stop -> Res.string.action_stop
                                RuntimeButtonState.Start -> Res.string.action_start
                            },
                        ),
                    )
                }
                if (status.runtime.sink != SinkState.Ready) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = MiuixIcons.VolumeOff,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MiuixTheme.colorScheme.error,
                        )
                        Text(
                            text = stringResource(Res.string.audio_not_ready),
                            style = MiuixTheme.textStyles.body1,
                            color = MiuixTheme.colorScheme.error,
                        )
                    }
                }
            }
        }

        SectionTitle(icon = MiuixIcons.Stopwatch, text = stringResource(Res.string.section_performance))
        Card(modifier = Modifier.fillMaxWidth()) {
            InfoRow(
                label = stringResource(Res.string.label_speed),
                value = "${oneDecimal(status.core.fps)} FPS",
                icon = MiuixIcons.Update,
            )
            HorizontalDivider()
            InfoRow(
                label = stringResource(Res.string.label_memory),
                value = "${oneDecimal(status.core.rssMb)} MB",
                icon = MiuixIcons.Layers,
            )
            HorizontalDivider()
            InfoRow(
                label = stringResource(Res.string.label_temperature),
                value = status.core.tempC?.let { "${oneDecimal(it)} °C" }
                    ?: stringResource(Res.string.value_unknown),
                icon = MiuixIcons.Info,
            )
            HorizontalDivider()
            InfoRow(
                label = stringResource(Res.string.label_load),
                value = status.core.load1?.let { twoDecimals(it) }
                    ?: stringResource(Res.string.value_unknown),
                icon = MiuixIcons.Tune,
            )
        }

        SectionTitle(icon = MiuixIcons.Scan, text = stringResource(Res.string.section_sensors))
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
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = MiuixIcons.VolumeUp,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                        Text(
                            text = stringResource(Res.string.label_volume),
                            style = MiuixTheme.textStyles.body1,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = MiuixIcons.VolumeOff,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                    Text(
                        text = stringResource(Res.string.label_muted),
                        style = MiuixTheme.textStyles.body1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
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
                icon = MiuixIcons.Theme,
            )
        }
    }
}

/**
 * The connection banner. Color-coded (green-ish for online, red for offline) with a matching
 * decorative icon; the label is a polite `liveRegion` so TalkBack announces connection changes
 * without the user focusing it (FE-INV-010). The `liveRegion` sits on the changing text node, not on
 * the card (CHG-FE-0024).
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
    val icon = when (state.connection) {
        ConnectionState.Connecting -> MiuixIcons.Reset
        ConnectionState.Online -> MiuixIcons.Basic.Check
        ConnectionState.Offline -> MiuixIcons.Close
        ConnectionState.Incompatible -> MiuixIcons.Info
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
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(color = container, contentColor = onContainer),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(26.dp),
                tint = onContainer,
            )
            Text(
                text = label,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = MiuixTheme.textStyles.title3,
                color = onContainer,
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
