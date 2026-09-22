package com.korealm.lumina.ui.people

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import com.korealm.lumina.ui.ConnectionState
import com.korealm.lumina.ui.components.InfoRow
import com.korealm.lumina.ui.components.SectionTitle
import com.korealm.lumina.ui.components.TokenWarning
import com.korealm.lumina.ui.picker.rememberPhotoPicker
import lumina.shared.generated.resources.Res
import lumina.shared.generated.resources.action_start
import lumina.shared.generated.resources.conn_connecting
import lumina.shared.generated.resources.conn_incompatible
import lumina.shared.generated.resources.conn_offline
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
import org.jetbrains.compose.resources.stringResource
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TextFieldDefaults
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Check
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Contacts
import top.yukonga.miuix.kmp.icon.extended.ContactsCircle
import top.yukonga.miuix.kmp.icon.extended.Image
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Photos
import top.yukonga.miuix.kmp.icon.extended.Play
import top.yukonga.miuix.kmp.icon.extended.Recording
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Reset
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * The Personas tab: the enrolled-people list with a refresh action, and an inline "Agregar persona"
 * card that starts a camera or photo enrollment.
 *
 * Responsibility: render [PeopleUiState] and emit [PeopleActions] only — no business logic
 * (FE-INV-051). Designed to FE-INV-026: one purpose, generous spacing, large type, visible labels and
 * a decorative MiuiX icon on every section, row and control (icons are `contentDescription = null`,
 * so TalkBack relies on the labels — FE-INV-010).
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

        SectionTitle(icon = MiuixIcons.ContactsCircle, text = stringResource(Res.string.section_people))
        Card(modifier = Modifier.fillMaxWidth()) {
            if (state.people.isEmpty()) {
                InfoRow(
                    label = stringResource(Res.string.people_empty),
                    value = "",
                    icon = MiuixIcons.Contacts,
                )
            } else {
                state.people.forEachIndexed { index, name ->
                    if (index > 0) HorizontalDivider()
                    InfoRow(label = name, value = "", icon = MiuixIcons.Contacts)
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
                Icon(
                    imageVector = MiuixIcons.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(Res.string.people_refresh))
            }
        }

        SectionTitle(icon = MiuixIcons.Add, text = stringResource(Res.string.people_add_section))
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
                    Icon(
                        imageVector = MiuixIcons.Image,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(Res.string.people_add_camera))
                }
                Button(
                    onClick = pickPhotos,
                    enabled = online && !busy,
                    colors = ButtonDefaults.buttonColorsPrimary(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = MiuixIcons.Photos,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
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

/** The "add person" name input with an inline error and a leading icon, matching the settings fields. */
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
            leadingIcon = {
                Icon(
                    imageVector = MiuixIcons.Contacts,
                    contentDescription = null,
                    // MiuiX pads only the text box, so the icon slot must carry its own spacing:
                    // 16 dp from the border (the field's content margin) and 8 dp before the text.
                    modifier = Modifier
                        .padding(start = TextFieldDefaults.InsideMargin.width, end = 8.dp)
                        .size(20.dp),
                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            },
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
 * per-frame counter is not (announcing every frame would drown the screen reader). The recording icon
 * is decorative (FE-INV-026).
 */
@Composable
private fun EnrollmentCard(state: PeopleUiState, actions: PeopleActions) {
    val colors = MiuixTheme.colorScheme
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = MiuixIcons.Recording,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = colors.onSurfaceContainer,
                )
                Text(
                    text = phaseLabel(state.phase),
                    style = MiuixTheme.textStyles.title4,
                    color = colors.onSurfaceContainer,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
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
                // silent (FE-INV-010). Off-tab announcements are handled by the shell's
                // EnrollmentStatusLine (CHG-FE-0021); per-frame progress is intentionally not announced.
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
                Icon(
                    imageVector = MiuixIcons.Close,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
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
                Icon(
                    imageVector = MiuixIcons.Play,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(Res.string.action_start))
            }
        }
    }
}

/** Color-coded connection banner with a matching decorative icon; its label is a `liveRegion`. */
@Composable
private fun ConnectionBanner(connection: ConnectionState) {
    val colors = MiuixTheme.colorScheme
    val label = when (connection) {
        ConnectionState.Connecting -> stringResource(Res.string.conn_connecting)
        ConnectionState.Online -> ""
        ConnectionState.Offline -> stringResource(Res.string.conn_offline)
        ConnectionState.Incompatible -> stringResource(Res.string.conn_incompatible)
    }
    val icon = when (connection) {
        ConnectionState.Connecting -> MiuixIcons.Reset
        ConnectionState.Online -> MiuixIcons.Basic.Check
        ConnectionState.Offline -> MiuixIcons.Close
        ConnectionState.Incompatible -> MiuixIcons.Info
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
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(color = container, contentColor = onContainer),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = onContainer,
            )
            Text(
                text = label,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = MiuixTheme.textStyles.title4,
                color = onContainer,
            )
        }
    }
}
