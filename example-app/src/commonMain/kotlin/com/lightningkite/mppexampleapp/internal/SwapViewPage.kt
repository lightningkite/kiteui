package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.col
import com.lightningkite.kiteui.views.direct.h1
import com.lightningkite.kiteui.views.direct.swapView
import com.lightningkite.kiteui.views.direct.swapping
import com.lightningkite.readable.sharedProcess
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Routable("swapview")
object SwapViewPage : Page {
    override fun ViewWriter.render() = col {
        val clock = sharedProcess {
            var tick = 0
            while (true) {
                emit(tick++)
                delay(500)
            }
        }
        val willDelete = swapView {
            swapping(
                current = { clock() },
                views = { tick ->
                    return@swapping when (tick % 3) {
                        0 -> h1("1")
                        1 -> h1("2")
                        2 -> h1("3")
                        else -> h1("-")
                    }
                }
            )
        }
        launch {
            delay(5000)
            removeChild(willDelete)
        }
    }
}