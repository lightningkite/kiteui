package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.InternalKiteUi
import platform.UIKit.UIView

@InternalKiteUi
public fun UIView.printablePath(): String = generateSequence(this) { superview }.toList().reversed().joinToString(">") { it::class.simpleName ?: "" }
@InternalKiteUi
public var debugMeasuring: Boolean = false
