package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.Property
import com.lightningkite.kiteui.views.*
import kotlinx.cinterop.*
import platform.CoreGraphics.*
import platform.QuartzCore.CALayer
import platform.QuartzCore.CATransform3DMakeScale
import platform.UIKit.*
import kotlin.math.min
import com.lightningkite.kiteui.objc.UIViewWithSpacingRulesProtocol
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue

@OptIn(ExperimentalForeignApi::class)
@Suppress("ACTUAL_WITHOUT_EXPECT")
actual class NIconView(): NView(CGRectMake(0.0,0.0,0.0,0.0)), UIViewWithSpacingRulesProtocol {
    init {
        setUserInteractionEnabled(false)
        if (ENABLE_DYNAMIC_TYPE) {
            NSNotificationCenter.defaultCenter.addObserverForName(UIContentSizeCategoryDidChangeNotification, null, NSOperationQueue.mainQueue) {
                informParentOfSizeChange()
            }
        }
    }

    val spacingOverride: Property<Dimension?> = Property<Dimension?>(null)
    override fun getSpacingOverrideProperty() = spacingOverride

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
        val axisTotalPadding = (extensionPadding ?: 0.0) * 2
        val contentAreaSize = currentSize.first - axisTotalPadding to currentSize.second - axisTotalPadding

        val scale = min(contentAreaSize.first / iconOriginalSize.first, contentAreaSize.second / iconOriginalSize.second)
        val nw = iconOriginalSize.first * scale
        val nh = iconOriginalSize.second * scale
        iconLayer?.transform = CATransform3DMakeScale(
            scale,
            scale,
            1.0
        )
        iconLayer?.frame = CGRectMake((currentSize.first - nw) / 2, (currentSize.second - nh) / 2, nw, nh)
    }

    override fun sizeThatFits(size: CValue<CGSize>): CValue<CGSize> {
        val axisTotalPadding = (extensionPadding ?: 0.0) * 2
        val scaleFactor = preferredScaleFactor()
        return CGSizeMake(
            icon?.width?.value?.let { it * scaleFactor + axisTotalPadding } ?: 0.0,
            icon?.height?.value?.let { it * scaleFactor + axisTotalPadding } ?: 0.0
        )
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
