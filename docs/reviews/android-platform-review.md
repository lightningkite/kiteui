# Android Platform Implementation Review

**Date:** 2025-11-08  
**Reviewer:** Claude (Automated Analysis)  
**Scope:** `/library/src/androidMain/kotlin/` (81 files)

## Executive Summary

This review analyzes the Android-specific implementation of KiteUI for lifecycle management, memory leaks, threading issues, resource handling, and Android API best practices. While the codebase shows good overall structure with proper use of reactive patterns, several **critical and high-priority issues** were identified that could lead to memory leaks, crashes, and resource exhaustion in production.

**Overall Risk Level:** ⚠️ **HIGH**

---

## 1. Overview of Android Integration

### Architecture
- **Activity:** `KiteUiActivity` - Single activity architecture with AppCompatActivity
- **View System:** Native Android views wrapped in `RView` hierarchy
- **Lifecycle:** Reactive context-based with `onRemove` cleanup callbacks
- **Threading:** Main thread enforcement with `Looper.myLooper()` checks
- **State Management:** Fine-grained reactivity using signals and reactive contexts

### Key Components
- **81 Kotlin files** in androidMain
- **View implementations:** TextView, ImageView, WebView, Video, ScrollView, etc.
- **External services:** Camera, file picker, downloads, sharing, geolocation
- **Media:** SoundPool, MediaPlayer, ExoPlayer integration
- **Network:** Ktor-based HTTP client with OkHttp engine

---

## 2. Critical Issues (P0 - Must Fix)

### 🔴 CRITICAL #1: MediaPlayer/ExoPlayer Resource Leaks
**File:** `SoundEffectPool.android.kt`, `Video.android.kt`

**Issue:**
```kotlin
// SoundEffectPool.android.kt:85-145
private val runningMediaPlayers = ArrayList<MediaPlayer>()
actual suspend fun AudioSource.load(): PlayableAudio {
    val player = MediaPlayer()
    // ... setup ...
    // NO cleanup mechanism for MediaPlayer when view is destroyed
    // NO call to player.release()
}
```

**Impact:**
- MediaPlayer holds native resources (audio decoders, memory buffers)
- ExoPlayer in Video.android.kt (line 32) is never released
- Global `runningMediaPlayers` list grows unbounded
- Can cause **OutOfMemoryError** and **audio glitches**

**Recommendation:**
```kotlin
// Add to RView lifecycle
override fun cleanup() {
    player.release()
    super.cleanup()
}

// Or use view's onRemove callback
init {
    onRemove {
        player.release()
        runningMediaPlayers.remove(player)
    }
}
```

---

### 🔴 CRITICAL #2: Activity Context Leak in RContext
**File:** `RContext.android.kt`, `ViewWriter.android.kt`

**Issue:**
```kotlin
// RContext.android.kt:13
actual class RContext(val activity: KiteUiActivity): RContextHelper() {
    // Stores direct reference to Activity - can leak if RContext outlives activity
}

// ViewWriter.android.kt:39-42
object AndroidAppContext {
    var activityCtxRef: WeakReference<KiteUiActivity>? = null
    var activityCtx: KiteUiActivity?
        get() = activityCtxRef?.get()
        set(value) { activityCtxRef = WeakReference(value) }
}
```

**Impact:**
- `RContext` holds strong reference to Activity
- If `RContext` is retained beyond Activity lifecycle (e.g., in background task, global state), entire Activity leaks
- WeakReference in `AndroidAppContext` is good, but `RContext.activity` is strong

**Recommendation:**
```kotlin
// Change RContext to use WeakReference
actual class RContext(activityRef: WeakReference<KiteUiActivity>): RContextHelper() {
    private val activityRef = activityRef
    val activity: KiteUiActivity 
        get() = activityRef.get() ?: throw IllegalStateException("Activity destroyed")
}
```

---

### 🔴 CRITICAL #3: Unbound Animator Accumulation
**File:** `RView.android.kt:271-343`, `modifiers.android.kt:423-573`

**Issue:**
```kotlin
// RView.android.kt:276-292
private var animatorTranslationX: ValueAnimator? = null
// ... 8 animator fields ...

private fun animateProperty(...): ValueAnimator? {
    existingAnimator?.cancel()  // Good: cancels previous
    // But: no listener cleanup, no removal from internal tracking
    return ValueAnimator.ofFloat(...).apply {
        duration = theme.transitionDuration.inWholeMilliseconds
        addUpdateListener { setter(it.animatedValue as Float) }
        start()  // Animator holds reference to View
    }
}
```

**Impact:**
- ValueAnimator holds strong reference to view through update listener
- If view is removed before animation completes, animator continues running
- Accumulation of animators on configuration changes
- Memory leak + wasted CPU cycles

**Recommendation:**
```kotlin
private fun animateProperty(...): ValueAnimator? {
    existingAnimator?.cancel()
    existingAnimator?.removeAllUpdateListeners()  // ADD THIS
    
    if (animationsEnabled) {
        return ValueAnimator.ofFloat(...).apply {
            addUpdateListener { setter(it.animatedValue as Float) }
            doOnEnd { removeAllUpdateListeners() }  // ADD THIS
            start()
        }
    } else {
        setter(targetValue)
        return null
    }
}

// In RView.onRemove:
override fun cleanup() {
    animatorTranslationX?.cancel()
    animatorTranslationX?.removeAllUpdateListeners()
    // ... for all 8 animators
    super.cleanup()
}
```

---

### 🔴 CRITICAL #4: VelocityTracker Not Recycled
**File:** `ScrollView.android.kt:38-82`

**Issue:**
```kotlin
class ScrollView(...) {
    private var vx = VelocityTracker.obtain()
    private var vy = VelocityTracker.obtain()
    
    setOnTouchListener { v, event ->
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                vx.recycle()  // Good
                vx = VelocityTracker.obtain()  // But: creates new instance
                vy.recycle()
                vy = VelocityTracker.obtain()
            }
            // ... but NO recycle on view destruction
        }
    }
}
```

**Impact:**
- VelocityTracker pools instances for performance
- Not recycling on view destruction leaks pooled objects
- Over time, pool exhaustion can occur

**Recommendation:**
```kotlin
init {
    onRemove {
        vx.recycle()
        vy.recycle()
    }
}
```

---

### 🔴 CRITICAL #5: WebView Not Destroyed
**File:** `WebView.android.kt:7-32`

**Issue:**
```kotlin
actual class WebView actual constructor(context: RContext): RView(context) {
    override val native = AndroidWebView(context.activity).apply {
    }
    // NO cleanup, NO destroy() call
}
```

**Impact:**
- WebView is notoriously leak-prone in Android
- Holds references to Activity through WebViewClient, WebChromeClient
- Can leak entire browser engine
- **Known Android memory leak vector**

**Recommendation:**
```kotlin
init {
    onRemove {
        native.loadUrl("about:blank")
        native.stopLoading()
        native.webChromeClient = null
        native.webViewClient = null
        native.destroy()
    }
}
```

**References:**
- [Android WebView Memory Leak](https://stackoverflow.com/questions/3130654/memory-leak-in-webview)
- [WebView Best Practices](https://developer.android.com/guide/webapps/webview)

---

## 3. High Priority Issues (P1)

### 🟠 HIGH #1: ViewTreeObserver Listener Leaks
**Files:** `ScrollView.android.kt:49-54, 256-264`, `KiteUiActivity.kt:155-170`

**Issue:**
```kotlin
// ScrollView.android.kt:50-54
val l: ViewTreeObserver.OnScrollChangedListener = ViewTreeObserver.OnScrollChangedListener {
    scrollChanged.invokeAll()
}
viewTreeObserver.addOnScrollChangedListener(l)
onRemove { viewTreeObserver.removeOnScrollChangedListener(l) }
```

**Problem:**
- `viewTreeObserver` can change when view is detached/reattached
- Storing old observer reference and removing from wrong instance
- Should store the view reference and get observer at removal time

**Recommendation:**
```kotlin
val listenerRef = WeakReference(this)
val l = ViewTreeObserver.OnScrollChangedListener {
    listenerRef.get()?.scrollChanged?.invokeAll()
}
native.viewTreeObserver.addOnScrollChangedListener(l)
onRemove { 
    native.viewTreeObserver.removeOnScrollChangedListener(l) 
}
```

---

### 🟠 HIGH #2: Glide Resource Management
**File:** `RawImageView.android.kt:113-124`

**Issue:**
```kotlin
when (val value = source) {
    is ImageLocal -> Glide.with(native).load(value.file.uri).finish()
    is ImageRaw -> Glide.with(native).load(value.data.data).finish()
    // ... NO cleanup, NO clear() call
}
```

**Impact:**
- Glide maintains internal cache and request tracking
- Not clearing requests on view destruction can leak bitmaps
- Memory pressure from unreleased bitmap resources

**Recommendation:**
```kotlin
init {
    // existing code
    onRemove {
        Glide.with(native).clear(native)
    }
}
```

---

### 🟠 HIGH #3: BottomSheetDialog Lifecycle
**File:** `BottomSheet.android.kt:27-66`

**Issue:**
```kotlin
actual fun ViewWriter.openBottomSheet(...) {
    val dialog = BottomSheetDialog(context.activity)
    // ... creates view ...
    dialog.show()
    // NO dismiss on activity destroy
    // NO cleanup of createdView
}
```

**Impact:**
- Dialog can outlive activity on configuration change
- `createdView` is not properly cleaned up
- Can cause window leaks

**Recommendation:**
```kotlin
val activity = context.activity
activity.lifecycle.addObserver(object : LifecycleObserver {
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        if (dialog.isShowing) {
            dialog.dismiss()
        }
        createdView?.shutdown()
    }
})
```

---

### 🟠 HIGH #4: FileReference URI Persistence
**File:** `fetch.android.kt:307-351`, `ExternalServices.android.kt:32-61`

**Issue:**
```kotlin
actual class FileReference(val uri: Uri)

// In requestFile:
cont.resume(od.parseResult(code, result)?.let(::FileReference))
```

**Impact:**
- URI may not have persistent permissions
- Content URIs from document picker require `takePersistableUriPermission`
- Files may become inaccessible after app restart

**Recommendation:**
```kotlin
actual class FileReference(val uri: Uri) {
    init {
        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            try {
                AndroidAppContext.applicationCtx.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: SecurityException) {
                // Permission not available for this URI
            }
        }
    }
}
```

---

### 🟠 HIGH #5: MotionEvent Recycling Issue
**File:** `ScrollView.android.kt:56-81`

**Issue:**
```kotlin
setOnTouchListener { v, event ->
    vx.addMovement(event)  // Potentially unsafe
    // MotionEvent is recycled by system after onTouch returns
}
```

**Impact:**
- `event` object is recycled by Android after `onTouchListener` returns
- Accessing it later (e.g., in async code) causes crashes
- While `vx.addMovement` is likely safe (copies internally), should verify

**Recommendation:**
- Current code appears safe, but add defensive comment
- Avoid storing MotionEvent references

---

## 4. Medium Priority Issues (P2)

### 🟡 MEDIUM #1: SharedPreferences Apply vs Commit
**File:** `PlatformStorage.android.kt:19-24`

**Issue:**
```kotlin
actual fun set(key: String, value: String) {
    preferences.edit().putString(key, value).apply()  // async
}
```

**Impact:**
- `.apply()` is asynchronous - writes may not complete before app death
- Critical data (auth tokens, user state) could be lost
- `.commit()` is synchronous and returns success boolean

**Recommendation:**
```kotlin
actual fun set(key: String, value: String) {
    val success = preferences.edit().putString(key, value).commit()
    if (!success) {
        LogRoot.warn("Failed to save preference: $key")
    }
}
```

---

### 🟡 MEDIUM #2: Nullable Player Access
**File:** `Video.android.kt:46-167`

**Issue:**
```kotlin
native.player!!.setMediaItem(...)  // Forced non-null assertion
native.player!!.prepare()
native.player!!.addListener(l)
```

**Impact:**
- Multiple `!!` operators throughout
- If `player` is null (shouldn't be, but defensive programming), crash
- Better to initialize player safely

**Recommendation:**
```kotlin
private val player: ExoPlayer = ExoPlayer.Builder(context.activity).build().also {
    native.player = it
}

// Then use: player.setMediaItem(...) without !!
```

---

### 🟡 MEDIUM #3: Animator Memory in SwapView
**File:** `SwapView.android.kt:186-191`

**Issue:**
```kotlin
private fun View.toBitmapDrawable(): BitmapDrawable {
    val b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    draw(android.graphics.Canvas(b))
    return BitmapDrawable(resources, b)
}
```

**Impact:**
- Creates full-size bitmap for transition animation
- Large views = large bitmaps = memory pressure
- Bitmap never explicitly recycled
- Can cause OutOfMemoryError on large screens

**Recommendation:**
```kotlin
// Add cleanup
private fun View.toBitmapDrawable(): BitmapDrawable {
    val b = Bitmap.createBitmap(
        width.coerceAtMost(1920),  // Limit size
        height.coerceAtMost(1920),
        Bitmap.Config.RGB_565  // Use 565 if alpha not needed
    )
    // ... existing code
    return BitmapDrawable(resources, b)
}

// In CustomTransition.createAnimator, add cleanup:
addListener(onEnd = {
    sceneRoot.overlay.remove(startDummy)
    endDummy?.let { 
        sceneRoot.overlay.remove(it)
        // Recycle bitmaps
        (it as? BitmapDrawable)?.bitmap?.recycle()
    }
    (startDummy as? BitmapDrawable)?.bitmap?.recycle()
    endValues.view.visibility = View.VISIBLE
})
```

---

### 🟡 MEDIUM #4: Download Manager Leak
**File:** `ExternalServices.android.kt:196-206`

**Issue:**
```kotlin
private fun downloadContinued(name: String, url: String) {
    val request = DownloadManager.Request(url.toUri())
        .setNotificationVisibility(...)
        .setDestinationInExternalPublicDir(...)
    (AndroidAppContext.applicationCtx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager)
        .enqueue(request)
    // NO tracking of download ID
    // NO BroadcastReceiver for completion
}
```

**Impact:**
- Cannot track download completion
- Cannot handle errors
- User has no feedback if download fails

**Recommendation:**
```kotlin
private fun downloadContinued(name: String, url: String) {
    val downloadManager = AndroidAppContext.applicationCtx
        .getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    val downloadId = downloadManager.enqueue(request)
    
    // Register receiver for completion
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (id == downloadId) {
                // Handle completion
                context.unregisterReceiver(this)
            }
        }
    }
    AndroidAppContext.applicationCtx.registerReceiver(
        receiver,
        IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
    )
}
```

---

### 🟡 MEDIUM #5: ThreadPoolExecutor Configuration
**File:** `ViewWriter.android.kt:43-45`

**Issue:**
```kotlin
val executor by lazy {
    ThreadPoolExecutor(1, 1, 10, TimeUnit.SECONDS, ArrayBlockingQueue(10))
}
```

**Impact:**
- Single thread executor with queue size of 10
- 11th task will throw `RejectedExecutionException`
- No rejection policy specified
- Tasks can be lost silently

**Recommendation:**
```kotlin
val executor by lazy {
    ThreadPoolExecutor(
        1, 1, 
        10, TimeUnit.SECONDS, 
        LinkedBlockingQueue(),  // Unbounded queue
        ThreadPoolExecutor.CallerRunsPolicy()  // Fallback policy
    )
}
```

---

## 5. Threading & Concurrency

### ✅ Good Practices
1. **Main thread enforcement:** `RView` constructor checks `Looper.myLooper()` (line 38)
2. **Handler usage:** Properly uses `Handler(Looper.getMainLooper())` for callbacks
3. **Coroutine dispatchers:** Correct use of `Dispatchers.Main` and `Dispatchers.IO`

### ⚠️ Concerns

**#1: WebSocket Threading**
**File:** `fetch.android.kt:209-274`
```kotlin
AppScope.launch(Dispatchers.IO) {
    client.webSocket(url) {
        withContext(Dispatchers.Main) {
            onOpen.forEach { it() }  // Could throw if list modified concurrently
        }
    }
}
```

**Issue:** `onOpen`, `onClose`, `onMessage` ArrayLists accessed from multiple threads without synchronization

**Fix:**
```kotlin
private val onOpen = Collections.synchronizedList(ArrayList<() -> Unit>())
```

---

**#2: Race Condition in keepScreenOn**
**File:** `AppState.android.kt:32-48`
```kotlin
private var currentLockCount = 0
actual fun keepScreenOn(scope: CoroutineScope) {
    if(currentLockCount++ == 0) {  // NOT THREAD-SAFE
        // ...
    }
}
```

**Fix:**
```kotlin
private val currentLockCount = AtomicInteger(0)
actual fun keepScreenOn(scope: CoroutineScope) {
    if(currentLockCount.incrementAndGet() == 1) {
        // ...
    }
}
```

---

## 6. Resource Management

### Critical Resources Requiring Cleanup

| Resource Type | File | Status | Risk |
|--------------|------|--------|------|
| MediaPlayer | SoundEffectPool.android.kt | ❌ Not released | HIGH |
| ExoPlayer | Video.android.kt | ❌ Not released | HIGH |
| WebView | WebView.android.kt | ❌ Not destroyed | HIGH |
| ValueAnimator | RView.android.kt | ⚠️ Partial cleanup | MEDIUM |
| VelocityTracker | ScrollView.android.kt | ❌ Not recycled | MEDIUM |
| Bitmap | SwapView.android.kt | ❌ Not recycled | MEDIUM |
| Glide requests | RawImageView.android.kt | ⚠️ No explicit clear | MEDIUM |
| SoundPool | SoundEffectPool.android.kt | ❌ Never released | LOW |
| HttpClient | fetch.android.kt | ✅ Singleton OK | LOW |

---

## 7. Android API Usage

### ✅ Proper Usage
1. **Activity Result API:** Uses modern `ActivityResultContracts` (ExternalServices.android.kt)
2. **Edge-to-edge:** Properly implements `enableEdgeToEdge()` and insets (KiteUiActivity.kt)
3. **Permissions:** Correct permission request flow with callbacks
4. **FileProvider:** Uses FileProvider for sharing files (ExternalServices.android.kt:253)

### ⚠️ Deprecated API Usage

**#1: onActivityResult (Deprecated)**
**File:** `KiteUiActivity.kt:119-124`
```kotlin
@Deprecated("Deprecated in Java")
override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    // Still using old callback-based API
}
```

**Impact:** Works but not future-proof

**Recommendation:** Migrate to `registerForActivityResult` with modern contracts

---

**#2: onBackPressed (Deprecated in API 33+)**
**File:** `KiteUiActivity.kt:232-236`
```kotlin
override fun onBackPressed() {
    if(!mainNavigator.goBack()) {
        super.onBackPressed()
    }
}
```

**Recommendation:**
```kotlin
init {
    onBackPressedDispatcher.addCallback(this) {
        if (!mainNavigator.goBack()) {
            isEnabled = false
            onBackPressedDispatcher.onBackPressed()
        }
    }
}
```

---

### 🐛 API Misuse

**#1: Suppressed MissingPermission**
**File:** `ExternalServices.android.kt:175, 208`
```kotlin
@SuppressLint("MissingPermission")
actual suspend fun RContext.download(...) {
    // Suppresses warning without actually checking permission
}
```

**Issue:** Permission may not be granted, causing crash

**Fix:** Properly request permission before suppressing

---

## 8. Memory Leak Detection

### Recommended Testing
1. **LeakCanary Integration:**
```kotlin
dependencies {
    debugImplementation 'com.squareup.leakcanary:leakcanary-android:2.12'
}
```

2. **Lifecycle Testing:**
- Rotate device multiple times
- Navigate between screens
- Play videos and destroy views
- Test WebView creation/destruction

3. **Heap Dump Analysis:**
- Take heap dumps after operations
- Look for `RView`, `MediaPlayer`, `WebView` instances
- Check retention paths

---

## 9. Performance Concerns

### 🔴 Performance #1: String Concatenation in Logging
**File:** Multiple files using `println()` and string interpolation

```kotlin
println("Swapping to $newViewHolder.  RV: ${newViewHolder}, V: ${newViewHolder?.native}")
```

**Issue:** String building happens even when logging disabled

**Fix:**
```kotlin
if (BuildConfig.DEBUG) {
    println("Swapping to $newViewHolder...")
}
```

---

### 🟡 Performance #2: Unnecessary View Invalidation
**File:** `modifiers.android.kt:447-464`

```kotlin
addUpdateListener {
    (native.layoutParams as? SimplifiedLinearLayoutLayoutParams)?.gapRatio = it.animatedFraction
}
```

**Issue:** May trigger layout pass on every animation frame

**Optimization:** Batch updates or use translation instead of layout changes

---

## 10. Security Issues

### 🟠 Security #1: Unvalidated Download Names
**File:** `ExternalServices.android.kt:173-184`
```kotlin
private val validDownloadName = Regex("[a-zA-Z0-9.\\-_]+")

if (!name.matches(validDownloadName)) throw IllegalArgumentException(...)
```

**Issue:** Regex validation but could still have path traversal attempts

**Recommendation:** Also validate against `..`, absolute paths

---

### 🟠 Security #2: WebView JavaScript
**File:** `WebView.android.kt:17-24`
```kotlin
actual var permitJs: Boolean
    get() = native.settings.javaScriptEnabled
    set(value) {
        native.settings.javaScriptEnabled = value
    }
```

**Issue:** No JavaScript bridge security
- Missing `addJavascriptInterface` restrictions
- No CSP (Content Security Policy)
- Loading arbitrary URLs without validation

**Recommendation:**
```kotlin
init {
    native.settings.apply {
        javaScriptEnabled = false  // Default to false
        allowFileAccess = false
        allowContentAccess = false
        databaseEnabled = false
    }
}
```

---

## 11. Recommendations Summary

### Immediate Actions (P0)
1. ✅ **Release MediaPlayer/ExoPlayer** in view cleanup
2. ✅ **Destroy WebView** properly with full cleanup
3. ✅ **Clean up ValueAnimators** - remove listeners
4. ✅ **Recycle VelocityTracker** on view destruction
5. ✅ **Fix Activity reference** in RContext (use WeakReference)

### Short-term (P1)
6. ✅ Add Glide request clearing
7. ✅ Fix ViewTreeObserver listener management
8. ✅ Add BottomSheet lifecycle observers
9. ✅ Implement URI permission persistence
10. ✅ Add thread-safety to WebSocket callbacks

### Medium-term (P2)
11. Use `.commit()` for critical SharedPreferences
12. Add bitmap recycling in transitions
13. Fix ThreadPoolExecutor rejection policy
14. Migrate deprecated APIs (onBackPressed, onActivityResult)
15. Add proper download tracking

### Testing & Monitoring
16. Integrate LeakCanary for leak detection
17. Add lifecycle test suite (rotation, navigation)
18. Profile memory usage under load
19. Test WebView scenarios extensively
20. Add crash reporting (Firebase Crashlytics)

---

## 12. Code Quality Metrics

### Positive Aspects
- ✅ Good separation of concerns (platform-specific vs common)
- ✅ Reactive architecture minimizes manual state management
- ✅ Proper use of Kotlin coroutines
- ✅ Modern Android APIs (ActivityResultContracts, etc.)
- ✅ Edge-to-edge support

### Areas for Improvement
- ❌ Extensive use of `!!` operators (115 occurrences)
- ❌ Missing resource cleanup in multiple places
- ❌ Inconsistent error handling
- ⚠️ Some suppressed lint warnings without proper fixes
- ⚠️ Limited documentation on lifecycle expectations

---

## 13. Testing Recommendations

### Unit Tests Needed
```kotlin
class RViewLifecycleTest {
    @Test
    fun `onRemove callbacks are invoked on view destruction`() {
        val view = RView(mockContext)
        var cleaned = false
        view.onRemove { cleaned = true }
        view.shutdown()
        assertTrue(cleaned)
    }
}
```

### Integration Tests
```kotlin
class MediaPlayerLeakTest {
    @Test
    fun `media player is released on view destruction`() {
        val audio = AudioResource(R.raw.test_sound)
        val playable = runBlocking { audio.load() }
        val view = // create view with audio
        view.shutdown()
        // Assert MediaPlayer.release() was called
    }
}
```

### LeakCanary Checks
- Enable LeakCanary in debug builds
- Test all major user flows
- Focus on: navigation, media playback, WebView usage

---

## 14. Conclusion

The Android implementation of KiteUI demonstrates good architectural decisions and modern Android practices. However, **critical resource management issues** must be addressed before production use. The most severe risks are:

1. **MediaPlayer/ExoPlayer leaks** - Can cause crashes and audio issues
2. **WebView leaks** - Well-known Android leak vector
3. **Activity context retention** - Can leak entire activities
4. **Unmanaged animators** - Accumulate over time

**Priority:** Address all P0 issues immediately. These are production blockers that will cause real-world issues at scale.

**Timeline Estimate:**
- P0 fixes: 2-3 days
- P1 fixes: 1 week
- P2 fixes: 2 weeks
- Testing & validation: 1 week

**Risk after fixes:** ⚠️ MEDIUM → ✅ LOW

---

## Appendix A: File-by-File Risk Assessment

| File | Risk | Primary Concerns |
|------|------|------------------|
| SoundEffectPool.android.kt | 🔴 HIGH | MediaPlayer never released |
| Video.android.kt | 🔴 HIGH | ExoPlayer never released |
| WebView.android.kt | 🔴 HIGH | WebView not destroyed |
| RView.android.kt | 🔴 HIGH | Animator accumulation |
| ScrollView.android.kt | 🟠 MEDIUM | VelocityTracker not recycled |
| SwapView.android.kt | 🟠 MEDIUM | Bitmap leaks in transitions |
| RawImageView.android.kt | 🟠 MEDIUM | Glide cleanup missing |
| BottomSheet.android.kt | 🟠 MEDIUM | Dialog lifecycle issues |
| fetch.android.kt | 🟡 LOW | Thread safety in WebSocket |
| KiteUiActivity.kt | 🟡 LOW | Deprecated API usage |

---

**End of Report**
