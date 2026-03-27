package com.lightningkite.kiteui.views.direct

import android.content.res.ColorStateList
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.graphics.drawable.shapes.RoundRectShape
import android.view.Gravity
import android.widget.SeekBar
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlin.math.roundToInt

actual class Slider actual constructor(context: RContext) : RView(context) {
    override val driverValue: String? get() = sliderDriverValue()
    override val driverActions get() = super.driverActions + sliderDriverActions()
    private val nativeSeekBar = SeekBar(context.activity)
    override val native = nativeSeekBar

    private val fillShape = ShapeDrawable().apply {
        shape = RoundRectShape(floatArrayOf(999f, 999f, 999f, 999f, 999f, 999f, 999f, 999f), null, null)
    }

    init {
        themeChoice += FieldSemantic
        nativeSeekBar.splitTrack = false
    }

    private val valueProp = Signal(0.5f)
    actual val value: MutableReactiveValue<Float>
        get() {
            return object : MutableReactiveValue<Float> {
                override fun addListener(listener: () -> Unit): () -> Unit {
                    val seekBarListener = object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                            if (fromUser) {
                                listener()
                            }
                        }

                        override fun onStartTrackingTouch(seekBar: SeekBar) {}

                        override fun onStopTrackingTouch(seekBar: SeekBar) {}
                    }

                    nativeSeekBar.setOnSeekBarChangeListener(seekBarListener)

                    return {
                        nativeSeekBar.setOnSeekBarChangeListener(null)
                    }
                }

                override var value: Float
                    get() {
                        val progress = nativeSeekBar.progress
                        val range = max - min
                        val rawValue = min + (progress / 1000f) * range

                        // Apply step if it's set
                        return step?.let { stepValue ->
                            if (stepValue > 0) {
                                val steps = ((rawValue - min) / stepValue).toInt()
                                min + (steps * stepValue)
                            } else {
                                rawValue
                            }
                        } ?: rawValue
                    }
                    set(value) {
                        // Ensure value is within min/max range
                        val clampedValue = value.coerceIn(min, max)

                        // Apply step if it's set
                        val finalValue = step?.let { stepValue ->
                            if (stepValue > 0) {
                                val steps = ((clampedValue - min) / stepValue).toInt()
                                min + (steps * stepValue)
                            } else {
                                clampedValue
                            }
                        } ?: clampedValue

                        // Convert to progress (0-1000)
                        val range = max - min
                        val normalizedValue = (finalValue - min) / range
                        val progress = (normalizedValue * 1000).toInt()

                        if (nativeSeekBar.progress != progress) {
                            nativeSeekBar.progress = progress
                        }
                    }
            }
        }

    actual var min: Float = 0f
        set(value) {
            field = value
            // Update current value to ensure it's within new range
            this.value.value = this.value.value
        }

    actual var max: Float = 1f
        set(value) {
            field = value
            // Update current value to ensure it's within new range
            this.value.value = this.value.value
        }

    actual var step: Float? = null
        set(value) {
            field = value
            // Android SeekBar doesn't have a built-in step property
            // We handle this in the value setter
        }

    init {
        // Android SeekBar uses integers for progress (0-1000)
        nativeSeekBar.max = 1000

        // Initialize with the property value
        value.value = valueProp.value
    }

    override fun refreshPadding() {
        native.setPadding(0, 0, 0, 0)
    }

    actual var enabled: Boolean
        get() = nativeSeekBar.isEnabled
        set(value) {
            nativeSeekBar.isEnabled = value
            refreshTheming()
        }

    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        var t = theme
        if (!enabled) t = t[DisabledSemantic]
        return super.applyState(t)
    }

    override fun applyTheme(theme: ThemeAndBack) {
        val fieldTheme = theme[FieldSemantic]
        super.applyTheme(fieldTheme)
        val t = fieldTheme.theme

        // Build custom progress drawable similar to ProgressBar
        val trackShape = ShapeDrawable().apply {
            shape = RoundRectShape(floatArrayOf(999f, 999f, 999f, 999f, 999f, 999f, 999f, 999f), null, null)
            paint.color = android.graphics.Color.TRANSPARENT
        }
        fillShape.paint.color = t.foreground.colorInt()
        val clipDrawable = ClipDrawable(fillShape, Gravity.START, ClipDrawable.HORIZONTAL)
        nativeSeekBar.progressDrawable = LayerDrawable(arrayOf(trackShape, clipDrawable)).apply {
            setId(0, android.R.id.background)
            setId(1, android.R.id.progress)
        }

        // Create neumorphic thumb
        val density = context.activity.resources.displayMetrics.density
        val thumbSize = (24 * density).roundToInt()
        val thumbDrawable = ShapeDrawable(OvalShape()).apply {
            intrinsicWidth = thumbSize
            intrinsicHeight = thumbSize
            paint.color = t.background.closestColor().colorInt()
            paint.isAntiAlias = true
            paint.setShadowLayer(8f * density, 4f * density, 4f * density, android.graphics.Color.argb(40, 0, 0, 0))
        }
        nativeSeekBar.thumb = thumbDrawable
    }
}
