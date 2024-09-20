package com.lightningkite.kiteui.reactive

import com.lightningkite.kiteui.ConsoleRoot
import com.lightningkite.kiteui.WaitGate
import com.lightningkite.kiteui.launch
import com.lightningkite.kiteui.reactive.lensing.WritableList
import com.lightningkite.kiteui.reactive.lensing.map
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class MappingKtTest {

    data class Sample(
        val x: Int,
        val y: List<Int>
    )

    @Test fun readableLenses() {
        val source = Property(42)
        val lenses = listOf(
            source.lens(
                get = { it + 1 },
                set = { it - 1 }
            ),
            (source as Writable<Int>).lens(
                get = { it + 1 },
                set = { it - 1 }
            ),
            source.lens(
                get = { it + 1 },
                modify = { _, it -> it - 1 }
            ),
            (source as Writable<Int>).lens(
                get = { it + 1 },
                modify = { _, it -> it - 1 }
            ),
            (source as Readable<Int>).lens(
                get = { it + 1 },
            ),
            (source as ImmediateReadable<Int>).lens(
                get = { it + 1 },
            )
        )
        for (view in lenses) {
            source.value = 41
            assertEquals(ReadableState(42), view.state)
            testContext {
                var seen = -1
                var sets = 0
                reactiveScope {
                    seen = view()
                    sets++
                }
                assertEquals(42, seen)
                assertEquals(1, sets)
                source.value = 42
                assertEquals(43, seen)
                assertEquals(2, sets)
                source.value = 42
                assertEquals(43, seen)
                assertEquals(2, sets)
                source.value = 43
                assertEquals(44, seen)
                assertEquals(3, sets)
            }
        }
    }

    @Test
    fun writableLenses() {
        val source = Property(42)
        val lenses = listOf(
            source.lens(
                get = { it + 1 },
                set = { it - 1 }
            ),
            (source as Writable<Int>).lens(
                get = { it + 1 },
                set = { it - 1 }
            ),
            source.lens(
                get = { it + 1 },
                modify = { _, it -> it - 1 }
            ),
            (source as Writable<Int>).lens(
                get = { it + 1 },
                modify = { _, it -> it - 1 }
            )
        )
        for (view in lenses) {
            source.value = 41
            assertEquals(ReadableState(42), view.state)
            testContext {
                var seen = -1
                var sets = 0
                reactiveScope {
                    seen = view()
                    sets++
                }
                assertEquals(42, seen)
                assertEquals(1, sets)
                launch { view.set(43) }
                assertEquals(43, seen)
                assertEquals(2, sets)
                launch { view.set(43) }
                assertEquals(43, seen)
                assertEquals(2, sets)
                source.value += 1
                assertEquals(44, seen)
                assertEquals(3, sets)
            }
        }
    }

    @Test
    fun subfield() {
        val source = Property(Sample(42, listOf(1, 2, 3)))
        val lenses = listOf(
            source.lens(
                get = { it.x },
                modify = { old, it -> old.copy(x = it) }
            ),
            (source as Writable<Sample>).lens(
                get = { it.x },
                modify = { old, it -> old.copy(x = it) }
            )
        )
        for (view in lenses) {
            source.value = Sample(42, listOf(1, 2, 3))
            assertEquals(ReadableState(42), view.state)
            testContext {
                var seen = -1
                var sets = 0
                reactiveScope {
                    seen = view()
                    sets++
                }
                assertEquals(42, seen)
                assertEquals(1, sets)
                launch { view.set(43) }
                assertEquals(43, seen)
                assertEquals(2, sets)
                launch { view.set(43) }
                assertEquals(43, seen)
                assertEquals(2, sets)
                source.value = source.value.copy(y = source.value.y + 4)
                assertEquals(43, seen)
                assertEquals(2, sets)
                source.value = source.value.copy(x = source.value.x + 1)
                assertEquals(44, seen)
                assertEquals(3, sets)
            }
        }
    }

    @Test
    fun subfieldLate() {
        val source = LateInitProperty<Sample>()
        val view = source.lens(
            get = { it.x },
            modify = { old, it ->
                println("Modify $old $it")
                old.copy(x = it)
            }
        )
        source.addListener {
            println("source raw: ${source.state}")
        }
//        view.addListener {
//            println("View raw: ${view.state}")
//        }
        assertEquals(ReadableState.notReady, view.state)
        testContext {
            var seen = -1
            var sets = 0
            reactiveScope {
                println("Rerunning...")
                seen = view()
                println("Got $seen")
                sets++
            }
            assertEquals(-1, seen)
            assertEquals(0, sets)
            launch { view.set(43) }
            assertEquals(-1, seen)
            assertEquals(0, sets)
            source.value = Sample(x = 42, y = listOf(1, 2, 3))
            // Weird trait here: set is queued!
            assertEquals(43, seen)
            assertEquals(2, sets)
            launch { view.set(44) }
            assertEquals(44, seen)
            assertEquals(3, sets)
            source.value = source.value.copy(y = source.value.y + 4)
            assertEquals(44, seen)
            assertEquals(3, sets)
            source.value = source.value.copy(x = source.value.x + 1)
            assertEquals(45, seen)
            assertEquals(4, sets)
        }
    }

    fun perElementTest(action: CalculationContext.(source: Property<List<Int>>, view: WritableList<Int, Int, WritableList<Int, Int, *>.ElementWritable>) -> Unit) {
        val source = Property(listOf(1, 2, 3)).apply {
            addListener {
                println("source: $value")
            }
        }
        val view = WritableList<Int, Int, WritableList<Int, Int, *>.ElementWritable>(
            source,
            ConsoleRoot,
            identity = { it },
            elementLens = { it })
        assertEquals(source.value, view.state.get().map { it.value })
        testContext {
            // The state of each subwritable always matches the source
            assertEquals(source.value, view.state.get().map { it.value }.also { println("Before: $it") })
            action(source, view)
            println("After values: ${source.value}")
            assertEquals(source.value, view.state.get().map {
                println("Checking item ${it.id}")
                it.value
            }.also { println("After: $it") })
        }
    }


    // Identity of sub writables remains the same for same elements
    @Test
    fun listIdentityWithoutListen() = perElementTest { source, view ->
        val two = view.state.get().find { it.value == 2 }!!
        val three = view.state.get().find { it.value == 3 }!!
        source.value = listOf(2, 3, 4)
        assertSame(view.state.get().find { it.value == 2 }, two)
        assertSame(view.state.get().find { it.value == 3 }, three)
    }

    // Identity of sub writables remains the same for same elements
    @Test
    fun listIdentity() = perElementTest { source, view ->
        val two = view.state.get().find { it.value == 2 }!!
        val three = view.state.get().find { it.value == 3 }!!
        reactiveScope { three() }
        source.value = listOf(2, 3, 4)
        assertSame(view.state.get().find { it.value == 2 }, two)
        assertSame(view.state.get().find { it.value == 3 }, three)
    }

    // Setting a sub writable updates the source
    @Test
    fun listSettingWithoutListen() = perElementTest { source, view ->
        val sub = view.state.get().find { it.value == 3 }!!
        launch {
            sub.set(4)
        }
        assertEquals(4, source.value.last())
        assertEquals(4, sub.value)
    }

    // Setting a sub writable updates the source
    @Test
    fun listSettingWithListen() = perElementTest { source, view ->
        val sub = view.state.get().find { it.value == 3 }!!
        reactiveScope { sub() }
        launch {
            sub.set(4)
        }
        assertEquals(4, source.value.last())
        assertEquals(4, sub.value)
    }

    // Insertion works
    @Test
    fun listInsertionWithoutListen() = perElementTest { source, view ->
        val sub = view.state.get().find { it.value == 3 }!!
        launch { view.elements.set(view.elements.awaitOnce() + view.newElement(4)) }
        assertEquals(4, source.value.size)
    }

    @Test
    fun listInsertion() = perElementTest { source, view ->
        val sub = view.state.get().find { it.value == 3 }!!
        reactiveScope { sub() }
        launch { view.elements.set(view.elements.awaitOnce() + view.newElement(4)) }
        assertEquals(4, source.value.size)
    }

    // Removal works
    @Test
    fun listRemovalWithoutListen() = perElementTest { source, view ->
        val sub = view.state.get().find { it.value == 3 }!!
        launch { view.elements.set(view.elements.awaitOnce().filter { it.value != 3 }) }
        assertEquals(2, source.value.size)
    }

    @Test
    fun listRemoval() = perElementTest { source, view ->
        val sub = view.state.get().find { it.value == 3 }!!
        reactiveScope {
            try {
                sub()
            } catch (e: Exception) {
                println("Blocked $e")
            }
        }
        launch { view.elements.set(view.elements.awaitOnce().filter { it.value != 3 }) }
        assertEquals(2, source.value.size)
    }

    // Removal works by identity
    @Test
    fun listRemovalByIdentityWithoutListen() = perElementTest { source, view ->
        launch { view.remove(3) }
        assertEquals(2, source.value.size)
    }

    @Test
    fun listRemovalByIdentity() = perElementTest { source, view ->
        reactiveScope { sub() }
        launch { view.remove(3) }
        assertEquals(2, source.value.size)
    }

    // Rearranging works and retains identity
    @Test
    fun listRearrangingWithoutListening() = perElementTest { source, view ->
        val sub = view.state.get().find { it.value == 3 }!!
        launch { view.elements.set(view.elements.awaitOnce().reversed()) }
        assertEquals(3, sub.value)
    }

    @Test
    fun listRearranging() = perElementTest { source, view ->
        val sub = view.state.get().find { it.value == 3 }!!
        reactiveScope { sub() }
        launch { view.elements.set(view.elements.awaitOnce().reversed()) }
        assertEquals(3, sub.value)
    }

    @Test
    fun listSetWaitsForCompletion() {
        val backing = Property(listOf(1, 2, 3)).apply {
            addListener {
                println("backing: $value")
            }
        }
        val setGate = WaitGate()
        val source = object : ImmediateReadable<List<Int>>, Writable<List<Int>> {
            override val value: List<Int> get() = backing.value
            override fun addListener(listener: () -> Unit): () -> Unit = backing.addListener(listener)

            override suspend fun set(value: List<Int>) {
                setGate.await()
                backing.value = value
            }
        }
        val view = WritableList<Int, Int, WritableList<Int, Int, *>.ElementWritable>(
            source,
            ConsoleRoot,
            identity = { it },
            elementLens = { it })
        assertEquals(source.value, view.state.get().map { it.value })
        testContext {
            // The state of each subwritable always matches the source
            assertEquals(source.value, view.state.get().map { it.value }.also { println("Before: $it") })
            val third = view.state.get().find { it.value == 3 }!!
            reactiveScope { println("third: ${third()}") }

            launch {
                third.set(4)
                println("Complete")
            }
            assertEquals(3, third.value)
            setGate.permitOnce()
            assertEquals(4, third.value)

            assertEquals(source.value, view.state.get().map { it.value }.also { println("After: $it") })
        }
    }

    @Test
    fun listConcurrentWorks() {
        val backing = Property(listOf(1, 2, 3)).apply {
            addListener {
                println("backing: $value")
            }
        }
        val setGate = WaitGate()
        val source = object : ImmediateReadable<List<Int>>, Writable<List<Int>> {
            override val value: List<Int> get() = backing.value
            override fun addListener(listener: () -> Unit): () -> Unit = backing.addListener(listener)

            override suspend fun set(value: List<Int>) {
                setGate.await()
                backing.value = value
            }
        }
        val view = WritableList<Int, Int, WritableList<Int, Int, *>.ElementWritable>(
            source,
            ConsoleRoot,
            identity = { it },
            elementLens = { it })
        assertEquals(source.value, view.state.get().map { it.value })
        testContext {
            // The state of each subwritable always matches the source
            assertEquals(source.value, view.state.get().map { it.value }.also { println("Before: $it") })
            val second = view.state.get().find { it.value == 2 }!!
            val third = view.state.get().find { it.value == 3 }!!
            reactiveScope { println("second: ${second()}") }
            reactiveScope { println("third: ${third()}") }

            launch {
                third.set(4)
                println("Complete third")
            }
            launch {
                second.set(3)
                println("Complete second")
            }
            assertEquals(2, second.value)
            assertEquals(3, third.value)
            setGate.permitOnce()
            assertEquals(3, second.value)
            assertEquals(4, third.value)

            assertEquals(source.value, view.state.get().map { it.value }.also { println("After: $it") })
        }
    }
}
