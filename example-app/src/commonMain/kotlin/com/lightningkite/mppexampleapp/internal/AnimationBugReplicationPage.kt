package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.ListSemantic
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.compact
import com.lightningkite.kiteui.views.direct.button
import com.lightningkite.kiteui.views.direct.changingWeight
import com.lightningkite.kiteui.views.direct.col
import com.lightningkite.kiteui.views.direct.onClick
import com.lightningkite.kiteui.views.direct.padded
import com.lightningkite.kiteui.views.direct.row
import com.lightningkite.kiteui.views.direct.scrolling
import com.lightningkite.kiteui.views.direct.shownWhen
import com.lightningkite.kiteui.views.direct.text
import com.lightningkite.kiteui.views.expanding
import com.lightningkite.kiteui.views.l2.icon
import com.lightningkite.reactive.context.invoke
import com.lightningkite.reactive.core.Signal

@Routable("animation-bug-replication")
class AnimationBugReplicationPage: Page {

    interface Subsection {
        fun ViewWriter.renderHeader()
        fun ViewWriter.renderBody()
    }
    class SampleSubsection: Subsection {
        override fun ViewWriter.renderHeader() {
            text("Sample Subsection")
        }
        override fun ViewWriter.renderBody() {
            col {
                text("Some Content")
                text("Some Content")
                text("Some Content")
                text("Some Content")
                text("Some Content")
                text("Some Content")
                text("Some Content")
                text("Some Content")
                text("Some Content")
                text("Some Content")
                text("Some Content")
                text("Some Content")
                text("Some Content")
                text("Some Content")
                text("Some Content")
                text("Some Content")
                text("Some Content")
            }

        }
    }
    val subpages = listOf<Subsection>(
        SampleSubsection(),
        SampleSubsection(),
        SampleSubsection(),
        SampleSubsection(),
    )

    val selectedSubpage = Signal(0)

    override fun ViewWriter.render() {
        themed(ListSemantic).col {
            for((index, subpage) in subpages.withIndex()) {
                changingWeight { if(index == selectedSubpage()) 1f else 0f }.col {
                    gap = 0.px
                    with(subpage) {
                        compact.button {
                            row {
                                expanding.renderHeader()
                            }
                            onClick {
                                if(selectedSubpage() == index) selectedSubpage.value = -1
                                else selectedSubpage.value = index
                            }
                        }
                    }
                    with(expanding.shownWhen { index == selectedSubpage() }.beforeNextElementSetup { debugName = "Subpage $index" }.scrolling.padded) {
                        with(subpage) { renderBody() }
                    }
                }
            }
        }
    }
}