package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.Cancellable
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Angle
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.reactive.CalculationContext
import com.lightningkite.kiteui.reactive.Property
import com.lightningkite.kiteui.views.direct.FrameLayout
import com.lightningkite.kiteui.views.direct.LinearLayout
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import platform.CoreGraphics.CGAffineTransformRotate
import platform.UIKit.*
import platform.darwin.NSObject
import platform.objc.sel_registerName
import kotlin.experimental.ExperimentalNativeApi
import com.lightningkite.kiteui.objc.UIViewWithSpacingRulesProtocol

actual fun NView.removeNView(child: NView) {
    child.removeFromSuperview()
    child.shutdown()
}

actual fun NView.listNViews(): List<NView> {
    return subviews.map { it as UIView }
}

var animationsEnabled: Boolean = true
actual inline fun NView.withoutAnimation(action: () -> Unit) {
    if (!animationsEnabled) {
        action()
        return
    }
    try {
        animationsEnabled = false
        action()
    } finally {
        animationsEnabled = true
    }
}

inline fun animateIfAllowed(crossinline action: () -> Unit) {
    if (animationsEnabled) UIView.animateWithDuration(0.15) {
        action()
    } else {
        action()
    }
}

actual fun NView.scrollIntoView(
    horizontal: Align?,
    vertical: Align?,
    animate: Boolean
) {
}

@OptIn(ExperimentalForeignApi::class)
actual fun NView.consumeInputEvents() {
    val actionHolder = object : NSObject() {
        @ObjCAction
        fun eventHandler() {
        }
    }
    val rec = UITapGestureRecognizer(actionHolder, sel_registerName("eventHandler"))
    addGestureRecognizer(rec)
    calculationContext.onRemove {
        // Retain the sleeve until disposed
        rec.enabled
        actionHolder.description
    }
}

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NView = UIView

@OptIn(ExperimentalNativeApi::class)
class NViewCalculationContext() : CalculationContext.WithLoadTracking(), Cancellable {

    val onRemove = ArrayList<() -> Unit>()
    override fun cancel() {
        onRemove.forEach { it() }
        onRemove.clear()
    }

    override fun onRemove(action: () -> Unit) {
        onRemove.add(action)
    }

    val loading = Property(false)
    override fun hideLoad() {
        loading.value = false
    }

    override fun showLoad() {
        loading.value = true
    }
}

fun UIView.shutdown() {
    UIViewCalcContext.getValue(this)?.cancel()
    ExtensionProperty.remove(this)
    subviews.forEach { (it as UIView).shutdown() }
}

val UIView.iosCalculationContext: NViewCalculationContext
    get() = UIViewCalcContext.getValue(this) ?: run {
        val new = NViewCalculationContext()
        UIViewCalcContext.setValue(this, new)
        new
    }
actual val NView.calculationContext: CalculationContext
    get() = iosCalculationContext

actual var NView.exists: Boolean
    get() = !hidden
    set(value) {
        if (animationsEnabled) UIView.animateWithDuration(0.15) {
            hidden = !value
            informParentOfSizeChange()
            layoutIfNeeded()
        } else {
            hidden = !value
            informParentOfSizeChange()
        }
    }

actual var NView.visible: Boolean
    get() = throw NotImplementedError()
    set(value) {
        animateIfAllowed {
            alpha = if (value) 1.0 else 0.0
        }
    }

@OptIn(ExperimentalForeignApi::class)
val UIView.spacingOverride: Property<Dimension?>?
    get() = (this as? UIViewWithSpacingRulesProtocol)
        ?.getSpacingOverrideProperty()
        ?.let { it as? Property<Dimension?> }

actual var NView.ignoreInteraction: Boolean
    get() = !this.userInteractionEnabled
    set(value) {
        userInteractionEnabled = !value
    }

actual var NView.spacing: Dimension
    get() = spacingOverride?.value ?: 0.px
    set(value) {
        spacingOverride?.value = value
    }

actual var NView.opacity: Double
    get() = throw NotImplementedError()
    set(value) {
        animateIfAllowed {
            alpha = value
        }
    }

@OptIn(ExperimentalForeignApi::class)
actual var NView.nativeRotation: Angle
    get() = throw NotImplementedError()
    set(value) {
        val rotation = CGAffineTransformRotate(this.transform, value.radians.toDouble())
        transform = rotation
    }

actual fun NView.clearNViews() {
    this.subviews.toList().forEach {
        (it as UIView).let {
            it.removeFromSuperview()
            it.shutdown()
        }
    }
}

actual fun NView.addNView(child: NView) {
//    child.setTranslatesAutoresizingMaskIntoConstraints(false)
    this.addSubview(child)
}

actual typealias NContext = UIViewController

actual val NView.nContext: NContext
    get() {
        return nextResponder?.let {
            if (it is UIViewController) it
            else if (it is UIView) it.nContext
            else throw IllegalStateException()
        } ?: throw IllegalStateException()
    }
actual val NContext.darkMode: Boolean?
    get() = when (traitCollection.userInterfaceStyle) {
        UIUserInterfaceStyle.UIUserInterfaceStyleDark -> true
        UIUserInterfaceStyle.UIUserInterfaceStyleLight -> false
        else -> null
    }