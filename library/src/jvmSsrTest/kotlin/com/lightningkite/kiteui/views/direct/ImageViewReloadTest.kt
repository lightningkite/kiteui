package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.ImageRemote
import com.lightningkite.kiteui.models.UrlCacheStrategy
import com.lightningkite.kiteui.testing.uiTest
import com.lightningkite.reactive.core.ReactiveState
import com.lightningkite.reactive.core.Signal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

/**
 * [ImageView] decides whether a newly assigned source is worth re-rendering, which for remote
 * images means asking [UrlCacheStrategy] how much of the URL identifies the picture.  These tests
 * cover the signed-URL case it exists for: a rotating signature in the query string names the same
 * picture, so re-rendering would only re-download it and flicker.
 */
class ImageViewReloadTest {

    private val picture = "https://example.com/picture"

    /** The image view currently on screen.  Rendering a new one replaces this. */
    private val ImageView.rendered: RawImageView get() = lastRender!!.single()

    private val ImageView.renderedUrl: String get() = (rendered.source as ImageRemote).url

    /**
     * Renders a single image whose source is recalculated whenever [url] or [revision] changes.
     * Bumping revision re-assigns a source equal to the current one, which is how a reactive
     * re-evaluation looks when nothing about the image actually changed.
     */
    private fun imageTest(
        strategy: UrlCacheStrategy,
        block: suspend (url: Signal<String>, revision: Signal<Int>, view: ImageView) -> Unit,
    ) {
        val url = Signal("$picture?sig=1")
        val revision = Signal(0)
        lateinit var view: ImageView
        uiTest(content = {
            view = image {
                ::source {
                    revision()
                    ImageRemote(url(), strategy)
                }
            }
        }) {
            block(url, revision, view)
        }
    }

    @Test
    fun pathOnlyKeepsTheImageWhenOnlyTheSignatureChanges() = imageTest(UrlCacheStrategy.PathOnly) { url, _, view ->
        val original = view.rendered
        url.value = "$picture?sig=2"
        assertSame(original, view.rendered, "a rotated signature must not build a new image view")
        assertEquals("$picture?sig=1", view.renderedUrl)
    }

    @Test
    fun pathOnlyReloadsWhenThePathChanges() = imageTest(UrlCacheStrategy.PathOnly) { url, _, view ->
        url.value = "https://example.com/other?sig=1"
        assertEquals("https://example.com/other?sig=1", view.renderedUrl)
    }

    @Test
    fun fullReloadsWhenTheSignatureChanges() = imageTest(UrlCacheStrategy.Full) { url, _, view ->
        val original = view.rendered
        url.value = "$picture?sig=2"
        assertNotSame(original, view.rendered)
        assertEquals("$picture?sig=2", view.renderedUrl)
    }

    @Test
    fun fullKeepsTheImageWhenTheUrlIsUnchanged() = imageTest(UrlCacheStrategy.Full) { _, revision, view ->
        val original = view.rendered
        revision.value++
        assertSame(original, view.rendered)
    }

    @Test
    fun noneReloadsEvenWhenTheUrlIsUnchanged() = imageTest(UrlCacheStrategy.None) { _, revision, view ->
        val original = view.rendered
        revision.value++
        assertNotSame(original, view.rendered, "None must treat every assignment as a new image")
    }

    /**
     * The expired-signature case.  A view that requested an already-stale URL fails, and the fresh
     * signature the app then obtains points at the same path - so the reload has to happen despite
     * [UrlCacheStrategy.PathOnly] considering the two URLs to be the same image.
     */
    @Test
    fun aFailedImageReloadsWhenAFreshSignatureArrives() = imageTest(UrlCacheStrategy.PathOnly) { url, _, view ->
        view.rendered._state.state = ReactiveState.exception(Exception("expired signature"))
        url.value = "$picture?sig=2"
        assertEquals("$picture?sig=2", view.renderedUrl)
    }

    /** The same recovery, for when the fresh signature arrives before the failure is reported. */
    @Test
    fun aFreshSignatureAssignedBeforeTheFailureIsUsed() = imageTest(UrlCacheStrategy.PathOnly) { url, _, view ->
        val failing = view.rendered
        url.value = "$picture?sig=2"
        assertSame(failing, view.rendered, "the fresh signature is skipped while the old one may yet load")

        failing._state.state = ReactiveState.exception(Exception("expired signature"))
        assertEquals("$picture?sig=2", view.renderedUrl)
    }

    /** A failure with nothing newer to fall back on stays failed rather than retrying forever. */
    @Test
    fun aFailedImageWithNoFreshSignatureIsNotRetried() = imageTest(UrlCacheStrategy.PathOnly) { _, _, view ->
        val failing = view.rendered
        failing._state.state = ReactiveState.exception(Exception("gone"))
        assertSame(failing, view.rendered)
    }
}
