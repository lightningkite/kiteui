package com.lightningkite.kiteui.navigation

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual object PlatformNavigator: LocalNavigator({ PlatformNavigator.routes }, LocalNavigator({ PlatformNavigator.routes }, null)) {
    private lateinit var _routes: Routes
    actual override var routes: Routes
        get() = _routes
        set(value) {
            _routes = value

            // The navigation stack could be recreated using savedInstanceState data in KiteUiActivity.onCreate; only
            // navigate to root if not
            if (isStackEmpty())
                navigate(routes.parse(UrlLikePath(listOf(), mapOf())) ?: routes.fallback)
        }
}