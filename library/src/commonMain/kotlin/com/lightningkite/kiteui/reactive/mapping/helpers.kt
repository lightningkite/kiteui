package com.lightningkite.kiteui.reactive.mapping

import com.lightningkite.kiteui.reactive.Writable

fun Writable<Int>.toDouble(): Writable<Double?> = transform(
    get = { it.toDouble() },
    set = { it?.toInt() ?: 0 }
)

