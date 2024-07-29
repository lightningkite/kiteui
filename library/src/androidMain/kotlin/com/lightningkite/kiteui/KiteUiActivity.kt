package com.lightningkite.kiteui

import com.lightningkite.kiteui.views.ViewWriter
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.ScreenNavigator
import com.lightningkite.kiteui.navigation.UrlLikePath
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import io.ktor.http.*
import timber.log.Timber

abstract class KiteUiActivity : AppCompatActivity() {
    open val theme: suspend () -> Theme get() = { Theme.placeholder }
    var savedInstanceState: Bundle? = null

    abstract val mainNavigator : ScreenNavigator

    val viewWriter = object: ViewWriter() {
        override val context: RContext = RContext(this@KiteUiActivity)
        override fun addChild(view: RView) {
            setContentView(view.native)
        }
        init {
            beforeNextElementSetup {
                ::themeChoice { ThemeDerivation.Set(theme()) }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppState._windowInfo.value = WindowStatistics(
            Dimension(resources.displayMetrics.widthPixels.toFloat()),
            Dimension(resources.displayMetrics.heightPixels.toFloat()),
            resources.displayMetrics.density,
        )
        AndroidAppContext.applicationCtx = this.applicationContext
        AndroidAppContext.activityCtx = this
        window?.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        Timber.plant(Timber.DebugTree())

        CalculationContext.NeverEnds.reactiveScope {
            window?.statusBarColor = theme().let { it.bar() }.background.closestColor().darken(0.3f).toInt()
        }

        savedInstanceState?.getStringArray("navStack")?.let {
            mainNavigator.stack.value = it.mapNotNull { mainNavigator.routes.parse(UrlLikePath.fromUrlString(it)) }
        } ?: run {
            mainNavigator.stack.value = (mainNavigator.routes.parse(UrlLikePath(listOf(), mapOf())) ?: mainNavigator.routes.fallback).let(::listOf)
        }
        this.savedInstanceState = savedInstanceState
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArray("navStack", mainNavigator.stack.value.mapNotNull { mainNavigator.routes.render(it)?.urlLikePath?.render() }.toTypedArray())
    }

    private var currentNum = 0
    private val onResults = HashMap<Int, (Int, Intent?)->Unit>()
    fun cancelOnResult(requestCode: Int) {
        onResults.remove(requestCode)
    }
    fun startActivityForResult(intent: Intent, options: Bundle? = null, onResult: (Int, Intent?)->Unit): Int {
        val requestCode = currentNum++
        onResults[requestCode] = onResult
        ActivityCompat.startActivityForResult(this, intent, requestCode, options)
        return requestCode
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        onResults[requestCode]?.invoke(resultCode, data)
        onResults.remove(requestCode)
        super.onActivityResult(requestCode, resultCode, data)
    }
    private val onPermissions = HashMap<Int, (PermissionResult)->Unit>()
    fun cancelOnPermissions(requestCode: Int) {
        onPermissions.remove(requestCode)
    }
    data class PermissionResult(val map: Map<String, Int>) {
        val accepted: Boolean get() = map.values.all { it == PackageManager.PERMISSION_GRANTED }
    }
    fun requestPermissions(vararg permissions: String, onResult: (PermissionResult)->Unit): Int {
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

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.data?.let {
            val path = UrlLikePath(
                segments = it.path?.split('/')?.filter { it.isNotBlank() } ?: listOf(),
                parameters = it.query?.removePrefix("?")?.split('&')?.associate {
                    it.decodeURLQueryComponent().substringBefore('=') to it.substringAfter('=', "").decodeURLQueryComponent()
                } ?: mapOf()
            )
            mainNavigator.routes.parse(path)?.let {
                mainNavigator.navigate(it)
            }
        }
    }

    override fun onResume() {
        super.onResume()

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
    }

    override fun onBackPressed() {
        if(!mainNavigator.goBack()) {
            super.onBackPressed()
        }
    }
}