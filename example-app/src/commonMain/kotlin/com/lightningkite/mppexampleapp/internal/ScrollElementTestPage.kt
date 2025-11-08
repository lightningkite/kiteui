package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.models.ListSemantic
import com.lightningkite.kiteui.models.SelectedSemantic
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.Recycler2
import com.lightningkite.kiteui.views.l2.children
import com.lightningkite.kiteui.views.l2.icon
import com.lightningkite.mppexampleapp.Resources
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlin.coroutines.CoroutineContext
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDate

@Routable("scroll-element")
object ScrollElementTestPage : Page {
    override val title: Reactive<String>
        get() = super.title

    override fun ViewWriter.render(): Unit = run {
        scrolling.col {
            repeat(3) {
                card.col {
                    h3("Header")
                    repeat(50) {
                        text("OK")
                    }
                    sizeConstraints(height = 10.rem).row {
                        expanding.space()
                        expanding.card.col {
                            repeat(50) {
                                text("OK")
                            }
                        }
                    }
                }
            }
        }
    }
}
