package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElement

/**
 * A progress ring, drawn entirely in CSS from a native `<progress>` element.
 *
 * The `<progress>` tag carries the semantics: assistive technology reads the value from it
 * directly, with no hand-maintained `role`/`aria-valuenow` pair to keep in sync, and it stays
 * meaningful if the stylesheet fails to load. The ring itself is a `conic-gradient` masked into a
 * band, driven by the `--kiteui-progress` custom property - so updating the ratio sets one property
 * and the browser repaints, rather than JavaScript recomputing arc geometry.
 *
 * Being CSS rather than script is also what lets one implementation serve both the browser and
 * server-side rendering: the markup this emits is complete, so an SSR page shows a correctly filled
 * ring before any script runs.
 */
public actual class CircularProgress actual constructor(context: ElementContext) : NativeElement(context) {
    init {
        native.tag = "progress"
        native.classes.add("kiteui-circular-progress")
        native.setAttribute("max", "1")
        native.setAttribute("value", "0")
        native.setStyleProperty("--kiteui-progress", "0")
    }

    public actual var ratio: Float = 0f
        set(value) {
            val clamped = value.coerceIn(0f, 1f)
            field = clamped
            native.setStyleProperty("--kiteui-progress", clamped.toString())
            native.setAttribute("value", clamped.toString())
        }
}
