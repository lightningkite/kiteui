package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.direct.scrolling
import com.lightningkite.mppexampleapp.Resources
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*

@Routable("layout-examples")
object LayoutExamplesPage : Page {
    override fun ElementWriter.CanAddTheme.render(): Unit = run {
        scrolling.col {
            h1 { content = "Sampling" }

            card.col {
                h2 { content = "Stack Layout" }
                sizedBox(SizeConstraints(minHeight = 200.px)).frame {
                    val aligns = listOf(Align.Start, Align.Center, Align.End)
                    for (h in aligns) {
                        for (v in aligns) {
                            align(h, v).text { content = "$h $v" }
                        }
                    }
                }
            }

            val showIcons = Signal(false)
            row {
                expanding.text("Show icons")
                switch { checked bind showIcons }
            }

            card.col {
                h2("Sample from another project")
                card.row {
                    centered.expanding.rowCollapsingToColumn(30.rem) {
                        centered.sizeConstraints(width = 5.rem, height = 5.rem).image {
                            source = Resources.imagesSnowyBackground
                            this.description = ""
                            scaleType = ImageScaleType.Crop
                        }
                        compact.col {
                            h2 {
                                ::content { "This is a really long name and will probably overlap" }
                            }
                            row {
                                centered.icon(
                                    Icon.starFilled.copy(width = 1.rem, height = 1.rem),
                                    "star"
                                )
                                centered.text {
                                    ::content { "Test content" }
                                }
                            }
                        }
                    }
//                    gravity(Align.End, Align.Center) - row {
                    align(Align.End, Align.Center).row {
                        centered.shownWhen { showIcons() }.toggleButton {
                            icon {
                                source = Icon.starFilled
                            }
                        }

                        centered.link {
                            icon {
                                source = Icon.done
                                description = "Update"
                            }
                        }
                    }
                }
                bar.row {
                    button { icon { source = Icon.arrowBack } }
                    expanding.h2("Dashboard")
                    row {
                        button { icon { source = Icon.notification } }
                        centered.menuButton {
                            col {
                                gap = 0.25.rem
                                centered.sizeConstraints(width = 2.rem, height = 2.rem).image {
                                    description = ""
                                    source = Resources.imagesSnowyBackground
                                }
                                subtext {
                                    ::shown { false }
                                    ::content {
                                        "Test dealership"
                                    }
                                }
                            }
                        }
                        frame {}
                    }
                }
            }

            card.col {
                h2("Collapsing layout")
                rowCollapsingToColumn(80.rem) {
                    expanding.card.frame { centered.text("A") }
                    expanding.important.frame { centered.text("B") }
                    expanding.critical.frame { centered.text("C") }
                }
                rowCollapsingToColumn(30.rem, 40.rem, 50.rem) {
                    expanding.card.frame { centered.text("A") }
                    expanding.important.frame { centered.text("B") }
                    expanding.critical.frame { centered.text("C") }
                }
            }

            card.col {
                h2 { content = "Column Gravity" }
                col {
                    val aligns = listOf(Align.Start, Align.Center, Align.End)
                    for (h in aligns) {
                        align(h, Align.Stretch).text { content = "$h" }
                    }
                }
            }

            card.col {
                h2 { content = "Row Gravity" }
                sizedBox(SizeConstraints(minHeight = 200.px)).row {
                    val aligns = listOf(Align.Start, Align.Center, Align.End)
                    for (v in aligns) {
                        align(Align.Stretch, v).text { content = "$v" }
                    }
                    for (v in aligns) {
                        align(Align.Stretch, v).text { content = "$v" }
                    }
                }
            }

            card.col {
                h2 { content = "Row Gravity / Weight" }
                sizedBox(SizeConstraints(minHeight = 200.px)).row {
                    val aligns = listOf(Align.Start, Align.Center, Align.End)
                    for (v in aligns) {
                        align(Align.Stretch, v).text { content = "$v" }
                    }
                    expanding.card.frame {
                        centered.text { content = "Expanding" }
                    }
                    for (v in aligns) {
                        align(Align.Stretch, v).text { content = "$v" }
                    }
                }
            }

            card.col {
                h2 { content = "Dynamic List" }
                val countString = Signal("5")
                scrollsHorizontally.row {
                    forEachUpdating(
                        remember {
                            (1..(countString().toIntOrNull()
                                ?: 1).coerceAtMost(100)).map { "Item $it" }
                        }
                    ) {
                        text { ::content.invoke { it() } }
                    }
                }
                label {
                    content = "Element count:"
                    textField { content bind countString }
                }
            }

            card.col {
                h2 { content = "Max Size" }
                val text = Signal(true)
                important.button {
                    text("Toggle text size")
                    onClick {
                        text.value = !text.value
                    }
                }
                run {
                    val amount = 20
                    align(Align.Start, Align.Start).sizeConstraints(maxWidth = amount.rem).important.frame {
                        text { ::content { if (text()) "maxWidth = $amount.rem with a lot of additional content to demonstrate large sizes.  Try adjusting the screen width smaller." else "maxWidth = $amount.rem" } }
                    }
                    align(Align.Start, Align.Start).sizeConstraints(width = amount.rem).important.frame {
                        text { ::content { if (text()) "width = $amount.rem with a lot of additional content to demonstrate large sizes.  Try adjusting the screen width smaller." else "width = $amount.rem" } }
                    }
                    align(Align.Start, Align.Start).sizeConstraints(minWidth = amount.rem).important.frame {
                        text { ::content { if (text()) "minWidth = $amount.rem with a lot of additional content to demonstrate large sizes.  Try adjusting the screen width smaller." else "minWidth = $amount.rem" } }
                    }
                }
                run {
                    val amount = 40
                    align(Align.Start, Align.Start).sizeConstraints(maxWidth = amount.rem).important.frame {
                        text { ::content { if (text()) "maxWidth = $amount.rem with a lot of additional content to demonstrate large sizes.  Try adjusting the screen width smaller." else "maxWidth = $amount.rem" } }
                    }
                    align(Align.Start, Align.Start).sizeConstraints(width = amount.rem).important.frame {
                        text { ::content { if (text()) "width = $amount.rem with a lot of additional content to demonstrate large sizes.  Try adjusting the screen width smaller." else "width = $amount.rem" } }
                    }
                    align(Align.Start, Align.Start).sizeConstraints(minWidth = amount.rem).important.frame {
                        text { ::content { if (text()) "minWidth = $amount.rem with a lot of additional content to demonstrate large sizes.  Try adjusting the screen width smaller." else "minWidth = $amount.rem" } }
                    }
                }
            }

            card.col {
                h2("Scroll text")
                sizeConstraints(height = 10.rem).scrolling.col {
                    col {
                        sizeConstraints(height = 100.rem).text("This item is really tall!")
                    }
                }
            }

            card.col {
                h2("Compact test")
                card.compact.col {
                    text("This one is compact")
                    text("This one is compact")
                }
                card.col {
                    text("This one is NOT compact")
                    text("This one is NOT compact")
                }
            }

            card.col {
                h2("Custom gap test")
                val showExtra = Signal(false)
                row {
                    checkbox { checked bind showExtra }
                    text("Show extra view")
                }
                shownWhen { showExtra() }.text("Showing an extra view!")
                card.row {
                    gap = 0.rem
                    text("0.0")
                    important.text("X")
                    shownWhen { showExtra() }.frame { important.text("X") }
                    frame { important.text("X") }
                }
                card.row {
                    gap = 0.5.rem
                    text("0.5")
                    important.text("X")
                    shownWhen { showExtra() }.frame { important.text("X") }
                    frame { important.text("X") }
                }
                card.row {
                    gap = 1.rem
                    text("1.0")
                    important.text("X")
                    shownWhen { showExtra() }.frame { important.text("X") }
                    frame { important.text("X") }
                }
                card.row {
                    gap = 2.rem
                    text("2.0")
                    important.text("X")
                    shownWhen { showExtra() }.frame { important.text("X") }
                    frame { important.text("X") }
                }
                card.button {
                    text("gap = 0.rem")
                }
                card.button {
                    text("gap = 0.5.rem")
                }
                card.button {
                    text("gap = 1.rem")
                }
                card.button {
                    text("gap = 2.rem")
                }
            }

            card.card.col {
                h2 { content = "Max Size / Image Interaction" }
                sizedBox(
                    SizeConstraints(
                        maxHeight = 10.rem
                    )
                ).image {
                    source = ImageRemote("https://picsum.photos/seed/test/1920/1080")
                }
                sizedBox(
                    SizeConstraints(
                        maxHeight = 10.rem
                    )
                ).image {
                    source = ImageRemote("https://picsum.photos/seed/test/600/300")
                }
                sizedBox(
                    SizeConstraints(
                        height = 10.rem
                    )
                ).image {
                    source = ImageRemote("https://picsum.photos/seed/test/600/300")
                }
                centered.sizedBox(
                    SizeConstraints(
                        width = 10.rem,
                        height = 10.rem
                    )
                ).image {
                    source = ImageRemote("https://picsum.photos/seed/test/600/300")
                    scaleType = ImageScaleType.Crop
                }
                centered.sizedBox(
                    SizeConstraints(
                        width = 10.rem,
                        height = 10.rem
                    )
                ).image {
                    source = ImageRemote("https://picsum.photos/seed/test/600/300")
                    scaleType = ImageScaleType.Fit
                }
            }
        }
    }
}
