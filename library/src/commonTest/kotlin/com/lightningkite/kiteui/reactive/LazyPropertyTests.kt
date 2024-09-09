package com.lightningkite.kiteui.reactive

import com.lightningkite.kiteui.ConsoleRoot
import com.lightningkite.kiteui.launch
import kotlin.test.Test
import kotlin.test.assertEquals

class LazyPropertySharedBehaviorTests {
    @Test
    fun sharedPassesNulls() {
        val a = LateInitProperty<Int?>()
        val b = LazyProperty { a() }
        var hits = 0
        testContext {
            reactiveScope {
                b()
                hits++
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
        val b = LazyProperty { a() }
        var hits = 0
        testContext {
            reactiveScope {
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
        val shared = LazyProperty {
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
        val a = LazyProperty {
            hits++
            property()
        }
        testContext {
            reactiveScope {
                a()
            }
            launch {
                a.await()
            }
            reactiveScope {
                a()
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
                a()
            }
            launch {
                a.await()
            }
            reactiveScope {
                a()
            }
        }.cancel()
        assertEquals(3, hits)
    }

    @Test fun sharedReloads() {
        val late = LateInitProperty<Int>()
        var starts = 0
        var hits = 0
        val a = LazyProperty {
            starts++
            val r = late()
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

class LazyPropertyTests {
    @Test fun sharedIsOverridden() {
        val late = LateInitProperty<Int>()
        val test = LazyProperty {
            println("In initial value")
            late()
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
            prop()
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
            prop()
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
            late()
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
            println("Calculation")
            hits++
            prop()
        }

        testContext {
            assertEquals(0, hits)

            println("Adding listener...")
            test.addListener {  }
            println("Added listener.")

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

    @Test fun testStupidCase() {
        val basis = Property("Test")
        val lazy = LazyProperty(stopListeningWhenOverridden = false, debug = ConsoleRoot) { basis() }
        val lensed = lazy.lens { it.take(3) }
        testContext {
            println(lensed.state)
            launch {
                println("Starting")
                println(lensed())
            }
        }
    }
}