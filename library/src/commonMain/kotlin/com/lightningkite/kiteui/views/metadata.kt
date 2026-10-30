package com.lightningkite.kiteui.views

public var ElementContext.appName: String? by lazyContextAddon(ContextAddon.Scope.Global) { it.bestGuessAtAppName() }

internal expect fun ElementContext.bestGuessAtAppName(): String?