package com.lightningkite.kiteui.views


import com.lightningkite.kiteui.AppScope
import com.lightningkite.kiteui.ExternalServices
import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.objc.cgRectValue
import com.lightningkite.kiteui.reactive.Readable
import com.lightningkite.kiteui.reactive.invoke
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.direct.observe
import kotlinx.cinterop.*
import kotlinx.coroutines.DelicateCoroutinesApi
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSNumber
import platform.Foundation.NSValue
import platform.UIKit.*
import platform.darwin.*
import platform.darwin.sel_registerName
import platform.objc.*

fun UIViewController.setup(theme: Theme, app: ViewWriter.() -> Unit) {
    setup({ theme }, app)
}

fun UIViewController.setup(themeReadable: Readable<Theme>, app: ViewWriter.() -> Unit) {
    setup({ themeReadable.invoke() }, app)
}

var rootViewController: UIViewController? = null
    private set

private val systemBarBackground = UIView()
val setSystemBarBackground = systemBarBackground::setBackgroundColor

fun UIViewController.setup(themeCalculation: ReactiveContext.() -> Theme, app: ViewWriter.() -> Unit) {
    rootViewController = this
    ExternalServices.currentPresenter = { presentViewController(it, animated = true, completion = null) }
//    UIView.setAnimationsEnabled(false)

    @OptIn(DelicateCoroutinesApi::class)
    val writer = object : ViewWriter(), CalculationContext by AppScope {
        override val context: RContext = RContext(this@setup)
        override fun addChild(view: RView) {
            this@setup.view.addSubview(view.native)
        }

        init {
            beforeNextElementSetup {
                ::themeChoice { ThemeDerivation.Set(themeCalculation()) }
            }
        }
    }
    writer.app()

    val subview = view.subviews.first() as UIView
    subview.translatesAutoresizingMaskIntoConstraints = false
    subview.topAnchor.constraintEqualToAnchor(view.safeAreaLayoutGuide.topAnchor).setActive(true)
    subview.leftAnchor.constraintEqualToAnchor(view.safeAreaLayoutGuide.leftAnchor).setActive(true)
    subview.rightAnchor.constraintEqualToAnchor(view.safeAreaLayoutGuide.rightAnchor).setActive(true)
    val bottom = view.safeAreaLayoutGuide.bottomAnchor.constraintEqualToAnchor(subview.bottomAnchor)
    bottom.setActive(true)

    view.addSubview(systemBarBackground)
    systemBarBackground.translatesAutoresizingMaskIntoConstraints = false
    systemBarBackground.topAnchor.constraintEqualToAnchor(view.topAnchor).setActive(true)
    systemBarBackground.leftAnchor.constraintEqualToAnchor(view.safeAreaLayoutGuide.leftAnchor).setActive(true)
    systemBarBackground.rightAnchor.constraintEqualToAnchor(view.safeAreaLayoutGuide.rightAnchor).setActive(true)
    systemBarBackground.bottomAnchor.constraintEqualToAnchor(view.safeAreaLayoutGuide.topAnchor).setActive(true)
    writer.reactiveScope {
        systemBarBackground.backgroundColor = themeCalculation()[SystemBarSemantic].theme.background.closestColor().toUiColor()
    }

    writer.reactiveScope {
        view.backgroundColor = themeCalculation()[BarSemantic].theme.background.closestColor().toUiColor()
    }

    ExternalServices.rootView = subview

    class Observer : NSObject() {
        var keyboardAnimationDuration: Double = 0.25

        @ObjCAction
        fun keyboardWillChangeFrame(notification: NSNotification?) {
            val userInfo = notification?.userInfo ?: return
            val keyboardFrameValue = userInfo[UIKeyboardFrameEndUserInfoKey] as? NSValue ?: return
            val keyboardHeight = cgRectValue(keyboardFrameValue).useContents { size.height }
            keyboardAnimationDuration =
                (userInfo[UIKeyboardAnimationDurationUserInfoKey] as? NSNumber)?.doubleValue ?: return
//            UIView.animateWithDuration(keyboardAnimationDuration) {
                bottom.constant =
                    keyboardHeight - (this@setup.view.window?.safeAreaInsets?.useContents { this.bottom } ?: 0.0)
//            }
            afterTimeout((keyboardAnimationDuration * 1000.0).toLong()) {
                this@setup.view.findFirstResponderChild()?.scrollToMe(true)
            }
        }

        @ObjCAction
        fun keyboardWillHideNotification() {
//            UIView.animateWithDuration(keyboardAnimationDuration) {
                bottom.constant = 0.0
//            }
        }

        @ObjCAction
        fun hideKeyboardWhenTappedAround() {
            view.findFirstResponderChild()?.resignFirstResponder()
        }

//        init {
//            memScoped {
//                val mc = alloc<UIntVar>()
//                val list = class_copyMethodList(object_getClass(this@Observer) as ObjCClass, mc.ptr)!!
//                for(i in 0 until mc.value.toInt()) {
//                    val x: Method = list[i]!!
//                    println(platform.objc.sel_getName(method_getName(x))?.toKString())
//                }
//                nativeHeap.free(list)
//            }
//        }
    }

    val observer: Observer = Observer()

    NSNotificationCenter.defaultCenter.addObserver(
        observer = observer,
        selector = sel_registerName("keyboardWillChangeFrame:"),
        name = UIKeyboardWillChangeFrameNotification,
        `object` = null
    )
    NSNotificationCenter.defaultCenter.addObserver(
        observer = observer,
        selector = sel_registerName("keyboardWillHideNotification"),
        name = UIKeyboardWillHideNotification,
        `object` = null
    )
    extensionStrongRef = observer

    val g = UITapGestureRecognizer(target = observer, action = sel_registerName("hideKeyboardWhenTappedAround"))
    g.cancelsTouchesInView = false
    view.addGestureRecognizer(g)

    val remover = subview.observe("bounds") {
        subview.layoutLayers()
    }
    view.addSubview(object: UIView(CGRectMake(0.0, 0.0, 0.0, 0.0)) {
        override fun willMoveToWindow(newWindow: UIWindow?) {
            super.willMoveToWindow(newWindow)
            if(newWindow == null) {
                remover()
            }
        }
    })
}