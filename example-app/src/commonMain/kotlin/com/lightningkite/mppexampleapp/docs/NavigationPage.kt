package com.lightningkite.mppexampleapp.docs

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.dialogPageNavigator
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.icon
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*

@Routable("docs/navigation")
object NavigationPage: DocPage {

    override val title: Reactive<String>
        get() = Constant("Navigation: how to manage screens")

    override val covers: List<String> = listOf(
        "Screen",
        "KiteUiScreen",
        "Navigator",
        "PlatformNavigator",
        "navigate",
        "pop",
        "dismiss",
        "replace",
    )

    override fun ElementWriter.CanAddTheme.render(): Unit = run {
        article {
            h1("Navigation")

            space()
            h2("Navigate with Links")
            text("You can navigate between screens using links.  These are preferred as they render as proper links in web.")
            text("The destinations of links are instances of KiteUiScreen, which ensures at the compilation level that you can't produce invalid links.")
            example(
                """
            link {
                to = DocSearchScreen
                centered.row {
                    centered.icon(Icon.arrowBack, "Back")
                    centered.expanding.text("Back to the search screen")
                }
            }
            """.trimIndent()
            ) {
                link {
                    to = { DocSearchPage }
                    centered.row {
                        centered.icon(Icon.arrowBack, "Back")
                        centered.expanding.text("Back to the search screen")
                    }
                }
            }

            space()
            h2("Navigate Programmatically")
            text("You can navigate programmatically as well, which is useful for circumstances such as navigating to a different screen after uploading data is complete.")
            example(
                """
                button {
                    onClick { 
                        navigator.navigate(DocSearchPage)
                    }
                    centered.row {
                        centered.icon(Icon.arrowBack, "Back")
                        centered.expanding.text("Back to the search screen")
                    }
                }
            """.trimIndent()
            ) {
                button {
                    onClick {
                        pageNavigator.navigate(DocSearchPage)
                    }
                    centered.row {
                        centered.icon(Icon.arrowBack, "Back")
                        centered.expanding.text("Back to the search screen")
                    }
                }
            }
            text("When navigating programmatically, you can also pop, reset, or replace.")
            example(
                """
                col {
                    button {
                        onClick {
                            navigator.goBack()
                        }
                        row {
                            centered.icon(Icon.arrowBack, "Back")
                            centered.expanding.text("Back to previous screen")
                        }
                    }
                    button {
                        onClick {
                            navigator.replace(DocSearchPage)
                        }
                        row {
                            centered.icon(Icon.arrowBack, "Back")
                            centered.expanding.text("Replace this view in history with the search screen")
                        }
                    }
                    button {
                        onClick {
                            navigator.reset(DocSearchPage)
                        }
                        row {
                            centered.icon(Icon.arrowBack, "Back")
                            centered.expanding.text("Clear the stack and go to the search screen")
                        }
                    }
                }
            """.trimIndent()
            ) {
                col {
                    button {
                        onClick {
                            pageNavigator.goBack()
                        }
                        row {
                            centered.icon(Icon.arrowBack, "Back")
                            centered.expanding.text("Back to previous screen")
                        }
                    }
                    button {
                        onClick {
                            pageNavigator.replace(DocSearchPage)
                        }
                        row {
                            centered.icon(Icon.arrowBack, "Back")
                            centered.expanding.text("Replace this view in history with the search screen")
                        }
                    }
                    button {
                        onClick {
                            pageNavigator.reset(DocSearchPage)
                        }
                        row {
                            centered.icon(Icon.arrowBack, "Back")
                            centered.expanding.text("Clear the stack and go to the search screen")
                        }
                    }
                }
            }

            space()
            h2("Dialogs")
            example(
                """
                button {
                    text("Open a dialog")
                    onClick {
                        navigator.dialog.navigate(object: KiteUiPage {
                            override fun ElementWriter.CanAddTheme.render(): Unit = run {
                                frame {
                                    centered.card.col {
                                        h1("Hello!")
                                        text("This is a dialog.")
                                        button {
                                            centered.text("Dismiss")
                                            onClick {
                                                navigator.dismiss()
                                            }
                                        }
                                    }
                                }
                            }
                        })
                    }
                }
            """.trimIndent()
            ) {
                button {
                    text("Open a dialog")
                    onClick {
                        dialogPageNavigator.navigate(object: Page {
                            override fun ElementWriter.CanAddTheme.render(): Unit = run {
                                frame {
                                    centered.card.col {
                                        h1("Hello!")
                                        text("This is a dialog.")
                                        button {
                                            centered.text("Dismiss")
                                            onClick {
                                                pageNavigator.dismiss()
                                            }
                                        }
                                    }
                                }
                            }
                        })
                    }
                }
            }
        }
    }

}