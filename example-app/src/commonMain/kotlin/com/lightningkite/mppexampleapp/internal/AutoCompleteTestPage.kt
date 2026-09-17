package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.Color
import com.lightningkite.kiteui.models.ThemeDerivation
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.field
import com.lightningkite.reactive.core.*

private val fruitSuggestions = listOf(
    "Apple", "Apricot", "Banana", "Blackberry", "Blueberry", "Cherry", "Clementine",
    "Coconut", "Cranberry", "Date", "Dragonfruit", "Elderberry", "Fig", "Grape",
    "Grapefruit", "Guava", "Kiwi", "Lemon", "Lime", "Lychee", "Mango",
)

/**
 * Manual + visual verification for `AutoCompleteTextField.suggestions` across platforms.
 *
 * `suggestions` used to be broken or a no-op on every platform: on web the setter stored the value
 * and did nothing else (the `<datalist>` wiring was commented out), and on Android reading the
 * property before it was ever written threw. See docs/MANUAL_VERIFICATION.md for the platform
 * checklist this page backs.
 */
@Routable("autocomplete-test")
object AutoCompleteTestPage : Page {
    override fun ElementWriter.CanAddTheme.render(): Unit = run {
        scrolling.col {
            h1("AutoCompleteTextField Verification")
            text(
                "Suggestion list for every field below is the same 21 fruit names. Type a couple of " +
                        "letters (e.g. \"c\" or \"gra\") in each field and check for a suggestion list."
            )

            web()
            android()
            ios()
            darkTheme()
        }
    }

    private fun ElementWriter.CanAddTheme.web() = card.col {
        h2("Web")
        text(
            "Expected: the browser's native autocomplete dropdown appears below the field, listing " +
                    "matching fruit names, backed by an HTML <datalist>. Before the fix, nothing appeared " +
                    "at all - the suggestions list was silently discarded."
        )
        val typed = Signal("")
        field("Fruit") {
            autoCompleteTextField {
                hint = "Start typing a fruit..."
                suggestions = fruitSuggestions
                content bind typed
            }
        }
        row {
            bold.text("Typed:")
            text { ::content { typed() } }
        }
    }

    private fun ElementWriter.CanAddTheme.android() = card.col {
        h2("Android")
        text(
            "Expected: the standard AutoCompleteTextView dropdown appears below the field as you type. " +
                    "Before the fix, reading `suggestions` before it was first set threw a crash if " +
                    "anything tried to inspect it - not reachable from this screen, but exercised by " +
                    "AutoCompleteTextFieldSuggestionsTest."
        )
        val typed = Signal("")
        field("Fruit") {
            autoCompleteTextField {
                hint = "Start typing a fruit..."
                suggestions = fruitSuggestions
                content bind typed
            }
        }
        row {
            bold.text("Typed:")
            text { ::content { typed() } }
        }
    }

    private fun ElementWriter.CanAddTheme.ios() = card.col {
        h2("iOS")
        text(
            "Expected: a dropdown appears below the field, listing matching fruit names, built on " +
                    "the same popover mechanism MenuButton.opensMenu() and hintPopover use (UIKit has " +
                    "no native completion affordance for UITextField, so this is a custom dropdown, " +
                    "not a system one). Tapping a row fills the field and closes the dropdown. The " +
                    "keyboard should stay up the whole time - opening the dropdown must not steal " +
                    "focus from the field."
        )
        val typed = Signal("")
        field("Fruit") {
            autoCompleteTextField {
                hint = "Start typing a fruit..."
                suggestions = fruitSuggestions
                content bind typed
            }
        }
        row {
            bold.text("Typed:")
            text { ::content { typed() } }
        }
    }

    /**
     * A forced dark container so the iOS hint-color fix is checkable without changing the app's
     * global theme: `updateHint()` used to set the plain `textField.placeholder`, which iOS always
     * draws in a fixed system grey - close to invisible against a dark background. Expected here:
     * the "Start typing..." hint text is clearly readable, not a barely-visible dark-grey-on-dark
     * smudge.
     */
    private fun ElementWriter.CanAddTheme.darkTheme() =
        themed(ThemeDerivation {
            it.copy(id = "autocompleteDarkCheck", background = Color.black, foreground = Color.white).withoutBack
        }).card.col {
            h2("Dark theme hint contrast (iOS focus)")
            text(
                "Expected: the empty field's hint text is clearly legible white-ish text on the dark " +
                        "card, not a near-invisible grey. Platform: iOS is the one under test for this " +
                        "specific fix; web and Android should already look fine here."
            )
            sizeConstraints(width = 20.rem).field("Fruit") {
                autoCompleteTextField {
                    hint = "Start typing a fruit..."
                    suggestions = fruitSuggestions
                }
            }
        }
}
