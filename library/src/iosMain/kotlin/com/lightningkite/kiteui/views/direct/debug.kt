package com.lightningkite.kiteui.views.direct

import platform.UIKit.UIView

public fun UIView.printablePath(): String = generateSequence(this) { superview }.toList().reversed().joinToString(">") { it::class.simpleName ?: "" }
public var debugMeasuring: Boolean = false
