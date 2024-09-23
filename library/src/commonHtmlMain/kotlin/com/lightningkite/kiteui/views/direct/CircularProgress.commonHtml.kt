package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.*

actual class CircularProgress actual constructor(context: RContext): RView(context) {

    val circleBackground = FutureElement().apply {
        tag = "path"
        classes.add("circle-background")
        setAttribute("d","M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831")
    }
    val circleProgress = FutureElement().apply {
        tag = "path"
        classes.add("circle-progress")
        setAttribute("stroke-dasharray","30,100")
        setAttribute("d","M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831")
    }



    init {
        native.tag = "div"
        native.classes.add("progress-ring")
        val svg = FutureElement().apply{
            tag = "svg"
            classes.add("progress-ring-svg")
            setAttribute("viewBox","0 0 36 36")
        }
        svg.appendChild(circleBackground)
        svg.appendChild(circleProgress)
        val innerContent = FutureElement().apply {
            tag = "div"
            classes.add("progress-ring-content")
        }
        svg.appendChild(innerContent)
        native.appendChild(svg)
    }

    override fun internalAddChild(index: Int, view: RView) {
        super.internalAddChild(index, view)
//        val test = view.children[0]
        Stack.internalAddChildStack(this,index,view)
    }

    actual var ratio: Float
        get() =  native.children[0].children[1].attributes.valueDouble?.toFloat()?:0f
        set(value) {
            native.children[0].children[1].setStyleProperty("stroke-dasharray","(${value*100},100)")
            native.children[0].children[1].setStyleProperty("valueDouble",value.toString())
        }
}