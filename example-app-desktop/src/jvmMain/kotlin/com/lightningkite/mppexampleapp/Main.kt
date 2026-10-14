package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.views.kiteUiWindow
import com.lightningkite.reactive.context.invoke

fun main() = kiteUiWindow(appId = "com.lightningkite.mppexampleapp", title = "KiteUI Example", theme = { appTheme() }) {
    app(PageNavigator { AutoRoutes })
}
