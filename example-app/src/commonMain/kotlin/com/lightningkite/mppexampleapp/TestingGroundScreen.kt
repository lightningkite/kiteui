package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.delay
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*

@Routable("testing")
object TestingGroundScreen: Screen {
    override fun ViewWriter.render() {
        col {
            h1("Experiments tests")
            val items = readable<List<Int>> {
                delay(1000L)
                emit(listOf(1, 2, 3, 4, 5, 6, 7, 8, 9))
            }
            col {
                forEach(items) {
                    card - text { ::content { it.toString() }}
                }
            }
        }
    }
}