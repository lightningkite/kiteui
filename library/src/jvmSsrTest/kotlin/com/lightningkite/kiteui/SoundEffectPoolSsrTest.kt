package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.AudioRemote
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Audio under SSR.
 *
 * `play` and `AudioSource.load` used to be `TODO(...)`, so any shared screen that made a sound
 * brought the server render down with a NotImplementedError. There is no audio device on a server,
 * but that is not an error condition - these must be inert. Every call below is expected to return
 * normally, and every property on what it returns is expected to be readable.
 */
class SoundEffectPoolSsrTest {

    private val sound = AudioRemote("https://example.com/ding.wav")

    @Test
    fun poolLifecycleIsCallable() = runTest {
        val pool = SoundEffectPool()
        pool.preload(sound)
        pool.play(sound)
        pool.unload(sound)
    }

    @Test
    fun playedEffectIsReadableAndWritable() = runTest {
        val effect = SoundEffectPool().play(sound)

        assertEquals(1f, effect.volume, "volume must be readable, not a TODO")
        assertTrue(effect.isPlaying, "play() reports the sound as started, matching the real platforms")

        effect.volume = 0.5f
        assertEquals(0.5f, effect.volume, "the handle must remember what callers set")

        effect.isPlaying = false
        assertFalse(effect.isPlaying)
    }

    @Test
    fun stoppingAPlayedEffectIsObservable() = runTest {
        val effect = SoundEffectPool().play(sound)
        effect.stop()
        assertFalse(effect.isPlaying, "stop() must be visible through isPlaying, as it is on every real platform")
    }

    @Test
    fun loadedAudioIsReadableAndWritable() = runTest {
        val audio = sound.load()

        assertEquals(1f, audio.volume)
        assertFalse(audio.loop)
        assertFalse(audio.isPlaying)
        assertEquals(Duration.ZERO, audio.currentTime.state.getOrNull())

        audio.volume = 0.25f
        audio.loop = true
        assertEquals(0.25f, audio.volume)
        assertTrue(audio.loop)

        var completed = false
        audio.onComplete { completed = true }
        assertFalse(completed, "nothing ever finishes playing on a server")
    }

    /**
     * `backgroundAudio` calls `play()` then spins until `isPlaying` turns true. If the inert handle
     * reported false forever that loop would never terminate, so this is the property that keeps SSR
     * from hanging rather than crashing.
     */
    @Test
    fun playThenStopTracksIsPlaying() = runTest {
        val audio = sound.load()

        audio.play()
        assertTrue(audio.isPlaying, "play() must be observable or backgroundAudio's retry loop never exits")

        audio.stop()
        assertFalse(audio.isPlaying)
    }

    @Test
    fun currentTimeIsSettable() = runTest {
        val audio = sound.load()
        audio.currentTime.set(5.seconds)
        assertEquals(5.seconds, audio.currentTime.state.getOrNull(), "seeking must not throw and must read back")
    }
}
