package com.lightningkite.kiteui

import android.content.Context
import android.graphics.Bitmap
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.signature.ObjectKey
import com.lightningkite.kiteui.views.AndroidAppContext
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals

/**
 * cleanImageCache() has no observable effect through Glide's public loading API alone - a repeat
 * load of the same model always "succeeds" whether or not it was served from cache. To make
 * eviction observable, this registers a fake Glide data source that counts how many times it is
 * actually asked to fetch bytes: a warm memory cache means a repeat load does not increase that
 * count; cleanImageCache() clearing it means the next load does.
 *
 * Requests opt out of Glide's disk cache (DiskCacheStrategy.NONE) so the test only exercises the
 * in-memory cache that cleanImageCache()'s clearMemory() call is responsible for - disk eviction
 * happens on a background thread inside cleanImageCache() and isn't asserted on here.
 */
private class CountingSource(val tag: String, val loadCount: AtomicInteger, val pngBytes: ByteArray)

private class CountingFetcher(private val source: CountingSource) : DataFetcher<InputStream> {
    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in InputStream>) {
        source.loadCount.incrementAndGet()
        callback.onDataReady(ByteArrayInputStream(source.pngBytes))
    }

    override fun cleanup() {}
    override fun cancel() {}
    override fun getDataClass(): Class<InputStream> = InputStream::class.java
    override fun getDataSource(): DataSource = DataSource.LOCAL
}

private class CountingModelLoader : ModelLoader<CountingSource, InputStream> {
    override fun buildLoadData(
        model: CountingSource, width: Int, height: Int, options: Options
    ): ModelLoader.LoadData<InputStream> =
        ModelLoader.LoadData(ObjectKey(model.tag), CountingFetcher(model))

    override fun handles(model: CountingSource): Boolean = true
}

private class CountingModelLoaderFactory : ModelLoaderFactory<CountingSource, InputStream> {
    override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<CountingSource, InputStream> =
        CountingModelLoader()

    override fun teardown() {}
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CleanImageCacheTest {
    private fun context(): Context {
        try {
            AndroidAppContext.applicationCtx
        } catch (_: UninitializedPropertyAccessException) {
            AndroidAppContext.applicationCtx = RuntimeEnvironment.getApplication()
        }
        return AndroidAppContext.applicationCtx
    }

    @Test
    fun cleanImageCacheForcesAFreshFetchInsteadOfServingTheCachedBitmap() {
        val context = context()
        Glide.get(context).registry.prepend(
            CountingSource::class.java, InputStream::class.java, CountingModelLoaderFactory()
        )
        val pngBytes = ByteArrayOutputStream().also {
            Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.PNG, 100, it)
        }.toByteArray()
        val loadCount = AtomicInteger(0)
        val source = CountingSource("clean-image-cache-test", loadCount, pngBytes)

        // RequestFutureTarget.get() asserts it isn't called on the main thread, and Robolectric
        // treats the JUnit test thread as main - so each blocking load runs on its own thread.
        // The target is explicitly cleared afterwards (Glide's own documented pattern for
        // synchronous test loads) so the decoded bitmap moves from Glide's "active resources" -
        // held only while a target is in use, unaffected by clearMemory() - into the passive LRU
        // memory cache that cleanImageCache() is actually responsible for evicting.
        fun load() {
            var error: Throwable? = null
            val thread = Thread {
                try {
                    val target =
                        Glide.with(context).load(source).diskCacheStrategy(DiskCacheStrategy.NONE).submit(4, 4)
                    target.get()
                    Glide.with(context).clear(target)
                } catch (e: Throwable) {
                    error = e
                }
            }
            thread.start()
            thread.join()
            error?.let { throw it }
        }

        load()
        load()
        assertEquals(1, loadCount.get(), "a repeat load against a warm memory cache should not re-fetch")

        cleanImageCache()
        shadowOf(android.os.Looper.getMainLooper()).idle() // run the main-thread clearMemory() post

        load()
        assertEquals(2, loadCount.get(), "cleanImageCache() should have evicted the cached bitmap, forcing a re-fetch")
    }
}
