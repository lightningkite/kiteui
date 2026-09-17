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
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

/**
 * Manual verification for a batch of iOS-only fixes that a JVM/SSR or Robolectric unit test can't
 * reach visually: colored (theme-aware) hints on [NumberInput] and [FormattedTextInput] (they
 * used to fall back to iOS's fixed system-grey placeholder), an overlay hint label on [TextArea]
 * (UITextView has no native placeholder at all, so the hint used to be silently dropped), and a
 * Clear affordance on [LocalDateField]/[LocalTimeField]/[LocalDateTimeField] (content was
 * nullable but a UIDatePicker can't represent "no selection" once a value's been picked, so there
 * was previously no way to unset one). See docs/MANUAL_VERIFICATION.md for the checklist this page
 * backs, and library/src/iosTest/kotlin/com/lightningkite/kiteui/views/direct/HintColorTest.kt +
 * LocalDateTimeFieldClearTest.kt for the automated coverage of the same fixes.
 */
@Routable("ios-text-inputs")
object IosTextInputsVerificationPage : Page {
    override fun ElementWriter.CanAddTheme.render(): Unit = run {
        scrolling.col {
            h1("iOS Text Input Verification")
            text(
                "Hint contrast for NumberField/FormattedTextInput/TextArea, checked against both a " +
                        "light and a dark container below, plus Clear buttons on the date/time fields. " +
                        "Platform: iOS is what's under test here - web and Android should already look " +
                        "fine on every section."
            )

            lightHintsSection()
            darkHintsSection()
            dateTimeClearSection()
        }
    }

    private fun ElementWriter.CanAddTheme.lightHintsSection() = card.col {
        h2("Light theme hints")
        hintsExplanation()
        hintFields()
    }

    /**
     * Forced dark container so the fix is checkable without changing the app's global theme,
     * mirroring AutoCompleteTestPage's darkTheme() section for the same kind of check.
     */
    private fun ElementWriter.CanAddTheme.darkHintsSection() =
        themed(ThemeDerivation {
            it.copy(id = "iosTextInputsDarkCheck", background = Color.black, foreground = Color.white).withoutBack
        }).card.col {
            h2("Dark theme hints")
            hintsExplanation()
            hintFields()
        }

    private fun ViewWriter.hintsExplanation() = text(
        "Expected: every hint below (\"Enter a number\", \"555-1234\", \"Write a few sentences...\") " +
                "is clearly legible against its background, not a near-invisible smudge. Before the " +
                "fix, NumberField/FormattedTextInput hints used iOS's fixed system-grey placeholder " +
                "(barely visible on the dark container), and TextArea's hint didn't render at all."
    )

    private fun ViewWriter.hintFields(): Unit = run {
        sizeConstraints(width = 20.rem).field("Number") {
            numberInput { hint = "Enter a number" }
        }
        sizeConstraints(width = 20.rem).field("Formatted") {
            formattedTextInput { hint = "555-1234" }
        }
        sizeConstraints(width = 20.rem, height = 6.rem).field("Text area") {
            scrolling.textArea { hint = "Write a few sentences..." }
        }
    }

    /**
     * Each field starts unset ("-"). "Set example value" sets content directly (simulating a
     * previously-picked value); the field's own Clear button - open the field, tap "Clear" in its
     * toolbar - is what's under test, and should null the content back out and show "-" again.
     */
    private fun ElementWriter.CanAddTheme.dateTimeClearSection() = card.col {
        h2("Date / time / date-time Clear")
        text(
            "Tap a field to open its picker, then tap \"Clear\" in the toolbar above the keyboard. " +
                    "Expected: the stored value below goes back to \"null\" and the field shows \"-\". " +
                    "Before the fix there was no Clear button at all - only \"Done\", so a value could " +
                    "never be unset once picked."
        )

        val date = Signal<LocalDate?>(null)
        row {
            sizeConstraints(width = 12.rem).field("Date") {
                localDateField { content bind date }
            }
            button {
                text("Set example value")
                onClick { date.value = LocalDate(2024, 6, 15) }
            }
        }
        row {
            bold.text("Stored:")
            text { ::content { "${date()}" } }
        }

        space()

        val time = Signal<LocalTime?>(null)
        row {
            sizeConstraints(width = 12.rem).field("Time") {
                localTimeField { content bind time }
            }
            button {
                text("Set example value")
                onClick { time.value = LocalTime(13, 30) }
            }
        }
        row {
            bold.text("Stored:")
            text { ::content { "${time()}" } }
        }

        space()

        val dateTime = Signal<LocalDateTime?>(null)
        row {
            sizeConstraints(width = 16.rem).field("Date + time") {
                localDateTimeField { content bind dateTime }
            }
            button {
                text("Set example value")
                onClick { dateTime.value = LocalDateTime(2024, 6, 15, 13, 30) }
            }
        }
        row {
            bold.text("Stored:")
            text { ::content { "${dateTime()}" } }
        }
    }
}
