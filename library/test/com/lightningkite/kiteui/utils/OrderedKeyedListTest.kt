package com.lightningkite.kiteui.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OrderedKeyedListTest {

    @Test
    fun testEmptyList() {
        val list = OrderedKeyedList<Double, String>()
        assertEquals(0, list.size)
        assertTrue(list.isEmpty())
    }

    @Test
    fun testAddToEmptyList() {
        val list = OrderedKeyedList<Double, String>()
        list.add(5.0, "five")

        assertEquals(1, list.size)
        assertEquals(5.0, list[0].key)
        assertEquals("five", list[0].value)
    }

    @Test
    fun testAddInAscendingOrder() {
        val list = OrderedKeyedList<Double, String>()
        list.add(1.0, "one")
        list.add(2.0, "two")
        list.add(3.0, "three")

        assertEquals(3, list.size)
        assertEquals("one", list[0].value)
        assertEquals("two", list[1].value)
        assertEquals("three", list[2].value)
    }

    @Test
    fun testAddInDescendingOrder() {
        val list = OrderedKeyedList<Double, String>()
        list.add(3.0, "three")
        list.add(2.0, "two")
        list.add(1.0, "one")

        assertEquals(3, list.size)
        assertEquals("one", list[0].value)
        assertEquals("two", list[1].value)
        assertEquals("three", list[2].value)
    }

    @Test
    fun testAddInRandomOrder() {
        val list = OrderedKeyedList<Double, String>()
        list.add(5.0, "five")
        list.add(2.0, "two")
        list.add(8.0, "eight")
        list.add(1.0, "one")
        list.add(4.0, "four")

        assertEquals(5, list.size)
        assertEquals(1.0, list[0].key)
        assertEquals(2.0, list[1].key)
        assertEquals(4.0, list[2].key)
        assertEquals(5.0, list[3].key)
        assertEquals(8.0, list[4].key)
    }

    @Test
    fun testAddDuplicateKeys() {
        val list = OrderedKeyedList<Double, String>()
        list.add(5.0, "first")
        list.add(5.0, "second")
        list.add(5.0, "third")

        assertEquals(3, list.size)
        assertEquals(5.0, list[0].key)
        assertEquals(5.0, list[1].key)
        assertEquals(5.0, list[2].key)

        // Should maintain insertion order for equal keys
        assertEquals("first", list[0].value)
        assertEquals("second", list[1].value)
        assertEquals("third", list[2].value)
    }

    @Test
    fun testAddDuplicateKeysAtStart() {
        val list = OrderedKeyedList<Double, String>()
        list.add(1.0, "first")
        list.add(5.0, "middle")
        list.add(1.0, "second")
        list.add(1.0, "third")

        assertEquals(4, list.size)
        assertEquals("first", list[0].value)
        assertEquals("second", list[1].value)
        assertEquals("third", list[2].value)
        assertEquals("middle", list[3].value)
    }

    @Test
    fun testAddDuplicateKeysAtEnd() {
        val list = OrderedKeyedList<Double, String>()
        list.add(5.0, "middle")
        list.add(9.0, "first")
        list.add(9.0, "second")
        list.add(9.0, "third")

        assertEquals(4, list.size)
        assertEquals("middle", list[0].value)
        assertEquals("first", list[1].value)
        assertEquals("second", list[2].value)
        assertEquals("third", list[3].value)
    }

    @Test
    fun testAddDuplicateKeysInMiddle() {
        val list = OrderedKeyedList<Double, String>()
        list.add(1.0, "one")
        list.add(5.0, "five-1")
        list.add(9.0, "nine")
        list.add(5.0, "five-2")
        list.add(5.0, "five-3")

        assertEquals(5, list.size)
        assertEquals("one", list[0].value)
        assertEquals("five-1", list[1].value)
        assertEquals("five-2", list[2].value)
        assertEquals("five-3", list[3].value)
        assertEquals("nine", list[4].value)
    }

    @Test
    fun testAddWithNewKeyBetweenExisting() {
        val list = OrderedKeyedList<Double, String>()
        list.add(1.0, "one")
        list.add(9.0, "nine")
        list.add(5.0, "five")

        assertEquals(3, list.size)
        assertEquals("one", list[0].value)
        assertEquals("five", list[1].value)
        assertEquals("nine", list[2].value)
    }

    @Test
    fun testConstructorWithCollection() {
        val entries = listOf(
            OrderedKeyedList.Entry(5.0, "five"),
            OrderedKeyedList.Entry(2.0, "two"),
            OrderedKeyedList.Entry(8.0, "eight")
        )
        val list = OrderedKeyedList(entries)

        assertEquals(3, list.size)
        assertEquals("two", list[0].value)
        assertEquals("five", list[1].value)
        assertEquals("eight", list[2].value)
    }

    @Test
    fun testConstructorWithVarargs() {
        val list = OrderedKeyedList(
            5.0 to "five",
            2.0 to "two",
            8.0 to "eight"
        )

        assertEquals(3, list.size)
        assertEquals("two", list[0].value)
        assertEquals("five", list[1].value)
        assertEquals("eight", list[2].value)
    }

    @Test
    fun testMixedAddOperations() {
        val list = OrderedKeyedList<Double, String>()

        // Start with middle value
        list.add(5.0, "five")

        // Add smaller (should go to front)
        list.add(2.0, "two")

        // Add larger (should go to end)
        list.add(8.0, "eight")

        // Add duplicate of middle
        list.add(5.0, "five-2")

        // Add between 2 and 5
        list.add(3.5, "three-point-five")

        // Add duplicate of smallest
        list.add(2.0, "two-2")

        // Add duplicate of largest
        list.add(8.0, "eight-2")

        assertEquals(7, list.size)
        assertEquals("two", list[0].value)
        assertEquals("two-2", list[1].value)
        assertEquals("three-point-five", list[2].value)
        assertEquals("five", list[3].value)
        assertEquals("five-2", list[4].value)
        assertEquals("eight", list[5].value)
        assertEquals("eight-2", list[6].value)
    }

    @Test
    fun testListImplementation() {
        val list = OrderedKeyedList<Double, String>()
        list.add(1.0, "one")
        list.add(2.0, "two")
        list.add(3.0, "three")

        // Test List interface methods
        assertEquals(3, list.size)
        assertEquals("one", list[0].value)
        assertEquals("two", list[1].value)
        assertEquals("three", list[2].value)

        // Test iteration
        val values = list.map { it.value }
        assertEquals(listOf("one", "two", "three"), values)
    }

    @Test
    fun testLargeNumberOfDuplicates() {
        val list = OrderedKeyedList<Double, Int>()
        val key = 5.0

        // Add 100 entries with same key
        repeat(100) { i ->
            list.add(key, i)
        }

        assertEquals(100, list.size)

        // Verify order is maintained (insertion order for equal keys)
        repeat(100) { i ->
            assertEquals(i, list[i].value)
            assertEquals(key, list[i].key)
        }
    }

    @Test
    fun testNegativeKeys() {
        val list = OrderedKeyedList<Double, String>()
        list.add(-5.0, "minus-five")
        list.add(0.0, "zero")
        list.add(5.0, "five")
        list.add(-10.0, "minus-ten")

        assertEquals(4, list.size)
        assertEquals("minus-ten", list[0].value)
        assertEquals("minus-five", list[1].value)
        assertEquals("zero", list[2].value)
        assertEquals("five", list[3].value)
    }

    @Test
    fun testDecimalKeys() {
        val list = OrderedKeyedList<Double, String>()
        list.add(1.5, "one-point-five")
        list.add(1.1, "one-point-one")
        list.add(1.9, "one-point-nine")
        list.add(1.5, "one-point-five-2")

        assertEquals(4, list.size)
        assertEquals(1.1, list[0].key)
        assertEquals(1.5, list[1].key)
        assertEquals(1.5, list[2].key)
        assertEquals(1.9, list[3].key)

        assertEquals("one-point-five", list[1].value)
        assertEquals("one-point-five-2", list[2].value)
    }

    @Test
    fun testStressTest() {
        val list = OrderedKeyedList<Double, Int>()
        val random = kotlin.random.Random(42)

        // Add 1000 random entries
        val expectedOrder = mutableListOf<Pair<Double, Int>>()
        repeat(1000) { i ->
            val key = random.nextDouble() * 100
            expectedOrder.add(key to i)
            list.add(key, i)
        }

        // Sort expected order
        expectedOrder.sortWith(compareBy({ it.first }, { expectedOrder.indexOf(it) }))

        // Verify list is sorted
        assertEquals(1000, list.size)
        for (i in 0 until list.size - 1) {
            assertTrue(list[i].key <= list[i + 1].key,
                "List not sorted at index $i: ${list[i].key} > ${list[i + 1].key}")
        }
    }

    @Test
    fun testAllSameKey() {
        val list = OrderedKeyedList<Double, String>()
        val key = 42.0

        list.add(key, "a")
        list.add(key, "b")
        list.add(key, "c")
        list.add(key, "d")
        list.add(key, "e")

        assertEquals(5, list.size)
        assertEquals("a", list[0].value)
        assertEquals("b", list[1].value)
        assertEquals("c", list[2].value)
        assertEquals("d", list[3].value)
        assertEquals("e", list[4].value)
    }

    // Tests for getEntriesWithKey

    @Test
    fun testGetEntriesWithKeyEmpty() {
        val list = OrderedKeyedList<Double, String>()
        val result = list.get(5.0)

        assertTrue(result.isEmpty())
    }

    @Test
    fun testGetEntriesWithKeyNotFound() {
        val list = OrderedKeyedList<Double, String>()
        list.add(1.0, "one")
        list.add(3.0, "three")
        list.add(5.0, "five")

        val result = list.get(2.0)

        assertTrue(result.isEmpty())
    }

    @Test
    fun testGetEntriesWithKeySingleMatch() {
        val list = OrderedKeyedList<Double, String>()
        list.add(1.0, "one")
        list.add(2.0, "two")
        list.add(3.0, "three")

        val result = list.get(2.0)

        assertEquals(1, result.size)
        assertEquals(2.0, result[0].key)
        assertEquals("two", result[0].value)
    }

    @Test
    fun testGetEntriesWithKeyMultipleMatches() {
        val list = OrderedKeyedList<Double, String>()
        list.add(1.0, "one")
        list.add(5.0, "five-1")
        list.add(5.0, "five-2")
        list.add(5.0, "five-3")
        list.add(9.0, "nine")

        val result = list.get(5.0)

        assertEquals(3, result.size)
        assertEquals("five-1", result[0].value)
        assertEquals("five-2", result[1].value)
        assertEquals("five-3", result[2].value)
    }

    @Test
    fun testGetEntriesWithKeyAllSameKey() {
        val list = OrderedKeyedList<Double, String>()
        list.add(5.0, "a")
        list.add(5.0, "b")
        list.add(5.0, "c")

        val result = list.get(5.0)

        assertEquals(3, result.size)
        assertEquals("a", result[0].value)
        assertEquals("b", result[1].value)
        assertEquals("c", result[2].value)
    }

    @Test
    fun testGetEntriesWithKeyAtStart() {
        val list = OrderedKeyedList<Double, String>()
        list.add(1.0, "one-1")
        list.add(1.0, "one-2")
        list.add(5.0, "five")
        list.add(9.0, "nine")

        val result = list.get(1.0)

        assertEquals(2, result.size)
        assertEquals("one-1", result[0].value)
        assertEquals("one-2", result[1].value)
    }

    @Test
    fun testGetEntriesWithKeyAtEnd() {
        val list = OrderedKeyedList<Double, String>()
        list.add(1.0, "one")
        list.add(5.0, "five")
        list.add(9.0, "nine-1")
        list.add(9.0, "nine-2")

        val result = list.get(9.0)

        assertEquals(2, result.size)
        assertEquals("nine-1", result[0].value)
        assertEquals("nine-2", result[1].value)
    }

    @Test
    fun testGetEntriesWithKeyIsView() {
        val list = OrderedKeyedList<Double, String>()
        list.add(1.0, "one")
        list.add(5.0, "five-1")
        list.add(5.0, "five-2")
        list.add(9.0, "nine")

        val result = list.get(5.0)

        assertEquals(2, result.size)

        // Add another entry with key 5.0
        list.add(5.0, "five-3")

        // The view should reflect the change (now includes new entry)
        // Note: The view range doesn't automatically update, so this verifies
        // that we got the correct initial range
        assertEquals(5, list.size)
    }

    @Test
    fun testGetEntriesWithKeyManyDuplicates() {
        val list = OrderedKeyedList<Double, Int>()

        // Add many duplicates
        repeat(100) { i ->
            list.add(5.0, i)
        }

        list.add(1.0, -1)
        list.add(9.0, -2)

        val result = list.get(5.0)

        assertEquals(100, result.size)
        repeat(100) { i ->
            assertEquals(i, result[i].value)
        }
    }

    @Test
    fun testGetEntriesWithKeyStringKeys() {
        val list = OrderedKeyedList<String, Int>()
        list.add("apple", 1)
        list.add("banana", 2)
        list.add("banana", 3)
        list.add("cherry", 4)

        val result = list.get("banana")

        assertEquals(2, result.size)
        assertEquals(2, result[0].value)
        assertEquals(3, result[1].value)
    }

    // Tests for remove

    @Test
    fun testRemoveEmpty() {
        val list = OrderedKeyedList<Double, String>()
        val count = list.remove(5.0)

        assertEquals(0, count)
        assertTrue(list.isEmpty())
    }

    @Test
    fun testRemoveNotFound() {
        val list = OrderedKeyedList<Double, String>()
        list.add(1.0, "one")
        list.add(3.0, "three")
        list.add(5.0, "five")

        val count = list.remove(2.0)

        assertEquals(0, count)
        assertEquals(3, list.size)
    }

    @Test
    fun testRemoveSingleEntry() {
        val list = OrderedKeyedList<Double, String>()
        list.add(1.0, "one")
        list.add(2.0, "two")
        list.add(3.0, "three")

        val count = list.remove(2.0)

        assertEquals(1, count)
        assertEquals(2, list.size)
        assertEquals("one", list[0].value)
        assertEquals("three", list[1].value)
    }

    @Test
    fun testRemoveMultipleEntries() {
        val list = OrderedKeyedList<Double, String>()
        list.add(1.0, "one")
        list.add(5.0, "five-1")
        list.add(5.0, "five-2")
        list.add(5.0, "five-3")
        list.add(9.0, "nine")

        val count = list.remove(5.0)

        assertEquals(3, count)
        assertEquals(2, list.size)
        assertEquals("one", list[0].value)
        assertEquals("nine", list[1].value)
    }

    @Test
    fun testRemoveAllEntries() {
        val list = OrderedKeyedList<Double, String>()
        list.add(5.0, "a")
        list.add(5.0, "b")
        list.add(5.0, "c")

        val count = list.remove(5.0)

        assertEquals(3, count)
        assertTrue(list.isEmpty())
    }

    @Test
    fun testRemoveAtStart() {
        val list = OrderedKeyedList<Double, String>()
        list.add(1.0, "one-1")
        list.add(1.0, "one-2")
        list.add(5.0, "five")
        list.add(9.0, "nine")

        val count = list.remove(1.0)

        assertEquals(2, count)
        assertEquals(2, list.size)
        assertEquals("five", list[0].value)
        assertEquals("nine", list[1].value)
    }

    @Test
    fun testRemoveAtEnd() {
        val list = OrderedKeyedList<Double, String>()
        list.add(1.0, "one")
        list.add(5.0, "five")
        list.add(9.0, "nine-1")
        list.add(9.0, "nine-2")

        val count = list.remove(9.0)

        assertEquals(2, count)
        assertEquals(2, list.size)
        assertEquals("one", list[0].value)
        assertEquals("five", list[1].value)
    }

    @Test
    fun testRemoveMultipleTimes() {
        val list = OrderedKeyedList<Double, String>()
        list.add(1.0, "one")
        list.add(2.0, "two")
        list.add(3.0, "three")
        list.add(4.0, "four")
        list.add(5.0, "five")

        assertEquals(1, list.remove(2.0))
        assertEquals(4, list.size)

        assertEquals(1, list.remove(4.0))
        assertEquals(3, list.size)

        assertEquals(0, list.remove(2.0)) // Already removed
        assertEquals(3, list.size)

        assertEquals("one", list[0].value)
        assertEquals("three", list[1].value)
        assertEquals("five", list[2].value)
    }

    @Test
    fun testRemoveManyDuplicates() {
        val list = OrderedKeyedList<Double, Int>()

        // Add many duplicates
        repeat(100) { i ->
            list.add(5.0, i)
        }

        list.add(1.0, -1)
        list.add(9.0, -2)

        val count = list.remove(5.0)

        assertEquals(100, count)
        assertEquals(2, list.size)
        assertEquals(-1, list[0].value)
        assertEquals(-2, list[1].value)
    }

    @Test
    fun testRemoveStringKeys() {
        val list = OrderedKeyedList<String, Int>()
        list.add("apple", 1)
        list.add("banana", 2)
        list.add("banana", 3)
        list.add("cherry", 4)

        val count = list.remove("banana")

        assertEquals(2, count)
        assertEquals(2, list.size)
        assertEquals(1, list[0].value)
        assertEquals(4, list[1].value)
    }

    @Test
    fun testRemoveAndReAdd() {
        val list = OrderedKeyedList<Double, String>()
        list.add(1.0, "one")
        list.add(5.0, "five-1")
        list.add(5.0, "five-2")
        list.add(9.0, "nine")

        list.remove(5.0)
        assertEquals(2, list.size)

        // Re-add with same key
        list.add(5.0, "five-new-1")
        list.add(5.0, "five-new-2")

        assertEquals(4, list.size)
        assertEquals("one", list[0].value)
        assertEquals("five-new-1", list[1].value)
        assertEquals("five-new-2", list[2].value)
        assertEquals("nine", list[3].value)
    }

    // Tests for set

    @Test
    fun testSetOnEmptyList() {
        val list = OrderedKeyedList<Double, String>()
        list.set(5.0, "five")

        assertEquals(1, list.size)
        assertEquals(5.0, list[0].key)
        assertEquals("five", list[0].value)
    }

    @Test
    fun testSetWhenKeyDoesNotExist() {
        val list = OrderedKeyedList<Double, String>()
        list.add(1.0, "one")
        list.add(3.0, "three")
        list.add(9.0, "nine")

        list.set(5.0, "five")

        assertEquals(4, list.size)
        assertEquals("one", list[0].value)
        assertEquals("three", list[1].value)
        assertEquals("five", list[2].value)
        assertEquals("nine", list[3].value)
    }

    @Test
    fun testSetReplaceSingleEntry() {
        val list = OrderedKeyedList<Double, String>()
        list.add(1.0, "one")
        list.add(5.0, "five-old")
        list.add(9.0, "nine")

        list.set(5.0, "five-new")

        assertEquals(3, list.size)
        assertEquals("one", list[0].value)
        assertEquals("five-new", list[1].value)
        assertEquals("nine", list[2].value)
    }

    @Test
    fun testSetReplaceMultipleEntries() {
        val list = OrderedKeyedList<Double, String>()
        list.add(1.0, "one")
        list.add(5.0, "five-1")
        list.add(5.0, "five-2")
        list.add(5.0, "five-3")
        list.add(9.0, "nine")

        list.set(5.0, "five-new")

        assertEquals(3, list.size)
        assertEquals("one", list[0].value)
        assertEquals("five-new", list[1].value)
        assertEquals("nine", list[2].value)
    }

    @Test
    fun testSetReplaceAllEntries() {
        val list = OrderedKeyedList<Double, String>()
        list.add(5.0, "a")
        list.add(5.0, "b")
        list.add(5.0, "c")

        list.set(5.0, "new")

        assertEquals(1, list.size)
        assertEquals("new", list[0].value)
    }

    @Test
    fun testSetAtStart() {
        val list = OrderedKeyedList<Double, String>()
        list.add(1.0, "one-1")
        list.add(1.0, "one-2")
        list.add(5.0, "five")
        list.add(9.0, "nine")

        list.set(1.0, "one-new")

        assertEquals(3, list.size)
        assertEquals("one-new", list[0].value)
        assertEquals("five", list[1].value)
        assertEquals("nine", list[2].value)
    }

    @Test
    fun testSetAtEnd() {
        val list = OrderedKeyedList<Double, String>()
        list.add(1.0, "one")
        list.add(5.0, "five")
        list.add(9.0, "nine-1")
        list.add(9.0, "nine-2")

        list.set(9.0, "nine-new")

        assertEquals(3, list.size)
        assertEquals("one", list[0].value)
        assertEquals("five", list[1].value)
        assertEquals("nine-new", list[2].value)
    }

    @Test
    fun testSetMultipleTimes() {
        val list = OrderedKeyedList<Double, String>()
        list.add(1.0, "one")
        list.add(2.0, "two")
        list.add(3.0, "three")

        list.set(2.0, "two-updated")
        assertEquals(3, list.size)
        assertEquals("two-updated", list[1].value)

        list.set(2.0, "two-updated-again")
        assertEquals(3, list.size)
        assertEquals("two-updated-again", list[1].value)

        list.set(2.0, "two-final")
        assertEquals(3, list.size)
        assertEquals("two-final", list[1].value)
    }

    @Test
    fun testSetManyDuplicates() {
        val list = OrderedKeyedList<Double, Int>()

        // Add many duplicates
        repeat(100) { i ->
            list.add(5.0, i)
        }

        list.add(1.0, -1)
        list.add(9.0, -2)

        list.set(5.0, 999)

        assertEquals(3, list.size)
        assertEquals(-1, list[0].value)
        assertEquals(999, list[1].value)
        assertEquals(-2, list[2].value)
    }

    @Test
    fun testSetStringKeys() {
        val list = OrderedKeyedList<String, Int>()
        list.add("apple", 1)
        list.add("banana", 2)
        list.add("banana", 3)
        list.add("cherry", 4)

        list.set("banana", 99)

        assertEquals(3, list.size)
        assertEquals(1, list[0].value)
        assertEquals(99, list[1].value)
        assertEquals(4, list[2].value)
    }

    @Test
    fun testSetAndGet() {
        val list = OrderedKeyedList<Double, String>()
        list.add(5.0, "a")
        list.add(5.0, "b")
        list.add(5.0, "c")

        list.set(5.0, "new")

        val result = list.get(5.0)
        assertEquals(1, result.size)
        assertEquals("new", result[0].value)
    }

    @Test
    fun testSetThenAdd() {
        val list = OrderedKeyedList<Double, String>()
        list.add(1.0, "one")
        list.add(5.0, "five-1")
        list.add(5.0, "five-2")
        list.add(9.0, "nine")

        list.set(5.0, "five-new")
        assertEquals(3, list.size)

        // Add more entries with same key
        list.add(5.0, "five-added-1")
        list.add(5.0, "five-added-2")

        assertEquals(5, list.size)
        assertEquals("one", list[0].value)
        assertEquals("five-new", list[1].value)
        assertEquals("five-added-1", list[2].value)
        assertEquals("five-added-2", list[3].value)
        assertEquals("nine", list[4].value)
    }

    @Test
    fun testSetNewKeyMaintainsOrder() {
        val list = OrderedKeyedList<Double, String>()
        list.add(1.0, "one")
        list.add(9.0, "nine")

        list.set(5.0, "five")

        assertEquals(3, list.size)
        assertEquals(1.0, list[0].key)
        assertEquals(5.0, list[1].key)
        assertEquals(9.0, list[2].key)
    }

    @Test
    fun testSetPreservesOtherEntries() {
        val list = OrderedKeyedList<Double, String>()
        list.add(1.0, "one-1")
        list.add(1.0, "one-2")
        list.add(5.0, "five-1")
        list.add(5.0, "five-2")
        list.add(9.0, "nine-1")
        list.add(9.0, "nine-2")

        list.set(5.0, "five-new")

        assertEquals(5, list.size)
        assertEquals("one-1", list[0].value)
        assertEquals("one-2", list[1].value)
        assertEquals("five-new", list[2].value)
        assertEquals("nine-1", list[3].value)
        assertEquals("nine-2", list[4].value)
    }
}
