package com.lightningkite.kiteui

import com.lightningkite.kiteui.views.ExtensionProperty
import com.lightningkite.kiteui.views.direct.ImageCache
import platform.Foundation.*
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.ref.WeakReference
import kotlin.native.runtime.GC
import kotlin.native.runtime.NativeRuntimeApi
import kotlin.reflect.KClass

actual fun debugger() {
}

@OptIn(ExperimentalNativeApi::class)
object ObjCountTrackers {
    private val allWeak = HashMap<KClass<*>, ArrayList<WeakReference<Any>>>()
    val alive: Map<KClass<*>, Int> get() {
        allWeak.values.forEach { it.removeAll { it.get() == null } }
        return allWeak.mapValues { it.value.size }
    }
    fun track(instance: Any) {
        allWeak.getOrPut(instance::class) {
            println("Tracking type ${instance::class.qualifiedName}")
            ArrayList()
        }.add(WeakReference(instance))
    }
}

@OptIn(NativeRuntimeApi::class, ExperimentalStdlibApi::class)
actual fun gc(): GCInfo {
    repeat(3) {
        GC.collect()
        NSRunLoop.currentRunLoop()
            .runMode(mode = NSDefaultRunLoopMode, beforeDate = NSDate.dateWithTimeIntervalSinceNow(0.1))
    }
    return GCInfo(GC.lastGCInfo!!.memoryUsageAfter["heap"]?.totalObjectsSizeBytes ?: -1L)
}

actual fun cleanImageCache() {
    ImageCache.imageCache.removeAllObjects()
    ImageCache.imageCacheSized.removeAllObjects()
}

actual fun gcReport() {
    ObjCountTrackers.alive.entries.forEach {
        println("${it.key.qualifiedName}.alive = ${it.value}")
    }
    ExtensionProperty.debug()
}

actual fun assertMainThread() {
    if(!NSThread.isMainThread) throw Error("NOT MAIN THREAD!!!")
}