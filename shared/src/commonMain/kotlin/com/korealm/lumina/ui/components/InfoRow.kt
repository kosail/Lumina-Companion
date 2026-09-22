package com.korealm.lumina.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * One label/value row inside a card, shared by the dashboard and the people screen.
 *
 * Responsibility: render the label and value and keep them as **one** accessibility node. The
 * `mergeDescendants` modifier makes TalkBack read "Etiqueta: valor" as a single focus target instead
 * of two separate stops (FE-INV-010, CHG-FE-0021); the visual layout is unchanged.
 *
 * An optional [icon] gives low-vision users a visual cue per row (FE-INV-026); it is decorative
 * (contentDescription `null`) because the label already conveys the meaning.
 *
 * @param label the left-hand label (already-resolved Spanish text).
 * @param value the right-hand value; omitted from the layout when empty.
 * @param icon optional leading icon.
 * @param modifier layout modifier from the caller.
 */
@Composable
internal fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            Text(
                text = label,
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
        if (value.isNotEmpty()) {
            Text(
                text = value,
                style = MiuixTheme.textStyles.title4,
                color = MiuixTheme.colorScheme.onSurfaceContainer,
            )
        }
    }
}
