package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.models.remMultiplier
import com.lightningkite.kiteui.navigation.ScreenNavigator
import com.lightningkite.kiteui.views.setup
import platform.UIKit.UIViewController

fun root(viewController: UIViewController) {
    remMultiplier = 1.0
    viewController.setup(appTheme) { app(ScreenNavigator { AutoRoutes }, ScreenNavigator { AutoRoutes }) }
}