package com.lightningkite.kiteui.views.direct

import platform.UIKit.UIView

internal fun UIView.printablePath(): String = generateSequence(this) { superview }.toList().reversed().joinToString(">") { it::class.simpleName ?: "" }
internal var debugMeasuring: Boolean = false
