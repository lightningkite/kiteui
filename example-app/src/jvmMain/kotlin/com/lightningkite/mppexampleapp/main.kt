package com.lightningkite.mppexampleapp

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.context.CalculationContext
import com.lightningkite.reactive.core.AppScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

import kotlin.coroutines.CoroutineContext


//fun root() {
//    object : ViewWriter(), CalculationContext by AppScope {
//        override val context: RContext = RContext()
//
//        override fun addChild(view: RView) {
////            TODO("Not yet implemented")
//            @Composable
//            view.compose()
//        }
//
////        @Composable
////        override fun addChild(view: RView) {
////            view.compose()
////        }
//
//        override val coroutineContext: CoroutineContext
//            get() = TODO("Not yet implemented")
//
//}
//}


fun main() {
    val mainNavigator: PageNavigator = PageNavigator { AutoRoutes }
    val dialogNavigator: PageNavigator = PageNavigator { AutoRoutes }
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "KiteUi Compose Backend test",
        ) {
            // Create the ViewWriter with proper coroutine context
            val viewWriter = object : ViewWriter(), CoroutineScope {
                override val context: RContext = RContext()
                override val coroutineContext: CoroutineContext = Dispatchers.Main

                var rootView: RView? = null

                override fun addChild(view: RView) {
                    println("DEBUG in addChild ${view.rView}")
                    rootView = view
                }
            }

            // Build your UI using the ViewWriter
            // Example: Create some views and add them to the writer
            // You would typically call your KiteUI building functions here

            // Render the root view if it exists

            with(viewWriter) {
                frame {
                    col {
                        text("A")
                        text("B")
                        text("C")
                    }
                }
                rootView?.compose()
            }
        }
    }
}

