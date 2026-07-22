@file:OptIn(ExperimentalNativeApi::class)

package com.lightningkite.kiteui.views


import com.lightningkite.kiteui.Build
import com.lightningkite.kiteui.OverrideOnly
import com.lightningkite.kiteui.WeakReference
import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.objc.cgRectValue
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.direct.observe
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlinx.cinterop.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSNumber
import platform.Foundation.NSThread
import platform.Foundation.NSValue
import platform.UIKit.*
import platform.darwin.*
import platform.darwin.sel_registerName
import platform.objc.*
import kotlin.coroutines.CoroutineContext
import kotlin.experimental.ExperimentalNativeApi

// Installs the reactive graph's thread-confinement guard once per process, debug builds only.
// Guarded by a top-level flag since `setup` runs once per root view controller, but an app could
// in principle create more than one over its lifetime.
private var reactiveThreadCheckInstalled = false
private fun installReactiveThreadCheckOnce() {
    if (reactiveThreadCheckInstalled) return
    reactiveThreadCheckInstalled = true
    if (Build.debug) {
        ReactiveThreadCheck.currentThread = { NSThread.currentThread }
    }
}

public fun UIViewController.setup(theme: Theme, app: ViewWriter.() -> Unit) {
    setup({ theme }, app)
}

public fun UIViewController.setup(themeReadable: Reactive<Theme>, app: ViewWriter.() -> Unit) {
    setup({ themeReadable.invoke() }, app)
}


public class KeyboardObserver(public val bottom: WeakReference<NSLayoutConstraint>, public val view: WeakReference<UIView>) : NSObject() {
    public var keyboardAnimationDuration: Double = 0.25

    @ObjCAction
    public fun keyboardWillChangeFrame(notification: NSNotification?) {
        val userInfo = notification?.userInfo ?: return
        val keyboardFrameValue = userInfo[UIKeyboardFrameEndUserInfoKey] as? NSValue ?: return
        val keyboardHeight = cgRectValue(keyboardFrameValue).useContents { size.height }
        keyboardAnimationDuration =
            (userInfo[UIKeyboardAnimationDurationUserInfoKey] as? NSNumber)?.doubleValue ?: return
//            UIView.animateWithDuration(keyboardAnimationDuration) {
        bottom.get()?.constant =
            keyboardHeight - (view.get()?.window?.safeAreaInsets?.useContents { this.bottom } ?: 0.0)
//            }
        afterTimeout((keyboardAnimationDuration * 1000.0).toLong()) {
            view.get()?.findFirstResponderChild()?.scrollToMeCenter(true)
        }
    }

    @ObjCAction
    public fun keyboardWillHideNotification() {
//            UIView.animateWithDuration(keyboardAnimationDuration) {
        bottom.get()?.constant = 0.0
//            }
    }

    @ObjCAction
    public fun hideKeyboardWhenTappedAround() {
        view.get()?.findFirstResponderChild()?.resignFirstResponder()
    }
}

public fun UIViewController.kiteUi(context: ElementContext = ElementContext(this@kiteUi), app: ViewWriter.() -> Unit) {
    definesPresentationContext = true
    val job = SupervisorJob()
    val scope = job + CoroutineExceptionHandler { coroutineContext, throwable ->
        Reactive.reportException(throwable)
    } + Dispatchers.Main.immediate
    val safeInsetProperty = Signal(Edges.ZERO)

    @OptIn(DelicateCoroutinesApi::class)
    val writer = object : ViewWriter {
        override val coroutineContext: CoroutineContext = scope
        override val context: ElementContext = context
        @OverrideOnly
        override fun willAddChild(element: Element) {
        }
        @OverrideOnly
        override fun addChild(element: Element) {
            this@kiteUi.view.addSubview(element.native)
        }
    }
    context.safeInsets = safeInsetProperty
    val created = writer.produceExactlyOneView { app() }

    val subview = created.native
    subview.translatesAutoresizingMaskIntoConstraints = false
    subview.topAnchor.constraintEqualToAnchor(view.topAnchor).setActive(true)
    subview.leftAnchor.constraintEqualToAnchor(view.leftAnchor).setActive(true)
    subview.rightAnchor.constraintEqualToAnchor(view.rightAnchor).setActive(true)
    val bottom = view.bottomAnchor.constraintEqualToAnchor(subview.bottomAnchor)
    bottom.setActive(true)

    val observer: KeyboardObserver = KeyboardObserver(
        WeakReference(bottom),
        WeakReference(view)
    )
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

    val g = UITapGestureRecognizer(target = observer, action = sel_registerName("hideKeyboardWhenTappedAround"))
    g.cancelsTouchesInView = false
    view.addGestureRecognizer(g)

    val safeInsets = {
        view.safeAreaInsets.useContents {
            safeInsetProperty.value = (Edges(
                left = Dimension(left),
                right = Dimension(right),
                top = Dimension(top),
                bottom = Dimension(this.bottom),
            ))
        }
    }
    val remover = subview.observe("bounds") {
        safeInsets()
    }
    view.addSubview(RemoveView(onRemove = {
        if (movingFromParentViewController || beingDismissed) {
            view.removeGestureRecognizer(g)
            NSNotificationCenter.defaultCenter.removeObserver(observer)
            remover()
            job.cancel()
            @OptIn(OverrideOnly::class)
            created.onShutdown()
            true
        } else false
    }))
    afterTimeout(10) {
        safeInsets()
    }
}

private class RemoveView(var onRemove: (() -> Boolean)? = null) : UIView(CGRectMake(0.0, 0.0, 0.0, 0.0)) {
    init {
        this.hidden = true
    }

    override fun willMoveToWindow(newWindow: UIWindow?) {
        super.willMoveToWindow(newWindow)
        if (newWindow == null) {
            if (onRemove?.invoke() == true) {
                onRemove = null
//                onSafeInsetsChange = null
            }
        }
    }
}

public fun UIViewController.setup(themeCalculation: ReactiveContext.() -> Theme, app: ViewWriter.() -> Unit) {
    installReactiveThreadCheckOnce()
    val systemBarBackground = UIView()

    view.addSubview(systemBarBackground)
    systemBarBackground.translatesAutoresizingMaskIntoConstraints = false
    systemBarBackground.topAnchor.constraintEqualToAnchor(view.topAnchor).setActive(true)
    systemBarBackground.leftAnchor.constraintEqualToAnchor(view.safeAreaLayoutGuide.leftAnchor).setActive(true)
    systemBarBackground.rightAnchor.constraintEqualToAnchor(view.safeAreaLayoutGuide.rightAnchor).setActive(true)
    systemBarBackground.bottomAnchor.constraintEqualToAnchor(view.safeAreaLayoutGuide.topAnchor).setActive(true)
    kiteUi {
        reactive {
            systemBarBackground.backgroundColor =
                themeCalculation()[SystemBarSemantic].theme.background.closestColor().toUiColor()
        }
        reactive {
            view.backgroundColor = themeCalculation()[BarSemantic].theme.background.closestColor().toUiColor()
        }
        beforeSetup {
            ::themeChoice { ThemeDerivation.SetAsBase(themeCalculation()) }
        }.app()
    }
}