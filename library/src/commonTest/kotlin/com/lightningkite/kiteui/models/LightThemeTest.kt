package com.lightningkite.kiteui.models

import com.lightningkite.kiteui.testing.BaseUiTest
import kotlinx.coroutines.test.runTest
import kotlin.math.absoluteValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Locks in the two properties that make [Theme.light] a *light* theme rather than a dark one with
 * pale colors: every surface keeps its text legible, and nesting a surface still changes it.
 *
 * The second one is the failure mode this theme exists to avoid. Derivations built on
 * [Color.highlight] move away from the current color, so on a white surface they have nowhere to
 * go - cards, hover states and code blocks silently collapse into the surface they sit on. Every
 * "must not equal its parent surface" assertion below would pass trivially on a dark theme and
 * fail on a light one built that way.
 */
class LightThemeTest : BaseUiTest() {

    private fun light(id: String, accent: Color? = null) =
        if (accent == null) Theme.light(id = id) else Theme.light(id = id, accent = accent)

    /** Applies a chain of semantics the way an element tree would. */
    private fun Theme.through(vararg semantics: Semantic): Theme =
        semantics.fold(withoutBack) { acc, semantic -> acc + semantic }.theme

    private fun assertLegible(theme: Theme, path: String) {
        val foreground = theme.foreground.closestColor()
        val background = theme.background.closestColor()
        val ratio = foreground contrastAgainst background
        assertTrue(
            ratio >= Color.WCAG_AA_NORMAL_TEXT_CONTRAST_RATIO,
            "$path: foreground on background has a contrast ratio of $ratio, " +
                "below the WCAG AA minimum of ${Color.WCAG_AA_NORMAL_TEXT_CONTRAST_RATIO}"
        )
    }

    // -------------------------------------------------------------------------
    // Legibility across the semantics that paint a surface
    // -------------------------------------------------------------------------

    @Test
    fun everySurfaceSemantic_keepsTextLegible() = runTest {
        val theme = light("light-legible")
        val paths: List<Pair<String, Array<Semantic>>> = listOf(
            "page" to arrayOf(),
            "outer" to arrayOf(OuterSemantic),
            "content" to arrayOf(MainContentSemantic),
            "bar" to arrayOf(BarSemantic),
            "nav" to arrayOf(NavSemantic),
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
            "card>h4" to arrayOf(CardSemantic, H4Semantic),
            "card>hover" to arrayOf(CardSemantic, HoverSemantic),
            "card>down" to arrayOf(CardSemantic, DownSemantic),
            "card>selected" to arrayOf(CardSemantic, SelectedSemantic),
            "card>unselected" to arrayOf(CardSemantic, UnselectedSemantic),
            "dialog" to arrayOf(DialogSemantic),
            "popover" to arrayOf(PopoverSemantic),
            "important" to arrayOf(ImportantSemantic),
            "important>hover" to arrayOf(ImportantSemantic, HoverSemantic),
            "important>selected" to arrayOf(ImportantSemantic, SelectedSemantic),
            "important>subtext" to arrayOf(ImportantSemantic, SubtextSemantic),
            "important>field" to arrayOf(ImportantSemantic, FieldSemantic),
            "important>important" to arrayOf(ImportantSemantic, ImportantSemantic),
            "critical" to arrayOf(CriticalSemantic),
            "warning" to arrayOf(WarningSemantic),
            "danger" to arrayOf(DangerSemantic),
            "affirmative" to arrayOf(AffirmativeSemantic),
            "bar>important" to arrayOf(BarSemantic, ImportantSemantic),
            "nav>selected" to arrayOf(NavSemantic, SelectedSemantic),
        )
        for ((name, semantics) in paths) assertLegible(theme.through(*semantics), name)
    }

    /**
     * `error` and `invalid` recolor text and outlines without owning the surface they land on, so
     * they are checked against the surfaces they realistically appear on rather than in isolation.
     */
    @Test
    fun errorText_isLegibleOnEverySurfaceItLandsOn() = runTest {
        val theme = light("light-error")
        assertLegible(theme.through(ErrorSemantic), "error")
        assertLegible(theme.through(CardSemantic, ErrorSemantic), "card>error")
        assertLegible(theme.through(CardSemantic, FieldSemantic, ErrorSemantic), "card>field>error")
    }

    // -------------------------------------------------------------------------
    // The surface ladder actually steps
    // -------------------------------------------------------------------------

    @Test
    fun card_changesTheSurfaceItSitsOn_atEveryNestingDepth() = runTest {
        val theme = light("light-ladder")
        val page = theme.background.closestColor()
        val card = theme.through(CardSemantic).background.closestColor()
        val nested = theme.through(CardSemantic, CardSemantic).background.closestColor()
        val deep = theme.through(CardSemantic, CardSemantic, CardSemantic).background.closestColor()

        assertNotEquals(page, card, "a card must not be the same color as the page behind it")
        assertNotEquals(card, nested, "a card inside a card must not be the same color as its parent")
        assertNotEquals(nested, deep, "the third nesting level must still separate from the second")
    }

    @Test
    fun interactionStates_changeTheSurface_evenOnWhite() = runTest {
        val theme = light("light-states")
        // A card is the top of the ladder (white), which is exactly where a highlight-based
        // derivation runs out of room and produces no visible feedback at all.
        val card = theme.through(CardSemantic)
        val surface = card.background.closestColor()

        assertNotEquals(surface, card.through(HoverSemantic).background.closestColor(), "hover must be visible on a white card")
        assertNotEquals(surface, card.through(DownSemantic).background.closestColor(), "down must be visible on a white card")
        assertNotEquals(surface, card.through(SelectedSemantic).background.closestColor(), "selected must be visible on a white card")
        assertNotEquals(surface, card.through(InsetSemantic).background.closestColor(), "inset must be visible on a white card")
        assertNotEquals(surface, card.through(CodeBlockSemantic).background.closestColor(), "a code block must be visible on a white card")
    }

    @Test
    fun down_isStrongerFeedbackThanHover() = runTest {
        val theme = light("light-feedback")
        val card = theme.through(CardSemantic)
        val surface = card.background.closestColor()
        val hovered = card.through(HoverSemantic).background.closestColor()
        val pressed = card.through(DownSemantic).background.closestColor()

        assertTrue(
            (pressed channelDifferenceSum surface) > (hovered channelDifferenceSum surface),
            "pressing must move the surface further than hovering does"
        )
    }

    // -------------------------------------------------------------------------
    // Accent handling
    // -------------------------------------------------------------------------

    /**
     * A brand color chosen for a logo is often too pale to use as text on white. The theme deepens
     * it rather than rejecting it or quietly dropping back to a neutral.
     */
    @Test
    fun paleAccent_isDeepenedUntilHeadingsAndSelectionAreLegible() = runTest {
        val theme = light("light-pale", accent = Color.fromHexString("#9AD1FF"))

        assertLegible(theme.through(CardSemantic, H1Semantic), "pale accent: card>h1")
        assertLegible(theme.through(CardSemantic, SelectedSemantic), "pale accent: card>selected")
        assertLegible(theme.through(ImportantSemantic), "pale accent: important")
    }

    /**
     * Headings build hierarchy out of weight, size and tracking, never color - the accent is
     * reserved for things you can act on. Leaving foreground alone is also what keeps a heading
     * legible on a strong fill without a special case.
     */
    @Test
    fun headings_buildHierarchyWithoutSpendingTheAccent() = runTest {
        val theme = light("light-headings")
        for (surface in listOf(CardSemantic, ImportantSemantic, DangerSemantic)) {
            val base = theme.through(surface)
            for (level in 1..6) {
                val heading = base.through(HeaderSizeSemantic(level))
                assertEquals(
                    base.foreground.closestColor(),
                    heading.foreground.closestColor(),
                    "h$level must inherit its surface's text color rather than recoloring itself"
                )
            }
            assertTrue(
                base.through(H1Semantic).font.weight > base.font.weight,
                "an h1 must carry more weight than body text"
            )
            assertTrue(
                base.through(H1Semantic).font.size > base.through(H4Semantic).font.size,
                "the type scale must still descend across header levels"
            )
        }
    }

    // -------------------------------------------------------------------------
    // The signature rule: an outline means "you can act on this"
    // -------------------------------------------------------------------------

    @Test
    fun surfaceSemantics_drawNoOutline() = runTest {
        val theme = light("light-no-lines")
        val surfaces: List<Pair<String, Array<Semantic>>> = listOf(
            "page" to arrayOf(),
            "outer" to arrayOf(OuterSemantic),
            "content" to arrayOf(MainContentSemantic),
            "bar" to arrayOf(BarSemantic),
            "nav" to arrayOf(NavSemantic),
            "card" to arrayOf(CardSemantic),
            "card>card" to arrayOf(CardSemantic, CardSemantic),
            "card>inset" to arrayOf(CardSemantic, InsetSemantic),
            "card>embedded" to arrayOf(CardSemantic, EmbeddedSemantic),
            "card>codeBlock" to arrayOf(CardSemantic, CodeBlockSemantic),
            "card>blockquote" to arrayOf(CardSemantic, BlockquoteSemantic),
            "dialog" to arrayOf(DialogSemantic),
            "popover" to arrayOf(PopoverSemantic),
            "important" to arrayOf(ImportantSemantic),
            "danger" to arrayOf(DangerSemantic),
        )
        for ((name, semantics) in surfaces) {
            assertEquals(
                0.dp, theme.through(*semantics).outlineWidth,
                "$name is a surface, not a control - it must separate by light, not by a line"
            )
        }
    }

    @Test
    fun interactiveSemantics_drawAnOutline() = runTest {
        val theme = light("light-lines")
        val card = theme.through(CardSemantic)
        val controls: List<Pair<String, Semantic>> = listOf(
            "field" to FieldSemantic,
            "selected" to SelectedSemantic,
            "focus" to FocusSemantic,
            "invalid" to InvalidSemantic,
        )
        for ((name, semantic) in controls) {
            assertTrue(
                card.through(semantic).outlineWidth > 0.dp,
                "$name is something you can act on - it must be outlined so the outline stays meaningful"
            )
        }
    }

    /**
     * A card rises to paper and casts a shadow; a card that had to recess instead (because it was
     * already on paper) must not, since a tray sunk into a sheet does not float above it.
     */
    @Test
    fun onlyRisingSurfaces_castShadows() = runTest {
        val theme = light("light-shadows")
        val card = theme.through(CardSemantic)
        val nested = theme.through(CardSemantic, CardSemantic)

        assertTrue(card.elevation > 0.dp, "a card lifted off the page must cast a shadow")
        assertEquals(0.dp, nested.elevation, "a card that recessed into a well must not cast a shadow")
        assertEquals(0.dp, theme.elevation, "the page itself must not cast a shadow")
        assertTrue(
            theme.through(DialogSemantic).elevation > card.elevation,
            "a dialog must sit further off the page than a card"
        )
    }

    /** The neutrals are tints of the accent, so an app's identity reaches the canvas. */
    @Test
    fun pageSurface_carriesTheAccentHue() = runTest {
        val accent = Color.fromHexString("#7A3E9D")
        val theme = light("light-hue", accent = accent)
        val page = theme.background.closestColor()

        assertTrue(
            (page.toHSP().hue angleTo accent.toHSP().hue).turns.absoluteValue < 0.02f,
            "the page must be a tint of the accent hue, not neutral gray"
        )
        assertTrue(page.perceivedBrightness > 0.9f, "the page must still read as a light surface")
    }
}
