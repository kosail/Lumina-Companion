package com.korealm.lumina.ui.theme

import androidx.compose.runtime.Composable
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Applies the MIUI theme (MiuiX) to the whole app (FE-INV-025).
 *
 * Responsibility: one place to install [MiuixTheme] so every MiuiX component resolves colors, text
 * styles and indication consistently. It wraps the default light color scheme and text styles; a
 * dark/high-contrast variant is a later accessibility step (FE-INV-026).
 *
 * Kotlin note: this is a composable wrapper (like a React provider) — everything inside `content`
 * can read `MiuixTheme.colorScheme`/`MiuixTheme.textStyles`.
 */
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MiuixTheme(content = content)
}
