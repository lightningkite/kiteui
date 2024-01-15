@file:OptIn(ExperimentalForeignApi::class)

package com.lightningkite.rock.views

import com.lightningkite.rock.models.Angle
import com.lightningkite.rock.reactive.*
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGAffineTransformRotate
import platform.UIKit.*

//inline fun <T : HTMLElement> ViewWriter.containsNext(name: String, noinline setup: T.() -> Unit): ViewWrapper =
//    wrapNext<T>(document.createElement(name) as T, setup)
//
//inline fun <T : HTMLElement> ViewWriter.element(name: String, noinline setup: T.() -> Unit) =
//    element(document.createElement(name) as T, setup)

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NView = UIView

class NViewCalculationContext(): CalculationContext {
    val onRemoveList = ArrayList<()->Unit>()
    override fun onRemove(action: () -> Unit) {
        onRemoveList.add(action)
    }

    override fun notifyStart() {
    }

    override fun notifyComplete(result: Result<Unit>) {
        super.notifyComplete(result)
    }

    fun shutdown() {
        onRemoveList.forEach { it() }
        onRemoveList.clear()
    }
}

fun UIView.shutdown() {
    UIViewCalcContext.getValue(this)?.shutdown()
    ExtensionProperty.remove(this)
    subviews.forEach { (it as UIView).shutdown() }
}

val UIView.iosCalculationContext: NViewCalculationContext get() = UIViewCalcContext.getValue(this) ?: run {
    val new = NViewCalculationContext()
    UIViewCalcContext.setValue(this, new)
    new
}
actual val NView.calculationContext: CalculationContext
    get() = iosCalculationContext

actual var NView.exists: Boolean
    get() = !hidden
    set(value) {
        hidden = !value
        informParentOfSizeChange()
    }

actual var NView.visible: Boolean
    get() = throw NotImplementedError()
    set(value) {
        alpha = if (value) 1.0 else 0.0
    }

actual var NView.opacity: Double
    get() = throw NotImplementedError()
    set(value) {
        alpha = value
    }

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
