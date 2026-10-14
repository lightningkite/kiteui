package com.lightningkite.mppexampleapp

import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
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
    fun homePageRenders() {
        // KiteUI's reactive work runs on the Swing thread, where a crash would otherwise only be printed.
        val uncaught = ArrayList<Throwable>()
        Thread.setDefaultUncaughtExceptionHandler { _, e -> uncaught += e }
        runComposeUiTest {
            val root = runBlocking(Dispatchers.Main) { KiteUiRoot("kiteui-example-test", { appTheme() }) { app(PageNavigator { AutoRoutes }) } }
            setContent { root.Content() }
            waitForIdle()
            val out = File(System.getProperty("user.dir"), "build/shots").apply { mkdirs() }
            ImageIO.write(runBlocking { root.screenshot() }.toAwtImage(), "png", File(out, "home.png"))
            root.close()
        }
        uncaught.firstOrNull()?.let { throw AssertionError("Uncaught exception on the UI thread", it) }
    }
}
