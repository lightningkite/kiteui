package com.lightningkite.kiteui.models

import com.lightningkite.kiteui.testing.BaseUiTest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * The contract every built-in theme signs, checked against all of them at once.
 *
 * The [builtIns] table is itself half the test: each entry calls its factory with three positional
 * arguments, so a theme whose first three parameters stop being `(id, background, accent)` fails to
 * compile here. That is the standardized shape - `background`'s brightness selects light or dark,
 * `accent` is the one brand color, and everything else is optional and comes after.
 *
 * The behavioral half is the set of properties a theme has to have to survive contact with the nav
 * components and with nesting. Per-theme character - Material's caps, Material 3's tonal palette,
 * Apple's grouped cells - is tested in [BuiltInThemeIdentityTest]; this file only asserts what they
 * all owe.
 */
class BuiltInThemeContractTest : BaseUiTest() {

    private class BuiltIn(val name: String, val build: (String, Color, Color) -> Theme)

    private val builtIns = listOf(
        BuiltIn("material") { id, background, accent -> Theme.material(id, background, accent) },
        BuiltIn("material3") { id, background, accent -> Theme.material3(id, background, accent) },
        BuiltIn("clean") { id, background, accent -> Theme.clean(id, background, accent) },
        BuiltIn("shadCnLike") { id, background, accent -> Theme.shadCnLike(id, background, accent) },
    )

    private val lightCanvas = Color.fromHexString("#FAFAFA")
    private val darkCanvas = Color.fromHexString("#121212")
    private val accent = Color.fromHexString("#2563EB")

    /** Every theme in both polarities, with ids unique per test so nothing aliases in the cache. */
    private fun eachPolarity(test: String): List<Pair<String, Theme>> = builtIns.flatMap {
        listOf(
            "${it.name} light" to it.build("$test-${it.name}-l", lightCanvas, accent),
            "${it.name} dark" to it.build("$test-${it.name}-d", darkCanvas, accent),
        )
    }

    /** Applies a chain of semantics the way an element tree would. */
    private fun Theme.through(vararg semantics: Semantic): Theme =
        semantics.fold(withoutBack) { acc, semantic -> acc + semantic }.theme

    private fun assertLegible(theme: Theme, path: String) {
        val ratio = theme.foreground.closestColor() contrastAgainst theme.background.closestColor()
        assertTrue(
            ratio >= Color.WCAG_AA_NORMAL_TEXT_CONTRAST_RATIO,
            "$path: contrast ratio $ratio is below the WCAG AA minimum of " +
                "${Color.WCAG_AA_NORMAL_TEXT_CONTRAST_RATIO}"
        )
    }

    // -------------------------------------------------------------------------
    // The standardized signature
    // -------------------------------------------------------------------------

    /**
     * Everything after `id` has a usable default, so a theme can be had for the asking. `id` itself
     * defaults too, which is what makes the one-liner in each factory's KDoc work.
     */
    @Test
    fun everyFactoryIsUsableWithNoArguments() = runTest {
        val defaults = listOf(
            "material" to Theme.material(),
            "material3" to Theme.material3(),
            "clean" to Theme.clean(),
            "shadCnLike" to Theme.shadCnLike(),
        )
        for ((name, theme) in defaults) assertLegible(theme, "$name (defaults)")
    }

    /** The brightness of `background` is what picks light or dark - no separate flag, in any of them. */
    @Test
    fun backgroundBrightnessSelectsPolarity() = runTest {
        for (builtIn in builtIns) {
            val light = builtIn.build("polarity-${builtIn.name}-l", lightCanvas, accent)
            val dark = builtIn.build("polarity-${builtIn.name}-d", darkCanvas, accent)

            assertTrue(
                light.background.closestColor().perceivedBrightness > 0.5f,
                "${builtIn.name}: a light canvas must produce a light page"
            )
            assertTrue(
                dark.background.closestColor().perceivedBrightness < 0.5f,
                "${builtIn.name}: a dark canvas must produce a dark page"
            )
            assertTrue(
                light.foreground.closestColor().perceivedBrightness < 0.5f,
                "${builtIn.name}: a light theme's text must be dark"
            )
            assertTrue(
                dark.foreground.closestColor().perceivedBrightness > 0.5f,
                "${builtIn.name}: a dark theme's text must be light"
            )
        }
    }

    /** The accent is the brand, so it has to actually reach the primary action. */
    @Test
    fun theAccentReachesThePrimaryAction() = runTest {
        for ((name, theme) in eachPolarity("accent")) {
            val fill = theme.through(ImportantSemantic).background.closestColor()
            assertTrue(
                (fill.toHSP().hue angleTo accent.toHSP().hue).turns.let { it * it } < 0.0025f,
                "$name: the primary action must be recognizably the accent's hue, but came out $fill"
            )
        }
    }

    // -------------------------------------------------------------------------
    // Legibility
    // -------------------------------------------------------------------------

    @Test
    fun everySurfaceSemanticKeepsTextLegible() = runTest {
        val paths: List<Pair<String, Array<Semantic>>> = listOf(
            "page" to arrayOf(),
            "outer" to arrayOf(OuterSemantic),
            "content" to arrayOf(MainContentSemantic),
            "bar" to arrayOf(BarSemantic),
            "bar>selected" to arrayOf(BarSemantic, SelectedSemantic),
            "bar>unselected" to arrayOf(BarSemantic, UnselectedSemantic),
            "nav" to arrayOf(NavSemantic),
            "nav>selected" to arrayOf(NavSemantic, SelectedSemantic),
            "nav>unselected" to arrayOf(NavSemantic, UnselectedSemantic),
            "nav>unselected>hover" to arrayOf(NavSemantic, UnselectedSemantic, HoverSemantic),
            "card" to arrayOf(CardSemantic),
            "card>card" to arrayOf(CardSemantic, CardSemantic),
            "card>card>card" to arrayOf(CardSemantic, CardSemantic, CardSemantic),
            "card>inset" to arrayOf(CardSemantic, InsetSemantic),
            "card>embedded" to arrayOf(CardSemantic, EmbeddedSemantic),
            "card>codeBlock" to arrayOf(CardSemantic, CodeBlockSemantic),
            "card>blockquote" to arrayOf(CardSemantic, BlockquoteSemantic),
            "card>field" to arrayOf(CardSemantic, FieldSemantic),
            "card>subtext" to arrayOf(CardSemantic, SubtextSemantic),
            "card>h1" to arrayOf(CardSemantic, H1Semantic),
            "card>hover" to arrayOf(CardSemantic, HoverSemantic),
            "card>down" to arrayOf(CardSemantic, DownSemantic),
            "card>selected" to arrayOf(CardSemantic, SelectedSemantic),
            "dialog" to arrayOf(DialogSemantic),
            "dialog>card" to arrayOf(DialogSemantic, CardSemantic),
            "popover" to arrayOf(PopoverSemantic),
            "important" to arrayOf(ImportantSemantic),
            "important>hover" to arrayOf(ImportantSemantic, HoverSemantic),
            "important>subtext" to arrayOf(ImportantSemantic, SubtextSemantic),
            "critical" to arrayOf(CriticalSemantic),
            "warning" to arrayOf(WarningSemantic),
            "danger" to arrayOf(DangerSemantic),
            "affirmative" to arrayOf(AffirmativeSemantic),
        )
        for ((name, theme) in eachPolarity("legible")) {
            for ((path, semantics) in paths) assertLegible(theme.through(*semantics), "$name: $path")
        }
    }

    @Test
    fun errorTextIsLegibleWhereverItLands() = runTest {
        for ((name, theme) in eachPolarity("error")) {
            assertLegible(theme.through(ErrorSemantic), "$name: error")
            assertLegible(theme.through(CardSemantic, ErrorSemantic), "$name: card>error")
            assertLegible(theme.through(CardSemantic, FieldSemantic, ErrorSemantic), "$name: card>field>error")
        }
    }

    // -------------------------------------------------------------------------
    // Nav items - the shape the nav components actually impose
    // -------------------------------------------------------------------------

    /**
     * The nav components pack their items with `gap = 0`, so an inactive item that paints a
     * background or an outline shows up as a box butted against its neighbors. Only the active one
     * may fill.
     */
    @Test
    fun inactiveNavItemsPaintNothing() = runTest {
        for ((name, theme) in eachPolarity("inactive")) {
            val unselected = theme.through(NavSemantic).withoutBack + UnselectedSemantic
            assertEquals(false, unselected.drawBackground, "$name: an inactive nav item must not fill")
            assertEquals(true, unselected.padding, "$name: an inactive nav item still needs its padding")
            assertEquals(0.dp, unselected.theme.outlineWidth, "$name: an inactive nav item must not be outlined")
        }
    }

    /**
     * Corner radius normally follows the container's spacing, which is zero in a nav list. Items
     * have to pin their own or they come out as hard-edged rectangles.
     */
    @Test
    fun navItemsPinTheirOwnCornerRadius() = runTest {
        for ((name, theme) in eachPolarity("radius")) {
            for (semantic in listOf(SelectedSemantic, UnselectedSemantic)) {
                val radii = theme.through(NavSemantic, semantic).cornerRadii
                assertTrue(
                    radii is CornerRadii.Fixed && radii.value > 0.dp,
                    "$name: ${semantic.key} must pin a fixed radius rather than inherit a zero-gap " +
                        "container's; got $radii"
                )
            }
        }
    }

    @Test
    fun theActiveNavItemStandsOut() = runTest {
        for ((name, theme) in eachPolarity("active")) {
            for (surface in listOf("nav" to NavSemantic, "bar" to BarSemantic)) {
                val base = theme.through(surface.second)
                val selected = base.through(SelectedSemantic)
                assertNotEquals(
                    base.background.closestColor(), selected.background.closestColor(),
                    "$name: the active item must be distinguishable from the ${surface.first} behind it"
                )
            }
        }
    }

    // -------------------------------------------------------------------------
    // Depth and interaction
    // -------------------------------------------------------------------------

    @Test
    fun cardsSeparateFromWhatTheySitOnAtEveryDepth() = runTest {
        for ((name, theme) in eachPolarity("depth")) {
            val page = theme.background.closestColor()
            val card = theme.through(CardSemantic).background.closestColor()
            val nested = theme.through(CardSemantic, CardSemantic).background.closestColor()
            val deep = theme.through(CardSemantic, CardSemantic, CardSemantic).background.closestColor()

            assertNotEquals(page, card, "$name: a card must not be the color of the page behind it")
            assertNotEquals(card, nested, "$name: a card inside a card must separate from its parent")
            assertNotEquals(nested, deep, "$name: the third level must still separate from the second")
        }
    }

    @Test
    fun pressingIsStrongerFeedbackThanHovering() = runTest {
        for ((name, theme) in eachPolarity("feedback")) {
            val card = theme.through(CardSemantic)
            val surface = card.background.closestColor()
            val hovered = card.through(HoverSemantic).background.closestColor()
            val pressed = card.through(DownSemantic).background.closestColor()

            assertNotEquals(surface, hovered, "$name: hover must be visible on a card")
            assertTrue(
                (pressed channelDifferenceSum surface) > (hovered channelDifferenceSum surface),
                "$name: pressing must move the surface further than hovering does"
            )
        }
    }

    /**
     * A solid fill is the case that catches naive state layers: a wash toward the accent does
     * nothing to a surface that already *is* the accent, leaving the primary button with no
     * feedback at all.
     */
    @Test
    fun interactionIsVisibleOnASolidFill() = runTest {
        for ((name, theme) in eachPolarity("fill")) {
            val important = theme.through(ImportantSemantic)
            assertNotEquals(
                important.background.closestColor(),
                important.through(HoverSemantic).background.closestColor(),
                "$name: hovering the primary action must do something"
            )
        }
    }

    /** `critical` is `important` twice, so it has to land somewhere other than back on itself. */
    @Test
    fun criticalIsDistinguishableFromImportant() = runTest {
        for ((name, theme) in eachPolarity("critical")) {
            val important = theme.through(ImportantSemantic).background.closestColor()
            val critical = theme.through(CriticalSemantic).background.closestColor()
            assertTrue(
                (important channelDifferenceSum critical) > 0.15f,
                "$name: critical must not collapse into important; got $important vs $critical"
            )
        }
    }

    /** A field is something you type into; it has to be findable on whatever it sits on. */
    @Test
    fun fieldsAreDistinguishableFromTheirSurface() = runTest {
        for ((name, theme) in eachPolarity("field")) {
            val card = theme.through(CardSemantic)
            val field = card.through(FieldSemantic)
            val filled = field.background.closestColor() != card.background.closestColor()
            val outlined = field.outlineWidth > 0.dp
            assertTrue(
                filled || outlined,
                "$name: a field must be marked out by a fill or an outline, and this one has neither"
            )
        }
    }
}
