package com.lightningkite.kiteui.map

import cocoapods.MapLibre.*
import com.lightningkite.kiteui.map.MapCache.Offline.Progress
import com.lightningkite.kiteui.map.MapCache.Offline.Region
import com.lightningkite.reactive.core.BaseReactive
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.ReactiveState
import com.lightningkite.reactive.core.Signal
import kotlinx.cinterop.cValue
import kotlinx.cinterop.useContents
import kotlinx.coroutines.delay
import org.maplibre.spatialk.geojson.BoundingBox
import org.maplibre.spatialk.geojson.Geometry as GeoJsonGeometry
import platform.Foundation.*
import platform.darwin.NSObjectProtocol

internal actual object PlatformMapCache {
    private val storage get() = MLNOfflineStorage.sharedOfflineStorage

    actual fun setAmbientSize(bytes: Long, onComplete: (error: Exception?) -> Unit) =
        storage.setMaximumAmbientCacheSize(bytes.toULong()) { onComplete(it?.toException()) }

    actual fun invalidateAmbient(onComplete: (error: Exception?) -> Unit) =
        storage.invalidateAmbientCacheWithCompletionHandler { onComplete(it?.toException()) }

    actual fun clearAmbient(onComplete: (error: Exception?) -> Unit) =
        storage.clearAmbientCacheWithCompletionHandler { onComplete(it?.toException()) }

    actual fun createRegion(definition: Region.Definition, onComplete: (OfflineResult<Region>) -> Unit) {
        val styleUrl = NSURL.URLWithString(definition.style.url)
        val region: MLNOfflineRegionProtocol = when (definition) {
            is Region.Definition.Bounds -> MLNTilePyramidOfflineRegion(
                styleURL = styleUrl,
                bounds = cValue<MLNCoordinateBounds> {
                    sw.latitude = definition.bounds.south
                    sw.longitude = definition.bounds.west
                    ne.latitude = definition.bounds.north
                    ne.longitude = definition.bounds.east
                },
                fromZoomLevel = definition.minZoom,
                toZoomLevel = definition.maxZoom,
            )
            is Region.Definition.Geometry -> MLNShapeOfflineRegion(
                styleURL = styleUrl,
                shape = shapeFromGeoJson(GeoJsonGeometry.toJson(definition.geometry))
                    ?: return onComplete(OfflineResult.Error("MapLibre couldn't parse the region's geometry")),
                fromZoomLevel = definition.minZoom,
                toZoomLevel = definition.maxZoom,
            )
        }
        storage.addPackForRegion(region, withContext = NSData()) { pack, error ->
            val wrapped = pack?.let { wrap(it, definition) }
            onComplete(
                when {
                    wrapped != null -> OfflineResult.Success(wrapped)
                    error != null -> OfflineResult.Error(error.message())
                    else -> OfflineResult.Error("MapLibre didn't create the offline pack")
                }
            )
        }
    }

    actual suspend fun getRegions(): OfflineResult<List<Region>> =
        OfflineResult.Success(loadedPacks().mapNotNull { wrap(it) })

    actual suspend fun getRegion(id: Long): OfflineResult<Region?> =
        OfflineResult.Success(loadedPacks().firstOrNull { it.regionId?.longLongValue == id }?.let { wrap(it) })

    actual fun mergeRegions(path: String, onComplete: (OfflineResult<List<Region>>) -> Unit) =
        storage.addContentsOfFile(path) { _, packs, error ->
            if (error != null) onComplete(OfflineResult.Error(error.message()))
            else {
                // These replace (and invalidate) any packs storage already had for the same regions
                @Suppress("UNCHECKED_CAST")
                onComplete(OfflineResult.Success((packs.orEmpty() as List<MLNOfflinePack>).mapNotNull { wrap(it) }))
            }
        }

    private suspend fun loadedPacks(): List<MLNOfflinePack> {
        // Packs load asynchronously when offline storage is first used, and until then this is
        // null. The only signal for that is KVO, so poll instead.
        while (storage.packs == null) delay(50)
        @Suppress("UNCHECKED_CAST")
        return storage.packs as List<MLNOfflinePack>
    }

    private fun wrap(pack: MLNOfflinePack, definition: Region.Definition? = pack.region.toDefinition()): Region? {
        val id = pack.regionId?.longLongValue ?: return null
        definition ?: return null
        val state = states[id]
        if (state == null) states[id] = RegionState(pack)
        else if (state.pack !== pack) state.replace(pack)
        return IosRegion(id, definition)
    }

    /**
     * State shared by every [IosRegion] with the same ID. Storage keeps one pack per region, but
     * replaces (and invalidates) it when a database containing that region is merged in, so the
     * current pack is tracked here.
     */
    private class RegionState(pack: MLNOfflinePack) {
        var pack: MLNOfflinePack = pack
            private set
        val downloading = Signal(false)
        val progress = ProgressReactive(pack, onComplete = { downloading.value = false })

        fun replace(newPack: MLNOfflinePack) {
            pack = newPack
            progress.replace(newPack)
            // The old pack's download stopped when it was invalidated
            downloading.value = false
        }
    }

    private val states = HashMap<Long, RegionState>()

    private class IosRegion(override val id: Long, override val definition: Region.Definition) : Region {
        private val state = states.getValue(id)

        override val progress: Reactive<Progress> get() = state.progress
        override val isDownloading: Reactive<Boolean> get() = state.downloading

        override fun startDownload() {
            if (state.downloading.value) return
            state.downloading.value = true
            state.pack.resume()
        }

        override fun pauseDownload() {
            if (!state.downloading.value) return
            state.downloading.value = false
            if (state.pack.state == MLNOfflinePackStateActive) state.pack.suspend()
        }

        override fun invalidate(onComplete: (OfflineResult<Unit>) -> Unit) =
            storage.invalidatePack(state.pack) { error -> onComplete(error.toResult()) }

        override fun delete(onComplete: (OfflineResult<Unit>) -> Unit) {
            pauseDownload()
            storage.removePack(state.pack) { error ->
                if (error == null) {
                    states.remove(id)?.progress?.release()
                }
                onComplete(error.toResult())
            }
        }
    }

    /** Listens to its pack's progress notifications for as long as the pack is current. */
    private class ProgressReactive(
        private var pack: MLNOfflinePack,
        private val onComplete: () -> Unit,
    ) : BaseReactive<Progress>() {
        private var observer: NSObjectProtocol? = null

        init {
            observe()
        }

        private fun observe() {
            observer = NSNotificationCenter.defaultCenter.addObserverForName(
                MLNOfflinePackProgressChangedNotification, `object` = pack, queue = NSOperationQueue.mainQueue,
            ) { _ ->
                val progress = pack.toProgress()
                state = ReactiveState(progress)
                if (progress.isComplete) onComplete()
            }
        }

        override fun activate() {
            // Packs loaded from storage don't know their progress until it's requested
            if (pack.state == MLNOfflinePackStateUnknown) pack.requestProgress()
            else state = ReactiveState(pack.toProgress())
        }

        fun replace(newPack: MLNOfflinePack) {
            release()
            pack = newPack
            observe()
            activate()
        }

        fun release() {
            observer?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
            observer = null
        }
    }
}

private fun MLNOfflineRegionProtocol.toDefinition(): Region.Definition? = when (this) {
    is MLNTilePyramidOfflineRegion -> Region.Definition.Bounds(
        bounds = bounds.useContents {
            BoundingBox(west = sw.longitude, south = sw.latitude, east = ne.longitude, north = ne.latitude)
        },
        minZoom = minimumZoomLevel,
        maxZoom = maximumZoomLevel,
        style = Style.Url(styleURL.absoluteString ?: return null),
    )
    is MLNShapeOfflineRegion -> Region.Definition.Geometry(
        geometry = GeoJsonGeometry.fromJsonOrNull(
            NSString.create(data = shape.geoJSONDataUsingEncoding(NSUTF8StringEncoding), encoding = NSUTF8StringEncoding)
                ?.toString() ?: return null
        ) ?: return null,
        minZoom = minimumZoomLevel,
        maxZoom = maximumZoomLevel,
        style = Style.Url(styleURL.absoluteString ?: return null),
    )
    else -> null
}

private fun MLNOfflinePack.toProgress() = progress.useContents {
    Progress(
        isComplete = state == MLNOfflinePackStateComplete,
        completedResources = countOfResourcesCompleted.toLong(),
        requiredResources = countOfResourcesExpected.toLong(),
        isRequiredResourceCountPrecise = countOfResourcesExpected == maximumResourcesExpected,
        completedBytes = countOfBytesCompleted.toLong(),
    )
}

private fun NSError.message() = localizedDescription

private fun NSError.toException() = IllegalStateException(message())

private fun NSError?.toResult(): OfflineResult<Unit> =
    if (this == null) OfflineResult.Success(Unit) else OfflineResult.Error(message())
