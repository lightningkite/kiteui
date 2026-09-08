package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.Element
import com.lightningkite.kiteui.views.KiteUiCss
import com.lightningkite.kiteui.views.direct.col
import com.lightningkite.kiteui.views.direct.text
import com.lightningkite.kiteui.views.native
import com.lightningkite.kiteui.views.themed
import org.w3c.dom.HTMLElement
import kotlin.test.*

/**
 * The half of the semantic-override story that only exists on the HTML targets: whether an
 * override that resolved correctly in the model actually reaches the browser.
 *
 * On web a theme is not carried on the element - it is delivered as a single generated CSS class
 * named `t-<Theme.id>`, and the CSS behind that class is generated once, the first time a theme
 * with that id is rendered. So on JS **[Theme.id] is the theme's identity**, and
 * [Theme.semanticOverrides] are not part of it. Two themes that share an id therefore share one
 * class: whichever renders first defines it, and the other one's overrides silently do nothing -
 * which reads, on screen, as the theme "reverting to the default". That failure mode does not
 * exist on Android/iOS/JVM, where the theme object itself drives drawing, which is why it is easy
 * to hit only on web.
 *
 * [com.lightningkite.kiteui.models.SemanticOverridesTest] covers the resolution rules on every
 * platform; these tests cover the trip from a resolved theme to a computed style.
 */
class SemanticOverridesCssTest {

    private object S1 : Semantic("css-s1") {
        override fun default(theme: Theme): ThemeAndBack = theme.withBack(background = defaultColor)
    }

    private object S2 : Semantic("css-s2") {
        override fun default(theme: Theme): ThemeAndBack = theme.withBack(background = defaultColor)
    }

    private object S3 : Semantic("css-s3") {
        override fun default(theme: Theme): ThemeAndBack = theme.withBack(background = defaultColor)
    }

    private data class FlavorSemantic(val flavor: String) : Semantic("css-flav-$flavor") {
        override fun default(theme: Theme): ThemeAndBack = theme.withBack(background = defaultColor)
    }

    private companion object {
        val defaultColor: Color = Color.fromHex(0xFF808080.toInt())
        val red: Color = Color.fromHex(0xFFFF0000.toInt())
        val green: Color = Color.fromHex(0xFF00FF00.toInt())
        val blue: Color = Color.fromHex(0xFF0000FF.toInt())
        val yellow: Color = Color.fromHex(0xFFFFFF00.toInt())

        const val DEFAULT_RGB = "rgb(128, 128, 128)"
        const val RED_RGB = "rgb(255, 0, 0)"
        const val GREEN_RGB = "rgb(0, 255, 0)"
        const val BLUE_RGB = "rgb(0, 0, 255)"
        const val YELLOW_RGB = "rgb(255, 255, 0)"

        var serial = 0
    }

    /** A distinct id per mount, so one test's generated CSS can never answer another test's query. */
    private fun freshTheme(vararg overrides: Semantic.Override<*>): Theme =
        Theme(id = "cssTestBase${serial++}", background = Color.white)
            .customize(newId = "cssTestApp${serial++}", semanticOverrides = SemanticOverrides(overrides.toList()))

    private val Element.backgroundColor: String
        get() = kotlinx.browser.window.getComputedStyle(native.element as HTMLElement).backgroundColor

    private val Element.classes: String
        get() = (native.element as HTMLElement).className

    // -------------------------------------------------------------------------
    // 1. An override reaches the DOM at all
    // -------------------------------------------------------------------------

    @Test
    fun withoutAnOverride_theSemanticsDefaultIsWhatGetsPainted() {
        val theme = freshTheme()
        lateinit var el: Element
        root(theme) { col { themed(S1).col { el = this; text("x") } } }
        assertEquals(DEFAULT_RGB, el.backgroundColor, "with no override, S1.default must be what reaches the browser")
    }

    @Test
    fun anInstanceOverrideReachesTheBrowser() {
        val theme = freshTheme(S1.override { it.withBack(background = red) })
        lateinit var el: Element
        root(theme) { col { themed(S1).col { el = this; text("x") } } }
        assertEquals(RED_RGB, el.backgroundColor, "the override, not S1.default, must be painted")
    }

    @Test
    fun aTypeOverrideReachesTheBrowser() {
        val theme = freshTheme(override<FlavorSemantic> { it.withBack(background = red) })
        lateinit var a: Element
        lateinit var b: Element
        root(theme) {
            col {
                themed(FlavorSemantic("a")).col { a = this; text("a") }
                themed(FlavorSemantic("b")).col { b = this; text("b") }
            }
        }
        assertEquals(RED_RGB, a.backgroundColor, "type override covers instance 'a'")
        assertEquals(RED_RGB, b.backgroundColor, "type override covers instance 'b'")
    }

    @Test
    fun anInstanceOverrideBeatsATypeOverrideInTheRenderedCss() {
        val hot = FlavorSemantic("hot")
        val theme = freshTheme(
            hot.override { it.withBack(background = green) },
            override<FlavorSemantic> { it.withBack(background = red) },
        )
        lateinit var hotEl: Element
        lateinit var mildEl: Element
        root(theme) {
            col {
                themed(hot).col { hotEl = this; text("hot") }
                themed(FlavorSemantic("mild")).col { mildEl = this; text("mild") }
            }
        }
        assertEquals(GREEN_RGB, hotEl.backgroundColor, "the instance override wins for 'hot'")
        assertEquals(RED_RGB, mildEl.backgroundColor, "'mild' falls through to the type override")
    }

    @Test
    fun eachOverriddenSemanticGetsItsOwnGeneratedClass() {
        val theme = freshTheme(
            S1.override { it.withBack(background = red) },
            S2.override { it.withBack(background = blue) },
        )
        lateinit var one: Element
        lateinit var two: Element
        root(theme) {
            col {
                themed(S1).col { one = this; text("1") }
                themed(S2).col { two = this; text("2") }
            }
        }
        assertTrue(one.classes.contains("t-${theme[S1].theme.id}"), "S1's element carries the class for S1's theme id")
        assertTrue(two.classes.contains("t-${theme[S2].theme.id}"), "S2's element carries the class for S2's theme id")
        assertNotEquals(one.backgroundColor, two.backgroundColor, "and the two classes must paint differently")
    }

    // -------------------------------------------------------------------------
    // 2. Nested overrides survive the trip to CSS
    //
    // S1 -> red at the top; S2 -> blue, and S1 -> green inside S2. All three have to
    // land on three different elements in one render pass.
    // -------------------------------------------------------------------------

    private fun nestedTheme(): Theme = freshTheme(
        S1.override { it.withBack(background = red) },
        S2.override {
            it.withBack(
                background = blue,
                semanticOverrides = SemanticOverrides(S1.override { inner -> inner.withBack(background = green) }),
            )
        },
    )

    @Test
    fun nestedOverrides_allThreeLandInTheSameRender() {
        val theme = nestedTheme()
        lateinit var outerS1: Element
        lateinit var s2: Element
        lateinit var innerS1: Element
        root(theme) {
            col {
                themed(S1).col { outerS1 = this; text("outer s1") }
                themed(S2).col {
                    s2 = this
                    text("s2")
                    themed(S1).col { innerS1 = this; text("inner s1") }
                }
            }
        }
        assertEquals(RED_RGB, outerS1.backgroundColor, "S1 outside S2 is red")
        assertEquals(BLUE_RGB, s2.backgroundColor, "S2 is blue")
        assertEquals(GREEN_RGB, innerS1.backgroundColor, "S1 nested inside S2 is green, not red and not the default")
    }

    @Test
    fun nestedOverrides_theInnerAndOuterFormsOfOneSemanticGetDifferentClasses() {
        // If both resolved to the same theme id they would share one generated class and the
        // inner one would silently take the outer one's colour.
        val theme = nestedTheme()
        lateinit var outerS1: Element
        lateinit var innerS1: Element
        root(theme) {
            col {
                themed(S1).col { outerS1 = this; text("outer") }
                themed(S2).col { themed(S1).col { innerS1 = this; text("inner") } }
            }
        }
        assertNotEquals(
            theme[S1].theme.id, theme[S2].theme[S1].theme.id,
            "the two derivations of S1 must have distinct theme ids, or they cannot have distinct CSS",
        )
        assertNotEquals(outerS1.classes, innerS1.classes, "and so distinct generated classes")
    }

    @Test
    fun nestedOverrides_doNotLeakIntoASiblingSubtree() {
        val theme = freshTheme(
            S1.override { it.withBack(background = red) },
            S2.override {
                it.withBack(
                    background = blue,
                    semanticOverrides = SemanticOverrides(S1.override { inner -> inner.withBack(background = green) }),
                )
            },
            S3.override { it.withBack(background = yellow) },
        )
        lateinit var underS2: Element
        lateinit var underS3: Element
        root(theme) {
            col {
                themed(S2).col { themed(S1).col { underS2 = this; text("in s2") } }
                themed(S3).col { themed(S1).col { underS3 = this; text("in s3") } }
            }
        }
        assertEquals(GREEN_RGB, underS2.backgroundColor, "S1 under S2 uses S2's nested override")
        assertEquals(RED_RGB, underS3.backgroundColor, "S1 under S3 still uses the outer override")
    }

    @Test
    fun nestedOverrides_renderTheSameWhicheverSubtreeIsMountedFirst() {
        // The generated CSS is cached per theme id for the life of the page, so a lookup that
        // resolved wrongly on first touch would stay wrong for every later element.
        val innerFirst = nestedTheme()
        lateinit var innerA: Element
        lateinit var outerA: Element
        root(innerFirst) {
            col {
                themed(S2).col { themed(S1).col { innerA = this; text("inner") } }
                themed(S1).col { outerA = this; text("outer") }
            }
        }

        val outerFirst = nestedTheme()
        lateinit var outerB: Element
        lateinit var innerB: Element
        root(outerFirst) {
            col {
                themed(S1).col { outerB = this; text("outer") }
                themed(S2).col { themed(S1).col { innerB = this; text("inner") } }
            }
        }

        assertEquals(GREEN_RGB, innerA.backgroundColor, "inner S1 is green when mounted first")
        assertEquals(GREEN_RGB, innerB.backgroundColor, "inner S1 is green when mounted second")
        assertEquals(RED_RGB, outerA.backgroundColor, "outer S1 is red when mounted second")
        assertEquals(RED_RGB, outerB.backgroundColor, "outer S1 is red when mounted first")
    }

    @Test
    fun nestedOverrides_threeLevelsDeepReachTheBrowser() {
        val theme = freshTheme(
            S1.override { it.withBack(background = red) },
            S2.override { it.withBack(background = blue) },
            S3.override {
                it.withBack(
                    background = yellow,
                    semanticOverrides = SemanticOverrides(
                        S2.override { lvl2 ->
                            lvl2.withBack(
                                background = defaultColor,
                                semanticOverrides = SemanticOverrides(
                                    S1.override { lvl1 -> lvl1.withBack(background = green) }
                                ),
                            )
                        }
                    ),
                )
            },
        )
        lateinit var s3El: Element
        lateinit var s2UnderS3: Element
        lateinit var s1UnderS3S2: Element
        lateinit var s1AtRoot: Element
        root(theme) {
            col {
                themed(S1).col { s1AtRoot = this; text("root s1") }
                themed(S3).col {
                    s3El = this
                    themed(S2).col {
                        s2UnderS3 = this
                        themed(S1).col { s1UnderS3S2 = this; text("deep s1") }
                    }
                }
            }
        }
        assertEquals(RED_RGB, s1AtRoot.backgroundColor, "level 0: S1 is red")
        assertEquals(YELLOW_RGB, s3El.backgroundColor, "level 1: S3 is yellow")
        assertEquals(DEFAULT_RGB, s2UnderS3.backgroundColor, "level 2: S2 under S3 uses S3's nested S2 override")
        assertEquals(GREEN_RGB, s1UnderS3S2.backgroundColor, "level 3: S1 under S3>S2 is green")
    }

    @Test
    fun anOverrideThatDelegatesToAnotherSemanticRendersAsThatSemantic() {
        val theme = freshTheme(
            S1.override { it.withBack(background = red) },
            S2.override { it[S1] },
        )
        lateinit var s2El: Element
        root(theme) { col { themed(S2).col { s2El = this; text("s2") } } }
        assertEquals(RED_RGB, s2El.backgroundColor, "S2 delegating to an overridden S1 must paint S1's colour")
    }

    // -------------------------------------------------------------------------
    // 3. The identity trap - the reason overrides can look like they do nothing on web
    // -------------------------------------------------------------------------

    /** Mounts both themes under one root, which is what an app actually has. */
    private fun mountBoth(first: Theme, second: Theme): Pair<Element, Element> {
        lateinit var a: Element
        lateinit var b: Element
        root(first) {
            col {
                themed(S1).col { a = this; text("first") }
                themed(ThemeDerivation.Set(second)).col {
                    themed(S1).col { b = this; text("second") }
                }
            }
        }
        return a to b
    }

    @Test
    fun twoThemesSharingAnIdShareOneGeneratedClass_soTheSecondOnesOverridesAreDropped() {
        // This is the shape of the bug, pinned as behaviour rather than left as a surprise: on web
        // a theme is its id. `plain` renders first and defines `t-<id>-css-s1`; `overridden` then
        // reuses that class and its S1 override never reaches any CSS, so the element shows the
        // default. Give the two themes distinct ids and both render correctly - see the next test.
        val id = "cssShared${serial++}"
        val plain = Theme(id = id, background = Color.white)
        val overridden = Theme(
            id = id,
            background = Color.white,
            semanticOverrides = SemanticOverrides(S1.override { it.withBack(background = red) }),
        )
        assertEquals(plain, overridden, "Theme.equals is id-only, so these two are interchangeable to any cache")
        assertEquals(
            plain[S1].theme.id, overridden[S1].theme.id,
            "and their derived themes collide too, because overrides are not part of the id",
        )

        val (plainEl, overriddenEl) = mountBoth(plain, overridden)

        assertEquals(DEFAULT_RGB, plainEl.backgroundColor, "the theme that rendered first defines the class")
        assertEquals(
            DEFAULT_RGB, overriddenEl.backgroundColor,
            "and the second theme inherits it - its override is silently dropped, which on screen reads as " +
                "the override 'not applying' and the theme reverting to the default",
        )
        assertEquals(plainEl.classes, overriddenEl.classes, "because both elements carry the same generated class")
    }

    @Test
    fun givingTheThemesDistinctIdsMakesBothSetsOfOverridesRender() {
        // The fix for the case above, from the caller's side.
        val n = serial++
        val plain = Theme(id = "cssDistinctA$n", background = Color.white)
        val overridden = Theme(
            id = "cssDistinctB$n",
            background = Color.white,
            semanticOverrides = SemanticOverrides(S1.override { it.withBack(background = red) }),
        )
        val (plainEl, overriddenEl) = mountBoth(plain, overridden)

        assertEquals(DEFAULT_RGB, plainEl.backgroundColor, "the un-overridden theme paints the default")
        assertEquals(RED_RGB, overriddenEl.backgroundColor, "the overridden theme paints its override")
    }

    /**
     * Runs [block] with [KiteUiCss.log] set to [cssLog], collecting everything KiteUiCss logs at
     * [level] or above. Restores the previous log afterwards.
     */
    private fun collectingCssLog(
        level: LogLevel = LogLevel.LOG,
        cssLog: Log? = Log.tag("KiteUiCss"),
        block: () -> Unit,
    ): List<String> {
        val collected = mutableListOf<String>()
        val interceptor = LogInterceptor { seen, tag, entries ->
            if (seen >= level && tag.contains("KiteUiCss")) collected.add(entries.joinToString(" "))
        }
        val previousLog = KiteUiCss.log
        KiteUiCss.log = cssLog
        Log.interceptors.add(interceptor)
        try {
            block()
        } finally {
            Log.interceptors.remove(interceptor)
            KiteUiCss.log = previousLog
        }
        return collected
    }

    /** The colliding pair used by the log tests: same id, different overrides. */
    private fun mountCollision(id: String) {
        val plain = Theme(id = id, background = Color.white)
        val overridden = Theme(
            id = id,
            background = Color.white,
            semanticOverrides = SemanticOverrides(S1.override { it.withBack(background = red) }),
        )
        mountBoth(plain, overridden)
    }

    @Test
    fun aCollidingIdIsReportedInsteadOfFailingSilently() {
        // The collision above has no other symptom, so the CSS layer names it.
        val id = "cssLog${serial++}"
        val messages = collectingCssLog { mountCollision(id) }
        assertTrue(
            messages.any { it.contains("Theme id collision") && it.contains(id) },
            "the CSS layer must name the colliding id; messages were $messages",
        )
    }

    @Test
    fun aCollisionIsReportedAtDebugLevelAndNotAsAWarning() {
        // It describes how an app is put together, not a runtime fault, so it must not reach a
        // log that only wants warnings and errors.
        val id = "cssLevel${serial++}"
        val aboveDebug = collectingCssLog(level = LogLevel.INFO) { mountCollision(id) }
        assertTrue(
            aboveDebug.none { it.contains("Theme id collision") },
            "nothing above LogLevel.LOG may be emitted; got $aboveDebug",
        )
    }

    @Test
    fun theDefaultLogDoesNotAcceptDebugMessages_soNothingIsReported() {
        assertFalse(
            KiteUiCss.log?.atLevel(LogLevel.LOG) ?: false,
            "CSS diagnostics must be opt-in - tracking them costs a retained Theme per generated class",
        )
        val id = "cssQuiet${serial++}"
        val messages = collectingCssLog(cssLog = KiteUiCss.log) { mountCollision(id) }
        assertTrue(messages.isEmpty(), "with the default log KiteUiCss must stay quiet; got $messages")
    }

    @Test
    fun aLogFilteredToWarnOrAboveAlsoTurnsDiagnosticsOff() {
        val id = "cssFiltered${serial++}"
        val messages = collectingCssLog(cssLog = Log.tag("KiteUiCss").warnOrAbove()) { mountCollision(id) }
        assertTrue(messages.isEmpty(), "a warn-or-above log must switch KiteUiCss off; got $messages")
    }

    @Test
    fun theCompanionLogCanBeSilencedEntirely() {
        val id = "cssSilent${serial++}"
        val messages = collectingCssLog(cssLog = null) { mountCollision(id) }
        assertTrue(messages.isEmpty(), "setting KiteUiCss.log to null must silence it; got $messages")
    }

    @Test
    fun renderingTheSameThemeTwiceIsNotReportedAsACollision() {
        lateinit var first: Element
        lateinit var second: Element
        val messages = collectingCssLog {
            val theme = freshTheme(S1.override { it.withBack(background = red) })
            val pair = mountBoth(theme, theme)
            first = pair.first
            second = pair.second
        }
        assertEquals(RED_RGB, first.backgroundColor, "first mount renders the override")
        assertEquals(RED_RGB, second.backgroundColor, "so does the second - reusing the class is the normal case")
        assertTrue(
            messages.none { it.contains("Theme id collision") },
            "re-rendering the same theme must not be reported as a collision; messages were $messages",
        )
    }

    @Test
    fun aCollisionIsReportedOnceRatherThanOncePerElement() {
        val id = "cssOnce${serial++}"
        val messages = collectingCssLog {
            val plain = Theme(id = id, background = Color.white)
            val overridden = Theme(
                id = id,
                background = Color.white,
                semanticOverrides = SemanticOverrides(S1.override { it.withBack(background = red) }),
            )
            root(plain) {
                col {
                    themed(S1).col { text("first") }
                    themed(ThemeDerivation.Set(overridden)).col {
                        repeat(4) { i -> themed(S1).col { text("collides $i") } }
                    }
                }
            }
        }
        // Both the base theme and the theme S1 derives from it collide, so two ids are named -
        // but each exactly once, however many elements carry them.
        val collisionMessages = messages.filter { it.contains("Theme id collision") }
        assertEquals(
            collisionMessages.size, collisionMessages.distinct().size,
            "each colliding id must be named once, not once per element; messages were $messages",
        )
        assertTrue(
            collisionMessages.any { it.contains("'$id'") },
            "the base theme's id must be named; messages were $messages",
        )
        assertTrue(
            collisionMessages.any { it.contains("'$id-${S1.key}'") },
            "so must the id S1 derives from it; messages were $messages",
        )
    }
}
