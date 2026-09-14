package com.lightningkite.kiteui.objc

import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreFoundation.CFRetain
import platform.CoreGraphics.CGColorRef
import platform.Foundation.CFBridgingRelease


public fun CGColorRef.toObjcId(): Any = CFBridgingRelease(CFRetain(this))!!