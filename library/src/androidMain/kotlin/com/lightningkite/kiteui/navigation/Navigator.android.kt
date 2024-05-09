package com.lightningkite.kiteui.navigation

import android.app.Activity
import com.lightningkite.kiteui.KiteUiActivity
import com.lightningkite.kiteui.views.RContext

actual fun ScreenStack.bindToPlatform(context: RContext) {
    context.activity.savedInstanceState?.getStringArray("navStack")?.let {
        ScreenStack.main.stack.value = it.mapNotNull { ScreenStack.mainRoutes.parse(UrlLikePath.fromUrlString(it)) }
    } ?: run {
        ScreenStack.main.stack.value = (ScreenStack.mainRoutes.parse(UrlLikePath(listOf(), mapOf())) ?: ScreenStack.mainRoutes.fallback).let(::listOf)
    }
}