package com.lightningkite.kiteui.views.direct

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView

/**
 * A frame that has transparency and blurs the views behind it.
 * If a background is applied to it, the background should go on top so that you can use a background with alpha to tint the glass effect.
 */
actual class GlassFrame actual constructor(context: RContext) : RView(context) {
    override val native = FrameLayout(context.activity)
    override fun defaultLayoutParams(): ViewGroup.LayoutParams =
        FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

    actual var blurStrength: Double = 10.0
        set(value) {
            field = value
            updateBlur()
        }

    private fun updateBlur() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            applyBlurEffect()
        } else {
            // For older Android versions, we can't use RenderEffect
            // We could implement a custom solution using RenderScript or other methods,
            // but for simplicity, we'll just set a semi-transparent background
            native.alpha = 0.8f
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun applyBlurEffect() {
        val blurEffect = RenderEffect.createBlurEffect(
            blurStrength.toFloat(), 
            blurStrength.toFloat(), 
            Shader.TileMode.CLAMP
        )
        native.setRenderEffect(blurEffect)
    }

    init {
        updateBlur()
    }
}