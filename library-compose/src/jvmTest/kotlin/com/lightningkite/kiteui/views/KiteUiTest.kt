package com.lightningkite.kiteui.views

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.lightningkite.kiteui.InMemoryPreferences
import com.lightningkite.kiteui.PlatformStorage
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.flat2
import com.lightningkite.kiteui.models.turns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.awt.image.BufferedImage
import java.io.File
import java.util.prefs.Preferences
import javax.imageio.ImageIO

val testTheme: Theme = Theme.flat2("test", 0.6.turns)

/** [runComposeUiTest] that closes the root [showKiteUi] made and deletes anything stored, even when the test fails. */
@OptIn(ExperimentalTestApi::class)
fun runKiteUiTest(test: suspend ComposeUiTest.() -> Unit): Unit = runComposeUiTest {
    check(Preferences.userRoot() is InMemoryPreferences) { "Run tests through Gradle's jvmTest, which keeps stored settings in memory" }
    try {
        test()
    } finally {
        KiteUiRoot.current?.let { root -> runBlocking(Dispatchers.Main) { root.close() } }
        if (PlatformStorage.appId != null) PlatformStorage.preferences.removeNode()
    }
}

@OptIn(ExperimentalTestApi::class)
fun ComposeUiTest.showKiteUi(theme: Theme = testTheme, app: ViewWriter.() -> Unit): KiteUiRoot {
    // Elements must be built on the main thread, as in the app.
    val root = runBlocking(Dispatchers.Main) { KiteUiRoot("kiteui-test", { theme }, app) }
    setContent { root.Content() }
    waitForIdle()
    return root
}

/** Saves the window to build/shots/[name].png, for looking at, and returns it. */
@OptIn(ExperimentalTestApi::class)
fun ComposeUiTest.saveScreenshot(root: KiteUiRoot, name: String): BufferedImage {
    waitForIdle()
    return saveScreenshot(runBlocking { root.screenshot() }, name)
}

fun saveScreenshot(image: ImageBitmap, name: String): BufferedImage {
    val awt = image.toAwtImage()
    val out = File(System.getProperty("user.dir"), "build/shots").apply { mkdirs() }
    ImageIO.write(awt, "png", File(out, "$name.png"))
    return awt
}
