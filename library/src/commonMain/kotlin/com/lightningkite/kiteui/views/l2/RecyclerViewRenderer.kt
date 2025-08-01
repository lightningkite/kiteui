package com.lightningkite.kiteui.views.l2

import com.lightningkite.signal.Readable
import com.lightningkite.kiteui.views.ViewModifiable
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.space

public interface RecyclerViewRenderer<in T> {
    public fun render(viewWriter: ViewWriter, data: Readable<T>, index: Readable<Int>): ViewModifiable

    public object Blank : RecyclerViewRenderer<Any?> {
        public override fun render(viewWriter: ViewWriter, data: Readable<Any?>, index: Readable<Int>): ViewModifiable {
            return with(viewWriter) { space() }
        }
    }
}