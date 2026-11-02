package com.lightningkite.kiteui.views

/**
 * The app's name, or `null` if it isn't set and the platform doesn't give one.
 */
public var ElementContext.appName: String? by lazyContextAddon(ContextAddon.Scope.Global) { it.bestGuessAtAppName() }

internal expect fun ElementContext.bestGuessAtAppName(): String?