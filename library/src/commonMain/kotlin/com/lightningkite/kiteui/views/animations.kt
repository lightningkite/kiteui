package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.ScreenTransition

expect fun Element.animateIn(transition: ScreenTransition, done: (() -> Unit)? = null)
expect fun Element.animateOut(transition: ScreenTransition, done: (() -> Unit)? = null)