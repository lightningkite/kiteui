package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.launchManualCancel
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*


actual class IconView actual constructor(context: RContext) : RView(context) {
    init {
        native.tag = "div"
        native.attributes["role"] = "img"
        native.classes.add("viewDraws")
        native.classes.add("icon")
    }

    actual var source: Icon? = null
        set(value) {
            field = value
            native.children.clear()
            value?.let { value ->
                native.children.add(FutureElement().apply {
                    tag = "svg"
                    attributes["xmlns"] = "http://www.w3.org/2000/svg"
                    style.width = value.width.value
                    style.height = value.height.value
                    style.record["fill"] = "currentColor"
                    attributes["viewBox"] = value.run { "$viewBoxMinX $viewBoxMinY $viewBoxWidth $viewBoxHeight" }
                    for(path in value.pathDatas) {
                        children.add(FutureElement().apply {
                            tag = "path"
                            attributes["d"] = path
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
                    ?: it.children.add(0, FutureElement().apply {
                        tag = "title"
                        content = value
                    })
            }
        }

}
