package com.lightningkite.kiteui.navigation

import com.lightningkite.kiteui.ConsoleRoot
import com.lightningkite.kiteui.views.RContext
import platform.Foundation.NSUserActivity

private var postedLandingUrl: UrlLikePath? = null
private var lastBoundPageNavigator: PageNavigator? = null

public actual fun PageNavigator.bindToPlatform(context: RContext) {
    lastBoundPageNavigator = this
    val landing = routes.parse(postedLandingUrl ?: UrlLikePath.EMPTY)
    stack.value = listOf(landing ?: routes.fallback)
}

// If called from scene(_:willConnectTo:options:) when the app is not already running, we need to wait until
// the lateinit ScreenStack.mainRoutes has been initialized, thus the need for postUserActivity() AND handleUserActivity()

// To be called from scene(_:willConnectTo:options:)
public fun postUserActivity(activity: NSUserActivity) {
    postedLandingUrl = activity.webpageUrlLikePath()
    ConsoleRoot.info("postUserActivity: $postedLandingUrl")
}

// To be called from scene(_:continue:)
public fun handleUserActivity(activity: NSUserActivity) {
    activity.webpageUrlLikePath()?.let { path ->
        ConsoleRoot.info("handleUserActivity: $path")
        lastBoundPageNavigator?.let { pageNavigator ->
            pageNavigator.navigate(pageNavigator.routes.parse(path) ?: return)
        }
    }
}

private fun NSUserActivity.webpageUrlLikePath(): UrlLikePath? {
    val pathOnly = webpageURL?.absoluteString?.replace(Regex("https?://.*?/"), "") ?: return null
    return UrlLikePath.fromUrlString(pathOnly)
}