package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.ConsoleRoot
import com.lightningkite.kiteui.navigation.*
import com.lightningkite.kiteui.views.*
import kotlinx.coroutines.launch


actual class Link actual constructor(context: RContext) : RView(context) {

    init {
        native.tag = "a"
        native.classes.add("kiteui-stack")
        native.classes.add("clickable")
        native.addEventListener("click") {
            if(newTab) return@addEventListener
            it.preventDefault()
            val destination = to?.invoke()
            if(destination != null) {
                if (resetsStack) {
                    onNavigator.reset(destination)
                } else {
                    onNavigator.navigate(destination)
                }
                onNavigate?.let {
                    launch { it() }
                }
            }
        }
    }

    override fun internalAddChild(index: Int, view: RView) {
        super.internalAddChild(index, view)
        Stack.internalAddChildStack(this, index, view)
    }

    override fun hasAlternateBackedStates(): Boolean = true

    actual var onNavigator: ScreenNavigator = (this as RView).screenNavigator
    actual var to: (() -> Screen)? = null
        set(value) {
            field = value
            value?.invoke()?.let {
                onNavigator.routes.render(it)?.let {
                    native.attributes.href = context.basePath + it.urlLikePath.render()
                }
            } ?: run { native.attributes.href = "" }
        }
    actual inline var newTab: Boolean
        get() = native.attributes.target == "_blank"
        set(value) {
            native.attributes.target = if (value) "_blank" else "_self"
        }
    actual var resetsStack: Boolean = false

    private var onNavigate: suspend () -> Unit = {}
    actual fun onNavigate(action: suspend () -> Unit): Unit {
        onNavigate = action
    }
}
