package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.models.*
import com.lightningkite.signal.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import kotlinx.browser.window
import org.w3c.dom.DOMRect
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.get
import kotlin.math.abs
import kotlin.math.roundToInt

public val data = Property<List<String>>((0..2500).map { "Item $it" }.toList())

public actual fun ViewWriter.platformSpecific() {
}