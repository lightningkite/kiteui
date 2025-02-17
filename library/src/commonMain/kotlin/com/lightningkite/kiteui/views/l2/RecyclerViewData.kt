package com.lightningkite.kiteui.views.l2

interface RecyclerViewData<T, ID> {
    val range: IntRange
    operator fun get(index: Int): T

    object Empty : RecyclerViewData<Unit, Unit> {
        override val range: IntRange get() = IntRange.EMPTY
        override fun get(index: Int): Unit = Unit
    }
    companion object {
        fun <T> fromList(list: List<T>) = object : RecyclerViewData<T, Any?> {
            override val range: IntRange = list.indices
            override fun get(index: Int): T {
                if (index !in list.indices) throw IllegalStateException("Index $index out of range for ${list.indices}")
                return list[index]
            }
        }
    }
}