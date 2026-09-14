package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.navigation.render
import com.lightningkite.kiteui.views.Element
import com.lightningkite.kiteui.views.direct.frame
import com.lightningkite.kiteui.views.direct.stack
import com.lightningkite.kiteui.views.direct.text
import com.lightningkite.kiteui.views.setup
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSBundle
import platform.UIKit.UIApplication
import platform.UIKit.UIScreen
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import kotlin.test.Ignore
import kotlin.test.Test

class LayoutTest {

    @Ignore
    @Test
    fun test() {
        val s = LayoutsTestPage()
        lateinit var root: Element
        val window = UIWindow(frame = CGRectMake(0.0, 0.0, 500.0, 1000.0))
        val vc = UIViewController(null, null)
        window.rootViewController = vc
        vc.view.setFrame(CGRectMake(0.0, 0.0, 500.0, 1000.0))
        vc.setup(Theme(id = "unitTest")) {
            frame {
                s.render(this)
            }.also { root = it }
        }
        vc.view.setNeedsLayout()
        vc.view.layoutIfNeeded()
        s.checks.forEach { it() }
    }
}
