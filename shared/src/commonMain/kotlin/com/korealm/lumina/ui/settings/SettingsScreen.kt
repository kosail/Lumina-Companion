package com.korealm.lumina.ui.settings

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.korealm.lumina.ui.components.SectionTitle
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
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TextFieldDefaults
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Check
import top.yukonga.miuix.kmp.icon.extended.Create
import top.yukonga.miuix.kmp.icon.extended.Link
import top.yukonga.miuix.kmp.icon.extended.Location
import top.yukonga.miuix.kmp.icon.extended.Lock
import top.yukonga.miuix.kmp.icon.extended.SearchDevice
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
 * (FE-INV-051). Designed to FE-INV-026: one purpose per card, large text, generous spacing, a
 * decorative MiuiX icon per section and field (contentDescription `null`; TalkBack uses the labels —
 * FE-INV-010), and a `liveRegion` confirmation so saving is announced.
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

        SectionTitle(
            icon = MiuixIcons.Link,
            text = stringResource(Res.string.settings_section_connection),
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SettingsField(
                    value = state.host,
                    onValueChange = actions.onHostChange,
                    label = stringResource(Res.string.settings_host),
                    icon = MiuixIcons.Location,
                    isError = state.errors.host,
                    errorText = stringResource(Res.string.settings_error_host),
                )
                SettingsField(
                    value = state.udpPort,
                    onValueChange = actions.onUdpPortChange,
                    label = stringResource(Res.string.settings_udp_port),
                    icon = MiuixIcons.SearchDevice,
                    isError = state.errors.udpPort,
                    errorText = stringResource(Res.string.settings_error_port),
                    keyboardType = KeyboardType.Number,
                )
                SettingsField(
                    value = state.tcpPort,
                    onValueChange = actions.onTcpPortChange,
                    label = stringResource(Res.string.settings_tcp_port),
                    icon = MiuixIcons.Link,
                    isError = state.errors.tcpPort,
                    errorText = stringResource(Res.string.settings_error_port),
                    keyboardType = KeyboardType.Number,
                )
            }
        }

        SectionTitle(
            icon = MiuixIcons.Lock,
            text = stringResource(Res.string.settings_section_token),
        )
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
                    leadingIcon = {
                        Icon(
                            imageVector = MiuixIcons.Lock,
                            contentDescription = null,
                            // MiuiX pads only the text box; the icon slot carries its own spacing.
                            modifier = Modifier
                                .padding(start = TextFieldDefaults.InsideMargin.width, end = 8.dp)
                                .size(20.dp),
                            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    },
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
            Icon(
                imageVector = MiuixIcons.Create,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(Res.string.settings_save))
        }

        if (state.saved) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = MiuixIcons.Basic.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MiuixTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(Res.string.settings_saved),
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.primary,
                )
            }
        }
    }
}

/**
 * One labelled [TextField] with a leading icon and an inline error message.
 *
 * @param value the field text.
 * @param onValueChange edit callback.
 * @param label the MiuiX floating label.
 * @param icon the decorative leading icon (FE-INV-026).
 * @param isError true when the field failed validation (recolors the label/border).
 * @param errorText the message shown below when [isError].
 * @param keyboardType soft-keyboard type; defaults to text.
 */
@Composable
private fun SettingsField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
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
            leadingIcon = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    // MiuiX pads only the text box; the icon slot carries its own spacing.
                    modifier = Modifier
                        .padding(start = TextFieldDefaults.InsideMargin.width, end = 8.dp)
                        .size(20.dp),
                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            },
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
