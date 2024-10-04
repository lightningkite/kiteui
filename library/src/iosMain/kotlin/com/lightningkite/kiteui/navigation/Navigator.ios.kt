package com.lightningkite.kiteui.navigation

import com.lightningkite.kiteui.views.RContext
import platform.Foundation.NSUserActivity

private var postedLandingUrl: UrlLikePath? = null
private var lastBoundScreenNavigator: ScreenNavigator? = null

actual fun ScreenNavigator.bindToPlatform(context: RContext) {
    lastBoundScreenNavigator = this
    val landing = routes.parse(postedLandingUrl ?: UrlLikePath.EMPTY)
    stack.value = listOf(landing ?: routes.fallback)
}

// If called from scene(_:willConnectTo:options:) when the app is not already running, we need to wait until
// the lateinit ScreenStack.mainRoutes has been initialized, thus the need for postUserActivity() AND handleUserActivity()

// To be called from scene(_:willConnectTo:options:)
fun postUserActivity(activity: NSUserActivity) {
    postedLandingUrl = activity.webpageUrlLikePath()
}

// To be called from scene(_:continue:)
fun handleUserActivity(activity: NSUserActivity) {
    activity.webpageUrlLikePath()?.let { path ->
        lastBoundScreenNavigator?.let { screenNavigator ->
            screenNavigator.navigate(screenNavigator.routes.parse(path) ?: return)
        }
    }
}

private fun NSUserActivity.webpageUrlLikePath(): UrlLikePath? {
    val pathOnly = webpageURL?.absoluteString?.replace(Regex("https?://.*?/"), "") ?: return null
    return UrlLikePath.fromUrlString(pathOnly)
}