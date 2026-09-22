package com.korealm.lumina.ui.people

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.korealm.lumina.protocol.EnrollPhase
import com.korealm.lumina.ui.ConnectionState
import com.korealm.lumina.ui.picker.rememberPhotoPicker
import lumina.shared.generated.resources.Res
import lumina.shared.generated.resources.action_start
import lumina.shared.generated.resources.conn_connecting
import lumina.shared.generated.resources.conn_incompatible
import lumina.shared.generated.resources.conn_offline
import lumina.shared.generated.resources.enroll_phase_capturing
import lumina.shared.generated.resources.enroll_phase_starting
import lumina.shared.generated.resources.enroll_phase_stopping
import lumina.shared.generated.resources.enroll_progress
import lumina.shared.generated.resources.people_add_camera
import lumina.shared.generated.resources.people_add_photos
import lumina.shared.generated.resources.people_add_section
import lumina.shared.generated.resources.people_cancel
import lumina.shared.generated.resources.people_empty
import lumina.shared.generated.resources.people_name_error
import lumina.shared.generated.resources.people_name_label
import lumina.shared.generated.resources.people_no_face
import lumina.shared.generated.resources.people_refresh
import lumina.shared.generated.resources.people_runtime_down
import lumina.shared.generated.resources.people_title
import lumina.shared.generated.resources.section_people
import lumina.shared.generated.resources.token_warning_body
import lumina.shared.generated.resources.token_warning_title
import org.jetbrains.compose.resources.stringResource
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TextFieldDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * The Personas tab: the enrolled-people list with a refresh action, and an inline "Agregar persona"
 * card that starts a camera or photo enrollment.
 *
 * Responsibility: render [PeopleUiState] and emit [PeopleActions] only — no business logic
 * (FE-INV-051). Designed to FE-INV-026: one purpose, generous spacing, large type, visible labels.
 * Talks never depend on color alone: state is always written out, and the connection state and
 * enrollment phase are `liveRegion`s (FE-INV-010).
 *
 * The photo picker is a documented platform fallback (FE-INV-025): Android uses the system Photo
 * Picker, desktop a file chooser; both hide behind [rememberPhotoPicker].
 *
 * @param state the current people state.
 * @param actions the user intents.
 * @param modifier layout modifier from the caller (the scaffold content padding).
 */
@Composable
fun PeopleScreen(
    state: PeopleUiState,
    actions: PeopleActions,
    modifier: Modifier = Modifier,
) {
    val online = state.connection == ConnectionState.Online
    val busy = state.enrolling || state.pending != null
    val pickPhotos = rememberPhotoPicker(actions.onImagesPicked)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(Res.string.people_title),
            style = MiuixTheme.textStyles.title1,
            color = MiuixTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )

        if (!online) ConnectionBanner(state.connection)
        if (state.needsToken) TokenWarning()

        SmallTitle(text = stringResource(Res.string.section_people))
        Card(modifier = Modifier.fillMaxWidth()) {
            if (state.people.isEmpty()) {
                InfoRow(label = stringResource(Res.string.people_empty), value = "")
            } else {
                state.people.forEachIndexed { index, name ->
                    if (index > 0) HorizontalDivider()
                    InfoRow(label = name, value = "")
                }
            }
            HorizontalDivider()
            Button(
                onClick = actions.onRefresh,
                enabled = online && !busy,
                colors = ButtonDefaults.buttonColorsPrimary(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                Text(stringResource(Res.string.people_refresh))
            }
        }

        SmallTitle(text = stringResource(Res.string.people_add_section))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                NameField(state = state, actions = actions, enabled = online && !state.enrolling)
                Button(
                    onClick = actions.onEnrollFromCamera,
                    enabled = online && !busy,
                    colors = ButtonDefaults.buttonColorsPrimary(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(Res.string.people_add_camera))
                }
                Button(
                    onClick = pickPhotos,
                    enabled = online && !busy,
                    colors = ButtonDefaults.buttonColorsPrimary(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(Res.string.people_add_photos))
                }
            }
        }

        if (state.enrolling) {
            EnrollmentCard(state = state, actions = actions)
        }
        if (state.canRestartRuntime && !state.enrolling) {
            RuntimeRecoveryCard(state = state, actions = actions)
        }
    }
}

/** The "add person" name input with an inline error, matching the settings fields. */
@Composable
private fun NameField(
    state: PeopleUiState,
    actions: PeopleActions,
    enabled: Boolean,
) {
    val defaultColors = TextFieldDefaults.textFieldColors()
    val errorColors = TextFieldDefaults.textFieldColors(
        labelColor = MiuixTheme.colorScheme.error,
        borderColor = MiuixTheme.colorScheme.error,
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TextField(
            value = state.name,
            onValueChange = actions.onNameChange,
            label = stringResource(Res.string.people_name_label),
            singleLine = true,
            enabled = enabled,
            colors = if (state.nameError) errorColors else defaultColors,
        )
        if (state.nameError) {
            Text(
                text = stringResource(Res.string.people_name_error),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.error,
                modifier = Modifier.padding(start = 16.dp),
            )
        }
    }
}

/**
 * Enrollment progress. The phase label is a `liveRegion` so a phase change is announced, while the
 * per-frame counter is not (announcing every frame would drown the screen reader).
 */
@Composable
private fun EnrollmentCard(state: PeopleUiState, actions: PeopleActions) {
    val colors = MiuixTheme.colorScheme
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = phaseLabel(state.phase),
                style = MiuixTheme.textStyles.title4,
                color = colors.onSurfaceContainer,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            LinearProgressIndicator(
                progress = progressFraction(state.captured, state.total),
                modifier = Modifier.fillMaxWidth(),
            )
            val captured = state.captured
            val total = state.total
            if (captured != null && total != null) {
                Text(
                    text = stringResource(Res.string.enroll_progress, captured, total),
                    style = MiuixTheme.textStyles.body1,
                    color = colors.onSurfaceVariantSummary,
                )
            }
            if (state.noFaceNotice) {
                // Keyed on the transition counter so each *new* notice (re)creates a live-region
                // node and is announced once; consecutive notice frames keep the same key and stay
                // silent (FE-INV-010).
                // TODO(ui): full enrollment-announcement overhaul (progress + off-tab) — FE-INV-010 / CHG-FE-0017.
                key(state.noFaceNoticeSeq) {
                    Text(
                        text = stringResource(Res.string.people_no_face),
                        style = MiuixTheme.textStyles.body1,
                        color = colors.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
            }
            Button(
                onClick = actions.onCancelEnrollment,
                enabled = state.pending != PendingPeopleAction.Cancel,
                colors = ButtonDefaults.buttonColorsPrimary(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.people_cancel))
            }
        }
    }
}

/** Recovery card shown when a failed enrollment left the runtime down (contract §6.5). */
@Composable
private fun RuntimeRecoveryCard(state: PeopleUiState, actions: PeopleActions) {
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
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(Res.string.people_runtime_down),
                style = MiuixTheme.textStyles.body1,
                color = colors.onErrorContainer,
            )
            Button(
                onClick = actions.onStartRuntime,
                enabled = state.pending == null,
                colors = ButtonDefaults.buttonColorsPrimary(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.action_start))
            }
        }
    }
}

/** Color-coded connection banner for the people screen; announced as a polite `liveRegion`. */
@Composable
private fun ConnectionBanner(connection: ConnectionState) {
    val colors = MiuixTheme.colorScheme
    val label = when (connection) {
        ConnectionState.Connecting -> stringResource(Res.string.conn_connecting)
        ConnectionState.Online -> ""
        ConnectionState.Offline -> stringResource(Res.string.conn_offline)
        ConnectionState.Incompatible -> stringResource(Res.string.conn_incompatible)
    }
    // "Conectando" is a neutral startup state, not a failure; only offline/incompatible are errors.
    val container = when (connection) {
        ConnectionState.Connecting -> colors.secondaryContainer
        ConnectionState.Online -> colors.tertiaryContainer
        ConnectionState.Offline, ConnectionState.Incompatible -> colors.errorContainer
    }
    val onContainer = when (connection) {
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
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            style = MiuixTheme.textStyles.title4,
            color = onContainer,
        )
    }
}

/** Persistent token warning, mirroring the dashboard's, shown after an `unauthorized` reply. */
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
private fun InfoRow(label: String, value: String, modifier: Modifier = Modifier) {
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

/** Maps the enrollment phase to a Spanish label. */
@Composable
private fun phaseLabel(phase: EnrollPhase?): String = when (phase) {
    EnrollPhase.StoppingRuntime -> stringResource(Res.string.enroll_phase_stopping)
    EnrollPhase.StartingRuntime -> stringResource(Res.string.enroll_phase_starting)
    EnrollPhase.Capturing, EnrollPhase.Unknown, null ->
        stringResource(Res.string.enroll_phase_capturing)
}

/**
 * The determinate progress in `0f..1f`, or `null` (indeterminate) when the frame counts are not
 * usable yet. Pure so the mapping is obvious and side-effect free.
 */
private fun progressFraction(captured: Int?, total: Int?): Float? {
    if (captured == null || total == null || total <= 0) return null
    return (captured.toFloat() / total).coerceIn(0f, 1f)
}
