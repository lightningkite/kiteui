package com.lightningkite.kiteui.models

import com.lightningkite.kiteui.testing.BaseUiTest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * What makes each built-in theme *itself*, as opposed to the shared contract in
 * [BuiltInThemeContractTest].
 *
 * These are the traits someone would name if asked to tell the design languages apart at a glance,
 * so they are the ones worth pinning: get one of them wrong and the theme stops being a credible
 * rendition of the thing it is named after, however correct its contrast ratios are.
 */
class BuiltInThemeIdentityTest : BaseUiTest() {

    private val accent = Color.fromHexString("#2563EB")
    private val lightCanvas = Color.fromHexString("#FAFAFA")
    private val darkCanvas = Color.fromHexString("#121212")

    private fun Theme.through(vararg semantics: Semantic): Theme =
        semantics.fold(withoutBack) { acc, semantic -> acc + semantic }.theme

    // -------------------------------------------------------------------------
    // Material 2
    // -------------------------------------------------------------------------

    /** The primary-colored app bar is the single most recognizable Material 2 element. */
    @Test
    fun material_paintsTheAppBarWithThePrimary_inLightOnly() = runTest {
        val light = Theme.material("m2-bar-l", lightCanvas, accent)
        assertEquals(
            accent, light.through(BarSemantic).background.closestColor(),
            "a light Material 2 app bar is the primary color"
        )

        // The spec drops it in dark mode: a saturated field that size overwhelms a near-black UI.
        val dark = Theme.material("m2-bar-d", darkCanvas, accent)
        assertNotEquals(
            accent, dark.through(BarSemantic).background.closestColor(),
            "a dark Material 2 app bar is a surface, not the primary color"
        )
    }

    /**
     * Dark Material 2 separates surfaces with a white overlay that rises with elevation, because a
     * shadow cast onto near-black cannot be seen. The dialog sits at 24dp and the card at 1dp, so
     * the dialog has to come out visibly lighter.
     */
    @Test
    fun material_usesElevationOverlaysInDark() = runTest {
        val theme = Theme.material("m2-overlay", darkCanvas, accent)
        val page = theme.background.closestColor()
        val card = theme.through(CardSemantic).background.closestColor()
        val dialog = theme.through(DialogSemantic).background.closestColor()

        assertTrue(card.perceivedBrightness > page.perceivedBrightness, "a card is lifted off the page")
        assertTrue(
            dialog.perceivedBrightness > card.perceivedBrightness,
            "a dialog sits far higher than a card, so its overlay is stronger"
        )
    }

    /** Caps-and-tracking on actions is the trait people recognize first. */
    @Test
    fun material_setsActionsInCapsAndDestinationsInSentenceCase() = runTest {
        val theme = Theme.material("m2-caps", lightCanvas, accent)

        val action = theme.through(ClickableSemantic)
        assertTrue(action.font.allCaps, "a Material 2 action is set in caps")
        assertTrue(action.font.additionalLetterSpacing > 0.px, "and tracked out")

        for (semantic in listOf(SelectedSemantic, UnselectedSemantic)) {
            assertEquals(
                false, theme.through(NavSemantic, ClickableSemantic, semantic).font.allCaps,
                "a nav destination is a place, not an action - ${semantic.key} opts back out of caps"
            )
        }
    }

    /** A primary too dark for a dark canvas is lightened toward its 200 tone, as the spec asks. */
    @Test
    fun material_rescuesADarkPrimaryOnADarkCanvas() = runTest {
        val deep = Color.fromHexString("#6200EE")
        val fill = Theme.material("m2-rescue", darkCanvas, deep).through(ImportantSemantic)
            .background.closestColor()

        assertTrue(
            fill.perceivedBrightness > deep.perceivedBrightness + 0.15f,
            "Material's own #6200EE has to come out as a lilac on a dark canvas, not as itself"
        )
        assertTrue(
            (fill.toHSP().hue angleTo deep.toHSP().hue).turns.let { it * it } < 0.0025f,
            "but it is still the same hue - a lighter tone, not a different color"
        )
    }

    // -------------------------------------------------------------------------
    // Material 3
    // -------------------------------------------------------------------------

    /**
     * The tonal palette is the whole idea: even the grays are low-chroma tints of the source color,
     * which is why a Material 3 screen looks tinted all over rather than colored only on controls.
     */
    @Test
    fun material3_growsItsNeutralsFromTheAccentHue() = runTest {
        val theme = Theme.material3("m3-tonal", lightCanvas, accent)
        for ((name, surface) in listOf("page" to theme, "card" to theme.through(CardSemantic))) {
            val color = surface.background.closestColor()
            assertTrue(
                (color.toHSP().hue angleTo accent.toHSP().hue).turns.let { it * it } < 0.0025f,
                "$name must be a tint of the source hue, not a neutral gray"
            )
        }
    }

    /** Primary lands at tone 40 in light and tone 80 in dark - light text on dark, and the reverse. */
    @Test
    fun material3_flipsThePrimaryToneBetweenPolarities() = runTest {
        val light = Theme.material3("m3-tone-l", lightCanvas, accent).through(ImportantSemantic)
        val dark = Theme.material3("m3-tone-d", darkCanvas, accent).through(ImportantSemantic)

        assertTrue(
            light.background.closestColor().perceivedBrightness < 0.6f,
            "a light theme's primary is a tone-40 fill carrying white text"
        )
        assertTrue(
            dark.background.closestColor().perceivedBrightness > 0.6f,
            "a dark theme's primary is a tone-80 fill carrying dark text"
        )
    }

    /** Tone replaces elevation: a Material 3 card is a fill, and casts nothing. */
    @Test
    fun material3_separatesByToneRatherThanShadow() = runTest {
        val theme = Theme.material3("m3-flat", lightCanvas, accent)
        assertEquals(0.dp, theme.through(CardSemantic).elevation, "a Material 3 card casts no shadow")
        assertEquals(0.dp, theme.through(BarSemantic).elevation, "nor does a top app bar")
        assertNotEquals(
            theme.background.closestColor(), theme.through(CardSemantic).background.closestColor(),
            "so the card has to separate by tone instead"
        )
    }

    /** The fully-rounded selection indicator is Material 3's loudest signature. */
    @Test
    fun material3_roundsControlsIntoPills() = runTest {
        val theme = Theme.material3("m3-pill", lightCanvas, accent)
        for ((name, semantic) in listOf("selection" to SelectedSemantic, "the primary button" to ImportantSemantic)) {
            val radii = theme.through(NavSemantic, semantic).cornerRadii
            assertTrue(
                radii is CornerRadii.Fixed && radii.value >= 16.dp,
                "$name is a pill in Material 3; got $radii"
            )
        }
    }

    /** Material 3 reverses Material 2 on caps, and that reversal is what dates a screen to one or the other. */
    @Test
    fun material3_setsActionsInSentenceCase() = runTest {
        val theme = Theme.material3("m3-case", lightCanvas, accent)
        assertEquals(false, theme.through(ClickableSemantic).font.allCaps, "Material 3 does not shout")
    }

    // -------------------------------------------------------------------------
    // Clean (Apple)
    // -------------------------------------------------------------------------

    /** Gray behind, white in front - the inversion that makes a screen read as iOS. */
    @Test
    fun clean_putsContentInCellsOnAGrayPage() = runTest {
        val theme = Theme.clean("ios-cells")
        val page = theme.background.closestColor()
        val cell = theme.through(CardSemantic).background.closestColor()

        assertTrue(cell.perceivedBrightness > page.perceivedBrightness, "a cell is brighter than the page it sits on")
        assertEquals(Color.white, cell, "and in light mode that cell is white")
    }

    /** True black in dark mode, so OLED panels switch those pixels off. */
    @Test
    fun clean_usesTrueBlackInDark() = runTest {
        val theme = Theme.clean("ios-black", background = Color.black)
        assertEquals(Color.black, theme.background.closestColor(), "the dark page is true black")
        assertTrue(
            theme.through(CardSemantic).background.closestColor().perceivedBrightness > 0.05f,
            "but cells lift off it"
        )
    }

    /** Bars are a blurred material rather than a fill - the detail most imitations skip. */
    @Test
    fun clean_makesBarsTranslucentAndBlurred() = runTest {
        val theme = Theme.clean("ios-bars")
        for ((name, semantic) in listOf("the app bar" to BarSemantic, "the nav" to NavSemantic)) {
            val bar = theme.through(semantic)
            assertTrue(bar.blurBackground > 0.px, "$name is blurred")
            assertTrue(
                bar.background.closestColor().alpha < 1f,
                "$name lets what is behind it show through"
            )
            assertEquals(0.dp, bar.elevation, "$name is separated by a hairline, not a shadow")
        }
    }

    /** Continuous curvature - the squircle - rather than circular arcs. */
    @Test
    fun clean_usesContinuousCorners() = runTest {
        assertEquals(CornerShape.Continuous, Theme.clean("ios-corners").cornerShape)
    }

    /** One tint, spent only on things you can act on. */
    @Test
    fun clean_spendsTheTintOnlyOnInteraction() = runTest {
        val theme = Theme.clean("ios-tint", accent = accent)
        assertEquals(
            accent, theme.through(ImportantSemantic).background.closestColor(),
            "the prominent button is the tint"
        )
        assertEquals(
            accent, theme.through(CardSemantic, FocusSemantic).outline.closestColor(),
            "so is the focus ring"
        )
        // Chrome stays neutral: the tint never touches a surface you cannot press.
        for ((name, semantic) in listOf("the page" to null, "the nav" to NavSemantic, "a cell" to CardSemantic)) {
            val surface = if (semantic == null) theme else theme.through(semantic)
            val color = surface.background.closestColor()
            val chroma = maxOf(color.red, color.green, color.blue) - minOf(color.red, color.green, color.blue)
            assertTrue(chroma < 0.05f, "$name must stay neutral, but its channels spread $chroma apart")
        }
    }
}
