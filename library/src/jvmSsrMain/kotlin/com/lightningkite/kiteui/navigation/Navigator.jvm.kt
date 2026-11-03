package com.lightningkite.kiteui.navigation

import com.lightningkite.kiteui.views.ElementContext

public actual fun PageNavigator.bindToPlatform(context: ElementContext) {
}

public actual fun PageNavigator.askForConfirmNavigateAway(): Boolean {
    return true
}