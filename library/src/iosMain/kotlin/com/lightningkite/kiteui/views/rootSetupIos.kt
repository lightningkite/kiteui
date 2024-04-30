package com.lightningkite.kiteui.views

import cnames.structs.objc_method
import com.lightningkite.kiteui.ExternalServices
import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.objc.cgRectValue
import com.lightningkite.kiteui.reactive.SoftInputOpen
import com.lightningkite.kiteui.views.direct.observe
import kotlinx.cinterop.*
import kotlinx.cinterop.CPointer
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSNumber
import platform.Foundation.NSValue
import platform.UIKit.*
import platform.darwin.*
import platform.darwin.sel_registerName
import platform.objc.*

@OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)
fun UIViewController.setup(app: ViewWriter.()->Unit) {
    ExternalServices.currentPresenter = { presentViewController(it, animated = true, completion = null) }
    val writer = ViewWriter(this.view, context = this)
    writer.app()
    val subview = view.subviews.first() as UIView
    subview.translatesAutoresizingMaskIntoConstraints = false
    subview.topAnchor.constraintEqualToAnchor(view.safeAreaLayoutGuide.topAnchor).setActive(true)
    subview.leftAnchor.constraintEqualToAnchor(view.safeAreaLayoutGuide.leftAnchor).setActive(true)
    subview.rightAnchor.constraintEqualToAnchor(view.safeAreaLayoutGuide.rightAnchor).setActive(true)
    val bottom = view.safeAreaLayoutGuide.bottomAnchor.constraintEqualToAnchor(subview.bottomAnchor)
    bottom.setActive(true)

    class Observer: NSObject() {
        var keyboardAnimationDuration: Double = 0.25

        @ObjCAction
        fun keyboardWillChangeFrame(notification: NSNotification?) {
            val userInfo = notification?.userInfo ?: return
            val keyboardFrameValue = userInfo[UIKeyboardFrameEndUserInfoKey] as? NSValue ?: return
            val keyboardHeight = cgRectValue(keyboardFrameValue).useContents { size.height }
            keyboardAnimationDuration = (userInfo[UIKeyboardAnimationDurationUserInfoKey] as? NSNumber)?.doubleValue ?: return
            UIView.animateWithDuration(keyboardAnimationDuration) {
                bottom.constant = keyboardHeight - (this@setup.view.window?.safeAreaInsets?.useContents { this.bottom } ?: 0.0)
            }
            afterTimeout((keyboardAnimationDuration * 1000.0).toLong()) {
//                this@setup.view.findFirstResponderChild()?.scrollToMe(true)
            }
        }
        @ObjCAction
        fun keyboardWillHideNotification() {
            UIView.animateWithDuration(keyboardAnimationDuration) {
                bottom.constant = 0.0
            }
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

    subview.observe("bounds") {
        println("Root bounds change")
        subview.layoutLayers()
    }
}