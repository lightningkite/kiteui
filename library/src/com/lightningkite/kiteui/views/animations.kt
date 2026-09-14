package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.ScreenTransition

public expect fun Element.animateIn(transition: ScreenTransition, done: (() -> Unit)? = null)
public expect fun Element.animateOut(transition: ScreenTransition, done: (() -> Unit)? = null)