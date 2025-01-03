package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.models.remMultiplier
import com.lightningkite.kiteui.navigation.ScreenNavigator
import com.lightningkite.kiteui.navigation.dialogScreenNavigator
import com.lightningkite.kiteui.navigation.mainScreenNavigator
import com.lightningkite.kiteui.navigation.render
import com.lightningkite.kiteui.views.RViewHelper
import com.lightningkite.kiteui.views.setup
import com.lightningkite.mppexampleapp.internal.LeakCheckerScreen
import platform.UIKit.UIViewController

fun root(viewController: UIViewController) {
    remMultiplier = 1.0
    RViewHelper.leakDetection = true
    viewController.setup(appTheme) {
//        mainScreenNavigator = ScreenNavigator { AutoRoutes }
//        dialogScreenNavigator = ScreenNavigator { AutoRoutes }
//        LeakCheckerScreen.render(this)
        app(ScreenNavigator { AutoRoutes }.apply { reset(LeakCheckerScreen) }, ScreenNavigator { AutoRoutes })
    }
}