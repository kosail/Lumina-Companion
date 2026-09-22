package com.korealm.lumina

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.korealm.lumina.di.startLuminaKoin
import lumina.shared.generated.resources.Res
import lumina.shared.generated.resources.logo
import org.jetbrains.compose.resources.painterResource

/**
 * Desktop entry point. Starts Koin once, then opens the shared [App] in a window.
 *
 * The desktop client is the fast development loop against the mock agent (`../Testing_server`).
 */
fun main() {
    startLuminaKoin()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Lumina",
            icon = painterResource(Res.drawable.logo),
        ) {
            App()
        }
    }
}
