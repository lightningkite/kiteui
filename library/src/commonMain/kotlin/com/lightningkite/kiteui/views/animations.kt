package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.ScreenTransition

expect fun RView.animateIn(transition: ScreenTransition, done: (() -> Unit)? = null)
expect fun RView.animateOut(transition: ScreenTransition, done: (() -> Unit)? = null)