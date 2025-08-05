package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.models.ScreenTransition

@InternalKiteUi
public expect fun RView.animateIn(transition: ScreenTransition, done: (() -> Unit)? = null)
@InternalKiteUi
public expect fun RView.animateOut(transition: ScreenTransition, done: (() -> Unit)? = null)