package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.core.Constant
import com.lightningkite.reactive.core.Reactive

@Routable("nested-popover-testing")
object NestedPopoverTestPage : Page {
    override val title: Reactive<String> = Constant("Nested Popover Testing")
    override fun ElementWriter.CanAddTheme.render() {
        scrolling.col {
            h1 { content = "Nested Popover Testing" }
            text("Hover or click a button to open a popover, then interact with the nested button inside. Moving outside the nested popover should close it but keep the parent open.")
            space()

            h2 { content = "Hover Mode (requireClick = false)" }
            row {
                card.menuButton {
                    text("Hover me")
                    requireClick = false
                    preferredDirection = PopoverPreferredDirection.belowCenter
                    opensMenu {
                        col {
                            text("Parent popover")
                            separator()
                            card.menuButton {
                                text("Hover for nested")
                                requireClick = false
                                preferredDirection = PopoverPreferredDirection.rightCenter
                                opensMenu {
                                    sizeConstraints(width = 15.rem).col {
                                        text("Nested popover")
                                        text("Moving away should close this but not the parent")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            space()
            h2 { content = "Click Mode (requireClick = true)" }
            row {
                card.menuButton {
                    text("Click me")
                    requireClick = true
                    preferredDirection = PopoverPreferredDirection.belowCenter
                    opensMenu {
                        col {
                            text("Parent popover")
                            separator()
                            card.menuButton {
                                text("Click for nested")
                                requireClick = true
                                preferredDirection = PopoverPreferredDirection.rightCenter
                                opensMenu {
                                    sizeConstraints(width = 15.rem).col {
                                        text("Nested popover")
                                        text("Clicking outside should close this but not the parent")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
