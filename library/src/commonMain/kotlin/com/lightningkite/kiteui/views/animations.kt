package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.ScreenTransition

public expect fun RView.animateIn(transition: ScreenTransition, done: (() -> Unit)? = null)
public expect fun RView.animateOut(transition: ScreenTransition, done: (() -> Unit)? = null)