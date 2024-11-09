package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.*
import kotlin.math.pow
import kotlin.math.roundToInt

actual class CircularProgress actual constructor(context: RContext): RView(context) {


    init {
        native.tag = "div"
        native.innerHtmlUnsafe = """
            <svg viewbox="0 0 36 36" class="progress-ring-svg" style="z-index: 1;"><path d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831" class="circle-background"></path><path stroke-dasharray="00,100" d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831" class="circle-progress"></path></svg>
        """.trimIndent()
    }



    actual var ratio: Float
        get() =  native.innerHtmlUnsafe?.let { getStrokeDashArrayValue(it) } ?: 0f
        set(value) {
            val updatedInnerHTML = native.innerHtmlUnsafe?.let { updateStrokeDashArray(it, roundTo(value*100, 2).toString()) }
            native.innerHtmlUnsafe = updatedInnerHTML
        }


    fun updateStrokeDashArray(svg: String, newValue: String): String {
        val regex = """stroke-dasharray="\d+,(\d+)"""".toRegex()
        val result = regex.replace(svg) { matchResult ->
            "stroke-dasharray=\"$newValue,${matchResult.groupValues[1]}\""
        }
        println("result $result")
        return result
    }
    fun getStrokeDashArrayValue(svg: String): Float? {
        val regex = """stroke-dasharray="(\d+),\d+"""".toRegex()
        val matchResult = regex.find(svg)
        val floatValue = matchResult?.groupValues?.get(1)?.toFloat()
        return floatValue // The first group contains the 30 (or whatever number is there)
    }
    fun roundTo(value: Float, decimals: Int): Float {
        return ((value * 10.0.pow(decimals)).roundToInt() / 10.0.pow(decimals)).toFloat()
    }
}

