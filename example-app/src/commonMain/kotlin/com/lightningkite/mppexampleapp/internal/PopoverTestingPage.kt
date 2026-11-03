package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*

@Routable("popover-testing")
object PopoverTestingPage : Page {
    override fun ElementWriter.CanAddTheme.render(): Unit {
        scrolling.frame {
            fun ElementWriter.testGrouping() = col {
                for (horizontal in listOf(false, true)) {
                    for (after in listOf(false, true)) {
                        row {
                            for (align in Align.entries) {
                                card.menuButton {
                                    text(buildString {
                                        if (horizontal) append("H") else append("V")
                                        if (after) append(">") else append("<")
                                        append(align.name.first())
                                    })
                                    preferredDirection = PopoverPreferredDirection(
                                        horizontal = horizontal,
                                        after = after,
                                        align = align
                                    )
                                    requireClick = true
                                    opensMenu {
                                        sizeConstraints(width = 20.rem, height = 20.rem).frame {
                                            centered.col {
                                                text("Popover!")
                                                if (horizontal) text("Horizontal") else text("Vertical")
                                                if (after) text("After") else text("Before")
                                                text(align.name)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                card.menuButton {
                    text("dumb")
                    preferredDirection = PopoverPreferredDirection(
                        horizontal = true,
                        after = true,
                        align = Align.Start
                    )
                    requireClick = true
                    opensMenu {
                        sizeConstraints(width = 1000.rem, height = 1000.rem).frame {
                            centered.col {
                                text("Popover!")
                                text("I take WAY too much space")
                            }
                        }
                    }
                }

                row {
                    var anchorTarget: Element? = null
                    button {
                        anchorTarget = this
                        text("Anchor Target")
                    }
                    space()
                    button {
                        text("Open at Target")
                        onClick {
                            openPopover(PopoverPreferredDirection.belowCenter, anchorTarget) {
                                text("Anchored to Target!")
                            }
                        }
                    }
                }
            }
            testGrouping()
        }
    }
}