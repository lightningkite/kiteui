package com.lightningkite.kiteui.reactive

import com.lightningkite.kiteui.*
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ReactivitySuspendingTests {

    @Test fun invokeAdapter() {
        testContext {
            val value = Property(1)
            val runner: ReactiveContext.()->Int = {
                value()
            }
            var read = -1
            load {
                read = runner()
            }
            assertEquals(read, value.value)
        }
    }
    @Test fun invokeAdapterSlow() {
        testContext {
            val value = LateInitProperty<Int>()
            val runner: ReactiveContext.()->Int = {
                value()
            }
            var read = -1
            load {
                read = runner()
            }
            assertEquals(read, -1)
            value.value = 1
            assertEquals(read, value.value)
        }
    }

    @Test
    fun waitingTest() {
        val property = Property<Int?>(null)
        val emissions = ArrayList<Int>()
        testContext {
            reactiveSuspending {
                emissions.add(property.waitForNotNull.await())
            }
            repeat(10) {
                println("Set to null")
                property.value = null
                println("Set to $it")
                property.value = it
            }
        }
        assertEquals((0..9).toList(), emissions)
    }

    @Test fun baselineScope() {
        testContext {
            val a = Property(0)
            var received = -1
            reactiveSuspending {
                received = a()
            }
            assertEquals(a.value, received)
            a.value++
            assertEquals(a.value, received)
            a.value++
            assertEquals(a.value, received)
            a.value++
            assertEquals(a.value, received)
        }
    }

    @Test fun launchReadableAwait() {
        testContext {
            val a = LateInitProperty<Int>()
            var received = -1
            onRemove { println("Shutting down...") }
            load {
                println("Started...")
                received = a.await()
            }
            println("Setting...")
            a.value = 42
            assertEquals(a.value, received)
        }
    }

    @Test fun sharedShutdownTest() {
        var onRemoveCalled = 0
        var scopeCalled = 0
        val shared = sharedSuspending(Dispatchers.Unconfined) {
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

    @Test fun basicer() {
        val a = Property(1)
        val b = Property(2)

        testContext {
            reactiveSuspending {
                println("Got ${a.await() + b.await()}")
            }
        }
        println("Done.")
    }

    @Test fun basics() {
        val a = Property(1)
        val b = sharedSuspending(Dispatchers.Unconfined) { println("CALC a"); a.await() }
//        val c = sharedSuspending(Dispatchers.Unconfined) { println("CALC b"); b.await() }
        var hits = 0

        testContext {
            reactiveSuspending(action = {
                println("#1 Got ${b.await()}")
                hits++
            })
            reactiveSuspending(action = {
                println("#2 Got ${b.await()}")
                hits++
            })
            assertEquals(2, hits)
            a.value = 2
            assertEquals(4, hits)
        }
        println("Done.")
    }

    @Test fun lateinit() {
        val a = LateInitProperty<Int>()
        var hits = 0

        testContext {
            load {
                println("launch ${a.await()}")
                hits++
            }
            reactiveSuspending {
                println("scope ${a.await()}")
                hits++
            }

            assertEquals(0, hits)
            a.value = 1
            assertEquals(2, hits)
            a.value = 2
            assertEquals(3, hits)
        }
        println("Done.")
    }

    @Test fun sharedTest() {
        val a = Property(1)
        val b = Property(2)
        var cInvocations = 0
        val c = sharedSuspending(Dispatchers.Unconfined) { cInvocations++; println("cInvocations: $cInvocations"); a.await() + b.await() }
        println("$c: c")
        var dInvocations = 0
        val d = sharedSuspending(Dispatchers.Unconfined) { dInvocations++; println("dInvocations: $dInvocations"); c.await() + c.await() }
        println("$d: d")
        var eInvocations = 0
        val e = sharedSuspending(Dispatchers.Unconfined) { eInvocations++; println("eInvocations: $eInvocations"); d.await() / 2 }
        println("$e: e")

        testContext {
            reactiveSuspending {
                e.await()
            }
            assertEquals(1, cInvocations)
            assertEquals(1, dInvocations)
            assertEquals(1, eInvocations)
            println("a.value = 3")
            a.value = 3
            assertEquals(2, cInvocations)
            assertEquals(2, dInvocations)
            assertEquals(2, eInvocations)
            println("b.value = 4")
            b.value = 4
            assertEquals(3, cInvocations)
            assertEquals(3, dInvocations)
            assertEquals(3, eInvocations)
        }
        println("Done.")
    }

    @Test fun sharedTest2() {
        val a = Property(1)
        val b = Property(2)
        var cInvocations = 0
        val c = sharedSuspending(Dispatchers.Unconfined) { cInvocations++; println("cInvocations: $cInvocations"); a.await() + b.await() }
        println("$c: c")
        var dInvocations = 0
        val d = sharedSuspending(Dispatchers.Unconfined) { dInvocations++; println("dInvocations: $dInvocations"); c.await() + b.await() }
        println("$d: d")
        var eInvocations = 0
        val e = sharedSuspending(Dispatchers.Unconfined) { eInvocations++; println("eInvocations: $eInvocations"); d.await() / 2 }
        println("$e: e")

        testContext {
            reactiveSuspending {
                e.await()
            }
            assertEquals(1, cInvocations)
            assertEquals(1, dInvocations)
            assertEquals(1, eInvocations)
            println("a.value = 3")
            a.value = 3
            assertEquals(2, cInvocations)
            assertEquals(2, dInvocations)
            assertEquals(2, eInvocations)
            println("b.value = 4")
            b.value = 4
            assertEquals(3, cInvocations)
            assertTrue(4 >= dInvocations)
            assertTrue(4 >= eInvocations)
        }
    }

    @Test fun sharedTest3() {
        val a = VirtualDelay { 1 }
        val c = sharedSuspending(Dispatchers.Unconfined) { a.await() }
        val d = sharedSuspending(Dispatchers.Unconfined) { c.await() }
        testContext {
            load { println("launch got " + d.await()) }
            reactiveSuspending { println("reactiveScope got " + d.await()) }
            println("Ready... GO!")
            a.go()
        }
    }

    @Test fun sharedTest4() {
        val property = LateInitProperty<LateInitProperty<Int>>()
        val shared = sharedSuspending(Dispatchers.Unconfined) { property.await().await() }
        var completions = 0
        testContext {
            reactiveSuspending { println("reactiveScope got " + shared.await()); completions++ }
            load { println("launch got " + shared.await()); completions++ }
            println("Ready... GO!")
            val lp2 = LateInitProperty<Int>()
            property.value = lp2
            lp2.value = 1
        }
        assertEquals(completions, 2)
    }

    @Test fun sharedTest5() {
        val property = LateInitProperty<Int>()
        val shared = sharedSuspending(Dispatchers.Unconfined) { property.await() }
        var completions = 0
        testContext {
            load { println("launchA got " + shared.await()); completions++ }
            load { println("launchB got " + shared.await()); completions++ }
            println("Ready... GO!")
            property.value = 1
        }
        assertEquals(completions, 2)
    }

    @Test fun websocketLikeTest() {
        val source = LateInitProperty<LateInitProperty<String>>()
        val socket = sharedSuspending(Dispatchers.Unconfined) { source.await() }
        val sublistener = sharedSuspending(Dispatchers.Unconfined) { socket.await().await() }
        testContext {
            reactiveSuspending { println(sublistener.await()) }
            println("Ready")
            val s2 = LateInitProperty<String>()
            source.value = s2
            s2.value = "A"
            s2.value = "B"
            s2.value = "C"
        }
    }

    @Test fun scopeSkippedIfLoading() {
        val source = LateInitProperty<Int>()
        var starts = 0
        var hits = 0
        testContext {
            reactiveSuspending {
                starts++
                source()
                hits++
            }
            assertEquals(1, starts)
            assertEquals(0, hits)
            source.value = 1
            assertEquals(1, starts)
            assertEquals(1, hits)
            source.unset()
            assertEquals(1, starts)
            assertEquals(1, hits)
            source.value = 2
            assertEquals(2, starts)
            assertEquals(2, hits)
        }
    }

    @Test fun bindTest() {
        val master = LateInitProperty<Int>()
        val secondary = Property<Int>(0)
        testContext {
            reactiveSuspending { println("master: ${master()}") }
            reactiveSuspending { println("secondary: ${secondary()}") }
            secondary bind master
            secondary.value = 1
            master.value = 5

        }
    }

    @Test fun dumbtest() {
        val listItem = LateInitProperty<Int>()
        val selected = Property<Int>(0)
        testContext {
            reactiveSuspending { println(listItem() == selected()) }
            listItem.value = 1
        }
    }

    @Test fun exceptionReruns() {
        class PublicReadable: BaseReadable<Int>() {
            override var state: ReadableState<Int>
                get() = super.state
                public set(value) { super.state = value }

            override fun addListener(listener: () -> Unit): () -> Unit {
                val r = super.addListener(listener)
                return {
                    r()
                }
            }
        }
        val exceptional = PublicReadable()
        testContext {
            var starts = 0
            var completes = 0
            reactiveSuspending(action =  {
                starts++
                exceptional()
                completes++
            })

            assertEquals(1, starts)
            assertEquals(0, completes)
            exceptional.state = ReadableState.exception(Exception())
            assertIs<Exception>(expectException())
            assertEquals(1, starts)
            assertEquals(0, completes)
            exceptional.state = ReadableState(1)
            assertEquals(2, starts)
            assertEquals(1, completes)
        }
    }
}
