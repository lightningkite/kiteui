package com.lightningkite.kiteui.models

import com.lightningkite.kiteui.reactive.AppState
import kotlinx.browser.document
import org.w3c.dom.HTMLDivElement

private val measuringDiv = (document.createElement("div") as HTMLDivElement).apply {
    id = "____--measuringDiv"
    style.height = "0"
    style.width = "0"
    style.position = "absolute"
    style.outline = "none"
    style.border = "none"
    style.padding = "none"
    style.margin = "none"
    style.boxSizing = "content-box"
    style.maxHeight = "unset"
    document.body!!.appendChild(this)
}
actual val Dimension.px: Double get() = value.roughPx
actual val Dimension.canvasUnits: Double get() = value.roughPx * AppState.windowInfo.value.density

private fun String.cssCalc(): Int {
    measuringDiv.style.height = this
    return measuringDiv.offsetHeight
}