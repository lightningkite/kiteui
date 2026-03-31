package com.lightningkite.kiteui.views

var ElementContext.appName: String? by lazyContextAddon { it.bestGuessAtAppName() }

internal expect fun ElementContext.bestGuessAtAppName(): String?