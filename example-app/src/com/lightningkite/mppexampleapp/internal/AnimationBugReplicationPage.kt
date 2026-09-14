package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.ListSemantic
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.beforeSetup
import com.lightningkite.kiteui.views.compact
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.direct.button
import com.lightningkite.kiteui.views.direct.col
import com.lightningkite.kiteui.views.direct.dynamicWeight
import com.lightningkite.kiteui.views.direct.onClick
import com.lightningkite.kiteui.views.direct.padded
import com.lightningkite.kiteui.views.direct.row
import com.lightningkite.kiteui.views.direct.scrolling
import com.lightningkite.kiteui.views.direct.shownWhen
import com.lightningkite.kiteui.views.direct.text
import com.lightningkite.kiteui.views.expanding
import com.lightningkite.reactive.context.invoke
import com.lightningkite.reactive.core.Signal

@Routable("animation-bug-replication")
class AnimationBugReplicationPage: Page {

    interface Subsection {
        fun ElementWriter.renderHeader()
        fun ElementWriter.renderBody()
    }

    class SampleSubsection: Subsection {
        override fun ElementWriter.renderHeader() {
            text("Sample Subsection")
        }
        override fun ElementWriter.renderBody() {
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

    override fun ElementWriter.CanAddTheme.render() {
        themed(ListSemantic).col {
            for((index, subpage) in subpages.withIndex()) {
                dynamicWeight { if(index == selectedSubpage()) 1f else 0f }.col {
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
                    expanding.shownWhen { index == selectedSubpage() }.beforeSetup { debugName = "Subpage $index" }.padded.scrolling.run {
                        subpage.run { renderBody() }
                    }
                }
            }
        }
    }
}