@file:Suppress("NOTHING_TO_INLINE")
package com.lightningkite.kiteui.models

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.math.PI
import kotlin.math.absoluteValue

@JvmInline
@Serializable
public value class Angle(public val turns: Float) {
    public constructor(turns: Double):this(turns.toFloat())
    public companion object {
        public const val RADIANS_PER_CIRCLE: Float = (PI * 2).toFloat()
        public const val DEGREES_PER_CIRCLE: Float = 360f
        public fun atan2(y: Float, x: Float): Angle = kotlin.math.atan2(y, x).radians
        public val zero: Angle = Angle(0f)
        public val circle: Angle = Angle(1f)
        public val halfTurn: Angle = Angle(.5f)
        public val quarterTurn: Angle = Angle(.25f)
        public val eighthTurn: Angle = Angle(.125f)
        public val thirdTurn: Angle = Angle(1 / 3.0f)
    }

    public inline val degrees: Float get() = turns * DEGREES_PER_CIRCLE
    public inline val radians: Float get() = turns * RADIANS_PER_CIRCLE

    //For absolute angles
    public inline infix fun angleTo(other: Angle): Angle {
        return Angle((other.turns - this.turns + .5f).positiveRemainder(1f) - .5f)
    }

    //For relative angles
    public inline operator fun plus(other: Angle): Angle = Angle(this.turns + other.turns)

    public inline operator fun minus(other: Angle): Angle = Angle(this.turns - other.turns)
    public inline operator fun times(scale: Float): Angle = Angle(this.turns * scale)
    public inline operator fun div(by: Float): Angle = Angle(this.turns / by)

    public fun normalized(): Angle = Angle(this.turns.plus(.5f).positiveRemainder(1f).minus(.5f))

    public inline fun sin(): Float = kotlin.math.sin(radians)
    public inline fun cos(): Float = kotlin.math.cos(radians)
    public inline fun tan(): Float = kotlin.math.tan(radians)

    public inline operator fun unaryMinus(): Angle = Angle(-turns)

    public val absoluteValue: Angle get() = Angle(turns.absoluteValue)
}

public inline val Int.turns: Angle get() = Angle(this.toFloat())
public inline val Int.degrees: Angle get() = Angle(this.toFloat() / Angle.DEGREES_PER_CIRCLE)
public inline val Int.radians: Angle get() = Angle(this.toFloat() / Angle.RADIANS_PER_CIRCLE)
public inline val Float.turns: Angle get() = Angle(this)
public inline val Float.degrees: Angle get() = Angle(this / Angle.DEGREES_PER_CIRCLE)
public inline val Float.radians: Angle get() = Angle(this / Angle.RADIANS_PER_CIRCLE)
public inline val Double.turns: Angle get() = Angle(this.toFloat())
public inline val Double.degrees: Angle get() = Angle(this.toFloat() / Angle.DEGREES_PER_CIRCLE)
public inline val Double.radians: Angle get() = Angle(this.toFloat() / Angle.RADIANS_PER_CIRCLE)
