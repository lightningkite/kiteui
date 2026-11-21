package com.lightningkite.kiteui.map

import android.content.Context
import com.lightningkite.kiteui.map.MapCache.Offline.Progress
import com.lightningkite.kiteui.map.MapCache.Offline.Region
import com.lightningkite.kiteui.views.AndroidAppContext
import com.lightningkite.reactive.core.BaseReactive
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.ReactiveState
import com.lightningkite.reactive.core.Signal
import kotlinx.coroutines.suspendCancellableCoroutine
import org.maplibre.android.MapLibre
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.offline.OfflineGeometryRegionDefinition
import org.maplibre.android.offline.OfflineManager
import org.maplibre.android.offline.OfflineRegion
import org.maplibre.android.offline.OfflineRegionDefinition
import org.maplibre.android.offline.OfflineRegionError
import org.maplibre.android.offline.OfflineRegionStatus
import org.maplibre.android.offline.OfflineTilePyramidRegionDefinition
import org.maplibre.geojson.gson.GeometryGeoJson
import org.maplibre.spatialk.geojson.BoundingBox
import org.maplibre.spatialk.geojson.Geometry as GeoJsonGeometry
import kotlin.coroutines.resume

internal actual object PlatformMapCache {
    private val context: Context get() = AndroidAppContext.applicationCtx

    private val manager: OfflineManager
        get() {
            // OfflineManager's FileSource requires MapLibre to be initialized first
            MapLibre.getInstance(context)
            return OfflineManager.getInstance(context)
        }

    actual fun setAmbientSize(bytes: Long, onComplete: (error: Exception?) -> Unit) =
        manager.setMaximumAmbientCacheSize(bytes, fileSourceCallback(onComplete))

    actual fun invalidateAmbient(onComplete: (error: Exception?) -> Unit) =
        manager.invalidateAmbientCache(fileSourceCallback(onComplete))

    actual fun clearAmbient(onComplete: (error: Exception?) -> Unit) =
        manager.clearAmbientCache(fileSourceCallback(onComplete))

    actual fun createRegion(definition: Region.Definition, onComplete: (OfflineResult<Region>) -> Unit) {
        val pixelRatio = context.resources.displayMetrics.density
        val nativeDefinition = when (definition) {
            is Region.Definition.Bounds -> OfflineTilePyramidRegionDefinition(
                definition.style.url,
                definition.bounds.run { LatLngBounds.from(north, east, south, west) },
                definition.minZoom,
                definition.maxZoom,
                pixelRatio,
            )
            is Region.Definition.Geometry -> OfflineGeometryRegionDefinition(
                definition.style.url,
                GeometryGeoJson.fromJson(GeoJsonGeometry.toJson(definition.geometry)),
                definition.minZoom,
                definition.maxZoom,
                pixelRatio,
            )
        }
        manager.createOfflineRegion(nativeDefinition, ByteArray(0), object : OfflineManager.CreateOfflineRegionCallback {
            override fun onCreate(offlineRegion: OfflineRegion) =
                onComplete(OfflineResult.Success(AndroidRegion(offlineRegion, definition)))

            override fun onError(error: String) = onComplete(OfflineResult.Error(error))
        })
    }

    actual suspend fun getRegions(): OfflineResult<List<Region>> = suspendCancellableCoroutine { continuation ->
        manager.listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
            override fun onList(offlineRegions: Array<OfflineRegion>?) =
                continuation.resume(OfflineResult.Success(offlineRegions.orEmpty().mapNotNull(::wrap)))

            override fun onError(error: String) = continuation.resume(OfflineResult.Error(error))
        })
    }

    actual suspend fun getRegion(id: Long): OfflineResult<Region?> = suspendCancellableCoroutine { continuation ->
        manager.getOfflineRegion(id, object : OfflineManager.GetOfflineRegionCallback {
            override fun onRegion(offlineRegion: OfflineRegion) =
                continuation.resume(OfflineResult.Success(wrap(offlineRegion)))

            override fun onRegionNotFound() = continuation.resume(OfflineResult.Success(null))

            override fun onError(error: String) = continuation.resume(OfflineResult.Error(error))
        })
    }

    actual fun mergeRegions(path: String, onComplete: (OfflineResult<List<Region>>) -> Unit) =
        manager.mergeOfflineRegions(path, object : OfflineManager.MergeOfflineRegionsCallback {
            override fun onMerge(offlineRegions: Array<OfflineRegion>?) =
                onComplete(OfflineResult.Success(offlineRegions.orEmpty().mapNotNull(::wrap)))

            override fun onError(error: String) = onComplete(OfflineResult.Error(error))
        })

    private fun wrap(native: OfflineRegion): Region? =
        native.definition.toDefinition()?.let { AndroidRegion(native, it) }

    private fun fileSourceCallback(onComplete: (Exception?) -> Unit) = object : OfflineManager.FileSourceCallback {
        override fun onSuccess() = onComplete(null)
        override fun onError(message: String) = onComplete(IllegalStateException(message))
    }

    /**
     * State shared by every [AndroidRegion] with the same ID. MapLibre keeps one observer per region
     * ID, and each [OfflineRegion] object tracks its own download state (ignoring changes to the state
     * it thinks it's in), so all downloads go through the first [OfflineRegion] seen for an ID.
     */
    private class RegionState(val native: OfflineRegion) {
        val progress = ProgressReactive(native)
        val downloading = Signal(false)

        init {
            native.setDeliverInactiveMessages(true)
            native.setObserver(object : OfflineRegion.OfflineRegionObserver {
                override fun onStatusChanged(status: OfflineRegionStatus) {
                    progress.set(status)
                    if (status.isComplete && downloading.value) {
                        downloading.value = false
                        native.setDownloadState(OfflineRegion.STATE_INACTIVE)
                    }
                }

                // MapLibre retries these on its own
                override fun onError(error: OfflineRegionError) {}
                override fun mapboxTileCountLimitExceeded(limit: Long) {}
            })
        }
    }

    private val states = HashMap<Long, RegionState>()

    private class AndroidRegion(native: OfflineRegion, override val definition: Region.Definition) : Region {
        override val id: Long = native.id
        private val state = states.getOrPut(id) { RegionState(native) }

        override val progress: Reactive<Progress> get() = state.progress
        override val isDownloading: Reactive<Boolean> get() = state.downloading

        override fun startDownload() {
            if (state.downloading.value) return
            state.downloading.value = true
            state.native.setDownloadState(OfflineRegion.STATE_ACTIVE)
        }

        override fun pauseDownload() {
            if (!state.downloading.value) return
            state.downloading.value = false
            state.native.setDownloadState(OfflineRegion.STATE_INACTIVE)
        }

        override fun invalidate(onComplete: (OfflineResult<Unit>) -> Unit) =
            state.native.invalidate(object : OfflineRegion.OfflineRegionInvalidateCallback {
                override fun onInvalidate() = onComplete(OfflineResult.Success(Unit))
                override fun onError(error: String) = onComplete(OfflineResult.Error(error))
            })

        override fun delete(onComplete: (OfflineResult<Unit>) -> Unit) {
            pauseDownload()
            state.native.delete(object : OfflineRegion.OfflineRegionDeleteCallback {
                override fun onDelete() {
                    states.remove(id)
                    onComplete(OfflineResult.Success(Unit))
                }

                override fun onError(error: String) = onComplete(OfflineResult.Error(error))
            })
        }
    }

    /** Fetches the status when first listened to, and is kept up to date by the region's observer after that. */
    private class ProgressReactive(private val native: OfflineRegion) : BaseReactive<Progress>() {
        override fun activate() = native.getStatus(object : OfflineRegion.OfflineRegionStatusCallback {
            override fun onStatus(status: OfflineRegionStatus?) {
                if (status != null) set(status)
                else state = ReactiveState.exception(IllegalStateException("Offline region has no status"))
            }

            override fun onError(error: String?) {
                state = ReactiveState.exception(IllegalStateException(error))
            }
        })

        fun set(status: OfflineRegionStatus) {
            state = ReactiveState(status.toProgress())
        }
    }
}

private fun OfflineRegionDefinition.toDefinition(): Region.Definition? = when (this) {
    is OfflineTilePyramidRegionDefinition -> Region.Definition.Bounds(
        bounds = bounds?.run {
            BoundingBox(west = longitudeWest, south = latitudeSouth, east = longitudeEast, north = latitudeNorth)
        } ?: return null,
        minZoom = minZoom,
        maxZoom = maxZoom,
        style = Style.Url(styleURL ?: return null),
    )
    is OfflineGeometryRegionDefinition -> Region.Definition.Geometry(
        geometry = GeoJsonGeometry.fromJson(geometry?.toJson() ?: return null),
        minZoom = minZoom,
        maxZoom = maxZoom,
        style = Style.Url(styleURL ?: return null),
    )
    else -> null
}

private fun OfflineRegionStatus.toProgress() = Progress(
    isComplete = isComplete,
    completedResources = completedResourceCount,
    requiredResources = requiredResourceCount,
    isRequiredResourceCountPrecise = isRequiredResourceCountPrecise,
    completedBytes = completedResourceSize,
)
