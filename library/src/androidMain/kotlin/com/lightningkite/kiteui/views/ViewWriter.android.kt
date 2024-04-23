package com.lightningkite.kiteui.views

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import com.lightningkite.kiteui.Cancellable
import com.lightningkite.kiteui.KiteUiActivity
import com.lightningkite.kiteui.models.Action
import com.lightningkite.kiteui.models.Angle
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.reactive.CalculationContext
import com.lightningkite.kiteui.reactive.Property
import com.lightningkite.kiteui.views.direct.DesiredSizeView
import com.lightningkite.kiteui.views.direct.HasSpacingMultiplier
import com.lightningkite.kiteui.views.direct.KiteUiLayoutTransition
import io.ktor.client.HttpClient
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

/**
 * A native view in the underlying view system.
 */
actual typealias NView = View

object AndroidAppContext {
    lateinit var applicationCtx: Context
    val res: Resources by lazy { applicationCtx.resources }
    val density: Float by lazy { res.displayMetrics.density }
    val oneRem: Float by lazy { density * 14 }
    var autoCompleteLayoutResource: Int = android.R.layout.simple_list_item_1
    val ktorClient: HttpClient by lazy {
        HttpClient() {
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
}

private val ViewCalculationContexts = WeakHashMap<View, NViewCalculationContext>()
internal val ViewAction = WeakHashMap<View, Action>()

fun View.shutdown() {
    ViewCalculationContexts[this]?.cancel()
    ViewCalculationContexts.remove(this)
    ViewAction.remove(this)
}

data class NViewCalculationContext(val native: View): CalculationContext.WithLoadTracking(), Cancellable {
    val onRemove = ArrayList<()->Unit>()
    override fun cancel() {
        onRemove.forEach { it() }
        onRemove.clear()
    }
    override fun onRemove(action: () -> Unit) {
        onRemove.add(action)
    }

    val loading = Property(false)
    override fun hideLoad() {
        loading.value = false
    }

    override fun showLoad() {
        loading.value = true
    }
}

val NView.maybeCalculationContext: NViewCalculationContext?
    get() = ViewCalculationContexts.get(this)

val NView.androidCalculationContext: NViewCalculationContext
    get() = ViewCalculationContexts.getOrPut(this) { NViewCalculationContext(this) }

actual val NView.calculationContext: CalculationContext
    get() = ViewCalculationContexts.getOrPut(this) { NViewCalculationContext(this) }

actual var NView.nativeRotation: Angle
    get() = Angle(rotation / Angle.DEGREES_PER_CIRCLE)
    set(value) {
        rotation = value.degrees
    }

actual var NView.opacity: Double
    get() {
        return this.alpha.toDouble()
    }

    set(value) {
        if (animationsEnabled) {
            ValueAnimator.ofFloat(this.alpha, value.toFloat()).apply { addUpdateListener {
                this@opacity.alpha = animatedValue as Float
            } }.start()
        } else {
            this.alpha = value.toFloat()
        }
    }

actual var NView.ignoreInteraction: Boolean
    get() = !isClickable
    set(value) {
        isClickable = !value
        isFocusable = !value
    }

private fun NView.assertLayoutTransitionReady() {
//    val animateHost = (parent as? ViewGroup)
//    if (animateHost?.layoutTransition == null) animateHost?.layoutTransition = KiteUiLayoutTransition()
}

actual var NView.exists: Boolean
    get() = visibility == View.VISIBLE
    set(value) {
        assertLayoutTransitionReady()
        visibility = if (value) {
            View.VISIBLE
        } else {
            View.GONE
        }
        (parent as? DesiredSizeView)?.apply {
            visibility = this@exists.visibility
        }
    }

actual var NView.visible: Boolean
    get() = visibility == View.VISIBLE
    set(value) {
        visibility = if (value) {
            View.VISIBLE
        } else {
            View.INVISIBLE
        }
    }

actual var NView.spacing: Dimension
    get() = (this as? HasSpacingMultiplier)?.spacingOverride?.value ?: 0.px
    set(value) {
        (this as? HasSpacingMultiplier)?.spacingOverride?.value = value
    }

actual fun NView.clearNViews() {
    if (this !is ViewGroup) throw RuntimeException("clearChildren can only be called on Android ViewGroups")
    this.children.forEach { it.shutdown() }
    this.removeAllViews()
}

actual fun NView.addNView(child: NView) {
    if (this !is ViewGroup) throw RuntimeException("addChild can only be called on Android ViewGroups")
    this.addView(child)
}