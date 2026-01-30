package com.lightningkite.kiteui.navigation

import com.lightningkite.kiteui.views.RContext

actual fun PageNavigator.bindToPlatform(context: RContext) {
    // For Swing, there's no URL bar, so navigate to root path "/" by default
    if (stack.value.isEmpty()) {
        navigateUrlLikePath("/")
    }
}