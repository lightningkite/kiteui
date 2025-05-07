package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.ClickableSemantic
import com.lightningkite.kiteui.navigation.*
import com.lightningkite.kiteui.views.*
import kotlinx.coroutines.launch


actual class Link actual constructor(context: RContext) : RView(context) {

    init {
        themeChoice += ClickableSemantic
        native.tag = "a"
        native.classes.add("kiteui-stack")
        native.classes.add("clickable")
        native.addEventListener("click") {
            onClick?.let { launch { it() } }
            if(newTab) return@addEventListener
            it.preventDefault()
            val destination = to?.invoke()
            if(destination != null) {
                launch {
                    onNavigate?.invoke()
                    if (resetsStack) {
                        onNavigator.reset(destination)
                    } else {
                        onNavigator.navigate(destination)
                    }
                }
            }
        }
    }

    override fun internalAddChild(index: Int, view: RView) {
        super.internalAddChild(index, view)
        Frame.internalAddChildStack(this, index, view)
    }

    actual var onNavigator: PageNavigator = (this as RView).pageNavigator
    actual var to: (() -> Page)? = null
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

    private var onNavigate: (suspend () -> Unit)? = null
    actual fun onNavigate(action: suspend () -> Unit): Unit {
        onNavigate = action
    }

    private var onClick: (suspend () -> Unit)? = null
    actual fun onClick(action: suspend () -> Unit): Unit {
        onClick = action
    }

    actual inline var enabled: Boolean
        get() = native.attributes.disabled != true
        set(value) {
            native.attributes.disabled = !value
        }
}
