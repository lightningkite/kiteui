package com.lightningkite.kiteui.models

import com.lightningkite.kiteui.testing.BaseUiTest
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Exercises [SemanticOverrides] end to end: how an override is looked up, how override sets
 * merge and propagate down a derivation chain, and what happens when overrides nest - an
 * override for one semantic installing a *different* override for another inside its own subtree.
 *
 * Two things make this worth covering densely rather than by inspection:
 *
 * 1. **The lookup is reflective.** `derive` falls back from `Key.Instance(semantic)` to
 *    `Key.Type(semantic::class)`, so correctness rests on `KClass` equality and hashCode behaving
 *    as map keys on every target. Kotlin/JS implements `KClass.hashCode()` as
 *    `simpleName.hashCode()` (0 for anonymous objects, and shared by two unrelated classes that
 *    happen to have the same simple name), which is a legal but lossy hash - so the type-key
 *    tests here deliberately include same-simple-name classes and anonymous-object semantics.
 *    These tests run on every platform, JS included; that is the point of putting them in
 *    commonTest rather than next to the JVM ones.
 *
 * 2. **Overrides compose by nesting, and nesting is where the ordering is easy to get wrong.**
 *    An override lambda receives the theme that owns the override set, so anything it derives
 *    inside itself sees the same overrides - unless it installs new ones, which must apply to its
 *    subtree only and must not leak back out to the theme it was derived from.
 *
 * What is *not* covered here: whether the derived theme reaches the screen. On the HTML targets a
 * theme is delivered as a single CSS class named after [Theme.id], and overrides are not part of
 * that id - see `SemanticOverridesCssTest` in jsTest for that half.
 */
class SemanticOverridesTest : BaseUiTest() {

    // -------------------------------------------------------------------------
    // Fixtures
    //
    // Every semantic's default paints a distinct, recognisable background so a
    // test can tell "the override ran" from "the default ran" by colour alone.
    // -------------------------------------------------------------------------

    private companion object {
        val defaultColor: Color = Color.fromHex(0x11111111)
        val red: Color = Color.fromHex(0xFFFF0000.toInt())
        val green: Color = Color.fromHex(0xFF00FF00.toInt())
        val blue: Color = Color.fromHex(0xFF0000FF.toInt())
        val yellow: Color = Color.fromHex(0xFFFFFF00.toInt())
        val cyan: Color = Color.fromHex(0xFF00FFFF.toInt())
    }

    private object S1 : Semantic("so-s1") {
        override fun default(theme: Theme): ThemeAndBack = theme.withBack(background = defaultColor)
    }

    private object S2 : Semantic("so-s2") {
        override fun default(theme: Theme): ThemeAndBack = theme.withBack(background = defaultColor)
    }

    private object S3 : Semantic("so-s3") {
        override fun default(theme: Theme): ThemeAndBack = theme.withBack(background = defaultColor)
    }

    /** A semantic with several instances, for separating instance keys from type keys. */
    private data class FlavorSemantic(val flavor: String) : Semantic("so-flav-$flavor") {
        override fun default(theme: Theme): ThemeAndBack = theme.withBack(background = defaultColor)
    }

    /** A second, unrelated multi-instance semantic type, so a type key can be shown to be narrow. */
    private data class SpiceSemantic(val spice: String) : Semantic("so-spice-$spice") {
        override fun default(theme: Theme): ThemeAndBack = theme.withBack(background = defaultColor)
    }

    private open class OpenSemantic : Semantic("so-open") {
        override fun default(theme: Theme): ThemeAndBack = theme.withBack(background = defaultColor)
    }

    private class SubSemantic : OpenSemantic()

    /**
     * Two distinct classes that share a simple name. On Kotlin/JS these two have the *same*
     * `KClass.hashCode()`, so they land in one hash bucket and are separated only by
     * `KClass.equals`. Type-keyed overrides must still tell them apart.
     */
    private object ScopeA {
        class Twin : Semantic("so-twin-a") {
            override fun default(theme: Theme): ThemeAndBack = theme.withBack(background = defaultColor)
        }
    }

    private object ScopeB {
        class Twin : Semantic("so-twin-b") {
            override fun default(theme: Theme): ThemeAndBack = theme.withBack(background = defaultColor)
        }
    }

    private var themeSerial = 0

    /** A fresh base theme per call, so no test can be affected by another test's [Theme] memoization. */
    private fun base(): Theme = Theme(id = "so-base-${themeSerial++}", background = Color.white)

    private fun Theme.with(vararg overrides: Semantic.Override<*>): Theme =
        customize(newId = "$id-c${themeSerial++}", semanticOverrides = SemanticOverrides(overrides.toList()))

    private val ThemeAndBack.background: Paint get() = theme.background

    // =========================================================================
    // 1. Lookup: instance key, type key, and the fallback to default
    // =========================================================================

    @Test
    fun noOverride_usesTheSemanticsOwnDefault() = runTest {
        val theme = base()
        assertEquals(defaultColor, theme[S1].background, "with no override registered, S1.default must run")
    }

    @Test
    fun instanceOverride_replacesTheDefault() = runTest {
        val theme = base().with(S1.override { it.withBack(background = red) })
        assertEquals(red, theme[S1].background, "the registered instance override must run instead of S1.default")
    }

    @Test
    fun overrideForOneSemantic_leavesOtherSemanticsAlone() = runTest {
        val theme = base().with(S1.override { it.withBack(background = red) })
        assertEquals(red, theme[S1].background, "S1 is overridden")
        assertEquals(defaultColor, theme[S2].background, "S2 was never overridden and must still use its default")
        assertEquals(defaultColor, theme[S3].background, "S3 was never overridden and must still use its default")
    }

    @Test
    fun typeOverride_appliesToEveryInstanceOfThatType() = runTest {
        val theme = base().with(override<FlavorSemantic> { it.withBack(background = red) })
        assertEquals(red, theme[FlavorSemantic("a")].background, "type override must cover instance 'a'")
        assertEquals(red, theme[FlavorSemantic("b")].background, "type override must cover instance 'b'")
        assertEquals(red, theme[FlavorSemantic("z")].background, "type override must cover any instance")
    }

    @Test
    fun typeOverride_doesNotBleedIntoAnUnrelatedSemanticType() = runTest {
        val theme = base().with(override<FlavorSemantic> { it.withBack(background = red) })
        assertEquals(
            defaultColor, theme[SpiceSemantic("a")].background,
            "a FlavorSemantic type override must not touch SpiceSemantic, whatever their instances look like",
        )
    }

    @Test
    fun instanceOverride_winsOverATypeOverrideForTheSameInstance() = runTest {
        val a = FlavorSemantic("a")
        val theme = base().with(
            a.override { it.withBack(background = green) },
            override<FlavorSemantic> { it.withBack(background = red) },
        )
        assertEquals(green, theme[a].background, "the instance key must be consulted before the type key")
        assertEquals(red, theme[FlavorSemantic("b")].background, "other instances still fall through to the type key")
    }

    @Test
    fun instanceOverride_winsRegardlessOfDeclarationOrder() = runTest {
        val a = FlavorSemantic("a")
        val theme = base().with(
            override<FlavorSemantic> { it.withBack(background = red) },
            a.override { it.withBack(background = green) },
        )
        assertEquals(
            green, theme[a].background,
            "instance-over-type is a lookup rule, not a declaration-order rule",
        )
    }

    @Test
    fun instanceOverride_matchesByEquality_notByReference() = runTest {
        // FlavorSemantic is a data class, so a freshly built FlavorSemantic("a") is a different
        // object but an equal key. Callers routinely construct semantics inline at the use site.
        val theme = base().with(FlavorSemantic("a").override { it.withBack(background = red) })
        assertEquals(
            red, theme[FlavorSemantic("a")].background,
            "an instance override must match an equal semantic, not only the exact object registered",
        )
        assertEquals(
            defaultColor, theme[FlavorSemantic("b")].background,
            "and must not match an unequal instance of the same type",
        )
    }

    @Test
    fun lastOverrideWinsWhenTheSameKeyIsRegisteredTwice() = runTest {
        val theme = base().with(
            S1.override { it.withBack(background = red) },
            S1.override { it.withBack(background = green) },
        )
        assertEquals(green, theme[S1].background, "a later entry for the same key must replace the earlier one")
    }

    // =========================================================================
    // 2. Reflective (type) keys: the parts that depend on KClass behaving
    // =========================================================================

    @Test
    fun typeOverride_isExactClass_andDoesNotCoverSubclasses() = runTest {
        // `derive` looks up Key.Type(semantic::class) - the runtime class, with no walk up the
        // hierarchy. This test pins that down as the contract rather than an accident.
        val theme = base().with(override<OpenSemantic> { it.withBack(background = red) })
        assertEquals(red, theme[OpenSemantic()].background, "the exact class matches")
        assertEquals(
            defaultColor, theme[SubSemantic()].background,
            "a subclass does NOT inherit its superclass's type override - the key is the runtime class",
        )
    }

    @Test
    fun typeOverride_onASubclass_appliesToThatSubclassOnly() = runTest {
        val theme = base().with(override<SubSemantic> { it.withBack(background = red) })
        assertEquals(red, theme[SubSemantic()].background, "the subclass's own type override applies")
        assertEquals(defaultColor, theme[OpenSemantic()].background, "and does not reach back up to the superclass")
    }

    @Test
    fun typeOverrides_separateTwoClassesThatShareASimpleName() = runTest {
        // On Kotlin/JS these two KClasses hash identically (hashCode is simpleName-based), so this
        // is the test that catches a lookup that leans on the hash instead of on equality.
        val theme = base().with(
            override<ScopeA.Twin> { it.withBack(background = red) },
            override<ScopeB.Twin> { it.withBack(background = green) },
        )
        assertEquals(red, theme[ScopeA.Twin()].background, "ScopeA.Twin must get its own override")
        assertEquals(
            green, theme[ScopeB.Twin()].background,
            "ScopeB.Twin must get its own override, even though it shares a simple name with ScopeA.Twin",
        )
    }

    @Test
    fun instanceOverride_worksOnAnAnonymousObjectSemantic() = runTest {
        // An anonymous object's KClass reports a blank simpleName on JS, hashing to 0.
        val anon = object : Semantic("so-anon") {
            override fun default(theme: Theme): ThemeAndBack = theme.withBack(background = defaultColor)
        }
        val theme = base().with(anon.override { it.withBack(background = red) })
        assertEquals(red, theme[anon].background, "an instance override must work for an anonymous-object semantic")
    }

    @Test
    fun twoAnonymousObjectSemantics_doNotShareAnOverride() = runTest {
        val first = object : Semantic("so-anon-1") {
            override fun default(theme: Theme): ThemeAndBack = theme.withBack(background = defaultColor)
        }
        val second = object : Semantic("so-anon-2") {
            override fun default(theme: Theme): ThemeAndBack = theme.withBack(background = defaultColor)
        }
        val theme = base().with(first.override { it.withBack(background = red) })
        assertEquals(red, theme[first].background, "the overridden anonymous semantic")
        assertEquals(
            defaultColor, theme[second].background,
            "a second anonymous semantic hashes the same way but must not pick up the first one's override",
        )
    }

    @Test
    fun instanceKeyAndTypeKeyForTheSameSemanticAreDistinctMapEntries() = runTest {
        // Both key kinds live in one map, and each is a single-property data class, so their
        // hashCodes can coincide. Only equals separates them.
        val a = FlavorSemantic("a")
        val theme = base().with(
            a.override { it.withBack(background = green) },
            override<FlavorSemantic> { it.withBack(background = red) },
        )
        assertEquals(green, theme[a].background, "the instance entry survived alongside the type entry")
        assertEquals(red, theme[FlavorSemantic("q")].background, "the type entry survived alongside the instance entry")
    }

    @Test
    fun typeOverrideReceivesTheActualInstance_soItCanReadItsProperties() = runTest {
        // The map stores the derivation with its receiver type erased and casts it back unchecked.
        // If the cast were ever wrong, reading a property off the receiver is where it would show.
        var seen: String? = null
        val theme = base().with(
            override<FlavorSemantic> { t ->
                seen = flavor
                t.withBack(background = if (flavor == "hot") red else blue)
            }
        )
        assertEquals(red, theme[FlavorSemantic("hot")].background, "the override read flavor=='hot' off its receiver")
        assertEquals("hot", seen, "the receiver handed to a type override is the semantic instance being derived")
        assertEquals(blue, theme[FlavorSemantic("mild")].background, "and it sees the right instance each time")
    }

    // =========================================================================
    // 3. Combining override sets: plus, customize, copy
    // =========================================================================

    @Test
    fun plus_keepsNonConflictingEntriesFromBothSides() = runTest {
        val combined = SemanticOverrides(S1.override { it.withBack(background = red) }) +
            SemanticOverrides(S2.override { it.withBack(background = green) })
        val theme = base().customize(newId = "so-plus-${themeSerial++}", semanticOverrides = combined)
        assertEquals(red, theme[S1].background, "the left side's S1 override survived the merge")
        assertEquals(green, theme[S2].background, "the right side's S2 override survived the merge")
    }

    @Test
    fun plus_rightSideWinsOnAConflict() = runTest {
        val combined = SemanticOverrides(S1.override { it.withBack(background = red) }) +
            SemanticOverrides(S1.override { it.withBack(background = green) })
        val theme = base().customize(newId = "so-plusc-${themeSerial++}", semanticOverrides = combined)
        assertEquals(green, theme[S1].background, "on the same key, the right-hand operand must win")
    }

    @Test
    fun plus_withEmpty_isIdentityInBothDirections() = runTest {
        val overrides = SemanticOverrides(S1.override { it.withBack(background = red) })
        val leftEmpty = base().customize(newId = "so-le-${themeSerial++}", semanticOverrides = SemanticOverrides.EMPTY + overrides)
        val rightEmpty = base().customize(newId = "so-re-${themeSerial++}", semanticOverrides = overrides + SemanticOverrides.EMPTY)
        assertEquals(red, leftEmpty[S1].background, "EMPTY + x must behave as x")
        assertEquals(red, rightEmpty[S1].background, "x + EMPTY must behave as x")
    }

    @Test
    fun plus_doesNotMutateEitherOperand() = runTest {
        val left = SemanticOverrides(S1.override { it.withBack(background = red) })
        val right = SemanticOverrides(S1.override { it.withBack(background = green) })
        left + right
        val fromLeft = base().customize(newId = "so-im-${themeSerial++}", semanticOverrides = left)
        assertEquals(red, fromLeft[S1].background, "merging must not write back into the left operand")
    }

    @Test
    fun customize_mergesTheParentsOverridesWithTheNewOnes() = runTest {
        val parent = base().with(S1.override { it.withBack(background = red) })
        val child = parent.customize(
            newId = "so-cust-${themeSerial++}",
            semanticOverrides = SemanticOverrides(S2.override { it.withBack(background = green) }),
        )
        assertEquals(red, child[S1].background, "the parent's S1 override is inherited")
        assertEquals(green, child[S2].background, "the new S2 override is added")
    }

    @Test
    fun customize_newOverridesWinOverInheritedOnesForTheSameKey() = runTest {
        val parent = base().with(S1.override { it.withBack(background = red) })
        val child = parent.customize(
            newId = "so-custw-${themeSerial++}",
            semanticOverrides = SemanticOverrides(S1.override { it.withBack(background = green) }),
        )
        assertEquals(green, child[S1].background, "the override passed to customize must replace the inherited one")
        assertEquals(red, parent[S1].background, "and the parent must be left as it was")
    }

    @Test
    fun copy_carriesOverridesToTheDerivedTheme() = runTest {
        val parent = base().with(S1.override { it.withBack(background = red) })
        val derived = parent.copy(id = "d")
        assertEquals(red, derived[S1].background, "a plain copy keeps the parent's overrides")
    }

    @Test
    fun copy_mergesAdditionalOverridesOnTopOfTheInheritedOnes() = runTest {
        val parent = base().with(S1.override { it.withBack(background = red) })
        val derived = parent.copy(
            id = "d",
            semanticOverrides = SemanticOverrides(S2.override { it.withBack(background = green) }),
        )
        assertEquals(red, derived[S1].background, "inherited S1 override still applies")
        assertEquals(green, derived[S2].background, "added S2 override applies")
        assertEquals(defaultColor, parent[S2].background, "and the parent did not gain the S2 override")
    }

    @Test
    fun overridesSurviveAChainOfSemanticDerivations() = runTest {
        val theme = base().with(
            S1.override { it.withBack(background = red) },
            S2.override { it.withBack(background = green) },
        )
        val twoDeep = theme[S2].theme[S2].theme
        assertEquals(
            red, twoDeep[S1].background,
            "an override registered at the root must still be found several derivations down",
        )
    }

    // =========================================================================
    // 4. Nested overrides - one override installing another for its own subtree
    //
    // This is the case the whole feature turns on: S1 and S2 are both overridden
    // at the top, and S2's override installs a *different* S1 override that must
    // apply inside S2's subtree and nowhere else.
    // =========================================================================

    /** S1 -> red at the top; S2 -> blue, and S1 -> green inside S2. */
    private fun nestedTheme(): Theme = base().with(
        S1.override { it.withBack(background = red) },
        S2.override {
            it.withBack(
                background = blue,
                semanticOverrides = SemanticOverrides(S1.override { inner -> inner.withBack(background = green) }),
            )
        },
    )

    @Test
    fun nested_outerSemanticKeepsTheOuterOverride() = runTest {
        assertEquals(red, nestedTheme()[S1].background, "outside S2, S1 must still be red")
    }

    @Test
    fun nested_theOverridingSemanticItselfApplies() = runTest {
        assertEquals(blue, nestedTheme()[S2].background, "S2's own override must apply")
    }

    @Test
    fun nested_innerOverrideWinsInsideTheSubtree() = runTest {
        val theme = nestedTheme()
        assertEquals(
            green, theme[S2].theme[S1].background,
            "inside S2's subtree, S2's nested S1 override must beat the outer S1 override",
        )
    }

    @Test
    fun nested_bothOverridesApplyInTheSameTraversal() = runTest {
        // The failure this guards against is a lookup that resolves one of the two and silently
        // drops the other, which is invisible if you only ever check them one at a time.
        val theme = nestedTheme()
        val outerS1 = theme[S1].background
        val s2 = theme[S2].background
        val innerS1 = theme[S2].theme[S1].background
        assertEquals(red, outerS1, "outer S1")
        assertEquals(blue, s2, "S2")
        assertEquals(green, innerS1, "S1 nested under S2")
        assertEquals(3, setOf(outerS1, s2, innerS1).size, "all three derivations must be distinguishable")
    }

    @Test
    fun nested_innerOverrideDoesNotLeakBackToTheParentTheme() = runTest {
        val theme = nestedTheme()
        theme[S2].theme[S1]   // force the nested derivation first
        assertEquals(
            red, theme[S1].background,
            "deriving inside S2 must not rewrite the parent theme's S1 override",
        )
    }

    @Test
    fun nested_innerOverrideDoesNotLeakIntoASiblingSubtree() = runTest {
        val theme = base().with(
            S1.override { it.withBack(background = red) },
            S2.override {
                it.withBack(
                    background = blue,
                    semanticOverrides = SemanticOverrides(S1.override { inner -> inner.withBack(background = green) }),
                )
            },
            S3.override { it.withBack(background = yellow) },
        )
        assertEquals(green, theme[S2].theme[S1].background, "S1 is green under S2")
        assertEquals(red, theme[S3].theme[S1].background, "but S1 is still red under S3 - the sibling subtree")
    }

    @Test
    fun nested_orderOfEvaluationDoesNotChangeTheResult() = runTest {
        // Theme memoizes each semantic's result, so an override that resolved wrongly on first
        // touch would stay wrong. Evaluate in both orders on two identical themes.
        val innerFirst = nestedTheme()
        val innerFirstInner = innerFirst[S2].theme[S1].background
        val innerFirstOuter = innerFirst[S1].background

        val outerFirst = nestedTheme()
        val outerFirstOuter = outerFirst[S1].background
        val outerFirstInner = outerFirst[S2].theme[S1].background

        assertEquals(innerFirstOuter, outerFirstOuter, "outer S1 must not depend on what was derived first")
        assertEquals(innerFirstInner, outerFirstInner, "inner S1 must not depend on what was derived first")
        assertEquals(red, outerFirstOuter, "outer S1 is red either way")
        assertEquals(green, outerFirstInner, "inner S1 is green either way")
    }

    @Test
    fun nested_threeLevelsDeepResolveIndependently() = runTest {
        // S3 installs an S2 override, which itself installs an S1 override.
        val theme = base().with(
            S1.override { it.withBack(background = red) },
            S2.override { it.withBack(background = blue) },
            S3.override {
                it.withBack(
                    background = yellow,
                    semanticOverrides = SemanticOverrides(
                        S2.override { lvl2 ->
                            lvl2.withBack(
                                background = cyan,
                                semanticOverrides = SemanticOverrides(
                                    S1.override { lvl1 -> lvl1.withBack(background = green) }
                                ),
                            )
                        }
                    ),
                )
            },
        )
        assertEquals(red, theme[S1].background, "level 0: S1 is red")
        assertEquals(blue, theme[S2].background, "level 0: S2 is blue")
        assertEquals(yellow, theme[S3].background, "level 1: S3 is yellow")
        assertEquals(cyan, theme[S3].theme[S2].background, "level 2: S2 under S3 is cyan, not blue")
        assertEquals(red, theme[S3].theme[S1].background, "level 2: S1 under S3 is untouched, still red")
        assertEquals(green, theme[S3].theme[S2].theme[S1].background, "level 3: S1 under S3>S2 is green")
    }

    @Test
    fun nested_anOverrideMayBuildOnAnotherOverriddenSemantic() = runTest {
        // S2's override derives S1 and builds on the result. The S1 it sees must be the
        // overridden one, not S1.default.
        val theme = base().with(
            S1.override { it.withBack(background = red, outlineWidth = 3.px) },
            S2.override { it[S1].theme.withBack(background = blue) },
        )
        val s2 = theme[S2].theme
        assertEquals(blue, s2.background, "S2's own background wins on top")
        assertEquals(
            3.px, s2.outlineWidth,
            "S2 built on the overridden S1 (which set outlineWidth), not on S1.default",
        )
    }

    @Test
    fun nested_anOverrideMayDelegateEntirelyToAnotherSemantic() = runTest {
        val theme = base().with(
            S1.override { it.withBack(background = red) },
            S2.override { it[S1] },
        )
        assertEquals(red, theme[S2].background, "S2 delegating to S1 must pick up S1's override")
    }

    @Test
    fun nested_anOverrideMayFallBackToTheSemanticsOwnDefault() = runTest {
        // `default` stays reachable from inside an override, which is how a conditional
        // override opts out for the cases it does not care about.
        val theme = base().with(
            S1.override { t -> if (t.background == Color.white) t.withBack(background = red) else default(t) }
        )
        assertEquals(red, theme[S1].background, "the condition held, so the override's own branch ran")
        val darkened = theme.copy(id = "dark", background = Color.black)
        assertEquals(defaultColor, darkened[S1].background, "the condition failed, so it fell back to S1.default")
    }

    @Test
    fun nested_typeOverrideInstalledByAnInstanceOverride() = runTest {
        val theme = base().with(
            override<FlavorSemantic> { it.withBack(background = red) },
            S2.override {
                it.withBack(
                    background = blue,
                    semanticOverrides = SemanticOverrides(override<FlavorSemantic> { f -> f.withBack(background = green) }),
                )
            },
        )
        assertEquals(red, theme[FlavorSemantic("x")].background, "outside S2, the outer type override applies")
        assertEquals(
            green, theme[S2].theme[FlavorSemantic("x")].background,
            "inside S2, the nested type override replaces it",
        )
    }

    @Test
    fun nested_instanceOverrideInsideASubtreeStillBeatsAnInheritedTypeOverride() = runTest {
        val hot = FlavorSemantic("hot")
        val theme = base().with(
            override<FlavorSemantic> { it.withBack(background = red) },
            S2.override {
                it.withBack(
                    background = blue,
                    semanticOverrides = SemanticOverrides(hot.override { f -> f.withBack(background = green) }),
                )
            },
        )
        val inside = theme[S2].theme
        assertEquals(green, inside[hot].background, "the subtree's instance override wins for 'hot'")
        assertEquals(red, inside[FlavorSemantic("mild")].background, "other flavors still use the inherited type override")
    }

    // =========================================================================
    // 5. Memoization - Theme[semantic] caches, and the cache must not be shared
    // =========================================================================

    @Test
    fun deriving_theSameSemanticTwice_returnsTheIdenticalResult() = runTest {
        val theme = base().with(S1.override { it.withBack(background = red) })
        assertSame(theme[S1], theme[S1], "Theme memoizes per semantic, so repeated derivation returns one object")
    }

    @Test
    fun overrideLambdaRunsOnceForRepeatedDerivations() = runTest {
        var runs = 0
        val theme = base().with(S1.override { runs++; it.withBack(background = red) })
        repeat(5) { theme[S1] }
        assertEquals(1, runs, "the memoized result must be reused rather than re-running the override")
    }

    @Test
    fun theCacheIsPerTheme_soDifferentOverridesGiveDifferentResults() = runTest {
        val redTheme = base().with(S1.override { it.withBack(background = red) })
        val greenTheme = base().with(S1.override { it.withBack(background = green) })
        assertEquals(red, redTheme[S1].background, "the red theme resolves S1 to red")
        assertEquals(green, greenTheme[S1].background, "the green theme resolves S1 to green - no cross-theme cache")
    }

    @Test
    fun memoizationSurvivesInterleavedDerivations() = runTest {
        val theme = nestedTheme()
        val first = theme[S1]
        theme[S2]
        theme[S2].theme[S1]
        theme[S3]
        assertSame(first, theme[S1], "unrelated derivations must not evict or replace a cached entry")
        assertEquals(red, theme[S1].background, "and the cached entry must still be the overridden one")
    }

    // =========================================================================
    // 6. ThemeAndBack flags coming out of an override
    // =========================================================================

    @Test
    fun overrideControlsTheBackgroundAndPaddingFlags() = runTest {
        val theme = base().with(
            S1.override { it.withBack(background = red) },
            S2.override { it.withBackNoPadding },
            S3.override { it.withoutBack },
        )
        assertTrue(theme[S1].drawBackground, "withBack draws a background")
        assertTrue(theme[S1].padding, "withBack pads")
        assertTrue(theme[S2].drawBackground, "withBackNoPadding draws a background")
        assertFalse(theme[S2].padding, "withBackNoPadding does not pad")
        assertFalse(theme[S3].drawBackground, "withoutBack draws no background")
        assertFalse(theme[S3].padding, "withoutBack does not pad")
    }

    @Test
    fun anOverrideReturningTheThemeUnchangedIsAValidNoOp() = runTest {
        val theme = base().with(S1.override { it.withoutBack })
        assertSame(theme, theme[S1].theme, "an override may decline to derive a new theme at all")
        assertEquals(theme.background, theme[S1].background, "and the theme is then genuinely unchanged")
    }

    @Test
    fun anOverriddenSemanticStillIdsItsDerivedThemeAfterTheSemanticKey() = runTest {
        // The derived id is what the HTML targets key their generated CSS class off, so it is
        // worth pinning: the override changes the theme's content but not its naming scheme.
        val theme = base().with(S1.override { it.withBack(background = red) })
        assertEquals("${theme.id}-${S1.key}", theme[S1].theme.id, "an override's result is still named <parent>-<key>")
    }

    // =========================================================================
    // 7. The identity trap: overrides are not part of Theme.id or Theme.equals
    //
    // Theme equality is id-only, and SemanticOverrides do not contribute to the id.
    // On the HTML targets the id *is* the theme as far as the DOM is concerned, so two
    // themes that share an id and differ only in overrides render identically - the
    // second one's overrides appear to do nothing. structurallyEquals is what backs
    // Theme.Debugger's collision check, so it has to be able to see the difference.
    // =========================================================================

    @Test
    fun twoThemesSharingAnIdCompareEqual_evenWithDifferentOverrides() = runTest {
        val plain = Theme(id = "so-identity")
        val overridden = Theme(id = "so-identity", semanticOverrides = SemanticOverrides(S1.override { it.withBack(background = red) }))
        assertEquals(plain, overridden, "Theme.equals is id-only by design")
        assertEquals(plain.hashCode(), overridden.hashCode(), "and so is hashCode")
    }

    @Test
    fun structurallyEquals_seesADifferenceInSemanticOverrides() = runTest {
        val plain = Theme(id = "so-se-1")
        val overridden = Theme(id = "so-se-1", semanticOverrides = SemanticOverrides(S1.override { it.withBack(background = red) }))
        assertFalse(
            plain.structurallyEquals(overridden),
            "themes with different overrides do not render the same, so the collision check must flag them",
        )
    }

    @Test
    fun structurallyEquals_isUnbotheredByEqualOverrideSets() = runTest {
        val overrides = SemanticOverrides(S1.override { it.withBack(background = red) })
        val a = Theme(id = "so-se-2", semanticOverrides = overrides)
        val b = Theme(id = "so-se-2", semanticOverrides = overrides)
        assertTrue(a.structurallyEquals(b), "re-registering the same theme under the same id is the normal case")
    }

    @Test
    fun structurallyEquals_treatsEmptyOverrideSetsAsEqual() = runTest {
        val a = Theme(id = "so-se-3", foreground = Color.red)
        val b = Theme(id = "so-se-3", foreground = Color.red)
        assertTrue(a.structurallyEquals(b), "two themes with no overrides at all must still compare structurally equal")
    }

    @Test
    fun debugger_catchesAnIdCollisionThatDiffersOnlyInOverrides() = runTest {
        // Without this, the only symptom is a JS-only wrong colour at runtime.
        Theme.Debugger.reset()
        Theme.Debugger.checkIdCollisions = true
        try {
            Theme(id = "so-collide")
            assertFailsWith<IllegalStateException>(
                "an id reused with a different override set must be reported, not silently aliased",
            ) {
                Theme(id = "so-collide", semanticOverrides = SemanticOverrides(S1.override { it.withBack(background = red) }))
            }
        } finally {
            Theme.Debugger.checkIdCollisions = false
            Theme.Debugger.reset()
        }
    }

    @Test
    fun copyWithAndWithoutOverrides_produceTheSameId() = runTest {
        // Pinning the hazard so a future change to the id scheme is a deliberate one: copy chains
        // ids from the semantic/derivation name only, so the override set is invisible in the id.
        val parent = base()
        val withoutOverrides = parent.copy(id = "variant")
        val withOverrides = parent.copy(
            id = "variant",
            semanticOverrides = SemanticOverrides(S1.override { it.withBack(background = red) }),
        )
        assertEquals(
            withoutOverrides.id, withOverrides.id,
            "copy ids do not encode the override set - two variants of one id are distinguishable only by " +
                "structurallyEquals, which is why Theme.Debugger.checkIdCollisions exists",
        )
        assertEquals(defaultColor, withoutOverrides[S1].background, "the two are genuinely different themes")
        assertEquals(red, withOverrides[S1].background, "even though they answer to the same id")
    }

    // =========================================================================
    // 8. Deprecated entry point still routes into the same lookup
    // =========================================================================

    @Suppress("DEPRECATION")
    @Test
    fun semanticOverridesOf_registersInstanceOverrides() = runTest {
        val theme = base().customize(
            newId = "so-legacy-${themeSerial++}",
            semanticOverrides = semanticOverridesOf(S1 to { t -> t.withBack(background = red) }),
        )
        assertEquals(red, theme[S1].background, "the deprecated builder must still register an instance override")
    }
}
