package com.lightningkite.kiteui.navigation

import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.ViewWriter

actual fun PageNavigator.bindToPlatform(context: RContext) {
}

actual fun PageNavigator.askForConfirmNavigateAway(): Boolean {
    return true
}