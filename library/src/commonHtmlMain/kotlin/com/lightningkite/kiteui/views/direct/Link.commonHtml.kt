package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.ConsoleRoot
import com.lightningkite.kiteui.launchManualCancel
import com.lightningkite.kiteui.navigation.*
import com.lightningkite.kiteui.views.*


actual class Link actual constructor(context: RContext) : RView(context) {

    init {
        native.tag = "a"
        native.classes.add("kiteui-stack")
        native.classes.add("clickable")
        native.addEventListener("click") {
            it.preventDefault()
            val destination = to()
            if(resetsStack) {
                ConsoleRoot.log("Navigating on ", onNavigator, " to ", destination)
                onNavigator.reset(destination)
                ConsoleRoot.log("Result is ", onNavigator.stack.value)
            } else {
                ConsoleRoot.log("Navigating on ", onNavigator, " to ", destination)
                onNavigator.navigate(destination)
                ConsoleRoot.log("Result is ", onNavigator.stack.value)
            }
            onNavigate?.let {
                launchManualCancel(it)
            }
        }
    }
    override fun internalAddChild(index: Int, view: RView) {
        super.internalAddChild(index, view)
        Stack.internalAddChildStack(this, index, view)
    }

    override fun hasAlternateBackedStates(): Boolean = true

    actual var onNavigator: KiteUiNavigator = (this as RView).screenNavigator
    actual var to: ()->Screen = { Screen.Empty }
        set(value) {
            field = value
            onNavigator.routes.render(value())?.let {
                native.attributes.href = context.basePath + it.urlLikePath.render()
            }
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
