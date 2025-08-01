package com.lightningkite.kiteui.views.l2

public interface RecyclerViewData<T, ID> {
    public val range: IntRange
    public operator fun get(index: Int): T

    public object Empty : RecyclerViewData<Unit, Unit> {
        public override val range: IntRange get() = IntRange.EMPTY
        public override fun get(index: Int): Unit = Unit
    }
    public companion object {
        public fun <T> fromList(list: List<T>): RecyclerViewData<T, Any?> = object : RecyclerViewData<T, Any?> {
            override val range: IntRange = list.indices
            override fun get(index: Int): T {
                if (index !in list.indices) throw IllegalStateException("Index $index out of range for ${list.indices}")
                return list[index]
            }
        }
    }
}