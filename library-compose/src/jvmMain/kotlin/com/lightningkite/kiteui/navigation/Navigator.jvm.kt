package com.lightningkite.kiteui.navigation

import com.lightningkite.kiteui.views.ElementContext

// Desktop apps have no URL to start from, so they open on the landing route, like iOS without a deep link.
// TODO: back navigation from the keyboard (Alt+Left) and mouse back button.
public actual fun PageNavigator.bindToPlatform(context: ElementContext) {
    stack.value = listOf(routes.parse(UrlLikePath.EMPTY) ?: routes.fallback)
}

// TODO: ask before closing the window with unsaved changes.
public actual fun PageNavigator.askForConfirmNavigateAway(): Boolean = true
