package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.Semantic
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.Transformation
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.render
import com.lightningkite.kiteui.views.Element
import com.lightningkite.kiteui.views.ElementWriter
import com.lightningkite.kiteui.views.direct.frame
import com.lightningkite.kiteui.views.native
import com.lightningkite.kiteui.views.setup
import com.lightningkite.kiteui.views.themed
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.QuartzCore.CATransform3DIdentity
import platform.QuartzCore.CATransform3DRotate
import platform.QuartzCore.CATransform3DScale
import platform.QuartzCore.CATransform3DTranslate
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import kotlin.math.PI
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

// The transform under test intentionally sets every component (translation on all three
// axes, both rotation axes plus the Z rotation, and non-uniform scale) at once. The bug this
// guards against was an if/else-if chain in NativeElement.ios.kt that applied only ONE of
// translate/rotate/scale (whichever branch matched first) and dropped rotationX/rotationY
// entirely, so a test that only exercises one field at a time would pass against the broken
// code too.
private val testTransform = Transformation(
    translationX = 10.0,
    translationY = 20.0,
    translationZ = 5.0,
    rotationX = 30.0,
    rotationY = 40.0,
    rotation = 50.0,
    scaleX = 2.0,
    scaleY = 3.0,
)

private object CompositeTransformSemantic : Semantic("compositeTransformTest") {
    override fun default(theme: Theme) = theme.withBack(transform = testTransform)
}

private class ThemeTransformPage : Page {
    lateinit var target: Element
    override fun ElementWriter.CanAddTheme.render(): Unit = run {
        themed(CompositeTransformSemantic).frame {
            target = this
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
class ThemeTransformTest {
    @Test
    fun translateRotateAndScaleAllComposeTogether() {
        val page = ThemeTransformPage()
        val window = UIWindow(frame = CGRectMake(0.0, 0.0, 500.0, 500.0))
        val vc = UIViewController(null, null)
        window.rootViewController = vc
        vc.view.setFrame(CGRectMake(0.0, 0.0, 500.0, 500.0))
        vc.setup(Theme(id = "unitTest")) {
            frame { page.render(this) }
        }
        vc.view.setNeedsLayout()
        vc.view.layoutIfNeeded()

        // Independently recompute the expected matrix by chaining the same CATransform3D
        // composition primitives NativeElement.ios.kt is expected to call, in the same order
        // (translate, then rotateX, then rotateY, then rotateZ, then scale). This is exactly
        // the composition the fix introduced; the old if/else-if chain could never produce it
        // since it only ever ran one branch and never touched rotationX/rotationY at all.
        var expected = CATransform3DIdentity.readValue()
        expected = CATransform3DTranslate(expected, testTransform.translationX, testTransform.translationY, testTransform.translationZ)
        expected = CATransform3DRotate(expected, testTransform.rotationX * (PI / 180.0), 1.0, 0.0, 0.0)
        expected = CATransform3DRotate(expected, testTransform.rotationY * (PI / 180.0), 0.0, 1.0, 0.0)
        expected = CATransform3DRotate(expected, testTransform.rotation * (PI / 180.0), 0.0, 0.0, 1.0)
        expected = CATransform3DScale(expected, testTransform.scaleX, testTransform.scaleY, 1.0)

        val actual = page.target.native.layer.transform

        val expectedValues = expected.useContents {
            listOf(m11, m12, m13, m14, m21, m22, m23, m24, m31, m32, m33, m34, m41, m42, m43, m44)
        }
        val actualValues = actual.useContents {
            listOf(m11, m12, m13, m14, m21, m22, m23, m24, m31, m32, m33, m34, m41, m42, m43, m44)
        }

        expectedValues.zip(actualValues).forEachIndexed { index, (e, a) ->
            assertTrue(abs(e - a) < 1e-6, "Matrix component $index: expected $e but was $a")
        }
    }
}
