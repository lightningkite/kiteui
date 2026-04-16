package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.ClickableSemantic
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.navigation.*
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.kiteui.views.*
import kotlinx.coroutines.launch


actual class Link actual constructor(context: ElementContext) : NativeContainerElementWithSecondaryAction(context) {
    override val driverActions get() = super.driverActions + linkDriverActions()
    override fun nativeSetAction(action: Action?) {
        native.setAttribute("aria-label", accessibleLabel ?: action?.title)
    }
    init {
        themeChoice += ClickableSemantic
        native.tag = "a"
        native.classes.add("kiteui-stack")
        native.classes.add("clickable")
        native.addEventListener("click") {
            launch {
                action?.startAction(this@launch)
                if (newTab) return@launch
                it.preventDefault() // don't use href
                to?.invoke()?.let { to ->
                    onNavigateAction?.startAction(this@launch)
                    if (resetsStack) onNavigator.reset(to)
                    else onNavigator.navigate(to)
                }
            }
        }
    }

    override fun nativeAddChild(index: Int, element: Element) {
        super.nativeAddChild(index, element)
        Frame.internalAddChildStack(this, index, element)
    }

    actual var onNavigator: PageNavigator = context.mainPageNavigator
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
}
