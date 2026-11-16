package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.field
import com.lightningkite.reactive.core.*

/**
 * Manual verification for the iOS fix where the keyboard stayed open after the element holding the
 * focused input disappeared (form hidden behind a success banner, `shownWhen` block collapsing, a
 * filtered-out list row, ...). UIKit keeps first responder on a merely-hidden view, so the fix
 * resigns focus at the moment of hiding - this page exercises every hide path.
 *
 * Sections 2-5 are the rigorous checks: the element hides *while you type*, with no tap in
 * between, so the root view's tap-to-dismiss gesture can't mask a regression. Section 1 is the
 * real-world button flow; section 6 covers the tap fallback and guards the tap-on-field behavior.
 */
@Routable("keyboard-dismiss-test")
object KeyboardDismissTestPage : Page {
    override val title: Reactive<String> = Constant("Keyboard Dismiss on Hide")

    override fun ElementWriter.CanAddTheme.render(): Unit = run {
        scrolling.col {
            h1("Keyboard Dismiss on Hide")
            text(
                "Checks that the keyboard slides down when the element holding the focused input " +
                        "disappears. This is the iOS-specific bug: UIKit keeps first responder on a " +
                        "hidden view, so before the fix the keyboard stayed open over the success " +
                        "banner. Do each section in order. If any section ever leaves the keyboard " +
                        "open, tap empty space to clear it - that tap-to-dismiss fallback is part " +
                        "of the fix too."
            )

            shownSubmitSection()
            separator()
            shownNoTapSection()
            separator()
            shownWhenColSection()
            separator()
            shownWhenTextAreaSection()
            separator()
            rowFilterSection()
            separator()
            tapFallbackSection()
        }
    }

    /** The reported flow: submit hides the form via `::shown`, success banner takes its place. */
    private fun ElementWriter.CanAddTheme.shownSubmitSection() {
        val submitted = Signal(false)
        card.col {
            h2("1. ::shown - submit hides the form (button)")
            text(
                "Do: tap the field so the keyboard opens, then tap Submit. Expect: the form is " +
                        "replaced by the success banner AND the keyboard slides down. Failure: the " +
                        "banner appears but the keyboard stays open."
            )
            col {
                ::shown { !submitted() }
                field("Name") {
                    textInput { hint = "Your name" }
                }
                important.button {
                    text("Submit")
                    onClick { submitted.value = true }
                }
            }
            col {
                ::shown { submitted() }
                text("Form submitted successfully!")
                button {
                    text("Reset")
                    onClick { submitted.value = false }
                }
            }
        }
    }

    /**
     * The rigorous variant of section 1: the form hides reactively mid-typing, so no tap is
     * involved and the tap-to-dismiss gesture cannot hide a regression.
     */
    private fun ElementWriter.CanAddTheme.shownNoTapSection() {
        val name = Signal("")
        card.col {
            h2("2. ::shown - form hides while you type (no tap)")
            text(
                "Do: tap the field and type 3 or more characters. Do NOT tap anything else. " +
                        "Expect: the moment the third character lands, the form disappears and " +
                        "the keyboard slides down. Failure: the keyboard stays open over the " +
                        "message below."
            )
            col {
                ::shown { name().length < 3 }
                field("Name") {
                    textInput {
                        hint = "Type abc"
                        content bind name
                    }
                }
            }
            col {
                ::shown { name().length >= 3 }
                text("The form hid itself - the keyboard should be closed now.")
                button {
                    text("Reset")
                    onClick { name.value = "" }
                }
            }
        }
    }

    /** Same hide-while-typing check, but through the `shownWhen` modifier wrapping a col. */
    private fun ElementWriter.CanAddTheme.shownWhenColSection() {
        val notes = Signal("")
        card.col {
            h2("3. shownWhen - wrapping a col")
            text(
                "Do: tap the field, type 4 or more characters, no taps after. Expect: the " +
                        "shownWhen block disappears and the keyboard slides down with it. " +
                        "Failure: the keyboard stays open."
            )
            shownWhen { notes().length < 4 }.col {
                field("Notes") {
                    textInput {
                        hint = "Type abcd"
                        content bind notes
                    }
                }
                text("This block disappears at 4 characters.")
            }
            col {
                ::shown { notes().length >= 4 }
                text("The shownWhen block hid itself - the keyboard should be closed now.")
                button {
                    text("Reset")
                    onClick { notes.value = "" }
                }
            }
        }
    }

    /** `shownWhen` applied directly to a text input rather than a container. */
    private fun ElementWriter.CanAddTheme.shownWhenTextAreaSection() {
        val essay = Signal("")
        card.col {
            h2("4. shownWhen - on the textArea itself")
            text(
                "Do: tap the text area, type 4 or more characters, no taps after. Expect: the " +
                        "text area disappears and the keyboard slides down. Failure: the keyboard " +
                        "stays open over an empty spot."
            )
            shownWhen { essay().length < 4 }.textArea {
                hint = "Type abcd to make me disappear"
                content bind essay
            }
            col {
                ::shown { essay().length >= 4 }
                text("The textArea hid itself - the keyboard should be closed now.")
                button {
                    text("Reset")
                    onClick { essay.value = "" }
                }
            }
        }
    }

    /** Hiding a row out of a list (the `foreach`/`colOf` path sets `shown = false` on the cell). */
    private fun ElementWriter.CanAddTheme.rowFilterSection() {
        val rows = Signal(listOf(Signal(""), Signal(""), Signal("")))
        card.col {
            h2("5. List rows hide when filtered out")
            text(
                "Do: tap into the SECOND row's field and type 3 or more characters, no taps " +
                        "after. Expect: that row disappears from the list AND the keyboard slides " +
                        "down. Failure: the keyboard stays open over the shortened list."
            )
            colOf(
                remember { rows().filter { it().length < 3 } },
                id = { it }
            ) { row ->
                // The render lambda isn't a ReactiveContext, so read the row's signal eagerly.
                val text = row.state.getOrNull()
                field("Row") {
                    textInput {
                        hint = "Type abc to remove this row"
                        if (text != null) content bind text
                    }
                }
            }
            button {
                text("Reset rows")
                onClick { rows.value.forEach { it.value = "" } }
            }
        }
    }

    /**
     * Guards both halves of the tap gesture work: tapping empty space still clears a keyboard,
     * and tapping into a field never dismisses it (the delegate added alongside the fix).
     */
    private fun ElementWriter.CanAddTheme.tapFallbackSection() = card.col {
        h2("6. Tap fallback & tap-into-field regression")
        text(
            "a) Tap field A so the keyboard opens, then tap this card's empty space (not a " +
                    "field). Expect: the keyboard closes."
        )
        text(
            "b) Tap field A, then tap field B. Expect: the keyboard stays open and moves to B - " +
                    "tapping into a field must never dismiss it."
        )
        row {
            expanding.field("A") {
                textInput { hint = "Field A" }
            }
            expanding.field("B") {
                textInput { hint = "Field B" }
            }
        }
    }
}