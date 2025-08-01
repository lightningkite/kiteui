package com.lightningkite.kiteui.objc

import com.lightningkite.kiteui.InternalKiteUi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreFoundation.CFRetain
import platform.CoreGraphics.CGColorRef
import platform.Foundation.CFBridgingRelease


@InternalKiteUi
public fun CGColorRef.toObjcId(): Any = CFBridgingRelease(CFRetain(this))!!