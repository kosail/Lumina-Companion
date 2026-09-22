package com.korealm.lumina.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.korealm.lumina.ui.components.BrandHeader
import com.korealm.lumina.ui.dashboard.ControlMessage
import com.korealm.lumina.ui.dashboard.DashboardActions
import com.korealm.lumina.ui.dashboard.DashboardScreen
import com.korealm.lumina.ui.dashboard.DashboardUiState
import com.korealm.lumina.ui.people.EnrollmentStatusLine
import com.korealm.lumina.ui.people.PeopleActions
import com.korealm.lumina.ui.people.PeopleMessage
import com.korealm.lumina.ui.people.PeopleScreen
import com.korealm.lumina.ui.people.PeopleUiState
import com.korealm.lumina.ui.settings.SettingsActions
import com.korealm.lumina.ui.settings.SettingsScreen
import com.korealm.lumina.ui.settings.SettingsUiState
import lumina.shared.generated.resources.Res
import lumina.shared.generated.resources.msg_bad_request
import lumina.shared.generated.resources.msg_busy
import lumina.shared.generated.resources.msg_enroll_cancelled
import lumina.shared.generated.resources.msg_enroll_failed
import lumina.shared.generated.resources.msg_internal
import lumina.shared.generated.resources.msg_io
import lumina.shared.generated.resources.msg_muted
import lumina.shared.generated.resources.msg_need_name
import lumina.shared.generated.resources.msg_people_refreshed
import lumina.shared.generated.resources.msg_person_enrolled
import lumina.shared.generated.resources.msg_photos_not_prepared
import lumina.shared.generated.resources.msg_runtime_started
import lumina.shared.generated.resources.msg_runtime_stopped
import lumina.shared.generated.resources.msg_too_many_photos
import lumina.shared.generated.resources.msg_unauthorized
import lumina.shared.generated.resources.msg_unmuted
import lumina.shared.generated.resources.msg_volume_changed
import lumina.shared.generated.resources.tab_dashboard
import lumina.shared.generated.resources.tab_people
import lumina.shared.generated.resources.tab_settings
import org.jetbrains.compose.resources.stringResource
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SnackbarDuration
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Contacts
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** The three top-level tabs (PLAN §5.6). */
private enum class LuminaTab { Dashboard, People, Settings }

/**
 * The app shell: a MiuiX [Scaffold] with a 3-tab [NavigationBar] and a [SnackbarHost] for action
 * feedback.
 *
 * Responsibility: pure rendering + local UI-only state (the selected tab) — no business logic
 * (FE-INV-051). It renders the dashboard (live telemetry + controls), the Personas screen (people +
 * enrollment), and the Ajustes screen. Labels are always visible (FE-INV-026) and each item meets
 * the 64 dp MiuiX touch target (FE-INV-010).
 *
 * The shell owns the single [SnackbarHostState]: both the dashboard's and the people screen's
 * one-shot messages are shown here and then cleared. MiuiX `Snackbar` announces itself via
 * `liveRegion` (FE-INV-010). It also hosts the compact enrollment status line shown while an
 * enrollment runs on a non-Personas tab, so progress is announced wherever the user is (CHG-FE-0021).
 *
 * @param dashboardState the dashboard state, produced by its view model.
 * @param dashboardActions the dashboard's user intents.
 * @param peopleState the people state, produced by its view model.
 * @param peopleActions the people screen's user intents.
 * @param settingsState the settings state, produced by its view model.
 * @param settingsActions the settings' user intents.
 */
@Composable
fun AppRoot(
    dashboardState: DashboardUiState,
    dashboardActions: DashboardActions,
    peopleState: PeopleUiState,
    peopleActions: PeopleActions,
    settingsState: SettingsUiState,
    settingsActions: SettingsActions,
) {
    var selectedTab by remember { mutableStateOf(LuminaTab.Dashboard) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Resolve the dashboard message text in composition (stringResource is @Composable) and show it
    // once per message id; a new message restarts the effect and replaces the previous snackbar.
    val dashboardMessage = dashboardState.message
    val dashboardMessageText = dashboardMessage?.let { controlMessageText(it.message) }
    LaunchedEffect(dashboardMessage?.id) {
        if (dashboardMessageText != null) {
            snackbarHostState.showSnackbar(
                message = dashboardMessageText,
                withDismissAction = true,
                duration = SnackbarDuration.Long,
            )
            dashboardActions.onDismissMessage()
        }
    }

    // The people screen's messages share the same host, keyed on their own id.
    val peopleMessage = peopleState.message
    val peopleText = peopleMessage?.let { peopleMessageText(it.message) }
    LaunchedEffect(peopleMessage?.id) {
        if (peopleText != null) {
            snackbarHostState.showSnackbar(
                message = peopleText,
                withDismissAction = true,
                duration = SnackbarDuration.Long,
            )
            peopleActions.onDismissMessage()
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == LuminaTab.Dashboard,
                    onClick = { selectedTab = LuminaTab.Dashboard },
                    icon = MiuixIcons.Home,
                    label = stringResource(Res.string.tab_dashboard),
                )
                NavigationBarItem(
                    selected = selectedTab == LuminaTab.People,
                    onClick = { selectedTab = LuminaTab.People },
                    icon = MiuixIcons.Contacts,
                    label = stringResource(Res.string.tab_people),
                )
                NavigationBarItem(
                    selected = selectedTab == LuminaTab.Settings,
                    onClick = { selectedTab = LuminaTab.Settings },
                    icon = MiuixIcons.Settings,
                    label = stringResource(Res.string.tab_settings),
                )
            }
        },
        snackbarHost = { SnackbarHost(state = snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Branding shown on every tab; it sits above the scrollable screens so it stays put.
            BrandHeader()

            // Enrollment announcements must survive a tab switch (FE-INV-010): the Personas screen
            // owns the full card (with the cancel action), so the shell shows this compact live-region
            // line on every *other* tab while an enrollment runs.
            if (selectedTab != LuminaTab.People) {
                EnrollmentStatusLine(
                    state = peopleState,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                )
            }
            when (selectedTab) {
                LuminaTab.Dashboard -> DashboardScreen(
                    state = dashboardState,
                    actions = dashboardActions,
                    modifier = Modifier.weight(1f),
                )

                LuminaTab.People -> PeopleScreen(
                    state = peopleState,
                    actions = peopleActions,
                    modifier = Modifier.weight(1f),
                )

                LuminaTab.Settings -> SettingsScreen(
                    state = settingsState,
                    actions = settingsActions,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** Maps a dashboard control outcome to its Spanish text (FE-INV-041). */
@Composable
private fun controlMessageText(message: ControlMessage): String = when (message) {
    ControlMessage.VolumeChanged -> stringResource(Res.string.msg_volume_changed)
    ControlMessage.Muted -> stringResource(Res.string.msg_muted)
    ControlMessage.Unmuted -> stringResource(Res.string.msg_unmuted)
    ControlMessage.RuntimeStarted -> stringResource(Res.string.msg_runtime_started)
    ControlMessage.RuntimeStopped -> stringResource(Res.string.msg_runtime_stopped)
    ControlMessage.Unauthorized -> stringResource(Res.string.msg_unauthorized)
    ControlMessage.Busy -> stringResource(Res.string.msg_busy)
    ControlMessage.BadRequest -> stringResource(Res.string.msg_bad_request)
    ControlMessage.Internal -> stringResource(Res.string.msg_internal)
    ControlMessage.Io -> stringResource(Res.string.msg_io)
}

/** Maps a people action outcome to its Spanish text (FE-INV-041). */
@Composable
private fun peopleMessageText(message: PeopleMessage): String = when (message) {
    PeopleMessage.Refreshed -> stringResource(Res.string.msg_people_refreshed)
    PeopleMessage.PersonEnrolled -> stringResource(Res.string.msg_person_enrolled)
    PeopleMessage.EnrollFailed -> stringResource(Res.string.msg_enroll_failed)
    PeopleMessage.EnrollCancelled -> stringResource(Res.string.msg_enroll_cancelled)
    PeopleMessage.NeedName -> stringResource(Res.string.msg_need_name)
    PeopleMessage.TooManyPhotos -> stringResource(Res.string.msg_too_many_photos)
    PeopleMessage.PhotosNotPrepared -> stringResource(Res.string.msg_photos_not_prepared)
    PeopleMessage.Unauthorized -> stringResource(Res.string.msg_unauthorized)
    PeopleMessage.Busy -> stringResource(Res.string.msg_busy)
    PeopleMessage.BadRequest -> stringResource(Res.string.msg_bad_request)
    PeopleMessage.Internal -> stringResource(Res.string.msg_internal)
    PeopleMessage.Io -> stringResource(Res.string.msg_io)
    PeopleMessage.RuntimeStarted -> stringResource(Res.string.msg_runtime_started)
}

/** Renders the shell with empty state so it can be previewed without Koin or a device. */
@Preview
@Composable
private fun AppRootPreview() {
    MiuixTheme {
        AppRoot(
            dashboardState = DashboardUiState(),
            dashboardActions = DashboardActions({}, {}, {}, {}, {}),
            peopleState = PeopleUiState(),
            peopleActions = PeopleActions({}, {}, {}, {}, {}, {}, {}),
            settingsState = SettingsUiState(),
            settingsActions = SettingsActions({}, {}, {}, {}, {}, {}),
        )
    }
}
