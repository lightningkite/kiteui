package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.objc.UIViewWithSpacingRulesProtocol
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlin.math.min
import kotlinx.cinterop.*
import platform.CoreGraphics.*
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.QuartzCore.CALayer
import platform.QuartzCore.CATransform3DMakeScale
import platform.UIKit.*

actual class IconView actual constructor(context: ElementContext): RView(context) {
    override val native = NIconView()
    actual var source: Icon?
        get() = native.icon
        set(value) {
            native.icon = value
        }
    actual var description: String?
        get() {
            return native.accessibilityLabel.toString()
        }
        set(value) {
            native.accessibilityLabel = value
        }

    override fun applyTheme(theme: ThemeAndBack) { super.applyTheme(theme); val theme = theme.theme
        native.iconPaint = theme.icon
    }
}


@Suppress("ACTUAL_WITHOUT_EXPECT")
actual class NIconView(): NView(CGRectMake(0.0,0.0,0.0,0.0)), UIViewWithSpacingRulesProtocol {
    init {
        userInteractionEnabled = false
        if (ENABLE_DYNAMIC_TYPE) {
            NSNotificationCenter.defaultCenter.addObserverForName(UIContentSizeCategoryDidChangeNotification, null, NSOperationQueue.mainQueue) {
                informParentOfSizeChange()
            }
        }
    }

    val spacingOverride: Signal<Dimension?> = Signal<Dimension?>(null)
    override fun getSpacingOverrideProperty() = spacingOverride

    override fun drawLayer(layer: CALayer, inContext: CGContextRef?) {
        super.drawLayer(layer, inContext)
    }
    private var iconLayer: CALayer? = null
    var icon: Icon? = null
        set(value) {
            field = value
            refresh()
            informParentOfSizeChange()
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
        val padding = (extensionPadding ?: Edges.ZERO).plus(extensionSafeInsetPadding ?: Edges.ZERO)
        val contentAreaSize = currentSize.first - padding.horizontalSum.value to currentSize.second - padding.verticalSum.value

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
        val padding = (extensionPadding ?: Edges.ZERO).plus(extensionSafeInsetPadding ?: Edges.ZERO)
        val scaleFactor = preferredScaleFactor()
        return CGSizeMake(
            icon?.width?.value?.let { it * scaleFactor + padding.horizontalSum.value } ?: 0.0,
            icon?.height?.value?.let { it * scaleFactor + padding.verticalSum.value } ?: 0.0
        )
    }

    override fun hitTest(point: CValue<CGPoint>, withEvent: UIEvent?): UIView? = null
    override fun pointInside(point: CValue<CGPoint>, withEvent: UIEvent?): Boolean = false
}
