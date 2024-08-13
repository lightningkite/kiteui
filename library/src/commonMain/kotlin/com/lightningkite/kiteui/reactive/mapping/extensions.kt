package com.lightningkite.kiteui.reactive.mapping

import com.lightningkite.kiteui.reactive.Readable
import com.lightningkite.kiteui.reactive.invoke

suspend fun <T, ID> WritableList<T, ID>.remove(element: Readable<WritableList<T, ID>.ElementWritable>) {
    remove(element())
}