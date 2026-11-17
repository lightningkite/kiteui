@file:OptIn(ExperimentalKiteUi::class)

package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.ExperimentalKiteUi
import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.WindowedList
import com.lightningkite.kiteui.views.l2.windowedList
import com.lightningkite.reactive.context.invoke
import com.lightningkite.reactive.core.Signal

private data class Message(val id: Int, val author: String, val body: String)

private const val sentence = "A message can be any length, so no two cells need be the same height. "

@Routable("windowed-list")
object WindowedListPage : Page {
    override fun ElementWriter.CanAddTheme.render(): Unit = run {
        val messages = Signal(List(2000) { index ->
            Message(
                id = index,
                author = if (index % 3 == 0) "Assistant" else "Customer ${index % 7}",
                body = sentence.repeat(1 + (index * 7) % 12).trim(),
            )
        })
        lateinit var list: WindowedList
        col {
            row {
                for (align in Align.values()) {
                    expanding.button {
                        subtext("Jump to #1000, ${align.name}")
                        onClick { list.scrollToIndex(1000, align, animate = false) }
                    }
                }
            }
            row {
                expanding.button {
                    subtext("Scroll to end")
                    onClick { list.scrollToIndex(messages.value.lastIndex, Align.End) }
                }
                expanding.button {
                    // While the list rests at its end, it follows this.  Scroll up first and it stays put.
                    subtext("Add message")
                    onClick { messages.value += Message(messages.value.size, "Assistant", sentence.repeat(3).trim()) }
                }
                expanding.button {
                    // What a streamed reply does: the same message, longer each time.
                    subtext("Grow last message")
                    onClick {
                        val last = messages.value.last()
                        messages.value = messages.value.dropLast(1) + last.copy(body = last.body + " " + sentence.trim())
                    }
                }
            }
            expanding.windowedList {
                list = this
                children(items = messages, id = { it.id }) { message ->
                    card.col {
                        row {
                            expanding.subtext { ::content { message().author } }
                            subtext { ::content { "#${message().id}" } }
                        }
                        text { ::content { message().body } }
                    }
                }
            }
            // After the list, which it reads from.
            subtext { ::content { "Showing #${list.firstIndex()} to #${list.lastIndex()} of ${messages().size}" } }
        }
    }
}
