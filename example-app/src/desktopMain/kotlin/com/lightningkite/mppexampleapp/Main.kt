package com.lightningkite.mppexampleapp

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "KiteUI Example App") {
        MaterialTheme {
            Surface(modifier = Modifier.fillMaxSize()) {
                // Here you would typically initialize your app's main UI
                // For example, you might call a function from your commonMain source set
                // that sets up your app's UI
                // AppUI()
            }
        }
    }
}