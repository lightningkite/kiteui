package com.lightningkite.kiteui.models

import com.lightningkite.kiteui.testing.BaseUiTest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Locks in what makes [Theme.shadCnLike] read as shadcn rather than as a generic flat theme: near
 * neutral grays, a border where a shadow would be, and chrome that stays out of the way.
 *
 * The nav assertions are the reason this file exists. The nav components pack their items with
 * `gap = 0`, so a theme that gives an item a background and a border - the stock behavior of
 * [UnselectedSemantic] - turns a sidebar into a stack of seamed boxes, and one that leaves the
 * corner radius adaptive squares them off completely. Both are locked down below.
 */
class ShadCnLikeThemeTest : BaseUiTest() {

    private val lightCanvas = Color.fromHexString("#FAFAFA")

    private fun light(id: String, accent: Color? = null) =
        if (accent == null) Theme.shadCnLike(id = id, background = lightCanvas)
        else Theme.shadCnLike(id = id, background = lightCanvas, accent = accent)

    private fun dark(id: String, accent: Color? = null) =
        if (accent == null) Theme.shadCnLike(id = id)
        else Theme.shadCnLike(id = id, accent = accent)

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
    // Legibility, in both polarities
    // -------------------------------------------------------------------------

    @Test
    fun everySurfaceSemantic_keepsTextLegible() = runTest {
        val paths: List<Pair<String, Array<Semantic>>> = listOf(
            "page" to arrayOf(),
            "outer" to arrayOf(OuterSemantic),
            "content" to arrayOf(MainContentSemantic),
            "bar" to arrayOf(BarSemantic),
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
            "card>label" to arrayOf(CardSemantic, TableHeaderSemantic),
            "card>h1" to arrayOf(CardSemantic, H1Semantic),
            "card>hover" to arrayOf(CardSemantic, HoverSemantic),
            "card>down" to arrayOf(CardSemantic, DownSemantic),
            "card>selected" to arrayOf(CardSemantic, SelectedSemantic),
            "dialog" to arrayOf(DialogSemantic),
            "popover" to arrayOf(PopoverSemantic),
            "important" to arrayOf(ImportantSemantic),
            "important>hover" to arrayOf(ImportantSemantic, HoverSemantic),
            "important>selected" to arrayOf(ImportantSemantic, SelectedSemantic),
            "important>subtext" to arrayOf(ImportantSemantic, SubtextSemantic),
            "critical" to arrayOf(CriticalSemantic),
            "warning" to arrayOf(WarningSemantic),
            "danger" to arrayOf(DangerSemantic),
            "affirmative" to arrayOf(AffirmativeSemantic),
        )
        for ((name, semantics) in paths) {
            assertLegible(light("shadcn-legible-light").through(*semantics), "light: $name")
            assertLegible(dark("shadcn-legible-dark").through(*semantics), "dark: $name")
        }
    }

    @Test
    fun errorText_isLegibleOnEverySurfaceItLandsOn() = runTest {
        for ((mode, theme) in listOf("light" to light("shadcn-err-l"), "dark" to dark("shadcn-err-d"))) {
            assertLegible(theme.through(ErrorSemantic), "$mode: error")
            assertLegible(theme.through(CardSemantic, ErrorSemantic), "$mode: card>error")
            assertLegible(theme.through(CardSemantic, FieldSemantic, ErrorSemantic), "$mode: card>field>error")
        }
    }

    // -------------------------------------------------------------------------
    // Nav items: the failure this theme was rewritten to fix
    // -------------------------------------------------------------------------

    /**
     * A nav list sets `gap = 0`, so an unselected item that paints a background or an outline shows
     * up as a box butted against its neighbors. Only the selected one may fill.
     */
    @Test
    fun unselectedNavItem_paintsNothing() = runTest {
        for ((mode, theme) in listOf("light" to light("shadcn-uns-l"), "dark" to dark("shadcn-uns-d"))) {
            val nav = theme.through(NavSemantic)
            val unselected = nav.withoutBack + UnselectedSemantic

            assertEquals(false, unselected.drawBackground, "$mode: an inactive nav item must not fill")
            assertEquals(true, unselected.padding, "$mode: an inactive nav item still needs its padding")
            assertEquals(0.dp, unselected.theme.outlineWidth, "$mode: an inactive nav item must not be outlined")
            assertEquals(
                nav.foreground.closestColor(), unselected.theme.foreground.closestColor(),
                "$mode: an inactive destination is still a destination - it keeps full-strength text"
            )
        }
    }

    /**
     * Corner radius is normally derived from the container's spacing, which is zero in a nav list.
     * Items have to pin their own radius or they come out as squares.
     */
    @Test
    fun navItems_roundThemselves_ratherThanFollowingTheirContainer() = runTest {
        for ((mode, theme) in listOf("light" to light("shadcn-rad-l"), "dark" to dark("shadcn-rad-d"))) {
            for (semantic in listOf(SelectedSemantic, UnselectedSemantic)) {
                val radii = theme.through(NavSemantic, semantic).cornerRadii
                assertTrue(
                    radii is CornerRadii.Fixed && radii.value > 0.dp,
                    "$mode: ${semantic.key} must pin a fixed radius, not inherit a zero-gap container's; got $radii"
                )
            }
        }
    }

    @Test
    fun selectedNavItem_readsAsSelected() = runTest {
        for ((mode, theme) in listOf("light" to light("shadcn-sel-l"), "dark" to dark("shadcn-sel-d"))) {
            val nav = theme.through(NavSemantic)
            val selected = nav.through(SelectedSemantic)

            assertNotEquals(
                nav.background.closestColor(), selected.background.closestColor(),
                "$mode: a selected item must be distinguishable from the nav behind it"
            )
            assertEquals(0.dp, selected.outlineWidth, "$mode: selection is a fill, never a border")
            assertTrue(selected.font.weight > nav.font.weight, "$mode: the active item also carries more weight")
        }
    }

    // -------------------------------------------------------------------------
    // The palette reads as neutral
    // -------------------------------------------------------------------------

    /**
     * The whole look rests on the chrome being gray. shadcn's zinc ramp sits within a couple of
     * points of neutral, so the theme's surfaces have to as well - the earlier version forced a
     * fixed 20% saturation onto every derived surface, which is what turned its navs lavender.
     */
    @Test
    fun chromeStaysNeutral_evenWithASaturatedAccent() = runTest {
        // Chrome only. Selection and hover are deliberately a wash of accent - they are things you
        // can act on, which is the one thing the accent is for.
        val surfaces = listOf(
            "nav" to arrayOf<Semantic>(NavSemantic),
            "bar" to arrayOf<Semantic>(BarSemantic),
            "card" to arrayOf<Semantic>(CardSemantic),
            "card>card" to arrayOf<Semantic>(CardSemantic, CardSemantic),
            "dialog" to arrayOf<Semantic>(DialogSemantic),
        )
        val themes = listOf(
            "light" to light("shadcn-neutral-l", accent = Color.fromHexString("#2563EB")),
            "dark" to dark("shadcn-neutral-d", accent = Color.fromHexString("#2563EB")),
        )
        for ((mode, theme) in themes) for ((name, semantics) in surfaces) {
            val color = theme.through(*semantics).background.closestColor()
            // Absolute channel spread, not HSP saturation: HSP measures chroma relative to the
            // brightest channel, so it reads 0.18 on shadcn's own near-black #09090B, which is two
            // points off neutral. Spread says what the eye sees at any brightness.
            val spread = maxOf(color.red, color.green, color.blue) - minOf(color.red, color.green, color.blue)
            assertTrue(
                spread < 0.045f,
                "$mode: $name must stay near-neutral beside a saturated accent, but its channels " +
                    "spread $spread apart"
            )
        }
    }

    /** The one place the accent is allowed to be loud. */
    @Test
    fun theAccentIsSpentOnActionsAndFocus() = runTest {
        val accent = Color.fromHexString("#2563EB")
        val theme = light("shadcn-accent", accent = accent)

        assertEquals(
            accent, theme.through(ImportantSemantic).background.closestColor(),
            "the primary action is the accent, undiluted"
        )
        assertEquals(
            accent, theme.through(CardSemantic, FocusSemantic).outline.closestColor(),
            "the focus ring is the accent too - both are things you can act on"
        )
    }

    // -------------------------------------------------------------------------
    // Structure: a border where another theme would put a shadow
    // -------------------------------------------------------------------------

    @Test
    fun surfacesAreBordered_andOnlyFloatingThingsCastShadows() = runTest {
        for ((mode, theme) in listOf("light" to light("shadcn-str-l"), "dark" to dark("shadcn-str-d"))) {
            for ((name, semantic) in listOf(
                "card" to CardSemantic,
                "field" to FieldSemantic,
                "dialog" to DialogSemantic,
                "popover" to PopoverSemantic,
            )) {
                assertTrue(
                    theme.through(semantic).outlineWidth > 0.dp,
                    "$mode: $name separates by a hairline border"
                )
            }
            assertEquals(0.dp, theme.elevation, "$mode: the page casts no shadow")
            assertEquals(0.dp, theme.through(CardSemantic).elevation, "$mode: a card is bordered, not raised")
            assertTrue(
                theme.through(DialogSemantic).elevation > 0.dp,
                "$mode: a dialog genuinely floats, so it is the exception"
            )
        }
    }

    /** A surface has to change when you stack another one on it, at every depth. */
    @Test
    fun card_changesTheSurfaceItSitsOn_atEveryNestingDepth() = runTest {
        for ((mode, theme) in listOf("light" to light("shadcn-lad-l"), "dark" to dark("shadcn-lad-d"))) {
            val page = theme.background.closestColor()
            val card = theme.through(CardSemantic).background.closestColor()
            val nested = theme.through(CardSemantic, CardSemantic).background.closestColor()
            val deep = theme.through(CardSemantic, CardSemantic, CardSemantic).background.closestColor()

            assertNotEquals(page, card, "$mode: a card must not be the color of the page behind it")
            assertNotEquals(card, nested, "$mode: a card inside a card must separate from its parent")
            assertNotEquals(nested, deep, "$mode: the third level must still separate from the second")
        }
    }

    @Test
    fun interactionStates_areVisible_andPressingIsStrongerThanHovering() = runTest {
        for ((mode, theme) in listOf("light" to light("shadcn-int-l"), "dark" to dark("shadcn-int-d"))) {
            val card = theme.through(CardSemantic)
            val surface = card.background.closestColor()
            val hovered = card.through(HoverSemantic).background.closestColor()
            val pressed = card.through(DownSemantic).background.closestColor()

            assertNotEquals(surface, hovered, "$mode: hover must be visible on a card")
            assertTrue(
                (pressed channelDifferenceSum surface) > (hovered channelDifferenceSum surface),
                "$mode: pressing must move the surface further than hovering does"
            )
        }
    }

    /**
     * A strong fill has nowhere to wash toward the accent - it *is* the accent - so its states move
     * back toward the canvas instead. Without that, a primary button gives no feedback at all.
     */
    @Test
    fun interactionStates_areVisibleOnASolidFill() = runTest {
        for ((mode, theme) in listOf("light" to light("shadcn-fill-l"), "dark" to dark("shadcn-fill-d"))) {
            val important = theme.through(ImportantSemantic)
            assertNotEquals(
                important.background.closestColor(),
                important.through(HoverSemantic).background.closestColor(),
                "$mode: hovering the primary action must do something"
            )
        }
    }

    /**
     * `critical` is `important` applied twice, so with a palette that holds one primary it has to
     * land somewhere visibly different rather than stacking the same fill on itself.
     */
    @Test
    fun critical_isDistinguishableFromImportant() = runTest {
        for ((mode, theme) in listOf("light" to light("shadcn-crit-l"), "dark" to dark("shadcn-crit-d"))) {
            val important = theme.through(ImportantSemantic).background.closestColor()
            val critical = theme.through(CriticalSemantic).background.closestColor()
            assertTrue(
                (important channelDifferenceSum critical) > 0.5f,
                "$mode: critical must not collapse into important; got $important vs $critical"
            )
        }
    }

    // -------------------------------------------------------------------------
    // Type
    // -------------------------------------------------------------------------

    /** 14px body with semibold, tightly tracked headings is most of what dates a screen as shadcn. */
    @Test
    fun typeIsSmallAndHeadingsAreSemibold() = runTest {
        val theme = light("shadcn-type")
        assertEquals(0.875.rem, theme.font.size, "body text is shadcn's `text-sm`")
        for (level in 1..6) {
            val heading = theme.through(HeaderSizeSemantic(level))
            assertTrue(heading.font.weight >= 600, "h$level must be semibold")
            assertEquals(
                theme.foreground.closestColor(), heading.foreground.closestColor(),
                "h$level builds hierarchy from size and weight, never from the accent"
            )
        }
        assertTrue(
            theme.through(H1Semantic).font.size > theme.through(H4Semantic).font.size,
            "the type scale still descends across header levels"
        )
    }
}
