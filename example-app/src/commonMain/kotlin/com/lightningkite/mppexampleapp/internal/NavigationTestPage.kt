package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.navigation.CanBlockBack
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.card
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.*
import com.lightningkite.reactive.context.invoke
import com.lightningkite.reactive.core.Signal


@Routable("navigation")
object NavigationTestPage : Page, CanBlockBack {
    override fun onNavigateAwayAttempt(): Boolean {
        return !blockNavigateAway.value
    }
    val blockNavigateAway = Signal(false)

    override fun ElementWriter.CanAddTheme.render(): Unit = run {
        col {
            h1 { content = "Navigation" }
            fun navSelector(label: String, value: ViewWriter.(AppNav.() -> Unit) -> Unit) {
                card.button {
                    text { content = label }
                    onClick {
                        appNavFactory set value
                    }
                }
            }
            h2 { content = "Layouts" }
            row {
                weight(1f).col {
                    navSelector("Hamburger Menu", ViewWriter::appNavHamburger)
                }
                weight(1f).col {
                    navSelector("Top Navigation", ViewWriter::appNavTop)
                }
            }
            row {
                weight(1f).col {
                    navSelector("Bottom Tab Navigation", ViewWriter::appNavBottomTabs)
                }
                weight(1f).col {
                    navSelector("Top and Left Navigation", ViewWriter::appNavTopAndLeft)
                }
            }


            row {
                checkbox {
                    checked bind blockNavigateAway
                }
                text("Require confirmation before leaving page.")
            }

            h2 { content = "Table of Contents" }

            card.row {
                col {
                    h3 { content = "Documentation" }
                    text {
                        content =
                            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Ut mollis felis ut mi aliquet, scelerisque laoreet tortor porttitor. Aliquam erat volutpat. Etiam a mauris eu tellus hendrerit mattis. Vivamus est nibh, feugiat a orci eu, facilisis vestibulum massa. Nam tempus enim in ipsum hendrerit, tincidunt dictum tellus lacinia. Sed sit amet dui consectetur, vulputate eros vel, consectetur urna. Morbi faucibus, odio sed tristique fringilla, risus tellus fringilla sapien, sed tincidunt velit nisi eget urna. Proin ante sem, lobortis vehicula nunc vitae, pulvinar aliquam nisi. Praesent placerat finibus felis, non pulvinar augue ullamcorper sed. Praesent ornare neque augue. Fusce elementum sem cursus, ullamcorper tellus quis, faucibus nisl. Integer tincidunt dapibus ultrices. Vivamus id volutpat orci, eget ultricies orci."
                    }
                }
            }
        }
    }
}
