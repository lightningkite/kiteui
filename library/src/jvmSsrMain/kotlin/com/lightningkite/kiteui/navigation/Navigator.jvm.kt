package com.lightningkite.kiteui.navigation

import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.ViewWriter

actual fun ScreenStack.bindToPlatform(context: RContext) {
}

actual fun ViewWriter.addListenerForNavigateAway(pageNav: PageNavigator) {}

actual fun PageNavigator.askForConfirmNavigateAway(): Boolean {
    return true
}