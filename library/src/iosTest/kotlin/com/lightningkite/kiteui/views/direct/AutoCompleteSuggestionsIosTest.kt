package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.views.ContainerElement
import com.lightningkite.kiteui.views.l2.overlayFrame
import com.lightningkite.kiteui.views.setup
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Regression coverage for iOS `AutoCompleteTextField.suggestions`. It used to be a documented no-op
 * (UIKit has no built-in completion affordance for `UITextField`); it now composes the dropdown out
 * of `Element.openPopover` - the same in-tree overlay mechanism `MenuButton.opensMenu()` and
 * `hintPopover` (see `HintPopoverTest`) are built on - anchored below the field, redrawing its rows
 * in place on every keystroke instead of reopening the popover.
 *
 * `AutoCompleteTextField.focused` and `recomputeSuggestionsPopover()` are exposed `internal`
 * specifically for this file, for the same reason `HintPopoverTrigger.handleState` is exposed
 * (see `HintPopoverTest`'s doc comment): `sendActionsForControlEvents` - the standard UIKit
 * mechanism for synthesizing a control event without a real touch/keyboard, and the only way
 * `content`'s own setter has to notify listeners of a programmatic change - does not deliver
 * target-action callbacks in this bare test binary (confirmed empirically: neither `content`'s own
 * listener nor a directly `addTarget`-registered one ever fired here, though both work in the real
 * app, which runs an actual `UIApplicationMain` event loop). So instead of relying on synthesized
 * events to trigger recomputation, tests call the recompute function directly after writing state.
 *
 * Selecting a row is driven via `AutoCompleteTextField.selectSuggestion` directly for the same
 * reason: a real tap can't be delivered through UIKit's touch pipeline from a test.
 *
 * KNOWN GAP - reported to team-lead, not silently worked around: design point 1 ("do not steal
 * first responder") should ideally be checked against a real `UITextField.isFirstResponder`, which
 * requires the field's window to be the *key* window (`becomeFirstResponder()` otherwise returns
 * `false` - confirmed empirically). But `UIWindow.makeKeyWindow()`/`makeKeyAndVisible()` reliably
 * crashes this test binary with `Trace/BPT trap` when launched via `:library:iosSimulatorArm64Test`
 * (Gradle's Kotlin/Native test runner) - while the exact same code runs and passes cleanly under a
 * direct `xcrun simctl spawn .../test.kexe`. Bisected thoroughly: the crash reproduces with
 * `makeKeyWindow()` alone (no `isHidden` change), on the very first test method run (not an
 * accumulation issue), and persists even when the window is retained for the harness's full
 * lifetime rather than left to be deallocated when `setUp()` returns (ruling out a dangling
 * key-window reference from ARC collecting an unretained local). No combination tried avoided it
 * under the Gradle-launched runner. `presentingThePopoverDoesNotStealFirstResponder` below instead
 * checks that this field's own `focused` tracking - which is what actually drives the popover
 * open/close decision - is not reset by presenting the popover; that is a real but weaker
 * guarantee than asserting genuine `isFirstResponder`, which is confirmed correct by code review
 * (`openPopover.ios.kt` contains no call anywhere in its implementation that touches first-responder
 * state) and was empirically verified with a real `becomeFirstResponder()`/`isFirstResponder` check
 * when run directly outside Gradle, just not reproducible through the mandated CI path.
 */
@OptIn(ExperimentalForeignApi::class)
class AutoCompleteSuggestionsIosTest {

    private val fruits = listOf("Apple", "Apricot", "Banana", "Grape", "Grapefruit")

    private class Harness(
        val overlay: ContainerElement,
        val field: AutoCompleteTextField,
        val baseline: Int,
    ) {
        fun type(query: String) {
            field.content.value = query
            field.recomputeSuggestionsPopover()
        }
    }

    private fun setUp(suggestions: List<String> = listOf()): Harness {
        val window = UIWindow(frame = CGRectMake(0.0, 0.0, 320.0, 480.0))
        val vc = UIViewController(null, null)
        window.rootViewController = vc
        vc.view.setFrame(CGRectMake(0.0, 0.0, 320.0, 480.0))

        lateinit var overlay: ContainerElement
        lateinit var field: AutoCompleteTextField
        vc.setup(Theme(id = "unitTest")) {
            col {
                overlay = this
                context.overlayFrame = this
                frame {
                    field = autoCompleteTextField {
                        this.suggestions = suggestions
                    }
                }
            }
        }
        vc.view.setNeedsLayout()
        vc.view.layoutIfNeeded()

        // Not a real becomeFirstResponder() - see this file's class doc comment for why, and what
        // that gap does and doesn't cover.
        field.focused = true

        return Harness(overlay, field, overlay.children.size)
    }

    @Test
    fun typingAMatchingQueryFiltersAndOpensThePopover() {
        val h = setUp(fruits)

        h.type("gra")
        assertEquals(h.baseline + 1, h.overlay.children.size, "a matching query should open exactly one popover")
    }

    @Test
    fun aQuerySpanningAWordBoundaryStillMatches() {
        // Matching must consider the whole suggestion, not only its individual words. Splitting on
        // whitespace alone would fail here, because no single word starts with "dragon f" - yet
        // Android's ArrayFilter matches it on the whole-string prefix, so iOS has to agree.
        val h = setUp(listOf("Dragon Fruit", "Apple"))

        h.type("dragon f")
        assertEquals(h.baseline + 1, h.overlay.children.size, "a query spanning a space should still match")
    }

    @Test
    fun matchingIsCaseInsensitiveOnBothWholeStringAndWordPrefixes() {
        val h = setUp(listOf("Dragon Fruit"))

        h.type("DRAGON FR")
        assertEquals(h.baseline + 1, h.overlay.children.size, "whole-string prefix must ignore case")

        h.type("fRuI")
        assertEquals(h.baseline + 1, h.overlay.children.size, "word prefix must ignore case too")
    }

    @Test
    fun presentingThePopoverDoesNotStealFirstResponder() {
        // See this file's class doc comment: a weaker proxy for a real isFirstResponder check,
        // which crashes this test binary via the mandated Gradle test runner. This at least proves
        // presenting the popover doesn't clear the field's own focus tracking.
        val h = setUp(fruits)
        assertTrue(h.field.focused, "precondition: focused before typing")

        h.type("gra")
        assertEquals(h.baseline + 1, h.overlay.children.size, "precondition: the popover actually opened")

        assertTrue(
            h.field.focused,
            "opening the suggestions popover must not clear the field's focus tracking - the keyboard has to stay up",
        )
    }

    @Test
    fun aQueryMatchingNothingNeverOpensThePopover() {
        val h = setUp(fruits)

        h.type("xyz")
        assertEquals(h.baseline, h.overlay.children.size, "no matches should mean no popover")
    }

    @Test
    fun matchesDroppingToZeroClosesAnOpenPopover() {
        val h = setUp(fruits)

        h.type("gra")
        assertEquals(h.baseline + 1, h.overlay.children.size, "precondition: popover open")

        h.type("grax")
        assertEquals(h.baseline, h.overlay.children.size, "matches dropping to zero should close the popover")
    }

    @Test
    fun repeatedTypingWithContinuedMatchesDoesNotStackPopovers() {
        val h = setUp(fruits)

        h.type("g")
        assertEquals(h.baseline + 1, h.overlay.children.size, "first keystroke should open one popover")

        // Each of these keeps at least one match ("Grape", "Grapefruit") - the popover must update
        // in place rather than closing and reopening (or stacking) on every keystroke.
        h.type("gr")
        assertEquals(h.baseline + 1, h.overlay.children.size, "still one popover after a second keystroke")

        h.type("gra")
        assertEquals(h.baseline + 1, h.overlay.children.size, "still one popover after a third keystroke")

        h.type("grap")
        assertEquals(h.baseline + 1, h.overlay.children.size, "still one popover after a fourth keystroke")
    }

    @Test
    fun blurringTheFieldClosesThePopover() {
        val h = setUp(fruits)

        h.type("gra")
        assertEquals(h.baseline + 1, h.overlay.children.size, "precondition: popover open")

        h.field.focused = false
        assertEquals(h.baseline, h.overlay.children.size, "losing focus should close the popover")
    }

    @Test
    fun selectingASuggestionSetsContentAndClosesThePopover() {
        val h = setUp(fruits)

        h.type("gra")
        assertEquals(h.baseline + 1, h.overlay.children.size, "precondition: popover open")

        h.field.selectSuggestion("Grapefruit")

        assertEquals("Grapefruit", h.field.content.value, "selecting a suggestion should set content")
        assertEquals(h.baseline, h.overlay.children.size, "selecting a suggestion should close the popover")
    }

    @Test
    fun selectingASuggestionDoesNotImmediatelyReopenThePopover() {
        // The regression this guards: content.value = "Grapefruit" is itself a query that matches
        // "Grapefruit" (a suggestion equal to itself), so a naive implementation observes the
        // content change, recomputes a one-item match list, and reopens immediately - right after
        // selectSuggestion() closed it for that same selection.
        //
        // The recompute below stands in for the UIControlEventEditingChanged the real app fires on
        // the write. The guard is deliberately a comparison against the field's current content
        // rather than a "skip the next recompute" flag, so it holds however many recomputes happen
        // and whenever they arrive - a counting flag protected the wrong call in a test binary,
        // where sendActionsForControlEvents delivers nothing.
        val h = setUp(fruits)

        h.type("gra")
        h.field.selectSuggestion("Grapefruit")
        h.field.recomputeSuggestionsPopover()
        h.field.recomputeSuggestionsPopover()

        assertEquals(h.baseline, h.overlay.children.size, "selection must not cause an immediate reopen")
    }

    @Test
    fun typingOnAfterASelectionOpensTheListAgain() {
        // The other side of the guard: it must release as soon as the text stops being the
        // selection, or the field would never suggest anything again after one pick.
        val h = setUp(fruits)

        h.type("gra")
        h.field.selectSuggestion("Grape")
        assertEquals(h.baseline, h.overlay.children.size, "precondition: closed by the selection")

        h.type("grap")

        assertEquals(h.baseline + 1, h.overlay.children.size, "editing after a selection must suggest again")
    }

    @Test
    fun removingTheFieldClosesAnOpenPopover() {
        val h = setUp(fruits)

        h.type("gra")
        assertEquals(h.baseline + 1, h.overlay.children.size, "precondition: popover open")

        // The field lives inside `frame { }`, the overlay's first child (index 0); the popover is
        // its second child (added as a sibling by openPopover, via context.overlayFrame). Removing
        // the frame tears the field down through its onShutdown/onRemove chain, which should close
        // the still-open popover too - leaving nothing behind.
        h.overlay.removeChild(0)

        assertEquals(0, h.overlay.children.size, "removing the field's container should also remove its open popover, leaving nothing behind")
    }
}
