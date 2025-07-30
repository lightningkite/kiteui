package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import platform.UIKit.UIApplicationOpenURLOptionsKey

abstract class KiteUiAppDelegate {
    open val theme: ReactiveContext.() -> Theme get() = { Theme.placeholder }
    abstract val mainNavigator : PageNavigator
    fun openURL(url: String, options: Map<UIApplicationOpenURLOptionsKey, Any?>) {
        mainNavigator.navigateUrlLikePath(url.substringAfter("://").substringAfter("/"))
    }
    fun applicationDidReceiveMemoryWarning() {
        repeat(3) { gc() }
    }
}