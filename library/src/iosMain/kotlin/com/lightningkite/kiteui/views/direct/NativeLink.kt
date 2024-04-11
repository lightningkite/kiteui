@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.launchGlobal
import com.lightningkite.kiteui.launchManualCancel
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.SizeConstraints
import com.lightningkite.kiteui.objc.UIViewWithSizeOverridesProtocol
import com.lightningkite.kiteui.views.*
import kotlinx.cinterop.*
import platform.CoreGraphics.*
import platform.Foundation.NSURL
import platform.QuartzCore.CATextLayer
import platform.UIKit.*
import platform.objc.sel_registerName
import kotlin.math.max
import com.lightningkite.kiteui.objc.UIViewWithSpacingRulesProtocol
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.navigation.ScreenStack
import com.lightningkite.kiteui.reactive.Property

//private val UIViewLayoutParams = ExtensionProperty<UIView, LayoutParams>()
//val UIView.layoutParams: LayoutParams by UIViewLayoutParams
//
//class LayoutParams()

@OptIn(ExperimentalForeignApi::class)
class NativeLink: UIButton(CGRectZero.readValue()), UIViewWithSizeOverridesProtocol, UIViewWithSpacingRulesProtocol {
    var padding: Double
        get() = extensionPadding ?: 0.0
        set(value) { extensionPadding = value }
    val spacingOverride: Property<Dimension?> = Property<Dimension?>(null)
    override fun getSpacingOverrideProperty() = spacingOverride

    private val childSizeCache: ArrayList<HashMap<Size, Size>> = ArrayList()
    override fun sizeThatFits(size: CValue<CGSize>): CValue<CGSize> = frameLayoutSizeThatFits(size, childSizeCache)
    override fun layoutSubviews() = frameLayoutLayoutSubviews(childSizeCache)
    override fun subviewDidChangeSizing(view: UIView?) = frameLayoutSubviewDidChangeSizing(view, childSizeCache)
    override fun didAddSubview(subview: UIView) {
        super.didAddSubview(subview)
        frameLayoutDidAddSubview(subview, childSizeCache)
    }
    override fun willRemoveSubview(subview: UIView) {
        if(this != null) frameLayoutWillRemoveSubview(subview, childSizeCache)
        super.willRemoveSubview(subview)
    }

    var toScreen: Screen? = null
    var onNavigator: ScreenStack? = null
    var toUrl: String? = null
    var newTab: Boolean = false
    var resetsStack: Boolean = false
    var onNavigate: suspend ()->Unit = {}

    init {
        addTarget(this, sel_registerName("clicked"), UIControlEventTouchUpInside)
    }

    @ObjCAction fun clicked() {
        toScreen?.let {
            if(resetsStack) {
                onNavigator?.reset(it)
            } else {
                onNavigator?.navigate(it)
            }
        }
        toUrl?.let { UIApplication.sharedApplication.openURL(NSURL(string = it)) }
        calculationContext.launchManualCancel(onNavigate)
    }

    override fun hitTest(point: CValue<CGPoint>, withEvent: UIEvent?): UIView? {
        return frameLayoutHitTest(point, withEvent)
    }

}