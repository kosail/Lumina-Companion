package com.korealm.lumina.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import lumina.shared.generated.resources.Res
import lumina.shared.generated.resources.settings_error_host
import lumina.shared.generated.resources.settings_error_port
import lumina.shared.generated.resources.settings_host
import lumina.shared.generated.resources.settings_save
import lumina.shared.generated.resources.settings_saved
import lumina.shared.generated.resources.settings_section_connection
import lumina.shared.generated.resources.settings_section_token
import lumina.shared.generated.resources.settings_title
import lumina.shared.generated.resources.settings_tcp_port
import lumina.shared.generated.resources.settings_token_hide
import lumina.shared.generated.resources.settings_token_hint
import lumina.shared.generated.resources.settings_token_show
import lumina.shared.generated.resources.settings_udp_port
import org.jetbrains.compose.resources.stringResource
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TextFieldDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * User intents emitted by the settings composable.
 *
 * Responsibility: keep the composable a pure renderer (FE-INV-051) without a long parameter list.
 *
 * @param onHostChange host field edited.
 * @param onUdpPortChange UDP port field edited.
 * @param onTcpPortChange TCP port field edited.
 * @param onTokenChange token field edited.
 * @param onToggleTokenVisibility show/hide the token.
 * @param onSave validate and persist the form.
 */
data class SettingsActions(
    val onHostChange: (String) -> Unit,
    val onUdpPortChange: (String) -> Unit,
    val onTcpPortChange: (String) -> Unit,
    val onTokenChange: (String) -> Unit,
    val onToggleTokenVisibility: () -> Unit,
    val onSave: () -> Unit,
)

/**
 * The Ajustes tab: gateway host/ports and the control token, persisted via `SettingsStore`.
 *
 * Responsibility: render [SettingsUiState] and emit [SettingsActions] — no business logic
 * (FE-INV-051). Designed to FE-INV-026: one purpose per card, large text, generous spacing, and a
 * `liveRegion` confirmation so saving is announced (FE-INV-010).
 *
 * The token field is masked with `PasswordVisualTransformation` (verified MiuiX 0.9.4 `TextField`
 * overload) and revealed only on request; the reveal control is a labelled button, not an icon, so a
 * screen reader announces its purpose (FE-INV-010/026).
 *
 * @param state the current form state.
 * @param actions the user intents.
 * @param modifier layout modifier from the caller (the scaffold content padding).
 */
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(Res.string.settings_title),
            style = MiuixTheme.textStyles.title1,
            color = MiuixTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )

        SmallTitle(text = stringResource(Res.string.settings_section_connection))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SettingsField(
                    value = state.host,
                    onValueChange = actions.onHostChange,
                    label = stringResource(Res.string.settings_host),
                    isError = state.errors.host,
                    errorText = stringResource(Res.string.settings_error_host),
                )
                SettingsField(
                    value = state.udpPort,
                    onValueChange = actions.onUdpPortChange,
                    label = stringResource(Res.string.settings_udp_port),
                    isError = state.errors.udpPort,
                    errorText = stringResource(Res.string.settings_error_port),
                    keyboardType = KeyboardType.Number,
                )
                SettingsField(
                    value = state.tcpPort,
                    onValueChange = actions.onTcpPortChange,
                    label = stringResource(Res.string.settings_tcp_port),
                    isError = state.errors.tcpPort,
                    errorText = stringResource(Res.string.settings_error_port),
                    keyboardType = KeyboardType.Number,
                )
            }
        }

        SmallTitle(text = stringResource(Res.string.settings_section_token))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(Res.string.settings_token_hint),
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                TextField(
                    value = state.token,
                    onValueChange = actions.onTokenChange,
                    label = stringResource(Res.string.settings_section_token),
                    singleLine = true,
                    visualTransformation = if (state.tokenRevealed) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        TextButton(
                            text = stringResource(
                                if (state.tokenRevealed) {
                                    Res.string.settings_token_hide
                                } else {
                                    Res.string.settings_token_show
                                },
                            ),
                            onClick = actions.onToggleTokenVisibility,
                        )
                    },
                )
            }
        }

        Button(
            onClick = actions.onSave,
            colors = ButtonDefaults.buttonColorsPrimary(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Text(stringResource(Res.string.settings_save))
        }

        if (state.saved) {
            Text(
                text = stringResource(Res.string.settings_saved),
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.primary,
                // Announced when it appears so a screen-reader user knows the save succeeded.
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

/**
 * One labelled [TextField] with an inline error message.
 *
 * @param value the field text.
 * @param onValueChange edit callback.
 * @param label the MiuiX floating label.
 * @param isError true when the field failed validation (recolors the label/border).
 * @param errorText the message shown below when [isError].
 * @param keyboardType soft-keyboard type; defaults to text.
 */
@Composable
private fun SettingsField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isError: Boolean,
    errorText: String,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    val defaultColors = TextFieldDefaults.textFieldColors()
    val errorColors = TextFieldDefaults.textFieldColors(
        labelColor = MiuixTheme.colorScheme.error,
        borderColor = MiuixTheme.colorScheme.error,
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            label = label,
            singleLine = true,
            colors = if (isError) errorColors else defaultColors,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        )
        if (isError) {
            Text(
                text = errorText,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.error,
                modifier = Modifier.padding(start = 16.dp),
            )
        }
    }
}
