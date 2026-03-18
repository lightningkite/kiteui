package com.lightningkite.kiteui.navigation

import com.lightningkite.kiteui.views.ElementContext

actual fun PageNavigator.bindToPlatform(context: ElementContext) {
}

actual fun PageNavigator.askForConfirmNavigateAway(): Boolean {
    return true
}