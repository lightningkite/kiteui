package com.lightningkite.kiteui.reactive

import com.lightningkite.kiteui.ConsoleRoot
import com.lightningkite.kiteui.launch
import com.lightningkite.kiteui.reactive.*
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertEquals

class SharedSuspendingTest {
    @Test
    fun sharedPassesNulls() {
        val a = LateInitProperty<Int?>()
        val b = sharedSuspending(Dispatchers.Unconfined) { a() }
        var hits = 0
        testContext {
            reactiveSuspending {
                Exception("Calculating...").printStackTrace()
                b()
                hits++
                println("Calculated")
            }
            assertEquals(0, hits)
            a.value = null
            assertEquals(1, hits)
            a.value = 1
            assertEquals(2, hits)
        }
    }

    @Test fun sharedDoesNotEmitSameValue() {
        val a = LateInitProperty<Int?>()
        val b = sharedSuspending(Dispatchers.Unconfined) { a() }
        var hits = 0
        testContext {
            reactiveSuspending {
                b()
                hits++
            }
            assertEquals(0, hits)
            a.value = null
            assertEquals(1, hits)
            a.value = null
            assertEquals(1, hits)
        }
    }

    @Test fun sharedTerminatesWhenNoOneIsListening() {
        var onRemoveCalled = 0
        var scopeCalled = 0
        val sharedSuspending = sharedSuspending(Dispatchers.Unconfined) {
            scopeCalled++
            onRemove { onRemoveCalled++ }
            42
        }
        assertEquals(0, scopeCalled)
        assertEquals(0, onRemoveCalled)
        val removeListener = sharedSuspending.addListener {  }
        assertEquals(1, scopeCalled)
        assertEquals(0, onRemoveCalled)
        removeListener()
        assertEquals(1, scopeCalled)
        assertEquals(1, onRemoveCalled)
    }

    @Test fun sharedSharesCalculations() {
        var hits = 0
        val property = Property(1)
        val a = sharedSuspending(Dispatchers.Unconfined) {
            hits++
            property()
        }
        testContext {
            reactiveSuspending {
                a()
            }
            launch(key = Unit) {
                a.await()
            }
            reactiveSuspending {
                a()
            }
            assertEquals(1, hits)

            property.value = 2
            assertEquals(2, hits)
        }

        // Shouldn't be listening anymore, so it does not trigger a hit
        property.value = 3
        assertEquals(2, hits)

        testContext {
            reactiveSuspending {
                a()
            }
            launch(key = Unit) {
                a.await()
            }
            reactiveSuspending {
                a()
            }
        }
        assertEquals(3, hits)
    }

    @Test fun sharedReloads() {
        val late = LateInitProperty<Int>()
        var starts = 0
        var hits = 0
        val a = SharedSuspendingReadable(coroutineContext = Dispatchers.Unconfined, useLastWhileLoading = false) {
            starts++
            val r = late()
            hits++
            r
        }.apply { debug = ConsoleRoot }
        testContext {
            late.addListener {}
            a.addListener {}

            println("listeners added")

            println("late.value = 1")
            late.value = 1
            println("late.value = 1 done")
            assertEquals(ReadableState(1), a.state)

            println("late.unset()")
            late.unset()
            println("late.unset() done")
            assertEquals(ReadableState.notReady, a.state)

            late.value = 2
            assertEquals(ReadableState(2), a.state)
        }
    }

}