package com.lightningkite.mppexampleapp.docs

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*

@Routable("docs/theme-tester")
object ThemeTesterPage : DocPage {
    override val covers: List<String> = listOf("theme tester", "semantic tester", "debug theme")

    data class SemanticOption(
        val name: String,
        val semantic: Semantic,
        val description: String
    )

    val availableSemantics = listOf(
        SemanticOption("card", CardSemantic, "Light separation in UI groups"),
        SemanticOption("group", GroupSemantic, "Grouping related elements"),
        SemanticOption("fieldTheme", FieldSemantic, "Field/input styling"),
        SemanticOption("buttonTheme", ButtonSemantic, "Button styling"),
        SemanticOption("bar", BarSemantic, "Top bar styling"),
        SemanticOption("nav", NavSemantic, "Navigation elements"),
        SemanticOption("important", ImportantSemantic, "Draw attention"),
        SemanticOption("critical", CriticalSemantic, "Maximum attention"),
        SemanticOption("warning", WarningSemantic, "Warning indication"),
        SemanticOption("danger", DangerSemantic, "Dangerous action"),
        SemanticOption("affirmative", AffirmativeSemantic, "Success/positive action"),
        SemanticOption("emphasized", EmphasizedSemantic, "Text emphasis"),
        SemanticOption("compact", CompactSemantic, "Reduced spacing"),
        SemanticOption("down", DownSemantic, "Pressed state"),
        SemanticOption("hover", HoverSemantic, "Hover state"),
        SemanticOption("selected", SelectedSemantic, "Selected state"),
        SemanticOption("disabled", DisabledSemantic, "Disabled state"),
        SemanticOption("embedded", EmbeddedSemantic, "Embedded content"),
    )

    override fun ViewWriter.render(): Unit = run {
        article {
            titledSection("Theme Tester") {
                text("This interactive tool helps you test and debug semantic theme combinations.")
                text("Select one or more semantics below to see how they affect the sample UI block.")

                val selectedSemantics = Signal<Set<String>>(setOf())
                val nestedTest = Signal(false)

                space()

                titledSection("Select Semantics to Test") {
                    card.col {
                        text("Primary semantics (creating backgrounds/cards):")
                        row {
                            for (option in availableSemantics.filter {
                                it.name in setOf("card", "group", "fieldTheme", "buttonTheme", "bar", "nav")
                            }) {
                                row {
                                    centered.checkbox {
                                        checked bind selectedSemantics.contains(option.name)
                                    }
                                    centered.text(option.name)
                                    textPopover(option.description).icon(Icon.help, "")
                                }
                            }
                        }

                        space()
                        text("Emphasis semantics:")
                        row {
                            for (option in availableSemantics.filter {
                                it.name in setOf("important", "critical", "warning", "danger", "affirmative", "emphasized")
                            }) {
                                row {
                                    centered.checkbox {
                                        checked bind selectedSemantics.contains(option.name)
                                    }
                                    centered.text(option.name)
                                    textPopover(option.description).icon(Icon.help, "")
                                }
                            }
                        }

                        space()
                        text("State semantics:")
                        row {
                            for (option in availableSemantics.filter {
                                it.name in setOf("compact", "down", "hover", "selected", "disabled", "embedded")
                            }) {
                                row {
                                    centered.checkbox {
                                        checked bind selectedSemantics.contains(option.name)
                                    }
                                    centered.text(option.name)
                                    textPopover(option.description).icon(Icon.help, "")
                                }
                            }
                        }

                        space()
                        row {
                            centered.checkbox {
                                checked bind nestedTest
                            }
                            centered.expanding.text("Show nested semantic test")
                        }
                    }
                }

                space()

                titledSection("Preview") {
                    card.col {
                        text {
                            ::content {
                                "Applied semantics: " + (selectedSemantics().takeIf { it.isNotEmpty() }
                                    ?.joinToString(" - ") ?: "none")
                            }
                        }

                        separator()

                        // Build the theme derivation based on selected semantics
                        dynamicTheme {
                            var derivation: ThemeDerivation = None
                            for (semanticName in selectedSemantics()) {
                                val semantic = availableSemantics.find { it.name == semanticName }?.semantic
                                if (semantic != null) {
                                    derivation += semantic
                                }
                            }
                            derivation
                        }

                        // Sample UI block
                        col {
                            h3("Sample UI Block")
                            text("This is regular text content")
                            subtext("This is subtext")

                            space()

                            row {
                                button { text("Button") }
                                button { text("Another Button") }
                            }

                            space()

                            field("Text Input") {
                                textInput { hint = "Enter something..." }
                            }

                            space()

                            row {
                                centered.checkbox { checked.value = false }
                                centered.text("Checkbox option")
                            }

                            space()

                            icon(Icon.star, "Star icon")

                            // Nested test if enabled
                            shownWhen { nestedTest() }.col {
                                space()
                                separator()
                                h4("Nested Semantic Test")
                                text("Below shows the same semantics nested inside themselves:")

                                dynamicTheme {
                                    var derivation: ThemeDerivation = None
                                    for (semanticName in selectedSemantics()) {
                                        val semantic = availableSemantics.find { it.name == semanticName }?.semantic
                                        if (semantic != null) {
                                            derivation += semantic
                                        }
                                    }
                                    derivation
                                }

                                col {
                                    text("Nested Level 1")

                                    dynamicTheme {
                                        var derivation: ThemeDerivation = None
                                        for (semanticName in selectedSemantics()) {
                                            val semantic = availableSemantics.find { it.name == semanticName }?.semantic
                                            if (semantic != null) {
                                                derivation += semantic
                                            }
                                        }
                                        derivation
                                    }

                                    col {
                                        text("Nested Level 2")

                                        dynamicTheme {
                                            var derivation: ThemeDerivation = None
                                            for (semanticName in selectedSemantics()) {
                                                val semantic = availableSemantics.find { it.name == semanticName }?.semantic
                                                if (semantic != null) {
                                                    derivation += semantic
                                                }
                                            }
                                            derivation
                                        }

                                        col {
                                            text("Nested Level 3")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                space()

                titledSection("Tips") {
                    text("• Try combining different semantics to see how they interact")
                    text("• Enable 'Show nested semantic test' to see how semantics behave when nested")
                    text("• Some semantics create backgrounds (card, important, etc.) while others modify appearance (emphasized, compact)")
                    text("• Inspect elements using browser dev tools to see the applied theme classes")
                }
            }
        }
    }
}
