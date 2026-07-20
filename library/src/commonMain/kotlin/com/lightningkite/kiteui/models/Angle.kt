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
        public const val RADIANS_PER_CIRCLE = (PI * 2).toFloat()
        public const val DEGREES_PER_CIRCLE = 360f
        public fun atan2(y: Float, x: Float) = kotlin.math.atan2(y, x).radians
        public val zero = Angle(0f)
        public val circle = Angle(1f)
        public val halfTurn = Angle(.5f)
        public val quarterTurn = Angle(.25f)
        public val eighthTurn = Angle(.125f)
        public val thirdTurn = Angle(1 / 3.0f)
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
    public inline operator fun times(scale: Float) = Angle(this.turns * scale)
    public inline operator fun div(by: Float) = Angle(this.turns / by)

    public fun normalized(): Angle = Angle(this.turns.plus(.5f).positiveRemainder(1f).minus(.5f))

    public inline fun sin(): Float = kotlin.math.sin(radians)
    public inline fun cos(): Float = kotlin.math.cos(radians)
    public inline fun tan(): Float = kotlin.math.tan(radians)

    public inline operator fun unaryMinus() = Angle(-turns)

    public val absoluteValue: Angle get() = Angle(turns.absoluteValue)
}

public inline val Int.turns get() = Angle(this.toFloat())
public inline val Int.degrees get() = Angle(this.toFloat() / Angle.DEGREES_PER_CIRCLE)
public inline val Int.radians get() = Angle(this.toFloat() / Angle.RADIANS_PER_CIRCLE)
public inline val Float.turns get() = Angle(this)
public inline val Float.degrees get() = Angle(this / Angle.DEGREES_PER_CIRCLE)
public inline val Float.radians get() = Angle(this / Angle.RADIANS_PER_CIRCLE)
public inline val Double.turns get() = Angle(this.toFloat())
public inline val Double.degrees get() = Angle(this.toFloat() / Angle.DEGREES_PER_CIRCLE)
public inline val Double.radians get() = Angle(this.toFloat() / Angle.RADIANS_PER_CIRCLE)
