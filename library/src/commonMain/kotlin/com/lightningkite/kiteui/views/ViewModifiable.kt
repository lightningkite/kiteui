package com.lightningkite.kiteui.views

import kotlinx.coroutines.CoroutineScope

public interface ViewModifiable: CoroutineScope {
    public val rView: RView
}