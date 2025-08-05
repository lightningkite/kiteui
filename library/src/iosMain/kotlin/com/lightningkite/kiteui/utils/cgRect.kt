@file:OptIn(ExperimentalForeignApi::class)
package com.lightningkite.kiteui.utils

import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.models.Rect
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGPointMake

@InternalKiteUi
public val CValue<CGRect>.local: Rect
    get() = useContents {
        return Rect(origin.x, origin.y, origin.x + size.width, origin.y + size.height)
    }

@InternalKiteUi
public typealias LocalPoint = Pair<Double, Double>

@InternalKiteUi
public val CValue<CGPoint>.local: LocalPoint
    get() = useContents {
        return x to y
    }

@InternalKiteUi
public operator fun LocalPoint.plus(other: LocalPoint) : LocalPoint =
    first + other.first to second + other.second

@InternalKiteUi
public operator fun LocalPoint.minus(other: LocalPoint) : LocalPoint =
    first - other.first to second - other.second

@InternalKiteUi
public operator fun LocalPoint.times(other: Double) : LocalPoint =
    first * other to second * other

@InternalKiteUi
public operator fun LocalPoint.div(other: Int) : LocalPoint =
    first / other to second / other

@InternalKiteUi
public val LocalPoint.cg: CValue<CGPoint>
    get() = CGPointMake(first, second)