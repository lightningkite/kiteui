package com.lightningkite.kiteui.reactive

import com.lightningkite.kiteui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.yield
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReactivityTests {
    @Test fun testAsync() {

        var cont: Continuation<String>? = null
        val item = asyncGlobal<String> {
            println("Calculating...")
            suspendCoroutineCancellable {
                cont = it
                return@suspendCoroutineCancellable {}
            }
        }
        launchGlobal {
            println("A: ${item.await()}")
        }
        launchGlobal {
            println("B: ${item.await()}")
        }
        cont?.resume("Success")

    }
    @Test
    fun waitingTest() {
        val property = Property<Int?>(null)
        val emissions = ArrayList<Int>()
        testContext {
            reactiveScope {
                emissions.add(property.waitForNotNull.await())
            }
            repeat(10) {
                property.value = null
                property.value = it
            }
        }
        assertEquals((0..9).toList(), emissions)
    }

    @Test fun baselineScope() {
        testContext {
            val a = Property(0)
            var received = -1
            ReactiveScopeData(this, action = {
                received = a()
            })
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
            launch {
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

    @Test fun basicer() {
        val a = Property(1)
        val b = Property(2)

        testContext {
            reactiveScope {
                println("Got ${a.await() + b.await()}")
            }
        }
        println("Done.")
    }

    @Test fun basics() {
        val a = Property(1)
        val b = shared { println("CALC a"); a.await() }
        val c = shared { println("CALC b"); b.await() }
        var hits = 0

        testContext {
            reactiveScope {
                println("#1 Got ${c.await()}")
                hits++
            }
            reactiveScope {
                println("#2 Got ${c.await()}")
                hits++
            }
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
            launch {
                println("launch ${a.await()}")
                hits++
            }
            reactiveScope {
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
        val c = shared { cInvocations++; println("cInvocations: $cInvocations"); a.await() + b.await() }
        println("$c: c")
        var dInvocations = 0
        val d = shared { dInvocations++; println("dInvocations: $dInvocations"); c.await() + c.await() }
        println("$d: d")
        var eInvocations = 0
        val e = shared { eInvocations++; println("eInvocations: $eInvocations"); d.await() / 2 }
        println("$e: e")

        testContext {
            reactiveScope {
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
        val c = shared { cInvocations++; println("cInvocations: $cInvocations"); a.await() + b.await() }
        println("$c: c")
        var dInvocations = 0
        val d = shared { dInvocations++; println("dInvocations: $dInvocations"); c.await() + b.await() }
        println("$d: d")
        var eInvocations = 0
        val e = shared { eInvocations++; println("eInvocations: $eInvocations"); d.await() / 2 }
        println("$e: e")

        testContext {
            reactiveScope {
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
        val c = shared { async { a.await() } }
        val d = shared { c.await() }
        testContext {
            launch { println("launch got " + d.await()) }
            reactiveScope { println("reactiveScope got " + d.await()) }
            println("Ready... GO!")
            a.go()
        }
    }

    @Test fun sharedTest4() {
        val property = LateInitProperty<LateInitProperty<Int>>()
        val shared = shared { property.await().await() }
        var completions = 0
        testContext {
            reactiveScope { println("reactiveScope got " + shared.await()); completions++ }
            launch { println("launch got " + shared.await()); completions++ }
            println("Ready... GO!")
            val lp2 = LateInitProperty<Int>()
            property.value = lp2
            lp2.value = 1
        }
        assertEquals(completions, 2)
    }

    @Test fun sharedTest5() {
        val property = LateInitProperty<Int>()
        val shared = shared { property.await() }
        var completions = 0
        testContext {
            launch { println("launchA got " + shared.await()); completions++ }
            launch { println("launchB got " + shared.await()); completions++ }
            println("Ready... GO!")
            property.value = 1
        }
        assertEquals(completions, 2)
    }

    @Test fun websocketLikeTest() {
        val source = LateInitProperty<LateInitProperty<String>>()
        val socket = shared { source.await() }
        val sublistener = shared { socket.await().await() }
        testContext {
            reactiveScope { println(sublistener.await()) }
            println("Ready")
            val s2 = LateInitProperty<String>()
            source.value = s2
            s2.value = "A"
            s2.value = "B"
            s2.value = "C"
        }
    }

    @Test fun bindTest() {
        val master = LateInitProperty<Int>()
        val secondary = Property<Int>(0)
        testContext {
            reactiveScope { println("master: ${master()}") }
            reactiveScope { println("secondary: ${secondary()}") }
            secondary bind master
            secondary.value = 1
            master.value = 5

        }
    }

    @Test fun dumbtest() {
        val listItem = LateInitProperty<Int>()
        val selected = Property<Int>(0)
        testContext {
            reactiveScope { println(listItem() == selected()) }
            listItem.value = 1
        }
    }
}

class VirtualDelay<T>(val action: () -> T) {
    val continuations = ArrayList<Continuation<T>>()
    var value: T? = null
    var ready: Boolean = false
    suspend fun await(): T {
        if(ready) return value as T
        return suspendCoroutineCancellable {
            continuations.add(it)
            return@suspendCoroutineCancellable {}
        }
    }
    fun clear() {
        ready = false
    }
    fun go() {
        val value = action()
        this.value = value
        ready = true
        for(continuation in continuations) {
            continuation.resume(value)
        }
        continuations.clear()
    }
}

class VirtualDelayer() {
    val continuations = ArrayList<Continuation<Unit>>()
    suspend fun await(): Unit {
        return suspendCoroutineCancellable {
            continuations.add(it)
            return@suspendCoroutineCancellable {}
        }
    }
    fun go() {
        for(continuation in continuations) {
            continuation.resume(Unit)
        }
        continuations.clear()
    }
}

fun testContext(action: CalculationContext.()->Unit): Job {
    var error: Throwable? = null
    val job = Job()
    var numOutstandingContracts = 0
    with(object: CalculationContext {
        override val coroutineContext: CoroutineContext = job + Dispatchers.Unconfined

        override fun notifyLongComplete(result: Result<Unit>) {
            numOutstandingContracts--
        }

        override fun notifyStart() {
            numOutstandingContracts++
        }

        override fun notifyComplete(result: Result<Unit>) {
            result.onFailure { t ->
                t.printStackTrace()
                error = t
            }
        }
    }) {
        CalculationContextStack.useIn(this) {
            action()
        }
        job.cancel()
        if(error != null) throw error!!
        assertEquals(0, numOutstandingContracts, "Some work was not completed.")
    }
    return job
}