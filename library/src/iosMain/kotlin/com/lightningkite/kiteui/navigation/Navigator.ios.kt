package com.lightningkite.kiteui.navigation

import com.lightningkite.kiteui.views.NContext

actual fun ScreenStack.bindToPlatform(context: NContext) {
    stack.value = (routes.parse(UrlLikePath(listOf(), mapOf())) ?: routes.fallback).let(::listOf)
}