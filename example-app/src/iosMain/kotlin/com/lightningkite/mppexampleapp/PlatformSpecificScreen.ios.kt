package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.ObjCountTrackers
import com.lightningkite.kiteui.WeakReference
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.navigatorView
import com.lightningkite.mppexampleapp.AutoRoutes
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlinx.cinterop.*
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UILabel
import platform.UIKit.UIView
import platform.UIKit.accessibilityValue
import platform.darwin.*

val subnav = PageNavigator { AutoRoutes }
@OptIn(ExperimentalForeignApi::class)
actual fun ViewWriter.platformSpecific(): ViewModifiable {
    return col {
        repeat(10) {
            text("TEST")
            separator()
        }
//        text("TEST")
//        card - button {
//            text("memory view toggle")
//            var last: MemoryView? = null
//            onClick {
//                if(subnav.stack.value.lastOrNull() is MemoryPage)
//                    subnav.reset(PlaceholderPage())
//                else
//                    subnav.reset(MemoryPage())
//            }
//        }
//        expanding - navigatorView(subnav)
    }
}

class PlaceholderPage: Page {
    override fun ViewWriter.render() = text("placeholder")
}

class MemoryPage: Page {
    override fun ViewWriter.render(): ViewModifiable {
//        return write(MemoryView(context)) {}

        return write(WrapperView(context)) {
            write(MemoryView2(context)) {
                onRemove { leakDetect() }
            }
            text("i will require things to be retained")
//            space(1.0)
        }

//        return write(MemoryView(context)) {}
//        return frame {
//            text("MEM VIEW ACTIVE")
//            centered - write(MemoryView(context)) {}
//        }
    }
}
class WrapperView(context: RContext): RView(context) {
    @OptIn(ExperimentalForeignApi::class)
    override val native: UIView = UIView(CGRectMake(0.0, 0.0, 0.0, 0.0))
}

class MemoryView(context: RContext): RView(context) {
    val disgustingAmountOfMemory = IntArray(9_000_000) { it }
    @OptIn(ExperimentalForeignApi::class)
    override val native: UIView = UILabel(CGRectMake(0.0, 0.0, 0.0, 0.0)).also {
        it.text = "Native grossness " + disgustingAmountOfMemory[864_518]
    }
}

class MemoryView2(context: RContext): RView(context) {
    @OptIn(ExperimentalForeignApi::class)
    override val native: UIView = UILabel(CGRectMake(0.0, 0.0, 0.0, 0.0)).also {
        it.text = "Huge accessibility label"
        it.accessibilityValue = CharArray(9_000_000) { 'A' }.concatToString()
    }
}

//@OptIn(ExperimentalForeignApi::class)
//class MemoryView(context: RContext): RView(context) {
//    @OptIn(ExperimentalForeignApi::class)
//    override val native: UIView = UIView(CGRectMake(0.0, 0.0, 0.0, 0.0))
//
//    init {
//        val temp = StupidBigView()
//        ObjCountTrackers.track(temp)
//        native.addSubview(temp)
////        temp.removeFromSuperview()
//        dispatch_after(dispatch_time(DISPATCH_TIME_NOW, 1000 * NSEC_PER_MSEC.toLong()), dispatch_get_main_queue()) {
//            temp.removeFromSuperview()
//        }
//    }
//}
//
//@OptIn(ExperimentalForeignApi::class)
//class StupidBigView: UIView(CGRectMake(0.0, 0.0, 0.0, 0.0)) {
//    init {
//        this.accessibilityValue = "Ughy".repeat(9_000_000)
//    }
//}