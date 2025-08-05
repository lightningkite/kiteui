package com.lightningkite.kiteui

import platform.Foundation.NSDate
import platform.Foundation.date
import platform.Foundation.timeIntervalSince1970

@InternalKiteUi
public actual fun clockMillis(): Double = NSDate.date().timeIntervalSince1970 * 1000.0