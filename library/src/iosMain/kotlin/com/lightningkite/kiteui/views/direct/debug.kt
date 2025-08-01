package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.InternalKiteUi
import platform.UIKit.UIView

@InternalKiteUi
public fun UIView.printablePath() = generateSequence(this) { superview }.toList().reversed().joinToString(">") { it::class.simpleName ?: "" }
public var debugMeasuring: Boolean = false
