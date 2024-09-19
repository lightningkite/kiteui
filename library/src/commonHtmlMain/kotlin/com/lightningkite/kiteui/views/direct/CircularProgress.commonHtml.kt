package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.*

actual class CircularProgress actual constructor(context: RContext): RView(context) {

    val circle = FutureElement().apply {
        tag = "circle"
        classes.add("progress-ring__circle")
        setAttribute("stroke","blue")
        setAttribute("stroke-width","10")
        setAttribute("fill","transparent")
        setAttribute("r","52")
        setAttribute("cx","60")
        setAttribute("cy","60")
        setAttribute("style","stroke-dashoffset: 65.3451px;")
    }

    init {
        native.tag = "div"
        native.style.alignSelf = "center"
        native.classes.add("progress-ring")
        native.classes.add("viewDraws")
        val svg = FutureElement().apply{
            tag = "svg"
            classes.add("progress-ring__svg")
            setAttribute("width","120vw")
            setAttribute("height","120vw")
        }
        svg.appendChild(circle)
        native.appendChild(svg)
        val innerContent = FutureElement().apply {
            tag = "div"
            classes.add("progress-ring__content")
            classes.add("kiteui-stack")
        }
        native.appendChild(innerContent)
    }

//    override fun internalAddChild(index: Int, view: RView) {
//        super.internalAddChild(index, view)
//        Stack.internalAddChildStack(this,index,view)
//    }

    actual var ratio: Float
        get() =  0f
        set(value) {
            native.children[0].children[0].setStyleProperty("stroke-dashoffset",(value*100).toString())
        }
}

// https://codepen.io/noleli/pen/xxoeLez
//html
//<progress value=".2" style="--value: .2"></progress>
//
//progress {
//    /* config */
//    --outer-radius: 4rem;
//    --inner-radius: 3rem;
//    --start-color: #4dbce0;
//    --end-color: oklch(from var(--start-color) calc(l / 3) calc(c * 2) h);
//
//    /* reset */
//    appearance: none;
//    background-color: transparent;
//    border: none;
//
//    /* the rest */
//    width: calc(2 * var(--outer-radius));
//    height: calc(2 * var(--outer-radius));
//    background-image: conic-gradient(
//    var(--start-color),
//    var(--end-color)
//    );
//
//    --endcap-radius: calc((var(--outer-radius) - var(--inner-radius)) / 2);
//    --middle-radius: calc((var(--outer-radius) + var(--inner-radius)) / 2);
//
//    /* https://dev.to/janeori/css-type-casting-to-numeric-tanatan2-scalars-582j */
//    --angle-inset: calc(2 * asin(tan(atan2(var(--endcap-radius), 2 * var(--middle-radius)))));
//
//    --value-angle: calc(var(--value) * ((2 * pi * 1rad - var(--angle-inset)) - var(--angle-inset)) + var(--angle-inset));
//
//    mask-mode: alpha;
//    mask-composite: intersect, add, add;
//    mask-image:
//    /* make it a ring */
//    radial-gradient(
//        transparent,
//        transparent calc(var(--inner-radius) - .5px),
//    white var(--inner-radius),
//    white var(--outer-radius),
//    transparent calc(var(--outer-radius) + .5px)
//    ),
//
//    /* value */
//    conic-gradient(
//        transparent,
//        transparent var(--angle-inset),
//    white var(--angle-inset),
//    white calc(var(--value-angle)),
//    transparent calc(var(--value-angle)),
//    transparent 1turn
//    ),
//
//    /* end caps */
//    radial-gradient(
//        circle at calc(50% + var(--middle-radius) * sin(var(--angle-inset))) calc(50% - var(--middle-radius) * cos(var(--angle-inset))),
//    white,
//    white var(--endcap-radius),
//    transparent var(--endcap-radius)
//    ),
//    radial-gradient(
//        circle at calc(50% + var(--middle-radius) * sin(var(--value-angle))) calc(50% - var(--middle-radius) * cos(var(--value-angle))),
//    white,
//    white var(--endcap-radius),
//    transparent var(--endcap-radius)
//    );
//}
//
//progress:is([value="1"], [value="0"]) {
//    --endcap-radius: 0px;
//    --angle-inset: 0rad;
//}
//
///* more reset, but notstandard selectors mess with nesting */
//progress::-webkit-progress-inner-element {
//    display: none;
//}
//progress::-moz-progress-bar {
//    background-color: transparent;
//}
