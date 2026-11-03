@file:OptIn(DelicateCoroutinesApi::class)

package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.AudioRaw
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.promise
import org.khronos.webgl.Int8Array
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag
import kotlin.js.Promise
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * [SharedAudioContext] - the fix for sound effects being permanently silent on web.
 *
 * Every [SoundEffectPool] used to construct and own its own `AudioContext`. Browsers create one
 * suspended and leave it that way until a user gesture resumes it; the only resume attempt lived
 * inside `play()`, so a pool that a gesture never happened to target (or whose gesture-time resume
 * attempt was refused, which is normal and silently swallowed there) stayed suspended forever - and
 * so did every sound it ever played. [SharedAudioContext] gives every pool the same context and
 * listens for the first gesture anywhere in the document to unlock it once, for all of them.
 *
 * A real user gesture can't be produced here. Karma runs with
 * `--autoplay-policy=no-user-gesture-required` (see `library/karma.config.d`) specifically so
 * `context.resume()` succeeds without one - which is also what makes [SoundEffectPoolVolumeTest]
 * possible - so a synthetic unlock resumes the context below even though a real browser would
 * ignore an untrusted gesture. What these tests can and do cover is the wiring: one context shared
 * by every pool, and [SharedAudioContext.enabled] tracking the context's own `statechange` event.
 */
class SharedAudioContextTest {

    /** 0.1s of silent 16-bit mono PCM - the least audio data worth decoding. */
    private fun silentWav(): Blob {
        val sampleRate = 8000
        val sampleCount = 800
        val dataLength = sampleCount * 2
        val bytes = ByteArray(44 + dataLength)
        var at = 0
        fun ascii(s: String) = s.forEach { bytes[at++] = it.code.toByte() }
        fun int32(v: Int) = repeat(4) { bytes[at++] = (v shr (it * 8)).toByte() }
        fun int16(v: Int) = repeat(2) { bytes[at++] = (v shr (it * 8)).toByte() }

        ascii("RIFF"); int32(36 + dataLength); ascii("WAVE")
        ascii("fmt "); int32(16)
        int16(1); int16(1); int32(sampleRate); int32(sampleRate * 2); int16(2); int16(16)
        ascii("data"); int32(dataLength)
        // Samples stay zero: silence.

        return Blob(arrayOf(Int8Array(bytes.toTypedArray())), BlobPropertyBag(type = "audio/wav"))
    }

    @Test
    fun sharedContextIsASingleInstance() {
        // The whole point of the fix: one AudioContext, not one per pool.
        assertSame(SharedAudioContext.context, SharedAudioContext.context)
    }

    @Test
    fun multiplePoolsPlayThroughTheSameContext() = GlobalScope.promise {
        val poolA = SoundEffectPool()
        val poolB = SoundEffectPool()

        SharedAudioContext.requestUnlock()
        var waited = 0
        while (SharedAudioContext.context.state != "running" && waited < 5000) {
            delay(50); waited += 50
        }

        val effectA = poolA.play(AudioRaw(silentWav()))
        val effectB = poolB.play(AudioRaw(silentWav()))

        // Both play audibly off the one context that was just unlocked. If either pool held its
        // own private context (the pre-fix behaviour), unlocking only the shared one would leave
        // that pool's context suspended - its clock would never advance and its source would never
        // reach `onended`.
        assertTrue(effectA.isPlaying)
        assertTrue(effectB.isPlaying)
    }

    /**
     * Forces a real suspend/resume cycle on the shared context (rather than trusting whatever
     * state earlier tests left it in) so this test's outcome does not depend on suite ordering.
     */
    @Test
    fun enabledTracksStatechange() = GlobalScope.promise {
        val ctx = SharedAudioContext.context
        var notifications = 0
        val remove = SharedAudioContext.enabled.addListener { notifications++ }

        (ctx.asDynamic().suspend() as Promise<Unit>).await()
        var waited = 0
        while (SharedAudioContext.enabled.state.getOrNull() != false && waited < 5000) {
            delay(50); waited += 50
        }
        assertEquals(false, SharedAudioContext.enabled.state.getOrNull(), "enabled must go false once the context suspends")

        SharedAudioContext.requestUnlock()
        waited = 0
        while (SharedAudioContext.enabled.state.getOrNull() != true && waited < 5000) {
            delay(50); waited += 50
        }
        assertEquals(true, SharedAudioContext.enabled.state.getOrNull(), "enabled must go true once requestUnlock resumes it")

        remove()
        assertTrue(notifications >= 2, "the statechange listener must fire for both transitions, not just the final read")
    }
}
