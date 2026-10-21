package com.lightningkite.mppexampleapp

import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.lightningkite.kiteui.exceptions.ExceptionHandler
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.views.KiteUiRoot
import com.lightningkite.reactive.context.invoke
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class ExampleAppTest {
    @Test
    fun homePageRenders() = runComposeUiTest {
        // Elements hand their coroutines' failures to the context's handlers, which would otherwise show a dialog.
        val failures = ArrayList<Exception>()
        val root = runBlocking(Dispatchers.Main) {
            KiteUiRoot("kiteui-example-test", { appTheme() }) {
                context.exceptionHandlers += ExceptionHandler(Float.MAX_VALUE) { e ->
                    failures += e
                    {}
                }
                app(PageNavigator { AutoRoutes })
            }
        }
        try {
            setContent { root.Content() }
            waitForIdle()
            onNodeWithText("KiteUI - Beautiful by Default").assertExists()
            val out = File(System.getProperty("user.dir"), "build/shots").apply { mkdirs() }
            ImageIO.write(runBlocking { root.screenshot() }.toAwtImage(), "png", File(out, "home.png"))
        } finally {
            runBlocking(Dispatchers.Main) { root.close() }
        }
        failures.firstOrNull()?.let { throw AssertionError("An element's coroutine failed", it) }
    }
}
