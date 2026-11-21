package com.lightningkite.kiteui.map

import com.lightningkite.kiteui.map.MapCache.Offline.Progress
import com.lightningkite.kiteui.map.MapCache.Offline.Region
import com.lightningkite.kiteui.map.maplibre.RequestParameters
import com.lightningkite.reactive.core.Constant

/**
 * MapLibre has no storage of its own on web, relying on the browser's HTTP cache instead. Pages can't
 * size or delete from that, so the ambient cache is managed by choosing each request's cache mode.
 * Offline regions aren't supported.
 */
internal actual object PlatformMapCache {
    /** The cache mode for each resource's next request, until it's been requested again, or null to use the browser's default. */
    private var refreshMode: String? = null
    /** The resources requested since [refreshMode] was set, which the browser's cache is up to date for. */
    private val refreshed = HashSet<String>()

    /** Used as every map's `transformRequest`. */
    fun transformRequest(url: String, @Suppress("UNUSED_PARAMETER") resourceType: String?): RequestParameters? {
        val mode = refreshMode ?: return null
        return if (refreshed.add(url)) RequestParameters(url = url, cache = mode) else null
    }

    private fun refresh(mode: String) {
        // Clearing is stronger than invalidating, so it isn't downgraded for resources not yet requested since
        if (refreshMode != "reload") refreshMode = mode
        refreshed.clear()
    }

    actual fun setAmbientSize(bytes: Long, onComplete: (error: Exception?) -> Unit) = onComplete(null)

    // Revalidates with the server, using the cached response if it's unchanged
    actual fun invalidateAmbient(onComplete: (error: Exception?) -> Unit) {
        refresh("no-cache")
        onComplete(null)
    }

    // Ignores the cached response and replaces it with a fresh one
    actual fun clearAmbient(onComplete: (error: Exception?) -> Unit) {
        refresh("reload")
        onComplete(null)
    }

    private var nextRegionId = 0L

    actual fun createRegion(definition: Region.Definition, onComplete: (OfflineResult<Region>) -> Unit) =
        onComplete(OfflineResult.Success(WebRegion(nextRegionId++, definition)))

    actual suspend fun getRegions(): OfflineResult<List<Region>> = OfflineResult.Success(emptyList())

    actual suspend fun getRegion(id: Long): OfflineResult<Region?> = OfflineResult.Success(null)

    actual fun mergeRegions(path: String, onComplete: (OfflineResult<List<Region>>) -> Unit) =
        onComplete(OfflineResult.Success(emptyList()))

    private class WebRegion(override val id: Long, override val definition: Region.Definition) : Region {
        override val progress = Constant(
            Progress(
                isComplete = false,
                completedResources = 0,
                requiredResources = 0,
                isRequiredResourceCountPrecise = false,
                completedBytes = 0,
            )
        )
        override val isDownloading = Constant(false)

        override fun startDownload() {}
        override fun pauseDownload() {}
        override fun invalidate(onComplete: (OfflineResult<Unit>) -> Unit) = onComplete(OfflineResult.Success(Unit))
        override fun delete(onComplete: (OfflineResult<Unit>) -> Unit) = onComplete(OfflineResult.Success(Unit))
    }
}
