package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.delay
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.reactive.Property
import com.lightningkite.kiteui.reactive.await
import com.lightningkite.kiteui.reactive.invoke
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*

@Routable("popover-testing")
object PopoverTestingScreen : Screen {
    override fun ViewWriter.render() {
        scrolls - col {
            h1 { content = "Popover Test" }
            stack {
                atStart - card - menuButton {
                    text("left")
                    preferredDirection = PopoverPreferredDirection.leftTop
                    opensMenu {
                        sizeConstraints(30.rem, 30.rem) - card - stack {
                            text("Popover")
                        }
                    }
                }
                centered - card - menuButton {
                    text("center")
                    preferredDirection = PopoverPreferredDirection.aboveCenter
                    opensMenu {
                        sizeConstraints(30.rem, 30.rem) - card - stack {
                            text("Popover")
                        }
                    }
                }
                atEnd - card - menuButton {
                    text("right")
                    preferredDirection = PopoverPreferredDirection.rightTop
                    opensMenu {
                        sizeConstraints(30.rem, 30.rem) - card - stack {
                            text("Popover")
                        }
                    }
                }
            }
            stack {
                atStart - card - menuButton {
                    text("left")
                    preferredDirection = PopoverPreferredDirection.aboveLeft
                    opensMenu {
                        sizeConstraints(30.rem, 30.rem) - card - stack {
                            text("Popover")
                        }
                    }
                }
                centered - card - menuButton {
                    text("center")
                    preferredDirection = PopoverPreferredDirection.aboveCenter
                    opensMenu {
                        sizeConstraints(30.rem, 30.rem) - card - stack {
                            text("Popover")
                        }
                    }
                }
                atEnd - card - menuButton {
                    text("right")
                    preferredDirection = PopoverPreferredDirection.aboveRight
                    opensMenu {
                        sizeConstraints(30.rem, 30.rem) - card - stack {
                            text("Popover")
                        }
                    }
                }
            }
            sizeConstraints(minHeight = 10.rem) - card - stack { text("Filler content") }
            sizeConstraints(minHeight = 10.rem) - card - stack { text("Filler content") }
            sizeConstraints(minHeight = 10.rem) - card - stack { text("Filler content") }
            sizeConstraints(minHeight = 10.rem) - card - stack { text("Filler content") }
            sizeConstraints(minHeight = 10.rem) - card - stack { text("Filler content") }
            sizeConstraints(minHeight = 10.rem) - card - stack { text("Filler content") }
            sizeConstraints(minHeight = 10.rem) - card - stack { text("Filler content") }
            sizeConstraints(minHeight = 10.rem) - card - stack { text("Filler content") }
        }
    }
}