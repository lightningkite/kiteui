package com.lightningkite.kiteui.views

import kotlinx.coroutines.CoroutineScope

interface ViewModifiable: CoroutineScope {
    val rView: RView
}