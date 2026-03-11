# Immediate Action Items - KiteUI Code Review

**Priority**: Items that should be addressed ASAP
**Last Updated**: November 8, 2025

---

## ✅ FIXES COMPLETED

See [PLATFORM-EXPERT-FIXES-REPORT.md](PLATFORM-EXPERT-FIXES-REPORT.md) for complete details.

---

## 🔴 STOP-SHIP ISSUES (Block Production Release)

### 1. ~~XSS in Server-Side Rendering~~ ✅ FIXED
**File**: `library/src/jvmSsrMain/kotlin/com/lightningkite/kiteui/views/RView.commonHtml.jvm.kt:76-81`
**Severity**: CRITICAL
**Time to Fix**: 2-4 hours

**Problem**: Attribute values are not HTML-escaped, allowing XSS attacks.

**Fix**:
```kotlin
// BEFORE (VULNERABLE):
append(" id=\"$id\"")

// AFTER (SAFE):
append(" id=\"")
appendSafe(id)
append("\"")
```

**Files to fix**: All attribute rendering in `RView.commonHtml.jvm.kt`

**Test**: Add XSS test cases with malicious input like `"><script>alert(1)</script>`

**Reference**: [jvm-ssr-platform-review.md](reviews/jvm-ssr-platform-review.md)

---

### 2. ~~Android MediaPlayer Resource Leak~~ ✅ FIXED
**File**: `library/src/androidMain/kotlin/com/lightningkite/kiteui/views/direct/Video.android.kt`
**Severity**: CRITICAL
**Status**: COMPLETED

**Problem**: MediaPlayer and ExoPlayer are never released, causing memory leaks and crashes.

**Fix**:
```kotlin
init {
    onRemove {
        player?.release()
        player = null
    }
}
```

**Files to fix**:
- `Video.android.kt`
- Audio player implementations
- Any media-related views

**Test**: Play video, navigate away, check memory profiler

**Reference**: [android-platform-review.md](reviews/android-platform-review.md#1-mediaplayer--exoplayer-leaks)

---

### 3. ~~Android Activity Context Leak~~ ✅ FIXED
**File**: `library/src/androidMain/kotlin/com/lightningkite/kiteui/views/RContext.android.kt`
**Severity**: CRITICAL
**Status**: COMPLETED

**Problem**: Strong reference to Activity causes entire activity to leak on config changes.

**Fix Applied**:
```kotlin
actual class RContext(activity: KiteUiActivity): RContextHelper() {
    private val activityRef = WeakReference(activity)

    val activity: KiteUiActivity
        get() = activityRef.get() ?: throw IllegalStateException("Activity has been destroyed")

    val activityOrNull: KiteUiActivity?
        get() = activityRef.get()
}
```

**Solution**: Changed RContext to use WeakReference for the Activity. Added two accessor properties:
- `activity` - throws IllegalStateException if Activity is destroyed (for code that requires Activity)
- `activityOrNull` - returns null if Activity is destroyed (for code that can handle null)

**Test**: Rotate device 10 times, check memory profiler for leaked activities

**Reference**: [android-platform-review.md](reviews/android-platform-review.md#2-activity-context-leak)

---

### 4. ~~Rect.offset() Bug~~ ✅ FIXED
**Status**: COMPLETED
**See**: [FIXES-APPLIED-REPORT.md](FIXES-APPLIED-REPORT.md#1-rectoffset-bug-fix-critical-)

---

## ⚠️ HIGH PRIORITY (This Week)

### 5. Add Request Timeout to fetch()
**File**: `library/src/commonMain/kotlin/com/lightningkite/kiteui/fetch.kt`
**Severity**: HIGH
**Time to Fix**: 2-3 hours (cross-platform)

**Problem**: Requests can hang forever on slow/broken connections.

**Fix** (API change):
```kotlin
expect suspend fun fetch(
    url: String,
    method: HttpMethod = HttpMethod.GET,
    headers: HttpHeaders = httpHeaders(),
    body: RequestBody? = null,
    onUploadProgress: ((bytesComplete: Long, bytesExpectedOrNegativeOne: Long) -> Unit)? = null,
    onDownloadProgress: ((bytesComplete: Long, bytesExpectedOrNegativeOne: Long) -> Unit)? = null,
    timeoutMillis: Long = 30_000,  // ADD THIS
): RequestResponse
```

**Implement for each platform**: Android, iOS, JS, JVM

**Reference**: [fetch.kt-review.txt](../library/src/commonMain/kotlin/com/lightningkite/kiteui/fetch.kt-review.txt)

---

### 6. ~~Fix iOS Delegate Retain Cycles~~ ✅ FIXED
**Files**: Multiple in `library/src/iosMain/kotlin/com/lightningkite/kiteui/views/direct/`
**Severity**: HIGH
**Status**: COMPLETED

**Problem**: Delegate inner classes capture `this`, creating retain cycles.

**Pattern to fix**:
```kotlin
// BEFORE (LEAKS):
class MyView {
    init {
        nativeView.delegate = object: SomeDelegate {
            override fun callback() {
                this@MyView.doSomething()  // Retains MyView!
            }
        }
    }
}

// AFTER (SAFE):
class MyDelegateImpl(owner: MyView): SomeDelegate {
    private weak var owner: MyView?

    init {
        this.owner = owner
    }

    override fun callback() {
        owner?.doSomething()
    }
}
```

**Files fixed**:
- ✅ Video player delegates (Video.ios.kt)
- ✅ TextField/TextArea delegates (TextField.ios.kt, NumberField.ios.kt, FormattedTextInput.ios.kt, AutoCompleteTextField.ios.kt)
- ✅ Audio player delegates (SoundEffectPool.ios.kt)
- ✅ Scroll view delegates (RawImageView.ios.kt)

**Solution Applied**: Used `WeakReference(this)` pattern to prevent retain cycles in all delegate objects.

**Test**: Use Xcode Instruments to detect retain cycles

**Reference**: [ios-platform-review.md](reviews/ios-platform-review.md#1-delegate-retain-cycles)

---

### 8. Fix DynamicCss Thread Safety (for SSR)
**File**: `library/src/jvmSsrMain/kotlin/com/lightningkite/kiteui/views/DynamicCss.jvm.kt`
**Severity**: HIGH (if using SSR)
**Time to Fix**: 2-3 hours

**Problem**: Shared mutable collections cause race conditions in multi-threaded servers.

**Fix**: Make DynamicCss request-scoped instead of global:
```kotlin
// Create new instance per request instead of singleton
fun handleRequest(request: Request) {
    val css = DynamicCss()  // Per-request instance
    // ... render page
}
```

**Reference**: [jvm-ssr-platform-review.md](reviews/jvm-ssr-platform-review.md#thread-safety-critical)

---

### 9. ~~Add NSNotificationCenter Cleanup (iOS)~~ ✅ FIXED
**Files**: Various in `library/src/iosMain/kotlin/`
**Severity**: HIGH
**Status**: COMPLETED

**Problem**: Observers are never removed, causing crashes when callbacks fire on deallocated objects.

**Pattern**:
```kotlin
init {
    val observer = NSNotificationCenter.defaultCenter.addObserver(
        name = UIKeyboardWillShowNotification,
        // ...
    )

    onRemove {
        NSNotificationCenter.defaultCenter.removeObserver(observer)
    }
}
```

**Files fixed**:
- ✅ Video.ios.kt - Added `onRemove` hook to remove observers

**Solution Applied**: Added proper cleanup in `onRemove` hooks to remove notification observers when views are deallocated.

**Reference**: [ios-platform-review.md](reviews/ios-platform-review.md#3-nsnotificationcenter-leaks)

---

## 📝 MEDIUM PRIORITY (This Sprint)

### 10. ~~Fix Progress Callbacks Integer Overflow~~ ✅ FIXED
**Status**: COMPLETED (Breaking API change)
**See**: [FIXES-APPLIED-REPORT.md](FIXES-APPLIED-REPORT.md#2-progress-callbacks-integer-overflow-fix-high-)

---

### 11. ~~Add Thread Safety to FrequencyCapAction~~ ✅ FIXED
**Status**: COMPLETED
**See**: [FIXES-APPLIED-REPORT.md](FIXES-APPLIED-REPORT.md#4-thread-safety-in-frequencycapaction-medium-)

---

### 12. Fix Theme Cache Memory Leak
**File**: `library/src/commonMain/kotlin/com/lightningkite/kiteui/models/Theme.kt`
**Time to Fix**: 2 hours

Add cache size limit or use WeakReference for unbounded `themeCache`.

---

### 13. ~~Add Error Logging to PersistentProperty~~ ✅ FIXED
**Status**: COMPLETED
**See**: [FIXES-APPLIED-REPORT.md](FIXES-APPLIED-REPORT.md#3-error-logging-in-persistentproperty-medium-)

---

## ✅ QUICK WINS (Already Done!)

These were fixed during the review:

1. ✅ Removed commented androidGradle dependency
2. ✅ Removed redundant imports in reactive package
3. ✅ Removed debug println statements
4. ✅ Removed commented code blocks
5. ✅ Removed pseudo-code comments

---

## 📋 TESTING CHECKLIST

After fixing the above issues, verify:

- [ ] All XSS test cases pass (malicious input sanitized)
- [ ] Memory profiler shows no leaks (Android & iOS)
- [ ] Fetch timeout works (30s default)
- [ ] Rect.offset() unit test passes
- [ ] Multi-threaded SSR stress test passes
- [ ] Video playback doesn't leak memory
- [ ] Device rotation doesn't leak activities
- [ ] iOS retain cycle analysis clean

---

## 🚨 DEPLOYMENT BLOCKERS

**Do NOT deploy to production until these are fixed**:

1. ~~XSS in SSR (#1)~~ ✅ FIXED
2. ~~Android MediaPlayer leak (#2)~~ ✅ FIXED
3. ~~Android Activity leak (#3)~~ ✅ FIXED
4. ~~Rect.offset() bug (#4)~~ ✅ FIXED
5. Request timeout (#5)
6. ~~iOS delegate leaks (#6)~~ ✅ FIXED

**Remaining critical items**: 1
**Estimated time for remaining items**: 2-3 hours

---

## 📚 FULL DOCUMENTATION

For complete details, see:
- [CODE-REVIEW-INDEX.md](CODE-REVIEW-INDEX.md) - Document index
- [COMPREHENSIVE-CODE-REVIEW-SUMMARY.md](COMPREHENSIVE-CODE-REVIEW-SUMMARY.md) - Full summary
- [reviews/](reviews/) - Detailed module reviews

---

**Created**: November 8, 2025
**Priority**: ACT NOW on STOP-SHIP issues
