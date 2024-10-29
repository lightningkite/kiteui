@file:OptIn(ExperimentalForeignApi::class)
package com.lightningkite.kiteui.utils

import com.lightningkite.kiteui.models.Rect
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRect

val CValue<CGRect>.local: Rect
    get() = useContents {
        return Rect(origin.x, origin.y, origin.x + size.width, origin.y + size.height)
    }