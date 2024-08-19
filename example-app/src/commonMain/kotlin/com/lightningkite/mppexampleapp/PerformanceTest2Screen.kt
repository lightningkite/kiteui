package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.delay
import com.lightningkite.kiteui.launch
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

@Routable("performance2")
object PerformanceTest2Screen : Screen {
    override fun ViewWriter.render() {
        scrolls - col {
            h1 { content = "Performance Test 2" }
            text("This screen is hammering UI updates, but mostly reactivity.")
            val slow = Property(true)
            val suspendless = Property(true)

            toggleButton {
                text("suspendless")
                checked bind suspendless
            }
            toggleButton {
                text("Slow")
                checked bind slow
            }

            row {
//                expanding - col {
//                    text("Suspendless")
//                    val tierOne = (0..1000).map { Property(it) }
//                    val tierTwo = tierOne.chunked(5).map { shared2 { it.sumOf { it() } } }
//                    val tierThree = tierTwo.chunked(5).map { shared2 { it.sumOf { it() } } }
//                    val tierFour = tierThree.chunked(5).map { shared2 { it.sumOf { it() } } }
//
//                    val taken = Property<Duration>(0.seconds)
//                    val iterations = Property<Int>(0)
//
//                    launch {
//                        while(true) {
//                            if(slow.value || !suspendless.value) delay(2000) else delay(10)
//                            iterations.value++
//                            taken.value += measureTime {
//                                tierOne.forEach { it.value = Random.nextInt(0, 20) }
//                            }
//                        }
//                    }
//                    text { reactive { content = "Average ${taken() / iterations().coerceAtLeast(1)}" } }
//
//                    for(item in tierFour) {
//                        reactive { item() }
//                    }
//                }
//                expanding - col {
//                    text("Suspending")
//                    val tierOne = (0..1000).map { Property(it) }
//                    val tierTwo = tierOne.chunked(5).map { shared { it.sumOf { it() } } }
//                    val tierThree = tierTwo.chunked(5).map { shared { it.sumOf { it() } } }
//                    val tierFour = tierThree.chunked(5).map { shared { it.sumOf { it() } } }
//
//                    val taken = Property<Duration>(0.seconds)
//                    val iterations = Property<Int>(0)
//                    val slow = Property(false)
//
//                    launch {
//                        while(true) {
//                            if(slow.value || suspendless.value) delay(2000) else delay(10)
//                            iterations.value++
//                            taken.value += measureTime {
//                                tierOne.forEach { it.value = Random.nextInt(0, 20) }
//                            }
//                        }
//                    }
//                    text { reactiveScope { content = "Average ${taken() / iterations().coerceAtLeast(1)}" } }
//
//                    for(item in tierFour) {
//                         reactiveScope { item() }
//                    }
//                }
            }
        }
    }
}