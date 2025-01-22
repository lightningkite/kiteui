package com.lightningkite.kiteui.views.l2

interface RecyclerViewData<T, ID> {
    val range: IntRange
    operator fun get(index: Int): T

    object Empty : RecyclerViewData<Unit, Unit> {
        override val range: IntRange get() = IntRange.EMPTY
        override fun get(index: Int): Unit = Unit
    }
}