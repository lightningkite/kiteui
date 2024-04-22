package com.lightningkite.kiteui

import com.lightningkite.kiteui.reactive.*
import kotlin.test.Test
import kotlin.test.assertEquals

class SharedTest {
    @Test
    fun sharedPassesNulls() {
        val a = LateInitProperty<Int?>()
        val b = shared { a.await() }
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

    @Test fun sharedDoesNotEmitSameValue() {
        val a = LateInitProperty<Int?>()
        val b = shared { a.await() }
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

    @Test fun sharedTerminatesWhenNoOneIsListening() {
        var onRemoveCalled = 0
        var scopeCalled = 0
        val shared = shared {
            scopeCalled++
            onRemove { onRemoveCalled++ }
            42
        }
        assertEquals(0, scopeCalled)
        assertEquals(0, onRemoveCalled)
        val removeListener = shared.addListener {  }
        assertEquals(1, scopeCalled)
        assertEquals(0, onRemoveCalled)
        removeListener()
        assertEquals(1, scopeCalled)
        assertEquals(1, onRemoveCalled)
    }

    @Test fun sharedSharesCalculations() {
        var hits = 0
        val property = Property(1)
        val a = shared {
            hits++
            property.await()
        }
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
            assertEquals(1, hits)

            property.value = 2
            assertEquals(2, hits)
        }.cancel()

        // Shouldn't be listening anymore, so it does not trigger a hit
        property.value = 3
        assertEquals(2, hits)

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

    @Test fun sharedSharesWithLaunch() {
        // Note that it can only share if it doesn't complete yet.
        // This is because it's considered 'inactive' once it completes the first time if there are no stable listeners.
        val delayed = VirtualDelay { 1 }
        var starts = 0
        var hits = 0
        val a = shared {
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

    @Test fun sharedWorksWithLaunch() {
        val delayed = VirtualDelay { 1 }
        var starts = 0
        var hits = 0
        val a = shared {
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
            delayed.go()
            assertEquals(1, starts)
            assertEquals(1, hits)

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

    @Test fun sharedReloads() {
        val late = LateInitProperty<Int>()
        var starts = 0
        var hits = 0
        val a = shared {
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

            late.unset()
            assertEquals(ReadableState.notReady, a.state)

            late.value = 2
            assertEquals(ReadableState(2), a.state)
        }
    }
}