package com.lightningkite.kiteui.views

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import androidx.core.view.children
import com.lightningkite.kiteui.KiteUiActivity
import com.lightningkite.kiteui.models.Action
import com.lightningkite.kiteui.models.Angle
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.reactive.CalculationContext
import com.lightningkite.kiteui.reactive.Property
import com.lightningkite.kiteui.reactive.invokeAllSafe
import com.lightningkite.kiteui.suspendCoroutineCancellable
import com.lightningkite.kiteui.views.direct.DesiredSizeView
import com.lightningkite.kiteui.views.direct.ViewPager
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.plugins.cache.storage.*
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.http.*
import java.lang.RuntimeException
import java.lang.ref.WeakReference
import java.util.WeakHashMap
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object AndroidAppContext {
    lateinit var applicationCtx: Context
    val res: Resources by lazy { applicationCtx.resources }
    val density: Float by lazy { res.displayMetrics.density }
    val oneRem: Float by lazy { density * 14 }
    var autoCompleteLayoutResource: Int = android.R.layout.simple_list_item_1
    val ktorClient: HttpClient by lazy {
        HttpClient(OkHttp) {
            install(WebSockets)
            install(HttpCache) {
                publicStorage(FileStorage(applicationCtx.cacheDir.resolve("cachehttp")))
            }
        }
    }
    var activityCtxRef: WeakReference<KiteUiActivity>? = null
    var activityCtx: KiteUiActivity?
        get() = activityCtxRef?.get()
        set(value) { activityCtxRef = WeakReference(value) }
    val executor by lazy {
        ThreadPoolExecutor(1, 1, 10, TimeUnit.SECONDS, ArrayBlockingQueue(10))
    }

    fun startActivityForResult(intent: Intent, options: Bundle? = null, onResult: (Int, Intent?)->Unit) = activityCtx?.startActivityForResult(intent = intent, options = options, onResult = onResult)
    fun requestPermissions(vararg permissions: String, onResult: (KiteUiActivity.PermissionResult)->Unit) = activityCtx?.requestPermissions(permissions = permissions, onResult = onResult)
    suspend fun requestPermissions(vararg permissions: String): KiteUiActivity.PermissionResult = suspendCoroutineCancellable { continuation ->
        val code = requestPermissions(*permissions) {
            continuation.resume(it)
        }
        return@suspendCoroutineCancellable {
            code?.let { c -> activityCtx?.cancelOnPermissions(c) }
        }
    }
}
