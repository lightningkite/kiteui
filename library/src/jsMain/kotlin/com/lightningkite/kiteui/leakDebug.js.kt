package com.lightningkite.kiteui

import com.lightningkite.kiteui.views.Element
import kotlinx.browser.window

/**
 * Installs browser-console helpers for investigating element leaks (see [Element.Debugger.countInstances]):
 *
 * - `kiteuiLeakEnable()` — start counting live [NativeElement] instances.
 * - `kiteuiLeak()` — read the current total and per-class live counts.
 *
 * Usage from the devtools console: call `kiteuiLeakEnable()`, note `kiteuiLeak().total`, navigate
 * away and back a few times, then read `kiteuiLeak()` again. A total that fails to return to
 * baseline (and a per-class breakdown that keeps growing) pinpoints the leaking element type.
 */
fun installLeakDebug() {
    val w = window.asDynamic()
    w.kiteuiLeakEnable = { ->
        Element.Debugger.countInstances = true
        "leak counting enabled"
    }
    w.kiteuiLeak = { ->
        val r = js("({})")
        r.total = Element.Debugger.liveInstanceTotal
        val byClass = js("({})")
        Element.Debugger.liveInstancesByClass.forEach { (k, v) -> byClass[k] = v }
        r.byClass = byClass
        r
    }
}
