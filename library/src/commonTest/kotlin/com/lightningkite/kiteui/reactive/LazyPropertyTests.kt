package com.lightningkite.kiteui.reactive

import com.lightningkite.kiteui.launch
import kotlin.test.Test
import kotlin.test.assertEquals

class LazyPropertySharedBehaviorTests {
    @Test
    fun sharedPassesNulls() {
        val a = LateInitProperty<Int?>()
        val b = LazyProperty { a.await() }
        var starts = 0
        var hits = 0
        with(CalculationContext.Standard()) {
            reactiveScope {
                starts++
                b.await()
                hits++
            }
            assertEquals(0, hits)
            assertEquals(1, starts)
            a.value = null
            assertEquals(1, hits)
            assertEquals(1, starts)
            a.value = 1
            assertEquals(2, hits)
            assertEquals(2, starts)
        }
    }

    @Test
    fun sharedDoesNotEmitSameValue() {
        val a = LateInitProperty<Int?>()
        val b = LazyProperty { a.await() }
        var starts = 0
        var hits = 0
        with(CalculationContext.Standard()) {
            reactiveScope {
                starts++
                b.await()
                hits++
            }
            assertEquals(0, hits)
            assertEquals(1, starts)
            a.value = null
            assertEquals(1, hits)
            assertEquals(1, starts)
            a.value = null
            assertEquals(1, hits)
            assertEquals(1, starts)
        }
    }

    @Test
    fun sharedTerminatesWhenNoOneIsListening() {
        var onRemoveCalled = 0
        var scopeCalled = 0
        val shared = LazyProperty {
            scopeCalled++
            onRemove { println("Remove called"); onRemoveCalled++ }
            42
        }

        assertEquals(0, scopeCalled)
        assertEquals(0, onRemoveCalled)
        val removeListener = shared.addListener { }
        assertEquals(1, scopeCalled, "Scope not called")
        assertEquals(0, onRemoveCalled)
        removeListener()
        assertEquals(1, scopeCalled)
        assertEquals(1, onRemoveCalled, "OnRemove not called")
    }

    @Test
    fun sharedSharesCalculations() {
        var hits = 0
        val property = Property(1)
        val a = LazyProperty {
            hits++
            property.await()
        }
        testContext {
            println("Starting scopes")

            reactiveScope {
                a.await()
            }
            launch {
                a.await()
            }
            reactiveScope {
                a.await()
            }
            assertEquals(1, hits)

            property.value = 2
            assertEquals(2, hits)
        }.cancel()

        assertEquals(ReadableState.notReady, a.state)

        // Shouldn't be listening anymore, so it does not trigger a hit
        property.value = 3
        assertEquals(2, hits, "Failed on cancelled context")

        testContext {
            reactiveScope {
                a.await()
            }
            launch {
                a.await()
            }
            reactiveScope {
                a.await()
            }
        }.cancel()
        assertEquals(3, hits)
    }

    @Test
    fun sharedSharesWithLaunch() {
        // Note that it can only share if it doesn't complete yet.
        // This is because it's considered 'inactive' once it completes the first time if there are no stable listeners.
        val delayed = VirtualDelay { 1 }
        var starts = 0
        var hits = 0
        val a = LazyProperty {
            starts++
            delayed.await()
            hits++
        }
        testContext {
            launch {
                a.await()
            }
            reactiveScope {
                a.await()
            }
            assertEquals(1, starts)
            assertEquals(0, hits)
            delayed.go()
            assertEquals(1, starts)
            assertEquals(1, hits)

        }.cancel()
    }

    @Test
    fun sharedWorksWithLaunch() {
        val delayed = VirtualDelay { 1 }
        var starts = 0
        var hits = 0
        val a = LazyProperty {
            starts++
            delayed.await()
            hits++
        }
        testContext {
            launch { a.await() }
            launch { a.await() }
            launch { a.await() }
            assertEquals(1, starts)
            assertEquals(0, hits)
            println("Going")
            delayed.go()
            assertEquals(1, starts)
            assertEquals(1, hits)

            println("Clearing")
            delayed.clear()
            launch { a.await() }
            launch { a.await() }
            launch { a.await() }
            assertEquals(2, starts)
            assertEquals(1, hits)
            delayed.go()
            assertEquals(2, starts)
            assertEquals(2, hits)
        }
    }

    @Test
    fun sharedReloads() {
        val late = LateInitProperty<Int>()
        var starts = 0
        var hits = 0
        val a = LazyProperty {
            starts++
            val r = late.await()
            hits++
            r
        }
        testContext {
            late.addListener {}
            a.addListener {}

            late.value = 1
            assertEquals(ReadableState(1), a.state)

            println("Unsetting")
            late.unset()
            assertEquals(ReadableState.notReady, a.state)

            late.value = 2
            assertEquals(ReadableState(2), a.state)
        }
    }

    interface CachingModelRestEndpoints {
        fun item(id: Int): Readable<Int?>
    }
    @Test fun recreation() {
        class CachingModelRestEndpointsC():
            CachingModelRestEndpoints {
            val items = HashMap<Int, LateInitProperty<Int?>>()
            override fun item(id: Int) = items.getOrPut(id) { LateInitProperty() }
        }
        data class Session(val dealerships: CachingModelRestEndpoints)
        val session = LateInitProperty<Session?>()
        fun Readable<CachingModelRestEndpoints>.flatten(): CachingModelRestEndpoints = object :
            CachingModelRestEndpoints {
            override fun item(id: Int): Readable<Int?> {
                return shared { this@flatten().item(id) }.let { shared { it()() } }
            }
        }
        val dealerships = shared { session.awaitNotNull().dealerships }.flatten()
        val currentId = Property<Int>(0)
        suspend fun alt() = dealerships.item(currentId()).await()
        val seller = shared { dealerships.item(currentId())() }
        val sellerNn = seller.waitForNotNull
        testContext {
            reactiveScope {println("session: ${session()}") }
            reactiveScope {println("alt: ${alt()}") }
            reactiveScope {println("seller: ${seller()}") }
            reactiveScope {println("sellerNn: ${sellerNn()}") }
            val e = CachingModelRestEndpointsC()
            session.value = Session(e)
            e.item(0).value = 1
            currentId.value = 1
            e.item(1).value = 2
            println("DONE")
        }
    }
}

class LazyPropertyTests {
    @Test fun sharedIsOverridden() {
        val late = LateInitProperty<Int>()
        val test = LazyProperty {
            println("In initial value")
            late.await()
        }
        testContext {
            test.addListener {  }

            assertEquals(ReadableState.notReady, test.state)

            late.value = 1
            assertEquals(ReadableState(1), test.state)

            test.value = 2
            assertEquals(ReadableState(2), test.state)
        }
    }

    @Test fun stopsListeningWhenOverridden() {
        var hits: Int = 0
        val prop = Property(1)
        val test = LazyProperty {
            hits++
            prop.await()
        }
        testContext {
            assertEquals(0, hits)

            test.addListener {  }
            assertEquals(1, hits)

            prop.value = 2
            assertEquals(ReadableState(2), test.state)
            assertEquals(2, hits)

            test.value = 0
            assertEquals(ReadableState(0), test.state)

            prop.value = 3
            assertEquals(ReadableState(0), test.state)
            assertEquals(2, hits)
        }
    }

    @Test fun startsListeningAgainOnceReset() {
        var hits: Int = 0
        val prop = Property(1)
        val test = LazyProperty {
            hits++
            prop.await()
        }
        testContext {
            assertEquals(0, hits)

            test.addListener {  }
            assertEquals(1, hits)

            prop.value = 2
            assertEquals(ReadableState(2), test.state)
            assertEquals(2, hits)

            test.value = 0
            assertEquals(ReadableState(0), test.state)
            assertEquals(2, hits)

            prop.value = 3
            assertEquals(2, hits)

            test.reset()
            assertEquals(ReadableState(3), test.state)

            prop.value = 4
            assertEquals(ReadableState(4), test.state)
        }
    }

    @Test fun useLastWhileLoadingWorks() {
        val late = LateInitProperty<Int>()
        val test = LazyProperty(useLastWhileLoading = true) {
            late.await()
        }

        testContext {
            test.addListener {  }

            assertEquals(ReadableState.notReady, test.state)

            late.value = 1
            assertEquals(ReadableState(1), test.state)

            test.value = 10
            assertEquals(ReadableState(10), test.state)

            late.unset()
            assertEquals(ReadableState(10), test.state)

            test.reset()
            assertEquals(ReadableState(10), test.state)

            late.value = 1
            assertEquals(ReadableState(1), test.state)
        }
    }

    @Test fun keepsListeningIfTold() {
        var hits = 0
        val prop = Property(0)
        val test = LazyProperty(stopListeningWhenOverridden = false) {
            hits++
            prop.await()
        }

        testContext {
            assertEquals(0, hits)

            test.addListener {  }

            assertEquals(1, hits)
            assertEquals(ReadableState(0), test.state)

            prop.value = 1

            assertEquals(2, hits)
            assertEquals(ReadableState(1), test.state)

            test.value = 10

            assertEquals(2, hits)
            assertEquals(ReadableState(10), test.state)

            prop.value = 2

            assertEquals(3, hits)
            assertEquals(ReadableState(10), test.state)

            test.reset()

            assertEquals(3, hits)
            assertEquals(ReadableState(2), test.state)
        }
    }
}