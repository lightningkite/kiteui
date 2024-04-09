package com.lightningkite.kiteui

import android.animation.ValueAnimator
import android.annotation.TargetApi
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.WindowStatistics
import com.lightningkite.kiteui.navigation.PlatformNavigator
import com.lightningkite.kiteui.navigation.ScreenStack
import com.lightningkite.kiteui.navigation.UrlLikePath
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.AndroidAppContext
import timber.log.Timber

abstract class KiteUiActivity : AppCompatActivity() {
    open val theme: suspend () -> Theme get() = { Theme() }
    var savedInstanceState: Bundle? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowInfo.value = WindowStatistics(
            Dimension(resources.displayMetrics.widthPixels.toFloat()),
            Dimension(resources.displayMetrics.heightPixels.toFloat()),
            resources.displayMetrics.density,
        )
        AndroidAppContext.applicationCtx = this.applicationContext
        AndroidAppContext.activityCtx = this
        window?.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        Timber.plant(Timber.DebugTree())

        CalculationContext.NeverEnds.reactiveScope {
            window?.statusBarColor = theme().let { it.bar() ?: it }.background.closestColor().darken(0.3f).toInt()
        }
        this.savedInstanceState = savedInstanceState
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArray("navStack", ScreenStack.main.stack.value.mapNotNull { ScreenStack.mainRoutes.render(it)?.urlLikePath?.render() }.toTypedArray())
    }

    private var currentNum = 0
    private val onResults = HashMap<Int, (Int, Intent?)->Unit>()
    fun startActivityForResult(intent: Intent, options: Bundle? = null, onResult: (Int, Intent?)->Unit) {
        val requestCode = currentNum++
        onResults[requestCode] = onResult
        ActivityCompat.startActivityForResult(this, intent, requestCode, options)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        onResults[requestCode]?.invoke(resultCode, data)
        onResults.remove(requestCode)
        super.onActivityResult(requestCode, resultCode, data)
    }
    private val onPermissions = HashMap<Int, (PermissionResult)->Unit>()
    data class PermissionResult(val map: Map<String, Int>) {
        val accepted: Boolean get() = map.values.all { it == PackageManager.PERMISSION_GRANTED }
    }
    fun requestPermissions(vararg permissions: String, onResult: (PermissionResult)->Unit) {
        val ungranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if(ungranted.isEmpty()) return onResult(PermissionResult(mapOf()))
        val requestCode = currentNum++
        onPermissions[requestCode] = onResult
        ActivityCompat.requestPermissions(this, ungranted.toTypedArray(), requestCode)
    }

    @TargetApi(23)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (Build.VERSION.SDK_INT >= 23) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            onPermissions[requestCode]?.invoke(PermissionResult(permissions.indices.associate { permissions[it] to grantResults[it] }))
            onPermissions.remove(requestCode)
        }
    }

    private var animator: ValueAnimator? = null
    private var suppressKeyboardChange = false
    private val keyboardTreeObs: ViewTreeObserver.OnGlobalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
        val rect = Rect()
        window.decorView.getWindowVisibleDisplayFrame(rect)
        val keyboardHeight = resources.displayMetrics.heightPixels - rect.bottom
        if (keyboardHeight.toFloat() > resources.displayMetrics.heightPixels * 0.15f) {
            suppressKeyboardChange = true
            SoftInputOpen.value = true
            suppressKeyboardChange = false
        } else {
            afterTimeout(30L) {
                suppressKeyboardChange = true
                SoftInputOpen.value = false
                suppressKeyboardChange = false
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
                AnimationFrame.frame()
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
        if(!PlatformNavigator.goBack()) {
            super.onBackPressed()
        }
    }
}