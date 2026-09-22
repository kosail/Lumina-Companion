package com.korealm.lumina

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.korealm.lumina.ui.AppRoot
import com.korealm.lumina.ui.dashboard.DashboardActions
import com.korealm.lumina.ui.dashboard.DashboardViewModel
import com.korealm.lumina.ui.people.PeopleActions
import com.korealm.lumina.ui.people.PeopleViewModel
import com.korealm.lumina.ui.settings.SettingsActions
import com.korealm.lumina.ui.settings.SettingsViewModel
import com.korealm.lumina.ui.theme.AppTheme
import org.koin.compose.koinInject

/**
 * Shared entry composable used by both the Android and desktop apps.
 *
 * Responsibility: install the theme, resolve the view models from Koin, and render [AppRoot] with
 * their state and intents. It contains no business logic (FE-INV-051).
 *
 * Kotlin note (Koin): `koinInject<T>()` is a composable that reads the dependency from the current
 * Koin context; because the view models are declared as `single`s, recomposition returns the same
 * instances. Koin must be started before this composes (`LuminaApplication`/`main`). The action
 * bundles are `remember`ed on their view model so the lambdas are stable across recompositions.
 *
 * The stateless [AppRoot] is previewed separately, so this composable does not need a `@Preview`.
 */
@Composable
fun App() {
    AppTheme {
        val dashboardViewModel = koinInject<DashboardViewModel>()
        val peopleViewModel = koinInject<PeopleViewModel>()
        val settingsViewModel = koinInject<SettingsViewModel>()
        val dashboardState by dashboardViewModel.uiState.collectAsState()
        val peopleState by peopleViewModel.uiState.collectAsState()
        val settingsState by settingsViewModel.uiState.collectAsState()

        AppRoot(
            dashboardState = dashboardState,
            dashboardActions = remember(dashboardViewModel) {
                DashboardActions(
                    onVolumeChange = dashboardViewModel::onVolumeChange,
                    onVolumeChangeFinished = dashboardViewModel::onVolumeChangeFinished,
                    onMuteToggle = dashboardViewModel::onMuteToggle,
                    onRuntimeToggle = dashboardViewModel::onRuntimeToggle,
                    onDismissMessage = dashboardViewModel::onDismissMessage,
                )
            },
            peopleState = peopleState,
            peopleActions = remember(peopleViewModel) {
                PeopleActions(
                    onNameChange = peopleViewModel::onNameChange,
                    onRefresh = peopleViewModel::onRefresh,
                    onEnrollFromCamera = peopleViewModel::onEnrollFromCamera,
                    onImagesPicked = peopleViewModel::onImagesPicked,
                    onCancelEnrollment = peopleViewModel::onCancelEnrollment,
                    onStartRuntime = peopleViewModel::onStartRuntime,
                    onDismissMessage = peopleViewModel::onDismissMessage,
                )
            },
            settingsState = settingsState,
            settingsActions = remember(settingsViewModel) {
                SettingsActions(
                    onHostChange = settingsViewModel::onHostChange,
                    onUdpPortChange = settingsViewModel::onUdpPortChange,
                    onTcpPortChange = settingsViewModel::onTcpPortChange,
                    onTokenChange = settingsViewModel::onTokenChange,
                    onToggleTokenVisibility = settingsViewModel::onToggleTokenVisibility,
                    onSave = settingsViewModel::onSave,
                )
            },
        )
    }
}
