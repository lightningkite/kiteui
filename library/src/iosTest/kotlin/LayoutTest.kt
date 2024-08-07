package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.navigation.render
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.direct.stack
import com.lightningkite.kiteui.views.setup
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSBundle
import platform.UIKit.UIApplication
import platform.UIKit.UIScreen
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import kotlin.test.Test

class LayoutTest {
    @OptIn(ExperimentalForeignApi::class)
    @Test
    fun test() {
        val s = LayoutsTestScreen()
        lateinit var root: RView
        val vc = object: UIViewController(null, null) {
            override fun viewDidLoad() {
                super.viewDidLoad()
                setup(Theme(id = "unitTest")) {
                    root = stack {
                        s.render(this)
                    }
                }
            }
        }
        val window = UIWindow(frame = CGRectMake(0.0, 0.0, 500.0, 1000.0))
        window.makeKeyAndVisible()
        window.rootViewController = vc
        vc.view.setNeedsLayout()
        vc.view.layoutIfNeeded()
        println(root.screenRectangle())
        s.checks.forEach { it() }
    }
}
