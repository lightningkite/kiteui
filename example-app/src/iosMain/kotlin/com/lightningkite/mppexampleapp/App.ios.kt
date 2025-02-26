package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.models.remMultiplier
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.views.RViewHelper
import com.lightningkite.kiteui.views.setup
import com.lightningkite.mppexampleapp.app
import com.lightningkite.mppexampleapp.internal.LeakCheckerPage
import platform.UIKit.UIViewController

fun root(viewController: UIViewController) {
    remMultiplier = 1.0
    RViewHelper.leakDetection = true
    viewController.setup(appTheme) {
//        mainPageNavigator = PageNavigator { AutoRoutes }
//        dialogPageNavigator = PageNavigator { AutoRoutes }
//        LeakCheckerScreen.render(this)
        app(PageNavigator { AutoRoutes }.apply { reset(LeakCheckerPage) }, PageNavigator { AutoRoutes })
    }
}