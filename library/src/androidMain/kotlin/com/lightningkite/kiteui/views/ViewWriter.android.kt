package com.lightningkite.kiteui.views

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import com.lightningkite.kiteui.CloseAbortsHandshakeWebSocketFactory
import com.lightningkite.kiteui.KiteUiActivity
import com.lightningkite.kiteui.userAgent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.cache.*
import io.ktor.client.plugins.cache.storage.*
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import com.lightningkite.kiteui.nativeCookies
import io.ktor.client.plugins.websocket.WebSockets
import kotlinx.coroutines.suspendCancellableCoroutine
import java.lang.ref.WeakReference
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration
import okhttp3.OkHttpClient

public object AndroidAppContext {
    public lateinit var applicationCtx: Context
    internal val res: Resources by lazy { applicationCtx.resources }
    public val density: Float by lazy { res.displayMetrics.density }
    public val oneRem: Float by lazy { density * 14 }
    public var autoCompleteLayoutResource: Int = android.R.layout.simple_list_item_1
    // A seam for host tests, which have neither the Keystore nor real preferences. Read once, when the client is made.
    internal var cookiesStorage: CookiesStorage = nativeCookies
    internal val ktorClient: HttpClient by lazy {
        val connectTimeout = 30.seconds
        val socketTimeout = 120.seconds
        // The sockets' handshakes are made by this client rather than one Ktor derives from HttpTimeout, so it is given the same timeouts.
        val webSocketClient = OkHttpClient.Builder()
            .connectTimeout(connectTimeout.toJavaDuration())
            .readTimeout(socketTimeout.toJavaDuration())
            .writeTimeout(socketTimeout.toJavaDuration())
            .build()
        HttpClient(OkHttp) {
            engine { webSocketFactory = CloseAbortsHandshakeWebSocketFactory(webSocketClient) }
            install(WebSockets)
            install(UserAgent) {
                agent = com.lightningkite.kiteui.Platform.userAgent
            }
            install(HttpCache) {
                publicStorage(FileStorage(applicationCtx.cacheDir.resolve("cachehttp")))
            }
            install(HttpCookies) { storage = cookiesStorage }
            install(HttpTimeout) {
                requestTimeoutMillis = 120_000
                connectTimeoutMillis = connectTimeout.inWholeMilliseconds
                socketTimeoutMillis = socketTimeout.inWholeMilliseconds
            }
        }
    }
    internal var activityCtxRef: WeakReference<KiteUiActivity>? = null
    public var activityCtx: KiteUiActivity?
        get() = activityCtxRef?.get()
        set(value) { activityCtxRef = WeakReference(value) }
    public val executor: ThreadPoolExecutor by lazy {
        ThreadPoolExecutor(1, 1, 10, TimeUnit.SECONDS, ArrayBlockingQueue(10))
    }

    /**
     * All KiteUI Android apps should have a file provider defined in their manifest with the authority on the file
     * provider set as follows, based on the Android package name. The file provider is used in the implementation of
     * several ExternalServices and is referenced using this authority string.
     */
    public val fileProviderAuthority: String get() = applicationCtx.packageName + ".fileprovider"

    public fun startActivityForResult(intent: Intent, options: Bundle? = null, onResult: (Int, Intent?)->Unit): Int? = activityCtx?.startActivityForResult(intent = intent, options = options, onResult = onResult)
    public fun requestPermissions(vararg permissions: String, onResult: (KiteUiActivity.PermissionResult)->Unit): Int? = activityCtx?.requestPermissions(permissions = permissions, onResult = onResult)
    public suspend fun requestPermissions(vararg permissions: String): KiteUiActivity.PermissionResult = suspendCancellableCoroutine { continuation ->
        val code = requestPermissions(*permissions) {
            continuation.resume(it)
        }
        continuation.invokeOnCancellation {
            code?.let { c -> activityCtx?.cancelOnPermissions(c) }
        }
    }
}
