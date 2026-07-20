package com.lightningkite.kiteui.models

import com.lightningkite.kiteui.testing.BaseUiTest
import kotlin.test.*
import kotlinx.coroutines.test.runTest

/**
 * Locks in the core model-level invariants for the theming system.
 *
 * Covers: ThemeAndBack flag OR-combining, SemanticOverrides lookup precedence,
 * Theme.copy revert propagation, Theme equality/hashCode identity contract,
 * and Theme[semantic] memoization.
 *
 * Invariants that require element-level observation (e.g. the
 * "same cascading theme → no child refresh" optimization, full ThemePipeline
 * ordering, or themeAndBack deduplication on the element tree) are NOT tested
 * here — they need an ElementWriter / NativeElement test harness.
 */
class ThemeTest: BaseUiTest() {

    // -------------------------------------------------------------------------
    // Fixtures
    // -------------------------------------------------------------------------

    private fun baseTheme(id: String = "base") = Theme(id = id)

    /** Singleton semantic whose default always returns withoutBack (no background, no padding). */
    private object NoBackSemantic : Semantic("noback-test") {
        override fun default(theme: Theme): ThemeAndBack = theme.withoutBack
    }

    /**
     * Class-based semantic with multiple instances, used to distinguish
     * instance-level vs type-level SemanticOverride precedence.
     */
    private class FlavorSemantic(val flavor: String) : Semantic("flav-test-$flavor") {
        override fun default(theme: Theme): ThemeAndBack = theme.withoutBack
    }

    // -------------------------------------------------------------------------
    // 1. ThemeAndBack.plus — drawBackground and padding flags are OR-combined
    //
    // The contract (from ThemeAndBack.plus): the result's drawBackground is
    // (existing.drawBackground || derivation.drawBackground), and similarly
    // for padding. Neither side can suppress what the other enabled.
    // -------------------------------------------------------------------------

    @Test
    fun themeAndBack_plus_bothFlagsOff_resultHasNoBackground() = runTest {
        val theme = baseTheme()
        val start = theme.withoutBack                       // drawBackground=false, padding=false
        val derivation = ThemeDerivation { it.withoutBack } // also false, false

        val result = start + derivation

        assertFalse(result.drawBackground, "drawBackground should remain false when both sides are false")
        assertFalse(result.padding, "padding should remain false when both sides are false")
    }

    @Test
    fun themeAndBack_plus_currentDrawBackground_survivesNoBackDerivation() = runTest {
        val theme = baseTheme()
        val start = theme.withBack                          // drawBackground=true
        val derivation = ThemeDerivation { it.withoutBack } // drawBackground=false

        val result = start + derivation

        assertTrue(
            result.drawBackground,
            "drawBackground already set in the existing ThemeAndBack must OR through even if the derivation returns false"
        )
    }

    @Test
    fun themeAndBack_plus_derivationDrawBackground_survivesNoBackStart() = runTest {
        val theme = baseTheme()
        val start = theme.withoutBack                      // drawBackground=false
        val derivation = ThemeDerivation { it.withBack }   // drawBackground=true

        val result = start + derivation

        assertTrue(
            result.drawBackground,
            "drawBackground set by the derivation must OR through even when the existing ThemeAndBack had none"
        )
    }

    @Test
    fun themeAndBack_plus_currentPaddingOnly_survivesNoBackDerivation() = runTest {
        val theme = baseTheme()
        val start = theme.withoutBackButPadding                    // drawBackground=false, padding=true
        val derivation = ThemeDerivation { it.withoutBack }        // padding=false

        val result = start + derivation

        assertFalse(result.drawBackground, "drawBackground should remain false")
        assertTrue(
            result.padding,
            "padding already set in the existing ThemeAndBack must OR through even if the derivation returns false"
        )
    }

    @Test
    fun themeAndBack_plus_derivationPaddingOnly_survivesNoBackStart() = runTest {
        val theme = baseTheme()
        val start = theme.withoutBack                              // padding=false
        val derivation = ThemeDerivation { it.withoutBackButPadding } // padding=true

        val result = start + derivation

        assertFalse(result.drawBackground, "drawBackground should remain false")
        assertTrue(
            result.padding,
            "padding set by the derivation must OR through even when the existing ThemeAndBack had none"
        )
    }

    @Test
    fun themeDerivationSet_alwaysProducesBackground() = runTest {
        // ThemeRules.md: "Switching themes will cause a background / card."
        val target = baseTheme("target")
        val derivation = ThemeDerivation.Set(target)

        val result = derivation.invoke(baseTheme("other"))

        assertTrue(result.drawBackground, "ThemeDerivation.Set must always produce drawBackground=true")
        assertEquals(target, result.theme, "resulting theme must be the target theme")
    }

    @Test
    fun themeDerivationNone_producesNoFlagsAndSameTheme() = runTest {
        val theme = baseTheme()

        val result = ThemeDerivation.None.invoke(theme)

        assertFalse(result.drawBackground, "ThemeDerivation.None must not add drawBackground")
        assertFalse(result.padding, "ThemeDerivation.None must not add padding")
        assertEquals(theme, result.theme, "ThemeDerivation.None must not change the theme")
    }

    // -------------------------------------------------------------------------
    // 2. SemanticOverrides — lookup precedence: instance > type > default
    //
    // derive(key, theme) checks:
    //   1. Instance key for the exact semantic object
    //   2. Type key for key::class
    //   3. Falls back to key.default(theme)
    // -------------------------------------------------------------------------

    @Test
    fun semanticOverrides_noOverride_fallsBackToDefault() = runTest {
        val theme = baseTheme()

        val result = theme[NoBackSemantic]

        assertFalse(result.drawBackground, "with no override, default (withoutBack) should be used")
    }

    @Test
    fun semanticOverrides_typeOverride_appliesWhenNoInstanceOverride() = runTest {
        val flavorB = FlavorSemantic("b")
        // Type-level override for all FlavorSemantic: return withBack (drawBackground=true)
        val theme = baseTheme().customize(
            newId = "test-type",
            semanticOverrides = SemanticOverrides(
                override<FlavorSemantic> { t -> t.withBack }
            )
        )

        val result = theme[flavorB]

        assertTrue(
            result.drawBackground,
            "type-level override should apply to any FlavorSemantic instance that has no specific instance override"
        )
    }

    @Test
    fun semanticOverrides_instanceOverride_winsOverTypeOverride() = runTest {
        val flavorA = FlavorSemantic("a")
        val flavorB = FlavorSemantic("b")
        // Instance override for flavorA → withBackNoPadding (back=true, padding=false)
        // Type override for all FlavorSemantic → withBack (back=true, padding=true)
        val theme = baseTheme().customize(
            newId = "test-prec",
            semanticOverrides = SemanticOverrides(
                flavorA.override { t -> t.withBackNoPadding },
                override<FlavorSemantic> { t -> t.withBack }
            )
        )

        val resultA = theme[flavorA]
        val resultB = theme[flavorB]

        assertTrue(resultA.drawBackground, "instance override for flavorA: drawBackground should be true")
        assertFalse(
            resultA.padding,
            "instance override for flavorA must win over the type override — padding should be false (withBackNoPadding)"
        )

        assertTrue(resultB.drawBackground, "flavorB falls through to type override: drawBackground should be true")
        assertTrue(
            resultB.padding,
            "flavorB has no instance override, so the type override (withBack) applies — padding should be true"
        )
    }

    @Test
    fun semanticOverrides_plus_rightSideWinsOnConflict() = runTest {
        val leftOverrides = SemanticOverrides(
            CardSemantic.override { t -> t.withoutBack }   // left: suppress background
        )
        val rightOverrides = SemanticOverrides(
            CardSemantic.override { t -> t.withBack }      // right: require background
        )
        val combined = leftOverrides + rightOverrides

        val theme = baseTheme().customize(newId = "test-plus", semanticOverrides = combined)
        val result = theme[CardSemantic]

        assertTrue(result.drawBackground, "right-side override should win when the same key is present in both operands")
    }

    // -------------------------------------------------------------------------
    // 3. Theme.copy — revert field propagation
    //
    // cascading=false: revert is set to (this.revert ?: this) — i.e. the earliest
    //                  ancestor in the non-cascading chain.
    // cascading=true:  revert = this.revert?.copy(...) — changes propagate through
    //                  the revert, or revert stays null when there is none.
    // -------------------------------------------------------------------------

    @Test
    fun theme_copy_nonCascading_setsRevertToCurrentTheme() = runTest {
        val base = baseTheme("base")

        val derived = base.copy(id = "nc", cascading = false)

        assertNotNull(derived.revert, "a non-cascading copy must set the revert field")
        assertEquals(base, derived.revert, "revert must point back to the original theme")
    }

    @Test
    fun theme_copy_nonCascading_preservesEarliestRevert() = runTest {
        val original = baseTheme("orig")
        val first = original.copy(id = "first", cascading = false)
        // first.revert == original

        val second = first.copy(id = "second", cascading = false)
        // second.revert = first.revert ?: first = original, NOT first

        assertEquals(
            original, second.revert,
            "a second non-cascading copy must preserve the earliest revert, not replace it with the immediate parent"
        )
    }

    @Test
    fun theme_copy_cascading_withNoExistingRevert_revertIsNull() = runTest {
        val base = baseTheme("base") // revert = null by default

        val derived = base.copy(id = "cc", cascading = true)

        assertNull(derived.revert, "cascading copy with no prior revert must leave revert as null")
    }

    @Test
    fun theme_copy_cascading_withExistingRevert_propagatesSameChangesToRevert() = runTest {
        val original = baseTheme("orig")
        val withRevert = original.copy(id = "nc", cascading = false)
        // withRevert.revert == original

        val cascadedChange = withRevert.copy(id = "cc", cascading = true, background = Color.red)
        // revert = original.copy(id="cc", cascading=true, background=Color.red)
        // so cascadedChange.revert.background == Color.red

        assertNotNull(cascadedChange.revert, "cascading copy with an existing revert must propagate it")
        assertEquals(
            Color.red,
            cascadedChange.revert!!.background.closestColor(),
            "cascading property changes must also be applied to the revert theme"
        )
    }

    // -------------------------------------------------------------------------
    // 4. Theme identity — equals/hashCode are strictly id-based
    //
    // Two Theme instances are equal iff their ids are equal, regardless of all
    // other properties. Derived themes gain new ids via id-chaining, making
    // them distinct from their parents.
    // -------------------------------------------------------------------------

    @Test
    fun theme_sameId_differentProperties_areEqual() = runTest {
        val t1 = Theme(id = "same-id")
        val t2 = Theme(id = "same-id", foreground = Color.red, background = Color.black)

        assertEquals(t1, t2, "themes with the same id must be equal even if all other properties differ")
        assertEquals(t1.hashCode(), t2.hashCode(), "equal themes must produce equal hashCodes")
    }

    @Test
    fun theme_differentId_areNotEqual() = runTest {
        val t1 = Theme(id = "alpha")
        val t2 = Theme(id = "beta")

        assertNotEquals(t1, t2, "themes with different ids must not be equal")
    }

    @Test
    fun theme_copy_producesChainedIdAndIsDistinctFromParent() = runTest {
        val base = baseTheme("base")

        val derived = base.copy(id = "crd")

        assertEquals("base-crd", derived.id, "Theme.copy must chain the suffix onto the parent id with a hyphen")
        assertNotEquals(base, derived, "derived theme must not equal its parent because their ids differ")
    }

    @Test
    fun theme_customize_usesExactIdWithoutChaining() = runTest {
        val base = baseTheme("base")

        val customized = base.customize(newId = "my-custom")

        assertEquals(
            "my-custom", customized.id,
            "Theme.customize must use the provided newId exactly — it must not prepend or append the parent id"
        )
    }

    @Test
    fun theme_customize_mergesParentSemanticOverrides() = runTest {
        // Base has an override for CardSemantic (suppress background).
        val base = baseTheme().customize(
            newId = "base-with-card-override",
            semanticOverrides = SemanticOverrides(
                CardSemantic.override { t -> t.withoutBack }
            )
        )
        // Customize without touching semanticOverrides.
        val derived = base.customize(newId = "derived")

        val result = derived[CardSemantic]

        assertFalse(
            result.drawBackground,
            "customize must preserve the parent's semanticOverrides when none are explicitly provided"
        )
    }

    // -------------------------------------------------------------------------
    // 5. Semantic derivation caching — theme[semantic] is memoized per (theme, semantic)
    //
    // Theme uses an internal HashMap. Applying the same Semantic to the same
    // Theme twice must return the identical ThemeAndBack instance.
    // -------------------------------------------------------------------------

    @Test
    fun theme_semanticGet_returnsCachedInstance_onRepeatLookup() = runTest {
        val theme = baseTheme()

        val first = theme[CardSemantic]
        val second = theme[CardSemantic]

        assertSame(first, second, "theme[semantic] must return the identical cached instance on a second call")
    }

    @Test
    fun theme_semanticGet_cardSemantic_producesBackground() = runTest {
        val theme = baseTheme()

        val result = theme[CardSemantic]

        assertTrue(result.drawBackground, "CardSemantic.default returns withBack — drawBackground must be true")
        assertTrue(result.padding, "CardSemantic.default returns withBack — padding must be true")
    }

    @Test
    fun theme_semanticGet_buttonSemantic_producesNoBackground() = runTest {
        val theme = baseTheme()

        val result = theme[ButtonSemantic]

        assertFalse(result.drawBackground, "ButtonSemantic.default returns withoutBack — drawBackground must be false")
    }

    @Test
    fun theme_semanticGet_derivedTheme_hasItsOwnCacheIndependentOfParent() = runTest {
        val parent = baseTheme("parent")
        val child = parent.copy(id = "child")

        val parentResult = parent[CardSemantic]
        val childResult = child[CardSemantic]

        // Both should produce a background, but the embedded theme objects differ.
        assertTrue(parentResult.drawBackground)
        assertTrue(childResult.drawBackground)
        assertNotSame(
            parentResult, childResult,
            "parent and child each have their own semantic cache — the same lookup must not return the same instance"
        )
        assertNotEquals(
            parentResult.theme, childResult.theme,
            "the ThemeAndBack returned for the child must embed the child theme, not the parent"
        )
    }
}
