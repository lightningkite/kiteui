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

/** A chat transcript: messages, with a divider wherever the day changes. */
private sealed interface TranscriptEntry {
    val id: String

    data class DayDivider(val day: Int) : TranscriptEntry {
        override val id: String get() = "day-$day"
    }

    data class Message(val number: Int, val author: String, val body: String) : TranscriptEntry {
        override val id: String get() = "message-$number"
    }
}

private const val line = "A message can be any length, so no two cells need be the same height. "

@Routable("windowed-list/types")
object WindowedListTypesPage : Page {
    override fun ElementWriter.CanAddTheme.render(): Unit = run {
        val entries = Signal(buildList {
            for (number in 0 until 2000) {
                // A new day every 15 messages.
                if (number % 15 == 0) add(TranscriptEntry.DayDivider(day = number / 15 + 1))
                add(
                    TranscriptEntry.Message(
                        number = number,
                        author = if (number % 3 == 0) "Assistant" else "Customer ${number % 7}",
                        body = line.repeat(1 + (number * 7) % 12).trim(),
                    )
                )
            }
        })
        lateinit var list: WindowedList
        col {
            row {
                expanding.button {
                    subtext("Jump to the middle")
                    onClick { list.scrollToIndex(entries.value.size / 2, Align.Start, animate = false) }
                }
                expanding.button {
                    subtext("Scroll to end")
                    onClick { list.scrollToIndex(entries.value.lastIndex, Align.End) }
                }
            }
            expanding.windowedList {
                list = this
                // A divider and a message share nothing, so each gets its own renderer.  A cell built by one
                // is never reused for the other.  The first rule that matches an entry is the one used.
                childrenMultipleTypes(items = entries, id = { it.id }) {
                    elementsMatching { it is TranscriptEntry.DayDivider } renderedAs { entry ->
                        centered.subtext { ::content { "Day ${(entry() as TranscriptEntry.DayDivider).day}" } }
                    }
                    elementsMatching { it is TranscriptEntry.Message } renderedAs { entry ->
                        card.col {
                            subtext { ::content { (entry() as TranscriptEntry.Message).author } }
                            text { ::content { (entry() as TranscriptEntry.Message).body } }
                        }
                    }
                }
            }
        }
    }
}
