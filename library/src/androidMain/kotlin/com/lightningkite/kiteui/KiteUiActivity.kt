package com.lightningkite.kiteui

import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.ViewGroupCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.lightningkite.kiteui.gamepad.Gamepads
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.navigation.UrlLikePath
import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.ReactiveContext
import com.lightningkite.reactive.context.onRemove
import com.lightningkite.reactive.core.Signal
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlin.math.max

public abstract class KiteUiActivity : AppCompatActivity() {
    public open val theme: ReactiveContext.() -> Theme get() = { Theme.placeholder }
    public var savedInstanceState: Bundle? = null

    public abstract val mainNavigator : PageNavigator

    public lateinit var root: Element
    private val safeInsetsProperty = Signal(Edges.ZERO)
    public val viewWriter: ViewWriter = object: ViewWriter, CoroutineScope by this.lifecycleScope {
        override val context: ElementContext = ElementContext(this@KiteUiActivity)
        init {
            context.safeInsets = safeInsetsProperty
        }

        @OverrideOnly
        override fun willAddChild(element: Element) {
            element::themeChoice { ThemeDerivation.SetAsBase(theme()) }
        }

        @OverrideOnly
        override fun addChild(element: Element) {
            root = element
            setContentView(element.native)
            ViewGroupCompat.installCompatInsetsDispatch(element.native)
            val l = OnApplyWindowInsetsListener { v: View, insetsGetter: WindowInsetsCompat ->
                val insetsSystem = insetsGetter.getInsets(WindowInsetsCompat.Type.systemBars())
                val insetsInput = insetsGetter.getInsets(WindowInsetsCompat.Type.ime())
                val safeInsets = Edges(
                    left = max(insetsSystem.left, insetsInput.left).px,
                    top = max(insetsSystem.top, insetsInput.top).px,
                    right = max(insetsSystem.right, insetsInput.right).px,
                    bottom = max(insetsSystem.bottom, insetsInput.bottom).px,
                )
                println("OnApplyWindowInsetsListener: $safeInsets")
                safeInsetsProperty.value = safeInsets
                WindowInsetsCompat.CONSUMED
            }
            ViewCompat.setOnApplyWindowInsetsListener(element.native, l)
            element.onRemove { ViewCompat.setOnApplyWindowInsetsListener(element.native, null) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppState._windowInfo.value = WindowStatistics(
            Dimension(resources.displayMetrics.widthPixels.toFloat()),
            Dimension(resources.displayMetrics.heightPixels.toFloat()),
            resources.displayMetrics.density,
        )
        AndroidAppContext.applicationCtx = this.applicationContext
        AndroidAppContext.activityCtx = this

        savedInstanceState?.getStringArray("navStack")?.let {
            mainNavigator.stack.value = it.mapNotNull { mainNavigator.routes.parse(UrlLikePath.fromUrlString(it)) }
        } ?: run {
            mainNavigator.stack.value = (mainNavigator.routes.parse(UrlLikePath(listOf(), mapOf())) ?: mainNavigator.routes.fallback).let(::listOf)
        }
        this.savedInstanceState = savedInstanceState
        onNewIntent(intent)

        // Use modern back handling API instead of deprecated onBackPressed()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Dismiss the topmost open dismissable dialog before navigating pages.
                if (viewWriter.context.dismissTopDialog()) return
                if (!mainNavigator.goBack()) {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArray("navStack", mainNavigator.stack.value.mapNotNull { mainNavigator.routes.render(it)?.urlLikePath?.render() }.toTypedArray())
    }

    private var currentNum = 0
    private val onResults = HashMap<Int, (Int, Intent?)->Unit>()
    public fun cancelOnResult(requestCode: Int) {
        onResults.remove(requestCode)
    }
    public fun startActivityForResult(intent: Intent, options: Bundle? = null, onResult: (Int, Intent?)->Unit): Int {
        val requestCode = currentNum++
        onResults[requestCode] = onResult
        ActivityCompat.startActivityForResult(this, intent, requestCode, options)
        return requestCode
    }
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        onResults[requestCode]?.invoke(resultCode, data)
        onResults.remove(requestCode)
        super.onActivityResult(requestCode, resultCode, data)
    }
    private val onPermissions = HashMap<Int, (PermissionResult)->Unit>()
    public fun cancelOnPermissions(requestCode: Int) {
        onPermissions.remove(requestCode)
    }
    public data class PermissionResult(val map: Map<String, Int>) {
        val accepted: Boolean get() = map.values.all { it == PackageManager.PERMISSION_GRANTED }
    }
    public fun requestPermissions(vararg permissions: String, onResult: (PermissionResult)->Unit): Int {
        val ungranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if(ungranted.isEmpty()) {
            onResult(PermissionResult(mapOf()))
            return -1
        }
        val requestCode = currentNum++
        onPermissions[requestCode] = onResult
        ActivityCompat.requestPermissions(this, ungranted.toTypedArray(), requestCode)
        return requestCode
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onPermissions[requestCode]?.invoke(PermissionResult(permissions.indices.associate { permissions[it] to grantResults[it] }))
        onPermissions.remove(requestCode)

    }

    private var animator: ValueAnimator? = null
    private var suppressKeyboardChange = false
    private val keyboardTreeObs: ViewTreeObserver.OnGlobalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
        val rect = Rect()
        window.decorView.getWindowVisibleDisplayFrame(rect)
        val keyboardHeight = resources.displayMetrics.heightPixels - rect.bottom
        if (keyboardHeight.toFloat() > resources.displayMetrics.heightPixels * 0.15f) {
            suppressKeyboardChange = true
            AppState._softInputOpen.value = true
            suppressKeyboardChange = false
        } else {
            afterTimeout(30L) {
                suppressKeyboardChange = true
                AppState._softInputOpen.value = false
                suppressKeyboardChange = false
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.data?.let { it ->
            val path = UrlLikePath(
                segments = it.path?.split('/')?.filter { it.isNotBlank() } ?: listOf(),
                parameters = it.query?.removePrefix("?")?.split('&')?.associate {
                    // Split on the raw '=' delimiter first, THEN decode each half; decoding before
                    // splitting would let an encoded '=' (%3D) inside a value corrupt the key/value split.
                    it.substringBefore('=').decodeURLQueryComponent() to it.substringAfter('=', "").decodeURLQueryComponent()
                } ?: mapOf()
            )
            mainNavigator.routes.parse(path)?.let {
                mainNavigator.navigate(it)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AppState._inForeground.value = true
        animator = ValueAnimator().apply {
            setIntValues(0, 100)
            duration = 10000L
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            var last = System.currentTimeMillis()
            addUpdateListener {
                AppState._animationFrame.invokeAll()
            }
            start()
        }

        this.findViewById<View>(android.R.id.content).viewTreeObserver.addOnGlobalLayoutListener(keyboardTreeObs)
//        keyboardSubscriber = ApplicationAccess.softInputActive.subscribe {
//            if (!suppressKeyboardChange) {
//                view.post {
//                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//                    if (it) {
//                        if (currentFocus == null) {
//                            FocusFinder.getInstance().findNextFocus(view as ViewGroup, view, View.FOCUS_DOWN)
//                        }
//                        currentFocus?.let {
//                            imm.showSoftInput(it, 0)
//                        }
//                    } else {
//                        imm.hideSoftInputFromWindow(view.windowToken, 0)
//                    }
//                }
//            }
//        }
    }

    override fun onPause() {
        this.findViewById<View>(android.R.id.content).viewTreeObserver.removeOnGlobalLayoutListener(keyboardTreeObs)
//        keyboardSubscriber?.dispose()
//        keyboardSubscriber = null
        animator?.pause()
        animator = null
        super.onPause()
        AppState._inForeground.value = false
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (Gamepads.handleMotionEvent(event)) return true
        return super.onGenericMotionEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (Gamepads.handleKeyDown(keyCode, event)) return true
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (Gamepads.handleKeyUp(keyCode, event)) return true
        return super.onKeyUp(keyCode, event)
    }
}