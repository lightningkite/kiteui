package com.lightningkite.kiteui.views

public var ElementContext.appName: String? by lazyContextAddon(global = true) { it.bestGuessAtAppName() }

internal expect fun ElementContext.bestGuessAtAppName(): String?