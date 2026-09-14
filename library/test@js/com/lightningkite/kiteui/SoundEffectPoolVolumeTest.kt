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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * `PlayingSoundEffect.volume` and `isPlaying` on web.
 *
 * Both getters were once unimplemented and threw, so a caller could not read back anything about a
 * sound it had just started. Reading the properties at all is therefore the assertion that matters
 * most here; the round-trips then show the values reach and return from the real `GainNode` and
 * source node rather than sitting in a field of our own.
 *
 * That the gain node is actually *in* the signal path is not something a test can observe - only
 * the ear can, which is what the `sound-effect-handle` screen in the example app is for.
 *
 * A real `AudioContext` is used. Chrome would normally keep it suspended until the page has user
 * activation, which a test cannot produce, so the karma launcher passes
 * `--autoplay-policy=no-user-gesture-required` (see `library/karma.config.d`). Without that the
 * context's clock never advances and [isPlayingBecomesFalseOnceTheSoundFinishes] could not exist -
 * every test here would be reading back a sound that had not actually started.
 */
class SoundEffectPoolVolumeTest {

    /**
     * One pool - and so one `AudioContext` - for the whole class. Browsers cap how many a single
     * document may have, so a context per test would put a low ceiling on this file.
     */
    private companion object {
        val pool = SoundEffectPool()
    }

    /** 0.1s of silent 16-bit mono PCM: the least audio data worth decoding. */
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
        int16(1)                      // PCM
        int16(1)                      // mono
        int32(sampleRate)
        int32(sampleRate * 2)         // byte rate
        int16(2)                      // block align
        int16(16)                     // bits per sample
        ascii("data"); int32(dataLength)
        // Samples stay zero: silence.

        return Blob(arrayOf(Int8Array(bytes.toTypedArray())), BlobPropertyBag(type = "audio/wav"))
    }

    private fun playSilence(check: suspend (PlayingSoundEffect) -> Unit) = GlobalScope.promise {
        check(pool.play(AudioRaw(silentWav())))
    }

    @Test
    fun volumeDefaultsToFullAndReadsBack() = playSilence { effect ->
        // A bare property read: this alone would have thrown NotImplementedError before the fix.
        assertEquals(1f, effect.volume, "a freshly played sound must start at full volume")
    }

    // 0.25 is exactly representable in float32, which is what AudioParam.value stores. Values that
    // are not - 0.3, say - come back very slightly changed, so exact equality is only fair here.
    @Test
    fun volumeRoundTripsThroughTheGainNode() = playSilence { effect ->
        effect.volume = 0.25f
        assertEquals(0.25f, effect.volume, "the value must come back out of the gain node")
    }

    @Test
    fun isPlayingStartsTrue() = playSilence { effect ->
        assertTrue(effect.isPlaying, "play() has already started the source")
    }

    @Test
    fun stopClearsIsPlaying() = playSilence { effect ->
        effect.stop()
        assertFalse(effect.isPlaying, "stop() must be observable through isPlaying")
    }

    /**
     * The one thing the rest of this file cannot see: a sound that ends on its own.
     *
     * Every other test here starts a sound and reads it back immediately, so all of them pass
     * whether or not audio is actually advancing - which is how a handle that stays `isPlaying`
     * forever got through. This one waits for the sound to finish, so it only passes if the
     * `AudioContext` is really running and `onended` really fires.
     */
    @Test
    fun isPlayingBecomesFalseOnceTheSoundFinishes() = GlobalScope.promise {
        val effect = pool.play(AudioRaw(silentWav()))
        assertTrue(effect.isPlaying, "precondition: the sound is playing")

        // The clip is 0.1s; poll well past that so a slow machine does not fail spuriously, but
        // bounded so a stuck handle fails rather than hanging the suite.
        var waited = 0
        while (effect.isPlaying && waited < 5000) {
            delay(50)
            waited += 50
        }

        assertFalse(
            effect.isPlaying,
            "isPlaying was still true ${waited}ms after a 0.1s sound started - onended never fired",
        )
    }

    @Test
    fun settingIsPlayingFalseStopsIt() = playSilence { effect ->
        effect.isPlaying = false
        assertFalse(effect.isPlaying, "assigning false is the documented way to stop a sound")
        effect.stop()
        assertFalse(effect.isPlaying, "stopping an already-stopped sound must be harmless")
    }
}
