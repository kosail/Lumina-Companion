package com.korealm.lumina.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * A section header with a leading icon, shared by the dashboard, people and settings screens.
 *
 * Responsibility: give low-vision users a visual cue per section (FE-INV-026). MiuiX `SmallTitle` has
 * no icon slot, so this mirrors its subtitle style (`textStyles.subtitle`, 28/8 dp inside margin) and
 * adds a decorative [Icon] (contentDescription `null`; the text already carries the meaning, so
 * TalkBack is unaffected — FE-INV-010).
 *
 * @param icon the leading icon.
 * @param text the section title (already resolved Spanish text).
 * @param modifier layout modifier from the caller.
 */
@Composable
internal fun SectionTitle(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    val color = MiuixTheme.colorScheme.onBackgroundVariant
    Row(
        modifier = modifier.padding(horizontal = 28.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = color,
        )
        Text(text = text, style = MiuixTheme.textStyles.subtitle, color = color)
    }
}
