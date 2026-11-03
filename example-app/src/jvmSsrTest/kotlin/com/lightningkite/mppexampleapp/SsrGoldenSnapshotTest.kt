package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.ssr.SsrRouter
import com.lightningkite.mppexampleapp.docs.RecyclerViewPage
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Locks the jvmSsr HTML serializer ([com.lightningkite.kiteui.views.FutureElement] +
 * `FutureElement.commonHtml.jvmSsr.kt`) against silent regressions.
 *
 * The server serializer and the client hydration code (`NativeElement.commonHtml.js.kt`) are two
 * hand-maintained implementations matched positionally - nothing in the type system keeps them in
 * sync. A snapshot test can't catch drift between the two runtimes (that needs a browser, and is
 * out of scope here - see [com.lightningkite.kiteui.ssr.HydrationContext]'s mismatch counter for
 * dev-time visibility into that instead), but it does catch unintended changes to *what the server
 * emits*, which is the half of the contract this module can check without a browser.
 *
 * Renders a few representative pages - chosen for real row/col/card structure, list rendering, and
 * nesting depth, not for UI coverage - and asserts the HTML is byte-for-byte stable.
 *
 * If a change is intentional, delete the corresponding file under `resources/golden/` and re-run:
 * the test regenerates the missing golden file from the current render and fails once (so the new
 * output gets reviewed and committed), then passes on the next run.
 */
class SsrGoldenSnapshotTest {
    private val router = SsrRouter(routes = AutoRoutes, theme = defaultTheme, basePath = "/")

    private fun render(page: Page): String = runBlocking {
        router.renderPageWithPreload(page)
    }

    /** Simple, static, deeply nested content - a baseline sanity check for the serializer. */
    @Test
    fun fourOhFour() = assertMatchesGolden("FourOhFour", FourOhFour())

    /** Real top-level page: rows/cols, cards-like buttons, and reactive nested content. */
    @Test
    fun homePage() = assertMatchesGolden("HomePage", HomePage())

    /** A list - several recyclerView variants (vertical, horizontal, grid) rendering item rows. */
    @Test
    fun recyclerViewPage() = assertMatchesGolden("RecyclerViewPage", RecyclerViewPage)

    private fun assertMatchesGolden(name: String, page: Page) {
        val actual = render(page)
        val file = goldenFile(name)
        if (!file.exists()) {
            file.parentFile.mkdirs()
            file.writeText(actual)
            error(
                "No golden file for '$name' yet - wrote the current render to ${file.absolutePath}. " +
                    "Review it, commit it, and re-run the test."
            )
        }
        assertEquals(
            file.readText(),
            actual,
            "SSR output for '$name' no longer matches the golden file at ${file.absolutePath}. " +
                "If this change is intentional, delete that file and re-run to regenerate it."
        )
    }

    /** Locates (without requiring it to already exist) `example-app/src/jvmSsrTest/resources/golden/<name>.html`. */
    private fun goldenFile(name: String): File {
        val marker = "src/commonMain/kotlin/com/lightningkite/mppexampleapp"
        var base: File? = File(".").absoluteFile
        repeat(6) {
            base?.let { b ->
                for (moduleRoot in listOf(b, File(b, "example-app"))) {
                    if (File(moduleRoot, marker).isDirectory) {
                        return File(moduleRoot, "src/jvmSsrTest/resources/golden/$name.html")
                    }
                }
            }
            base = base?.parentFile
        }
        error("Could not locate example-app module root from ${File(".").absolutePath}")
    }
}
