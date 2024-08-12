package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.navigation.ScreenNavigator
import com.lightningkite.kiteui.views.setup
import platform.UIKit.UIViewController

fun root(viewController: UIViewController) {
    viewController.setup(appTheme) { app(ScreenNavigator { AutoRoutes }, ScreenNavigator { AutoRoutes }) }
}