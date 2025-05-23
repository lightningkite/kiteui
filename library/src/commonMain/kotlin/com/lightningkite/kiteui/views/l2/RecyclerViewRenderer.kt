package com.lightningkite.kiteui.views.l2

import com.lightningkite.signal.Readable
import com.lightningkite.kiteui.views.ViewModifiable
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.space

interface RecyclerViewRenderer<in T> {
    fun render(viewWriter: ViewWriter, data: Readable<T>, index: Readable<Int>): ViewModifiable

    object Blank : RecyclerViewRenderer<Any?> {
        override fun render(viewWriter: ViewWriter, data: Readable<Any?>, index: Readable<Int>): ViewModifiable {
            return with(viewWriter) { space() }
        }
    }
}