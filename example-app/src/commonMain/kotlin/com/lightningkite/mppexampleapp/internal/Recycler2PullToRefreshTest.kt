package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.locale.RenderSize
import com.lightningkite.kiteui.locale.renderTimeToString
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import kotlinx.coroutines.delay
import kotlin.time.Clock.System.now
import kotlin.time.Duration.Companion.seconds

@Routable("recycler2-test")
object Recycler2PullToRefreshTest : Page {

    @QueryParameter
    val elementCount = Signal(10_000)

    val refresh = BasicListenable()

    val loadedData = rememberSuspending {
        rerunOn(refresh)
        delay(1.seconds)
        List(elementCount()) { "${it}-${now().renderTimeToString(RenderSize.Full)}" }
    }


    override fun ViewWriter.render(): Unit = run {
        col {
            expanding.recyclerView(Action("refresh") { refresh.invokeAll() }) {
                log = LogRoot.tag("R2")
                placer = RecyclerViewPlacerVerticalGrid(1).also { it.log = LogRoot.tag("Placer") }

                rendererSet = object : RecyclerViewRendererSet<String, Int> {
                    override fun id(item: String): Int = item.substringBefore('-').toInt()
                    override fun renderer(item: String): RecyclerViewRenderer<String> =
                        object : RecyclerViewRenderer<String> {
                            override fun render(
                                viewWriter: ViewWriter,
                                data: Reactive<String>,
                                index: Reactive<Int>
                            ) {
                                with(viewWriter) {
                                    card.frame {
                                        centered.text { ::content { "Item: ${data()}" } }
                                    }
                                }
                            }
                        }
                }
                reactive {
                    val d = loadedData()
                    data = object : RecyclerViewData<String, Int> {
                        override val range: IntRange = d.indices
                        override fun get(index: Int): String = d[index]
                    }
                }
            }
        }
    }
}