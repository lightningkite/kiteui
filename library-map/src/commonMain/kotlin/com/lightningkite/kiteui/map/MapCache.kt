package com.lightningkite.kiteui.map

import com.lightningkite.reactive.core.Reactive
import org.maplibre.spatialk.geojson.BoundingBox
import org.maplibre.spatialk.geojson.Geometry as GeoJsonGeometry

object MapCache {
    object Ambient {
        fun setSize(bytes: Long, onComplete: (error: Exception?) -> Unit = {}) =
            PlatformMapCache.setAmbientSize(bytes, onComplete)

        fun invalidate(onComplete: (error: Exception?) -> Unit = {}) = PlatformMapCache.invalidateAmbient(onComplete)
        fun clear(onComplete: (error: Exception?) -> Unit = {}) = PlatformMapCache.clearAmbient(onComplete)
    }

    object Offline {
        fun createRegion(definition: Region.Definition, onComplete: (OfflineResult<Region>) -> Unit) =
            PlatformMapCache.createRegion(definition, onComplete)

        suspend fun getRegions(): OfflineResult<List<Region>> = PlatformMapCache.getRegions()
        suspend fun getRegion(id: Long): OfflineResult<Region?> = PlatformMapCache.getRegion(id)

        fun mergeRegions(path: String, onComplete: (OfflineResult<List<Region>>) -> Unit) =
            PlatformMapCache.mergeRegions(path, onComplete)

        interface Region {
            val id: Long
            val definition: Definition

            val progress: Reactive<Progress>
            val isDownloading: Reactive<Boolean>

            fun startDownload()
            fun pauseDownload()

            fun invalidate(onComplete: (OfflineResult<Unit>) -> Unit = {})
            fun delete(onComplete: (OfflineResult<Unit>) -> Unit = {})

            sealed interface Definition {
                val minZoom: Double
                val maxZoom: Double
                val style: Style.Url

                data class Bounds(
                    val bounds: BoundingBox,
                    override val minZoom: Double,
                    override val maxZoom: Double,
                    override val style: Style.Url,
                ) : Definition

                data class Geometry(
                    val geometry: GeoJsonGeometry,
                    override val minZoom: Double,
                    override val maxZoom: Double,
                    override val style: Style.Url,
                ) : Definition
            }
        }

        data class Progress(
            val isComplete: Boolean,
            val completedResources: Long,
            val requiredResources: Long,
            val isRequiredResourceCountPrecise: Boolean,
            val completedBytes: Long,
        )
    }
}

sealed interface OfflineResult<out T> {
    data class Success<T>(val value: T) : OfflineResult<T>
    data class Error(val message: String) : OfflineResult<Nothing>
}

internal expect object PlatformMapCache {
    fun setAmbientSize(bytes: Long, onComplete: (error: Exception?) -> Unit)
    fun invalidateAmbient(onComplete: (error: Exception?) -> Unit)
    fun clearAmbient(onComplete: (error: Exception?) -> Unit)

    fun createRegion(
        definition: MapCache.Offline.Region.Definition,
        onComplete: (OfflineResult<MapCache.Offline.Region>) -> Unit,
    )

    suspend fun getRegions(): OfflineResult<List<MapCache.Offline.Region>>
    suspend fun getRegion(id: Long): OfflineResult<MapCache.Offline.Region?>
    fun mergeRegions(path: String, onComplete: (OfflineResult<List<MapCache.Offline.Region>>) -> Unit)
}
