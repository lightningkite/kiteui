package com.lightningkite.kiteui.views.l2

public interface RecyclerViewData<T, ID> {
    public val range: IntRange
    public operator fun get(index: Int): T

    public object Empty : RecyclerViewData<Unit, Unit> {
        public override val range: IntRange get() = IntRange.EMPTY
        public override fun get(index: Int): Unit = Unit
    }

    class FromList<T>(val list: List<T>) : RecyclerViewData<T, Any?> {
        override val range: IntRange = list.indices
        override fun get(index: Int): T {
            if (index !in list.indices) throw IndexOutOfBoundsException("Index $index out of range for ${list.indices}")
            return list[index]
        }
    }

    companion object {
        fun <T> fromList(list: List<T>) = FromList(list)
    }
}