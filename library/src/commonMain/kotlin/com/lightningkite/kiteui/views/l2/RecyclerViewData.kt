package com.lightningkite.kiteui.views.l2

public interface RecyclerViewData<T, ID> {
    public val range: IntRange
    public operator fun get(index: Int): T

    public object Empty : RecyclerViewData<Unit, Unit> {
        public override val range: IntRange get() = IntRange.EMPTY
        public override fun get(index: Int): Unit = Unit
    }

    public class FromList<T>(public val list: List<T>) : RecyclerViewData<T, Any?> {
        public override val range: IntRange = list.indices
        public override fun get(index: Int): T {
            if (index !in list.indices) throw IndexOutOfBoundsException("Index $index out of range for ${list.indices}")
            return list[index]
        }
    }

    public companion object {
        public fun <T> fromList(list: List<T>): FromList<T> = FromList(list)
    }
}