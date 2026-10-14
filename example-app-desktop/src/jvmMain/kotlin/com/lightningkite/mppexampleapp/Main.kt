package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.Platform
import com.lightningkite.kiteui.isDevelopment
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.views.AiDriver
import com.lightningkite.kiteui.views.KiteUiRoot
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.kiteUiHeadless
import com.lightningkite.kiteui.views.kiteUiWindow
import com.lightningkite.reactive.context.invoke

fun main(args: Array<String>) {
    val navigator = PageNavigator { AutoRoutes }
    val content: ViewWriter.() -> Unit = {
        app(navigator)
        if (Platform.isDevelopment) AiDriver.connect(
            appName = "example",
            rootView = { KiteUiRoot.current?.root },
            navigator = { navigator },
        )
    }
    val appId = "com.lightningkite.mppexampleapp"
    if ("--headless" in args) kiteUiHeadless(appId, theme = { appTheme() }, app = content)
    else kiteUiWindow(appId, title = "KiteUI Example", theme = { appTheme() }, app = content)
}
