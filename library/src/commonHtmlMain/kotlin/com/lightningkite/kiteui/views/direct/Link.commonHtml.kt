package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.models.ClickableSemantic
import com.lightningkite.kiteui.navigation.*
import com.lightningkite.kiteui.views.*
import kotlinx.coroutines.launch


@InternalKiteUi
public actual class Link public actual constructor(context: RContext) : RView(context) {

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

    public actual var onNavigator: PageNavigator = (this as RView).pageNavigator
    public actual var to: (() -> Page)? = null
        set(value) {
            field = value
            value?.invoke()?.let {
                onNavigator.routes.render(it)?.let {
                    native.attributes.href = context.basePath + it.urlLikePath.render()
                }
            } ?: run { native.attributes.href = "" }
        }
    public actual inline var newTab: Boolean
        get() = native.attributes.target == "_blank"
        set(value) {
            native.attributes.target = if (value) "_blank" else "_self"
        }
    public actual var resetsStack: Boolean = false

    private var onNavigate: (suspend () -> Unit)? = null
    public actual fun onNavigate(action: suspend () -> Unit): Unit {
        onNavigate = action
    }

    private var onClick: (suspend () -> Unit)? = null
    public actual fun onClick(action: suspend () -> Unit): Unit {
        onClick = action
    }

    public actual inline var enabled: Boolean
        get() = native.attributes.disabled != true
        set(value) {
            native.attributes.disabled = !value
        }
}
