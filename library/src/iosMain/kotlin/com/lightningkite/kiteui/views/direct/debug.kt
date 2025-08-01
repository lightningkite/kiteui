package com.lightningkite.kiteui.views.direct

import platform.UIKit.UIView

public fun UIView.printablePath() = generateSequence(this) { superview }.toList().reversed().joinToString(">") { it::class.simpleName ?: "" }
public var debugMeasuring: Boolean = false
