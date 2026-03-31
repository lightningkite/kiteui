package com.lightningkite.kiteui.testing

import android.os.Bundle
import android.view.View
import com.github.takahirom.roborazzi.captureRoboImage
import com.lightningkite.kiteui.KiteUiActivity
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.navigation.Routes
import com.lightningkite.kiteui.views.Element
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.frame
import com.lightningkite.kiteui.views.native
import com.lightningkite.reactive.context.*
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import java.io.File

/**
 * Android implementation of TestHarness using Robolectric.
 *
 * Uses the KiteUiActivity pattern which is the standard way to
 * initialize KiteUI on Android.
 */
actual class TestHarness {
    actual val supported: Boolean = true
    actual val async: AsyncTestSupport = AsyncTestSupport()
    private var controller: org.robolectric.android.controller.ActivityController<TestActivity>? = null
    private var rootView: Element? = null

    init {
        // Initialize AndroidAppContext as early as possible
        // This is required because Theme's static initialization accesses AndroidAppContext.oneRem
        try {
            com.lightningkite.kiteui.views.AndroidAppContext.applicationCtx
        } catch (e: UninitializedPropertyAccessException) {
            // Not initialized yet, initialize it now
            com.lightningkite.kiteui.views.AndroidAppContext.applicationCtx =
                RuntimeEnvironment.getApplication()
        }
    }

    /**
     * Test activity that holds the test UI.
     */
    class TestActivity : KiteUiActivity() {
        override val mainNavigator: PageNavigator = PageNavigator {
            Routes(listOf(), mapOf(), Page.Empty)
        }

        var testTheme: Theme = Theme(id = "test")
        var testContent: (ViewWriter.() -> Unit)? = null
        var rootView: Element? = null

        override val theme: ReactiveContext.() -> Theme = {
            testTheme
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            // Use a simple Material3 theme for testing
            // We don't set a theme explicitly - the activity will use the default from manifest
            val content = testContent
            if (content != null) {
                with(viewWriter) {
                    frame {
                        content()
                    }.also { rootView = it }
                }
            }
        }
    }

    actual fun render(theme: Theme, content: ViewWriter.() -> Unit): Element {
        val ctrl = Robolectric.buildActivity(TestActivity::class.java)
        controller = ctrl

        // CRITICAL FIX: Initialize AndroidAppContext BEFORE any view creation
        // This must happen before onCreate() because some view initialization code
        // accesses AndroidAppContext.res which depends on applicationCtx being set
        com.lightningkite.kiteui.views.AndroidAppContext.applicationCtx = ctrl.get().applicationContext

        ctrl.get().testTheme = theme
        ctrl.get().testContent = content
        ctrl.setup()

        // Get the root view that was captured in onCreate
        val root = ctrl.get().rootView
            ?: throw IllegalStateException("No root view found after setup")

        rootView = root
        return root
    }

    actual fun screenshot(name: String): ByteArray? {
        val root = rootView ?: return null
        return captureViewScreenshot(root.native, name)
    }

    actual fun screenshotView(view: Element, name: String): ByteArray? {
        return captureViewScreenshot(view.native, name)
    }

    private fun captureViewScreenshot(view: View, name: String): ByteArray? {
        try {
            // Ensure the view is properly measured and laid out before capturing
            // This is critical for Roborazzi to render actual pixels instead of a view hierarchy dump
            val displayMetrics = view.context.resources.displayMetrics
            val width = displayMetrics.widthPixels
            val height = displayMetrics.heightPixels

            view.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
            )
            view.layout(0, 0, view.measuredWidth, view.measuredHeight)

            // Roborazzi saves relative to current working directory during test execution
            // Tests run from the module directory (library/ or example-app/)
            val moduleDir = File(".").absoluteFile

            // Debug logging to understand the directory structure
            println("Working directory: ${moduleDir.absolutePath}")
            println("View dimensions: ${view.measuredWidth}x${view.measuredHeight}")

            val roborazziFile = File(moduleDir, "build/outputs/roborazzi/$name.png")
            roborazziFile.parentFile?.mkdirs()

            // Capture using Roborazzi - this actually renders the view
            view.captureRoboImage(roborazziFile.absolutePath)

            // Copy to our screenshots directory for easier access
            val screenshotFile = File(moduleDir, "local/screenshots/android/$name.png")
            screenshotFile.parentFile?.mkdirs()

            return if (roborazziFile.exists()) {
                val bytes = roborazziFile.readBytes()
                screenshotFile.writeBytes(bytes)
                println("Screenshot saved to: ${screenshotFile.absolutePath}")
                bytes
            } else {
                println("Roborazzi file not created: ${roborazziFile.absolutePath}")
                null
            }
        } catch (e: Exception) {
            println("Failed to capture screenshot: ${e.message}")
            e.printStackTrace()
            return null
        }
    }

    actual fun cleanup() {
        try {
            controller?.pause()
            controller?.stop()
        } catch (e: IllegalStateException) {
            // Ignore theme-related exceptions during test cleanup
            // This can happen if the activity doesn't have an AppCompat theme
            if (!e.message.orEmpty().contains("Theme.AppCompat")) {
                throw e
            }
        }
        controller = null
        rootView = null
    }

    // Convenience methods that delegate to AsyncTestSupport

    actual suspend fun waitUntilIdle() {
        async.waitUntilIdle()
    }

    actual suspend fun waitFor(timeout: kotlin.time.Duration, condition: () -> Boolean) {
        async.waitFor(timeout, condition)
    }

    actual suspend fun advanceTimeBy(duration: kotlin.time.Duration) {
        async.advanceTimeBy(duration)
    }
}
