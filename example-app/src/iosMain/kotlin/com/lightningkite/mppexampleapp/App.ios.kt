package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.models.remMultiplier
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.*
import com.lightningkite.kiteui.views.setup
import com.lightningkite.mppexampleapp.app
import com.lightningkite.mppexampleapp.internal.LeakCheckerPage
import platform.UIKit.UIViewController

fun root(viewController: UIViewController) {
    remMultiplier = 1.0
//    RViewHelper.leakDetection = true
    viewController.setup(appTheme) {
//        mainPageNavigator = PageNavigator { AutoRoutes }
//        LeakCheckerScreen.render(this)
        app(PageNavigator { AutoRoutes })
//        col {
//            text("Hello world")
//            button {
//                text("Press me")
//            }
//        }
//        text("Hello world")
    }
}