package com.korealm.lumina

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import lumina.shared.generated.resources.Res
import lumina.shared.generated.resources.logo
import org.jetbrains.compose.resources.painterResource

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Lumina",
        icon = painterResource(Res.drawable.logo),
    ) {
        App()
    }
}