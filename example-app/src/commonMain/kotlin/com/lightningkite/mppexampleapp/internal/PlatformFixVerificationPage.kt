package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Blob
import com.lightningkite.kiteui.ExperimentalKiteUi
import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.Platform
import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.SoundEffectPool
import com.lightningkite.kiteui.Untested
import com.lightningkite.kiteui.current
import com.lightningkite.kiteui.fetch
import com.lightningkite.kiteui.lottie.views.direct.lottie
import com.lightningkite.kiteui.models.AudioRaw
import com.lightningkite.kiteui.models.AudioRemote
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.dialog
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*

/**
 * Manual verification for a second batch of review fixes that no automated test can reach:
 * the failure mode is audible sound, a rendered native view, or hardware/permission state.
 * Companion to [ReviewFixVerificationPage], which covers the fixes that page already handles -
 * see docs/MANUAL_VERIFICATION.md for the combined checklist.
 *
 * Each section says what to do, what platform it applies to, and what correct looks like.
 * Where the result is a judgement call (does it sound right, does the ring look right) that is
 * stated plainly rather than dressed up as a computed pass/fail.
 */
@OptIn(ExperimentalKiteUi::class, Untested::class, InternalKiteUi::class)
@Routable("platform-fixes")
object PlatformFixVerificationPage : Page {
    override val title: Reactive<String> get() = Constant("Platform Fix Verification")

    // Known-good remote audio file, already used elsewhere in this app for manual audio checks.
    private const val REMOTE_AUDIO_URL = "https://www2.cs.uic.edu/~i101/SoundFiles/CantinaBand3.wav"

    override fun ElementWriter.CanAddTheme.render(): Unit = run {
        scrolling.col {
            h1("Platform Fix Verification")
            text(
                "Manual checks for a second batch of review fixes - sound, native rendering, and " +
                        "hardware/permission ordering. See docs/MANUAL_VERIFICATION.md for the full " +
                        "cross-page checklist organised by platform."
            )

            soundEffectPoolSection()
            circularProgressIosSection()
            lottieTeardownSection()
            cameraPermissionSection()
            overlayReentrancySection()
            resourceLeakSection()
        }
    }

    /**
     * AudioRemote and AudioRaw playback through SoundEffectPool were unimplemented (Android) or a
     * silent no-op (iOS); both now play. Resources.audioTaunt (used by AudioPage) is an
     * AudioResource, which already worked, so it does not exercise this fix - these two source
     * types specifically do.
     */
    private fun ElementWriter.CanAddTheme.soundEffectPoolSection() = card.col {
        h2("1. SoundEffectPool: AudioRemote and AudioRaw")
        text("Platform: Android and iOS changed here. JS already worked and is included for comparison.")
        text(
            "Whether the sound plays, and whether it sounds right, is a judgement call - there is no " +
                    "sensor for audio output. Tap each button and listen."
        )

        if (Platform.current == Platform.Desktop) {
            text("SoundEffectPool.play() is not implemented on the desktop/SSR target - nothing to check here.")
        } else {
            val pool = SoundEffectPool()
            button {
                text("Play via AudioRemote (network fetch by URL)")
                onClick { pool.play(AudioRemote(REMOTE_AUDIO_URL)) }
            }
            if (Platform.current == Platform.Android || Platform.current == Platform.Web) {
                button {
                    text("Play via AudioRaw (bytes fetched ourselves, then handed over as raw data)")
                    onClick {
                        val blob: Blob = fetch(REMOTE_AUDIO_URL).blob()
                        pool.play(AudioRaw(blob))
                    }
                }
            } else {
                text("AudioRaw is not implemented on iOS yet (pre-existing gap, not part of this fix) - button hidden here to avoid a guaranteed crash.")
            }
        }
    }

    /**
     * The iOS ring was a blank view with no drawing logic at all; it now strokes a CAShapeLayer
     * arc. ReviewFixVerificationPage's circular-progress section predates this fix and says iOS is
     * unchanged - that note is now stale. This section is the current source of truth for iOS.
     */
    private fun ElementWriter.CanAddTheme.circularProgressIosSection() = card.col {
        h2("2. iOS circular progress ring")
        text("Platform: iOS is the one under test. It was entirely blank before this fix.")
        text(
            "Expected: a ring with a hollow centre, not a blank view, not a filled disc. Whether it " +
                    "looks right (round cap, correct arc position) is a judgement call - eyeball it."
        )
        row {
            listOf(0.25f, 0.5f, 0.75f, 1f).forEach { r ->
                col {
                    sizeConstraints(width = 4.rem, height = 4.rem).circularProgress { ratio = r }
                    centered.text(r.toString())
                }
            }
        }
    }

    /**
     * cleanup() (which cancels the render scope and stops the animation-frame listener) existed but
     * was never called, so every navigation away from a Lottie-containing screen leaked a
     * CoroutineScope and left a frame callback firing against a removed view. It is now registered
     * via onRemove. leakDetect() is the same GC-based check LeakCheckerPage uses elsewhere in this
     * app: it weak-references the removed element, forces a GC after a delay, and logs to the
     * "ElementLeaks" tag if the reference is still alive.
     */
    private fun ElementWriter.CanAddTheme.lottieTeardownSection() = card.col {
        h2("3. iOS LottieView resource release")
        text("Platform: iOS is the one under test; the mechanism works everywhere else too as a cross-check.")
        text(
            "Toggle the animation off and on a handful of times, then watch the console/log output " +
                    "for the \"ElementLeaks\" tag. Expected: no warning appears after a few seconds " +
                    "(GC and the delayed recheck need a moment to catch up). A warning naming LottieView " +
                    "means the render scope or frame listener leaked again."
        )

        val shown = Signal(true)
        button {
            text { ::content { if (shown()) "Remove animation" else "Show animation" } }
            onClick { shown.value = !shown.value }
        }
        frame {
            reactive {
                if (children.isNotEmpty()) {
                    children[0].underlyingNativeElement.leakDetect()
                    removeChild(0)
                }
                if (shown()) {
                    sizeConstraints(width = 150.px, height = 150.px).lottie(
                        url = "https://assets2.lottiefiles.com/packages/lf20_p8bfn5to.json",
                        description = "Leak-detect test animation"
                    ) {
                        loop = true
                        autoPlay = true
                    }
                }
            }
        }
    }

    /**
     * CameraPreview.android.kt used to bind and start the camera controller before requesting the
     * CAMERA permission, which could throw a SecurityException. CameraScannerTestPage already
     * exercises the full permission-then-bind flow end to end, so this links there instead of
     * duplicating a camera screen.
     */
    private fun ElementWriter.CanAddTheme.cameraPermissionSection() = card.col {
        h2("4. Android camera permission ordering")
        text("Platform: Android is the one that changed.")
        text(
            "Already covered by the Camera Scanner Test page - use that screen rather than a copy here. " +
                    "To exercise the fix: revoke the camera permission for this app in system settings, " +
                    "relaunch, and open the link below. Expected: the permission prompt appears and the " +
                    "preview starts after accepting, with no crash and no camera-in-use error beforehand. " +
                    "Before this fix, binding happened first and could throw before the prompt ever showed."
        )
        link {
            to = { CameraScannerTestPage }
            text("Open Camera Scanner Test")
        }
    }

    /**
     * Android's overlay close() cleared state after starting the exit animation, so a second call
     * during that animation (e.g. two rapid taps) animated/removed the same element twice and
     * crashed. It now clears state first, making a repeat call a no-op. This drives close() twice
     * in the same click handler rather than relying on tap timing, so the result is deterministic.
     *
     * NOTE: a teammate is attempting to cover this with an automated Robolectric test. If that
     * lands, this section may become redundant - left in place until that is confirmed.
     */
    private fun ElementWriter.CanAddTheme.overlayReentrancySection() = card.col {
        h2("5. Android overlay close() reentrancy")
        text("Platform: Android is the one that changed; other platforms should already tolerate this.")
        text(
            "May be superseded by an automated test currently in progress - kept here until that " +
                    "lands. Tap the button below to open a dialog whose own button calls close() twice " +
                    "in a row. Expected: the dialog closes normally with no crash."
        )
        button {
            text("Open dialog with double-close button")
            onClick {
                context.dialog { close ->
                    card.col {
                        h3("Double-close test")
                        text("This button calls close() twice in a row.")
                        button {
                            text("Close x2")
                            onClick {
                                close()
                                close()
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Three leftover resource leaks from the same review batch, grouped because none of them
     * warrant a full screen on their own.
     */
    private fun ElementWriter.CanAddTheme.resourceLeakSection() = card.col {
        h2("6. Resource leaks")

        h3("Android: FileOutputStream on the capture-failure path")
        text(
            "Platform: Android. The leak only happened when Bitmap.compress() failed mid-write, which " +
                    "cannot be triggered from the UI - the fix (FileOutputStream.use { }) closes the " +
                    "stream on both the success and failure path. Nothing to manually test here; a normal " +
                    "successful capture on the Camera Scanner Test page confirms the happy path still works."
        )

        h3("JS: audio source nodes not all stopped")
        text(
            "Platform: JS/Web. AudioPlayback.stop() used to stop only the most recently scheduled " +
                    "buffer node; if several were already scheduled ahead of playback, the rest kept " +
                    "playing after stop(). Already covered by the Audio Capture & Playback Test page - use " +
                    "that screen rather than a copy here. To exercise the fix: start the loopback test, " +
                    "speak for a couple of seconds so \"Buffered Audio\" reads a few hundred ms, then press " +
                    "Stop. Expected: audio stops immediately, with no trailing playback of already-buffered " +
                    "chunks."
        )
        link {
            to = { AudioTestPage }
            text("Open Audio Capture & Playback Test")
        }

        h3("JS: popstate listener leaked on stack-reset navigation")
        text(
            "Platform: Web/JS only - this exercises the browser History API, so the button below is " +
                    "harmless (just a normal navigation) on Android/iOS. A stack reset (navigating such " +
                    "that the whole app stack collapses to one entry, e.g. context.pageNavigator.reset(...)) " +
                    "used to add a fresh popstate listener without ever removing it if the browser never " +
                    "fired the expected popstate, leaking one listener per reset. The fix uses a single " +
                    "reusable listener slot instead."
        )
        button {
            text("Trigger a stack-reset navigation (pushes RootPage, then resets back to this page)")
            onClick {
                context.pageNavigator.navigate(RootPage)
                context.pageNavigator.reset(PlatformFixVerificationPage)
            }
        }
        text(
            "After clicking: press the browser Back button a few times. Expected: it does not resurrect " +
                    "the intermediate Root Page step (the reset erases it from history), and Back/Forward " +
                    "keep working normally afterward with no console errors about duplicate or missing " +
                    "listeners."
        )
    }
}
