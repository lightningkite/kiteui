package com.lightningkite.kiteui.codegen

import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * Guards the extraction of the generators out of the Gradle plugin: calling [Codegen] directly must
 * produce exactly what the Gradle tasks produce. Runs against example-app, whose resources cover
 * fonts, SVGs, images, audio and video, so a regression in any generator shows up here.
 *
 * This also pins down a version skew worth knowing about. The plugin is self-hosted through
 * `buildSrc`, which pins fontbox 2.0.27, while `:codegen` builds against the catalog's 3.0.7. Font
 * family and sub-family names come out of fontbox and feed the generated declarations, the Android
 * `<font-family>` XML and the iOS `UIAppFonts` array, so comparing this module's output against the
 * Gradle output compares the two fontbox versions on real fonts as a side effect. Keep the whole
 * `Info.plist` and the `fonts_*.xml` families in the comparison, because those are where a
 * disagreement between the two versions would surface.
 *
 * Skips when example-app has not been generated yet; run `./gradlew :example-app:kiteuiResourcesAll`
 * first.
 */
class DirectCallMatchesGradleTest {
    // Canonical, because the iOS generator echoes the resource's absolute path into a trailing
    // comment in the generated Kotlin. The Gradle task passes an absolute `project.file(...)`, so
    // anything else here would differ in that comment alone and mask a real difference.
    private val exampleApp = File("../example-app").canonicalFile
    private val iosProject = File("../example-app-ios/KiteUI Example App").canonicalFile
    // `resources` at the module root, not `src/commonMain/resources`: example-app moved to the
    // Kotlin Toolchain layout. The Gradle output it is compared against is unaffected.
    private val resources = exampleApp.resolve("resources")
    private val fromGradle = exampleApp.resolve("build/generated")
    private val packageName = "com.lightningkite.mppexampleapp"

    private fun scratch(name: String): File =
        File(System.getProperty("java.io.tmpdir")).resolve("kiteui-codegen-verify/$name").apply {
            deleteRecursively()
            mkdirs()
        }

    /**
     * Compares generated text, with the resources root spelled the same on both sides.
     *
     * The iOS generator echoes each resource's absolute path into a trailing comment. The Gradle
     * output this is compared against was produced when example-app used the Gradle source layout,
     * so those comments still say `src/commonMain/resources`. Normalising that one prefix keeps the
     * comparison on the generated declarations rather than on where the fixtures happen to live.
     */
    private fun assertSameAs(expected: File, actual: File) {
        fun File.normalized() = readText().replace("/src/commonMain/resources/", "/resources/")
        assertEquals("${expected.name} differs", expected.normalized(), actual.normalized())
    }

    /**
     * Asserts every file this run wrote under [actual] is byte-identical to its counterpart in the
     * Gradle destination [expected].
     *
     * Deliberately one-directional. These generators write into directories that live in git — the
     * Android `res/` tree and the Xcode project — and they never delete, so those directories also
     * hold orphans from older naming schemes and resources that no longer exist. Requiring the
     * reverse direction would assert against that accumulated history rather than against the
     * generator. The set of files is still pinned, because the manifests that enumerate them —
     * `Info.plist`, the `fonts_*.xml` font families, and the generated Kotlin — are compared whole.
     */
    private fun assertMatchesWhereWritten(expected: File, actual: File) {
        val written = actual.walkTopDown().filter { it.isFile }
            .map { it.relativeTo(actual).path }.toSortedSet()
        assertEquals("${actual.name} wrote nothing", true, written.isNotEmpty())
        written.forEach {
            assertEquals(
                "${expected.name}/$it differs",
                expected.resolve(it).readBytes().toList(),
                actual.resolve(it).readBytes().toList(),
            )
        }
    }

    @Test
    fun commonMatches() {
        assumeTrue(fromGradle.exists())
        val out = scratch("common").resolve("Resources.kt")
        Codegen.resourcesCommon(resources, packageName, out)
        assertSameAs(fromGradle.resolve("kiteui-common/Resources.kt"), out)
    }

    @Test
    fun jsMatches() {
        assumeTrue(fromGradle.exists())
        val dir = scratch("js")
        val out = dir.resolve("Resources.js.kt")
        Codegen.resourcesJs(resources, packageName, listOf(dir.resolve(".gitignore")), out)
        assertSameAs(fromGradle.resolve("kiteui-js/Resources.js.kt"), out)
    }

    @Test
    fun jvmMatches() {
        assumeTrue(fromGradle.exists())
        val dir = scratch("jvm")
        val out = dir.resolve("Resources.jvm.kt")
        Codegen.resourcesJvm(resources, packageName, listOf(dir.resolve(".gitignore")), out)
        assertSameAs(fromGradle.resolve("kiteui-jvm/Resources.jvm.kt"), out)
    }

    @Test
    fun androidMatches() {
        assumeTrue(fromGradle.exists())
        val dir = scratch("android")
        val out = dir.resolve("Resources.android.kt")
        val res = dir.resolve("res")
        Codegen.resourcesAndroid(resources, packageName, res, out)
        assertSameAs(fromGradle.resolve("kiteui-android/Resources.android.kt"), out)
        // Only the directories the generator owns; the rest of res/ is hand-maintained.
        listOf("font", "drawable-xhdpi", "raw").forEach {
            assertMatchesWhereWritten(exampleApp.resolve("res/$it"), res.resolve(it))
        }
    }

    @Test
    fun iosMatches() {
        assumeTrue(fromGradle.exists())
        val dir = scratch("ios")
        val out = dir.resolve("Resources.ios.kt")
        val plist = dir.resolve("Info.plist")
        // resourcesIos rewrites an existing plist in place rather than authoring one, so the
        // comparison has to start from the real file. The rewrite replaces the whole UIAppFonts
        // array, so re-running it over its own output is idempotent.
        iosProject.resolve("Info.plist").copyTo(plist, overwrite = true)
        Codegen.resourcesIos(
            resources = resources,
            packageName = packageName,
            outPlist = plist,
            outNonAssets = dir.resolve("resourcesFromCommon"),
            outAssets = dir.resolve("Assets.xcassets"),
            outKt = out,
        )
        assertSameAs(fromGradle.resolve("kiteui-ios/Resources.ios.kt"), out)
        assertSameAs(iosProject.resolve("Info.plist"), plist)
        assertMatchesWhereWritten(iosProject.resolve("resourcesFromCommon"), dir.resolve("resourcesFromCommon"))
        assertMatchesWhereWritten(iosProject.resolve("Assets.xcassets"), dir.resolve("Assets.xcassets"))
    }

    @Test
    fun autoRoutesMatch() {
        assumeTrue(fromGradle.exists())
        val out = scratch("routes").resolve("autoroutes.kt")
        Codegen.autoRoutes(exampleApp.resolve("src"), out)
        assertSameAs(fromGradle.resolve("kiteui-common/autoroutes.kt"), out)
    }
}
