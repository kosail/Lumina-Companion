package com.korealm.lumina.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import lumina.shared.generated.resources.LogoLong
import lumina.shared.generated.resources.LogoLongDarkMode
import lumina.shared.generated.resources.Res
import org.jetbrains.compose.resources.painterResource

/**
 * The app's brand header: the long horizontal Lúmina logo, shown at the top of the shell on every tab.
 *
 * Responsibility: render the decorative logo once so each screen does not have to repeat it. It
 * follows the theme: `LogoLong` (dark glyphs) in light mode and `LogoLongDarkMode` (white glyphs) in
 * dark mode ([isSystemInDarkTheme]). The height is capped so the wide image (1920 x 637, ~3.01:1)
 * stays a slim banner on large windows (full width on a phone, centered otherwise) and never crowds
 * the content (FE-INV-026).
 *
 * Accessibility: the logo is decorative, so [contentDescription] is `null` and TalkBack skips it
 * (FE-INV-010.5) — the app's name is already announced by the tab labels and screen titles.
 *
 * @param modifier layout modifier from the shell.
 */
@Composable
internal fun BrandHeader(modifier: Modifier = Modifier) {
    val logo = if (isSystemInDarkTheme()) Res.drawable.LogoLongDarkMode else Res.drawable.LogoLong
    Image(
        painter = painterResource(logo),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 120.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}
