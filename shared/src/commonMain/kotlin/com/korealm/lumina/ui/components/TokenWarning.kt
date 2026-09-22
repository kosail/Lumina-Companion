package com.korealm.lumina.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import lumina.shared.generated.resources.Res
import lumina.shared.generated.resources.token_warning_body
import lumina.shared.generated.resources.token_warning_title
import org.jetbrains.compose.resources.stringResource
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Lock
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Persistent warning shown after an `unauthorized` reply: the token is wrong and no control command
 * will work until it is fixed in Ajustes (contract §4.9, §9).
 *
 * Responsibility: one shared warning so the dashboard and the people screen cannot drift. Kept
 * visible (not a transient snackbar) because it needs a user action, and marked as a polite
 * `liveRegion` so it is announced when it appears (FE-INV-010). The leading lock icon is decorative
 * (FE-INV-026).
 *
 * @param modifier layout modifier from the caller.
 */
@Composable
internal fun TokenWarning(modifier: Modifier = Modifier) {
    val colors = MiuixTheme.colorScheme
    Card(
        modifier = modifier
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = MiuixIcons.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = colors.onErrorContainer,
                )
                Text(
                    text = stringResource(Res.string.token_warning_title),
                    style = MiuixTheme.textStyles.title4,
                    color = colors.onErrorContainer,
                )
            }
            Text(
                text = stringResource(Res.string.token_warning_body),
                style = MiuixTheme.textStyles.body1,
                color = colors.onErrorContainer,
            )
        }
    }
}
