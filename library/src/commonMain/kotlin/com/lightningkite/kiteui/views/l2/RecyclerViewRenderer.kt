package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.ViewModifiable
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.space
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*

public interface RecyclerViewRenderer<in T> {
    public fun render(viewWriter: ViewWriter, data: Reactive<T>, index: Reactive<Int>): ViewModifiable

    public object Blank : RecyclerViewRenderer<Any?> {
        public override fun render(viewWriter: ViewWriter, data: Reactive<Any?>, index: Reactive<Int>): ViewModifiable {
            return with(viewWriter) { space() }
        }
    }
}