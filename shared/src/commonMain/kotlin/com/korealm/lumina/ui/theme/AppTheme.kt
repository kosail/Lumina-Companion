package com.korealm.lumina.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

/**
 * Applies the MIUI theme (MiuiX) to the whole app (FE-INV-025).
 *
 * Responsibility: one place to install [MiuixTheme] so every MiuiX component resolves colors, text
 * styles and indication consistently. It follows the system light/dark setting (FE-INV-026): the
 * MiuiX [darkColorScheme] is used when [isSystemInDarkTheme] is true, otherwise [lightColorScheme].
 *
 * Kotlin note: this is a composable wrapper (like a React provider) — everything inside `content`
 * can read `MiuixTheme.colorScheme`/`MiuixTheme.textStyles`. The `remember(dark)` avoids rebuilding
 * the large color scheme on every recomposition.
 */
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    MiuixTheme(
        colors = remember(dark) { if (dark) darkColorScheme() else lightColorScheme() },
        content = content,
    )
}
