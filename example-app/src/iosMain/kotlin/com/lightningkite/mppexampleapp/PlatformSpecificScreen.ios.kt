package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.ObjCountTrackers
import com.lightningkite.kiteui.WeakReference
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.navigation.ScreenNavigator
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.navigatorView
import kotlinx.cinterop.*
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UILabel
import platform.UIKit.UIView
import platform.UIKit.accessibilityValue
import platform.darwin.*

val subnav = ScreenNavigator { AutoRoutes }
@OptIn(ExperimentalForeignApi::class)
actual fun ViewWriter.platformSpecific() {
    col {
        text("TEST")
        card - button {
            text("memory view toggle")
            var last: MemoryView? = null
            onClick {
                if(subnav.stack.value.lastOrNull() is MemoryScreen)
                    subnav.reset(PlaceholderScreen())
                else
                    subnav.reset(MemoryScreen())
            }
        }
        expanding - navigatorView(subnav)
    }
}

class PlaceholderScreen: Screen {
    override fun ViewWriter.render() = text("placeholder")
}

class MemoryScreen: Screen {
    override fun ViewWriter.render(): Any? {
//        return write(MemoryView(context)) {}

        return write(WrapperView(context)) {
            write(MemoryView2(context)) {
                onRemove { leakDetect() }
            }
            text("i will require things to be retained")
//            space(1.0)
        }

//        return write(MemoryView(context)) {}
//        return stack {
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