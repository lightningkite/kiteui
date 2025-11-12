package com.lightningkite.kiteui.testing

import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.views.RView
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
import platform.UIKit.*
import platform.Foundation.NSData
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
    private var window: UIWindow? = null
    private var viewController: UIViewController? = null
    private var rootView: RView? = null

    actual fun render(theme: Theme, content: ViewWriter.() -> Unit): RView {
        // Create window and view controller
        val win = UIWindow(frame = CGRectMake(0.0, 0.0, 500.0, 1000.0))
        val vc = UIViewController(null, null)

        // Set up the view controller
        win.rootViewController = vc

        // Set the view frame and background color from theme
        vc.view.setFrame(CGRectMake(0.0, 0.0, 500.0, 1000.0))
        vc.view.backgroundColor = UIColor.whiteColor  // Default to white background

        // Capture the root view using setup
        lateinit var capturedRoot: RView
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
        return captureViewScreenshot(vc.view)
    }

    actual fun screenshotView(view: RView, name: String): ByteArray? {
        return captureViewScreenshot(view.native)
    }

    private fun captureViewScreenshot(view: UIView): ByteArray? {
        // Use layer.render instead of drawViewHierarchyInRect for unit tests
        // This is more reliable when the view isn't in a visible window
        val bounds = view.bounds
        val size = CGSizeMake(bounds.useContents { size.width }, bounds.useContents { size.height })
        UIGraphicsBeginImageContextWithOptions(size, true, 0.0)  // Set opaque to true for white background
        val context = UIGraphicsGetCurrentContext() ?: return null

        // Fill with white background first
        UIColor.whiteColor.setFill()
        CGContextFillRect(context, view.bounds)

        // Render the layer hierarchy
        view.layer.renderInContext(context)

        // Get the image
        val image = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()

        if (image == null) {
            println("Failed to capture image from context")
            return null
        }

        // Convert to PNG data
        val pngData = UIImagePNGRepresentation(image) ?: return null

        // Convert NSData to ByteArray
        val length = pngData.length.toInt()
        val byteArray = ByteArray(length)

        byteArray.usePinned { pinned ->
            memcpy(pinned.addressOf(0), pngData.bytes, pngData.length)
        }

        return byteArray
    }

    actual fun cleanup() {
        rootView?.shutdown()
        window = null
        viewController = null
        rootView = null
    }
}
