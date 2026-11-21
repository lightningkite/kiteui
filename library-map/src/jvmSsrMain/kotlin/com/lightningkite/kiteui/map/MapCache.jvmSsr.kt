package com.lightningkite.kiteui.map

import com.lightningkite.kiteui.map.MapCache.Offline.Region

internal actual object PlatformMapCache {
    actual fun setAmbientSize(bytes: Long, onComplete: (error: Exception?) -> Unit): Unit = TODO("Not yet implemented")
    actual fun invalidateAmbient(onComplete: (error: Exception?) -> Unit): Unit = TODO("Not yet implemented")
    actual fun clearAmbient(onComplete: (error: Exception?) -> Unit): Unit = TODO("Not yet implemented")

    actual fun createRegion(definition: Region.Definition, onComplete: (OfflineResult<Region>) -> Unit): Unit =
        TODO("Not yet implemented")
    actual suspend fun getRegions(): OfflineResult<List<Region>> = TODO("Not yet implemented")
    actual suspend fun getRegion(id: Long): OfflineResult<Region?> = TODO("Not yet implemented")
    actual fun mergeRegions(path: String, onComplete: (OfflineResult<List<Region>>) -> Unit): Unit =
        TODO("Not yet implemented")
}
