package com.lightningkite.kiteui.validation

import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.ViewWriter


fun RView.validates(readable: Readable<*>) {
    reactiveScope { readable.await() }
}

class InvalidException(
    val title: String,
    val description: String,
    val code: Int
): Exception(title)