package com.lightningkite.kiteui.utils

/**
 * A sorted list where each value has a corresponding key. The list is always sorted by keys.
 *
 * Duplicate keys are allowed and maintain insertion order. All operations use binary search for efficiency.
 *
 * Performance:
 * - `add(key, value)`: O(log n)
 * - `get(key)`: O(log n) - returns all entries with the key
 * - `remove(key)`: O(log n + k) where k = entries removed
 * - `set(key, value)`: O(log n + k) - replaces all entries with key
 */
public class OrderedKeyedList<K : Comparable<K>, V>(private val list: ArrayList<Entry<K, V>>) : List<OrderedKeyedList.Entry<K, V>> by list {
    public constructor() : this(ArrayList())
    public constructor(sizeHint: Int) : this(ArrayList(sizeHint))
    public constructor(init: Collection<Entry<K, V>>) : this(ArrayList(init))
    public constructor(vararg entries: Pair<K, V>) : this(entries.mapTo(ArrayList()) { Entry(it.first, it.second) })

    /**
     * A key-value pair stored in the list.
     */
    public data class Entry<K : Comparable<K>, T>(
        val key: K,
        val value: T
    )

    init {
        list.sortBy { it.key }
    }

    /**
     * Adds the [value] to the list with the keyed-position of [key].
     *
     * If there are already entries in this list with an equal [key] value this
     * will be added at the end of the sublist of entries with this key.
     *
     * The operation is `O(log(n))`
     * */
    public fun add(key: K, value: V) {
        if (list.isEmpty()) list.add(Entry(key, value))
        else list.add(
            insertIndexAfterLast(key),
            Entry(key, value)
        )
    }

    private fun sublist(key: K): MutableList<Entry<K, V>>? {
        if (list.isEmpty()) return null

        val firstIdx = indexOfFirst(key)

        // Key not found in list
        if (firstIdx == list.size || list[firstIdx].key != key) return null

        val oneAfter = insertIndexAfterLast(key)

        // Return a subList view (O(1), not a copy)
        return list.subList(firstIdx, oneAfter)
    }

    /**
     * Returns a view of all entries with the specified [key].
     *
     * @return a view (not a copy) of the underlying list, so changes to the [OrderedKeyedList] will be reflected in the view.
     */
    public fun get(key: K): List<Entry<K, V>> = sublist(key) ?: emptyList()

    /**
     * Removes all entries with the specified [key].
     *
     * @return the number of entries removed.
     */
    public fun remove(key: K): Int {
        val list = sublist(key) ?: return 0
        val c = list.size
        list.clear()
        return c
    }

    /**
     * Replaces all entries with the specified [key] with the single provided [value].
     *
     * After this the only entry with [key] will be the provided [value]
     */
    public fun set(key: K, value: V) {
        val list = sublist(key)

        if (list == null) {
            add(key, value)
            return
        }

        list.clear()
        list.add(Entry(key, value))
    }

    private inline fun optimizedBinarySearchFirstIndexWhereFalse(queryLeft: (K) -> Boolean): Int {
        var left = 0
        var right = list.size

        // Binary search for the leftmost position with this key
        while (left < right) {
            val mid = left + (right - left) / 2
            if (queryLeft(list[mid].key)) {
                left = mid + 1
            } else {
                right = mid
            }
        }

        return left
    }

    private fun indexOfFirst(key: K): Int = optimizedBinarySearchFirstIndexWhereFalse { it < key }
    private fun insertIndexAfterLast(key: K): Int = optimizedBinarySearchFirstIndexWhereFalse { it <= key }
}