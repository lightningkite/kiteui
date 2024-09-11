package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.launchManualCancel
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*


actual class IconView actual constructor(context: RContext) : RView(context) {
    init {
        native.tag = "div"
        native.setAttribute("role", "img")
        native.classes.add("viewDraws")
        native.classes.add("icon")
    }
    override fun internalAddChild(index: Int, view: RView) {
        super.internalAddChild(index, view)
        Stack.internalAddChildStack(this, index, view)
    }

    actual var source: Icon? = null
        set(value) {
            field = value
            native.clearChildren()
            value?.let { value ->
                native.appendChild(FutureElement().apply {
                    tag = "svg"
                    xmlns = "http://www.w3.org/2000/svg"
                    style.width = value.width.value
                    style.height = value.height.value
                    setStyleProperty("fill", "currentColor")
                    setStyleProperty("stroke", "currentColor")
                    setStyleProperty("stroke-width", "0")
                    setAttribute("viewBox", value.run { "$viewBoxMinX $viewBoxMinY $viewBoxWidth $viewBoxHeight" })
                    for(path in value.pathDatas) {
                        appendChild(FutureElement().apply {
                            tag = "path"
                            xmlns = "http://www.w3.org/2000/svg"
                            setAttribute("d", path)
                        })
                    }
                    for(path in value.strokePathDatas) {
                        appendChild(FutureElement().apply {
                            tag = "path"
                            xmlns = "http://www.w3.org/2000/svg"
                            setStyleProperty("stroke-width", path.strokeWidth.px.toString())
                            setStyleProperty("fill", path.fill?.closestColor()?.toWeb() ?: "none")
                            setStyleProperty("stroke-linecap", path.strokeLineCap.toString().lowercase())
                            setAttribute("d", path.path)
                        })
                    }
                })
            }
        }

    actual var description: String? = null
        set(value) {
            field = value
            native.children.firstOrNull()?.let {
                it.children.find { it.tag == "title" }?.let { it.content = value }
                    ?: it.appendChild(FutureElement().apply {
                        tag = "title"
                        content = value
                    })
            }
        }

}
