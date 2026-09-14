package com.lightningkite.kiteui.testing

import com.lightningkite.kiteui.OverrideOnly
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.views.Element
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.frame
import com.lightningkite.kiteui.views.setup
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.CoreGraphics.CGContextFillRect
import platform.Foundation.NSFileManager
import platform.Foundation.writeToFile
import platform.UIKit.*
import platform.posix.memcpy

/**
 * iOS implementation of TestHarness.
 *
 * Uses the UIWindow + UIViewController + setup() pattern.
 * NOTE: Do NOT call makeKeyAndVisible() - it causes SIGTRAP in test environment.
 * NOTE: Do NOT call setup() inside viewDidLoad() - it causes SIGTRAP in test environment.
 */
@OptIn(ExperimentalForeignApi::class)
actual class TestHarness {
    actual val supported: Boolean = true
    actual val async: AsyncTestSupport = AsyncTestSupport()
    private var window: UIWindow? = null
    private var viewController: UIViewController? = null
    private var rootView: Element? = null

    actual fun render(theme: Theme, content: ViewWriter.() -> Unit): Element {
        // Create window and view controller
        val win = UIWindow(frame = CGRectMake(0.0, 0.0, 500.0, 1000.0))
        val vc = UIViewController(null, null)

        // Set up the view controller
        win.rootViewController = vc

        // Set the view frame and background color from theme
        vc.view.setFrame(CGRectMake(0.0, 0.0, 500.0, 1000.0))
        vc.view.backgroundColor = UIColor.whiteColor  // Default to white background

        // Capture the root view using setup
        lateinit var capturedRoot: Element
        vc.setup(theme) {
            frame {
                content()
            }.also { capturedRoot = it }
        }

        // Trigger layout
        vc.view.setNeedsLayout()
        vc.view.layoutIfNeeded()

        // Store references
        window = win
        viewController = vc
        rootView = capturedRoot

        return capturedRoot
    }

    actual fun screenshot(name: String): ByteArray? {
        val vc = viewController ?: return null
        return captureViewScreenshot(vc.view, name)
    }

    actual fun screenshotView(view: Element, name: String): ByteArray? {
        return captureViewScreenshot(view.underlyingNativeElement.native, name)
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun captureViewScreenshot(view: UIView, name: String): ByteArray? {
        println("=== iOS Screenshot Capture ===")
        println("Screenshot name: $name")

        // Use layer.render instead of drawViewHierarchyInRect for unit tests
        // This is more reliable when the view isn't in a visible window
        val bounds = view.bounds
        val width = bounds.useContents { size.width }
        val height = bounds.useContents { size.height }
        val size = CGSizeMake(width, height)
        println("View size: $width x $height")

        UIGraphicsBeginImageContextWithOptions(size, true, 0.0)  // Set opaque to true for white background
        val context = UIGraphicsGetCurrentContext()

        if (context == null) {
            println("ERROR: Failed to get graphics context")
            return null
        }

        // Fill with white background first
        UIColor.whiteColor.setFill()
        CGContextFillRect(context, view.bounds)

        // Render the layer hierarchy
        view.layer.renderInContext(context)

        // Get the image
        val image = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()

        if (image == null) {
            println("ERROR: Failed to capture image from context")
            return null
        }

        // Convert to PNG data
        val pngData = UIImagePNGRepresentation(image)
        if (pngData == null) {
            println("ERROR: Failed to convert image to PNG")
            return null
        }

        // Convert NSData to ByteArray
        val length = pngData.length.toInt()
        val byteArray = ByteArray(length)

        byteArray.usePinned { pinned ->
            memcpy(pinned.addressOf(0), pngData.bytes, pngData.length)
        }

        // Save screenshot to file
        try {
            val fileManager = NSFileManager.defaultManager

            // Check for environment variable override
            val envProjectDir = platform.Foundation.NSProcessInfo.processInfo.environment["KITEUI_PROJECT_DIR"]
            val baseDir = if (envProjectDir != null) {
                val envPath = envProjectDir.toString()
                println("Using KITEUI_PROJECT_DIR: $envPath")
                envPath
            } else {
                fileManager.currentDirectoryPath
            }

            // Create screenshots directory
            val screenshotDir = "$baseDir/local/screenshots/ios"

            fileManager.createDirectoryAtPath(
                screenshotDir,
                withIntermediateDirectories = true,
                attributes = null,
                error = null
            )

            // Save the file
            val screenshotPath = "$screenshotDir/$name.png"
            pngData.writeToFile(screenshotPath, atomically = true)

            println("✓ Screenshot saved to: $screenshotPath")
        } catch (e: Exception) {
            println("ERROR: Exception while saving screenshot: ${e.message}")
        }

        return byteArray
    }

    @OptIn(OverrideOnly::class)
    actual fun cleanup() {
        rootView?.onShutdown()
        window = null
        viewController = null
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
