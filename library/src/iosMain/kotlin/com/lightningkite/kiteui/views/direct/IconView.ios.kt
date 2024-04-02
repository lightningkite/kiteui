package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import kotlinx.cinterop.*
import platform.CoreGraphics.*
import platform.QuartzCore.CALayer
import platform.QuartzCore.CATransform3DMakeScale
import platform.UIKit.*

@OptIn(ExperimentalForeignApi::class)
@Suppress("ACTUAL_WITHOUT_EXPECT")
actual class NIconView(): NView(CGRectMake(0.0,0.0,0.0,0.0)) {
    init {
        setUserInteractionEnabled(false)
    }
    override fun drawLayer(layer: CALayer, inContext: CGContextRef?) {
        super.drawLayer(layer, inContext)
    }
    private var iconLayer: CALayer? = null
    var icon: Icon? = null
        set(value) {
            field = value
            refresh()
        }
    var iconPaint: Paint = Color.black
        set(value) {
            field = value
            refresh()
        }
    var iconOriginalSize: Pair<CGFloat, CGFloat> = 1.0 to 1.0
    private fun refresh() {
        iconLayer?.removeFromSuperlayer()
        iconLayer = icon?.toImageSource(iconPaint)?.caLayer()?.also {
            iconOriginalSize = it.frame.useContents { size.width to size.height }
            layer.addSublayer(it)
        }
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        val currentSize = bounds.useContents { size.width to size.height }
        iconLayer?.transform = CATransform3DMakeScale(
            currentSize.first / iconOriginalSize.first,
            currentSize.second / iconOriginalSize.second,
            1.0
        )
        iconLayer?.frame = bounds
    }

    override fun sizeThatFits(size: CValue<CGSize>): CValue<CGSize> {
        return CGSizeMake(icon?.width?.value ?: 0.0, icon?.height?.value ?: 0.0)
    }

    override fun hitTest(point: CValue<CGPoint>, withEvent: UIEvent?): UIView? = null
    override fun pointInside(point: CValue<CGPoint>, withEvent: UIEvent?): Boolean = false
}

@ViewDsl
actual inline fun ViewWriter.iconActual(crossinline setup: IconView.() -> Unit): Unit = element(NIconView()) {
    handleTheme(
        this, viewDraws = true, viewLoads = true,
        foreground = { theme ->
            this.iconPaint = theme.icon
        },
    ) {
        this.contentMode = UIViewContentMode.UIViewContentModeScaleAspectFit
        setup(IconView(this))
    }
}

actual inline var IconView.source: Icon?
    get() = native.icon
    set(value) {
        native.icon = value
    }
actual inline var IconView.description: String?
    get() = native.accessibilityLabel
    set(value) {
        native.accessibilityLabel = value
    }
