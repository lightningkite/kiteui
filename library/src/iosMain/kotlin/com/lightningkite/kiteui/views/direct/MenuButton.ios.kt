package com.lightningkite.kiteui.views.direct

import ViewWriter
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.models.times
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.navigation.ScreenStack
import com.lightningkite.kiteui.reactive.Property
import com.lightningkite.kiteui.reactive.invoke
import com.lightningkite.kiteui.views.*
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGRectZero
import platform.CoreGraphics.CGSize
import platform.UIKit.UIButton
import platform.UIKit.UIControlEventTouchUpInside
import platform.UIKit.UIEvent
import platform.UIKit.UIView
import com.lightningkite.kiteui.objc.UIViewWithSizeOverridesProtocol
import com.lightningkite.kiteui.objc.UIViewWithSpacingRulesProtocol

@Suppress("ACTUAL_WITHOUT_EXPECT")
@OptIn(ExperimentalForeignApi::class)
actual class NMenuButton: UIButton(CGRectZero.readValue()), UIViewWithSizeOverridesProtocol, UIViewWithSpacingRulesProtocol {
    var padding: Double
        get() = extensionPadding ?: 0.0
        set(value) { extensionPadding = value }

    lateinit var navigator: ScreenStack
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

    override fun hitTest(point: CValue<CGPoint>, withEvent: UIEvent?): UIView? {
        return frameLayoutHitTest(point, withEvent)
    }
//    init {
//        addTarget(this, sel_registerName("test"), UIControlEventTouchUpInside)
//    }
//
//    @ObjCAction
//    fun test() {
//        println("test")
//    }
}
@ViewDsl
actual inline fun ViewWriter.menuButtonActual(crossinline setup: MenuButton.() -> Unit): Unit = element(NMenuButton()) {
    val l = iosCalculationContext.loading
    this.navigator = this@menuButtonActual.navigator
    handleThemeControl(this) {
        setup(MenuButton(this))
    }
}


actual fun MenuButton.opensMenu(action: ViewWriter.() -> Unit) {
    native.onEvent(UIControlEventTouchUpInside) {
        val originalNavigator = native.navigator
        native.navigator.dialog.navigate(object : Screen {
            override fun ViewWriter.render() {
                val dialogNavigator = navigator
                dismissBackground {
                    centered - card - stack {
                        val theme = currentTheme
                        ::spacing { theme().spacing * 2 }
                        popoverClosers.add {
                            dialogNavigator.dismiss()
                        }
                        navigator = originalNavigator
                        action()
                        navigator = dialogNavigator
                    }
                }
            }
        })
    }
}

actual inline var MenuButton.enabled: Boolean
    get() = native.enabled
    set(value) {
        native.enabled = value
    }
actual var MenuButton.requireClick: Boolean
    get() = true
    set(value) {}
actual var MenuButton.preferredDirection: PopoverPreferredDirection
    get() = PopoverPreferredDirection.belowLeft
    set(value) {}