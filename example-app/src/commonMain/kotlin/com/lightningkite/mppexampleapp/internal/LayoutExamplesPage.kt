package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.readable.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.direct.scrolling
import com.lightningkite.kiteui.views.l2.icon
import com.lightningkite.mppexampleapp.Resources

@Routable("layout-examples")
object LayoutExamplesPage : Page {
    override fun ViewWriter.render(): ViewModifiable = run {
        scrolling - col {
            h1 { content = "Sampling" }

            card - col {
                h2 { content = "Stack Layout" }
                frame {
                    val aligns = listOf(Align.Start, Align.Center, Align.End)
                    for (h in aligns) {
                        for (v in aligns) {
                            text { content = "$h $v" } in align(h, v)
                        }
                    }
                } in sizedBox(SizeConstraints(minHeight = 200.px))
            }

            val showIcons = Property(false)
            row {
                expanding - text("Show icons")
                switch { checked bind showIcons }
            }

            card - col {
                h2("Sample from another project")
                card - row {
                    expanding - centered - rowCollapsingToColumn(30.rem) {
                        centered - sizeConstraints(width = 5.rem, height = 5.rem) - image {
                            source = Resources.imagesSolera
                            this.description = ""
                            scaleType = ImageScaleType.Crop
                        }
                        compact - col {
                            h2 {
                                ::content { "This is a really long name and will probably overlap" }
                            }
                            row {
                                centered - icon(
                                    Icon.starFilled.copy(width = 1.rem, height = 1.rem),
                                    "star"
                                )
                                centered - text {
                                    ::content { "Test content" }
                                }
                            }
                        }
                    }
//                    gravity(Align.End, Align.Center) - row {
                    align(Align.End, Align.Center) - row {
                        shownWhen { showIcons() } - centered - toggleButton {
                            icon {
                                source = Icon.starFilled
                            }
                        }

                        shownWhen { true } - centered - link {
                            icon {
                                source = Icon.done
                                description = "Update"
                            }
                        }
                    }
                }
                bar - row {
                    button { icon { source = Icon.arrowBack }}
                    expanding - h2("Dashboard")
                    row {
                        button { icon { source = Icon.notification }}
                        centered - menuButton {
                            col {
                                gap = 0.25.rem
                                centered - sizeConstraints(width = 2.rem, height = 2.rem) - image {
                                    description = ""
                                    source = Resources.imagesSolera
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

            card - col {
                h2("Collapsing layout")
                rowCollapsingToColumn(80.rem) {
                    expanding - card - frame { centered - text("A") }
                    expanding - important - frame { centered - text("B") }
                    expanding - critical - frame { centered - text("C") }
                }
                rowCollapsingToColumn(30.rem, 40.rem, 50.rem) {
                    expanding - card - frame { centered - text("A") }
                    expanding - important - frame { centered - text("B") }
                    expanding - critical - frame { centered - text("C") }
                }
            }

            card - col {
                h2 { content = "Column Gravity" }
                col {
                    val aligns = listOf(Align.Start, Align.Center, Align.End)
                    for (h in aligns) {
                        text { content = "$h" } in align(h, Align.Stretch)
                    }
                }
            }

            card - col {
                h2 { content = "Row Gravity" }
                row {
                    val aligns = listOf(Align.Start, Align.Center, Align.End)
                    for (v in aligns) {
                        align(Align.Stretch, v) - text { content = "$v" }
                    }
                    for (v in aligns) {
                        align(Align.Stretch, v) - text { content = "$v" }
                    }
                } in sizedBox(SizeConstraints(minHeight = 200.px))
            }

            card - col {
                h2 { content = "Row Gravity / Weight" }
                row {
                    val aligns = listOf(Align.Start, Align.Center, Align.End)
                    for (v in aligns) {
                        align(Align.Stretch, v) - text { content = "$v" }
                    }
                    expanding - card - frame {
                        centered - text { content = "Expanding" }
                    }
                    for (v in aligns) {
                        align(Align.Stretch, v) - text { content = "$v" }
                    }
                } in sizedBox(SizeConstraints(minHeight = 200.px))
            }

            card - col {
                h2 { content = "Dynamic List" }
                val countString = Property("5")
                scrollsHorizontally - row {
                    forEachUpdating(
                        shared {
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

            card - col {
                h2 { content = "Max Size" }
                val text = Property(true)
                important - button {
                    text("Toggle text size")
                    onClick {
                        text.value = !text.value
                    }
                }
                run {
                    val amount = 20
                    align(Align.Start, Align.Start) - sizeConstraints(maxWidth = amount.rem) - important - frame {
                        text { ::content { if (text()) "maxWidth = $amount.rem with a lot of additional content to demonstrate large sizes.  Try adjusting the screen width smaller." else "maxWidth = $amount.rem" }}
                    }
                    align(Align.Start, Align.Start) - sizeConstraints(width = amount.rem) - important - frame {
                        text { ::content { if (text()) "width = $amount.rem with a lot of additional content to demonstrate large sizes.  Try adjusting the screen width smaller." else "width = $amount.rem" }}
                    }
                    align(Align.Start, Align.Start) - sizeConstraints(minWidth = amount.rem) - important - frame {
                        text { ::content { if (text()) "minWidth = $amount.rem with a lot of additional content to demonstrate large sizes.  Try adjusting the screen width smaller." else "minWidth = $amount.rem" }}
                    }
                }
                run {
                    val amount = 40
                    align(Align.Start, Align.Start) - sizeConstraints(maxWidth = amount.rem) - important - frame {
                        text { ::content { if (text()) "maxWidth = $amount.rem with a lot of additional content to demonstrate large sizes.  Try adjusting the screen width smaller." else "maxWidth = $amount.rem" }}
                    }
                    align(Align.Start, Align.Start) - sizeConstraints(width = amount.rem) - important - frame {
                        text { ::content { if (text()) "width = $amount.rem with a lot of additional content to demonstrate large sizes.  Try adjusting the screen width smaller." else "width = $amount.rem" }}
                    }
                    align(Align.Start, Align.Start) - sizeConstraints(minWidth = amount.rem) - important - frame {
                        text { ::content { if (text()) "minWidth = $amount.rem with a lot of additional content to demonstrate large sizes.  Try adjusting the screen width smaller." else "minWidth = $amount.rem" }}
                    }
                }
            }

            card - col {
                h2("Scroll text")
                sizeConstraints(height = 10.rem) - scrolling - col {
                    col {
                        sizeConstraints(height = 100.rem) - text("This item is really tall!")
                    }
                }
            }

            card - col {
                h2("Compact test")
                card - compact - col {
                    text("This one is compact")
                    text("This one is compact")
                }
                card - col {
                    text("This one is NOT compact")
                    text("This one is NOT compact")
                }
            }

            card - col {
                h2("Custom spacing test")
                val showExtra = Property(false)
                row {
                    checkbox { checked bind showExtra }
                    text("Show extra view")
                }
                shownWhen { showExtra() } - text("Showing an extra view!")
                card - row {
                    gap = 0.rem
                    text("0.0")
                    important - text("X")
                    shownWhen { showExtra() } - frame { important - text("X") }
                    frame { important - text("X") }
                }
                card - row {
                    gap = 0.5.rem
                    text("0.5")
                    important - text("X")
                    shownWhen { showExtra() } - frame { important - text("X") }
                    frame { important - text("X") }
                }
                card - row {
                    gap = 1.rem
                    text("1.0")
                    important - text("X")
                    shownWhen { showExtra() } - frame { important - text("X") }
                    frame { important - text("X") }
                }
                card - row {
                    gap = 2.rem
                    text("2.0")
                    important - text("X")
                    shownWhen { showExtra() } - frame { important - text("X") }
                    frame { important - text("X") }
                }
                card - button {
                    gap = 0.rem
                    text("spacing = 0.rem")
                }
                card - button {
                    gap = 0.5.rem
                    text("spacing = 0.5.rem")
                }
                card - button {
                    gap = 1.rem
                    text("spacing = 1.rem")
                }
                card - button {
                    gap = 2.rem
                    text("spacing = 2.rem")
                }
            }

            card - col {
                h2 { content = "Max Size / Image Interaction" }
                image {
                    source = ImageRemote("https://picsum.photos/seed/test/1920/1080")
                } in sizedBox(
                    SizeConstraints(
                        maxHeight = 10.rem
                    )
                )
                image {
                    source = ImageRemote("https://picsum.photos/seed/test/600/300")
                } in sizedBox(
                    SizeConstraints(
                        maxHeight = 10.rem
                    )
                )
                image {
                    source = ImageRemote("https://picsum.photos/seed/test/600/300")
                } in sizedBox(
                    SizeConstraints(
                        height = 10.rem
                    )
                )
                image {
                    source = ImageRemote("https://picsum.photos/seed/test/600/300")
                    scaleType = ImageScaleType.Crop
                } in sizedBox(
                    SizeConstraints(
                        width = 10.rem,
                        height = 10.rem
                    )
                )  in centered
                image {
                    source = ImageRemote("https://picsum.photos/seed/test/600/300")
                    scaleType = ImageScaleType.Fit
                } in sizedBox(
                    SizeConstraints(
                        width = 10.rem,
                        height = 10.rem
                    )
                )  in centered
            } in card
        }
    }
}
