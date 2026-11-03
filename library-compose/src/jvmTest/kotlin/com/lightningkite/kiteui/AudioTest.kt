package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.AudioRaw
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.FloatControl
import kotlin.math.PI
import kotlin.math.sin
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit

// Plays through FakeMixerProvider's silent clips, so it runs without sound hardware.
class AudioTest {
    @BeforeTest
    fun useFakeMixer() {
        System.setProperty("javax.sound.sampled.Clip", FakeMixerProvider::class.java.name)
    }

    @AfterTest
    fun stopUsingFakeMixer() {
        // Clips close in the background, so wait for this test's to finish before the next test counts them.
        runBlocking { eventually { FakeMixer.openClips.isEmpty() } }
        System.clearProperty("javax.sound.sampled.Clip")
    }

    private suspend fun eventually(condition: () -> Boolean) = withTimeout(5_000) {
        while (!condition()) delay(10)
    }

    // Half a second of a 440Hz tone, mono, at the given sample size.
    private fun wav(bits: Int): AudioRaw {
        val format = AudioFormat(22050f, bits, 1, true, false)
        val bytesPerSample = bits / 8
        val pcm = ByteArray(22050 / 2 * bytesPerSample)
        for (i in 0 until pcm.size / bytesPerSample) {
            val sample = (sin(2 * PI * 440 * i / 22050) * 0.25 * (1 shl (bits - 1))).toInt()
            for (byte in 0 until bytesPerSample) pcm[i * bytesPerSample + byte] = (sample shr (8 * byte)).toByte()
        }
        val wav = ByteArrayOutputStream().also {
            AudioSystem.write(AudioInputStream(ByteArrayInputStream(pcm), format, pcm.size / format.frameSize.toLong()), AudioFileFormat.Type.WAVE, it)
        }.toByteArray()
        return AudioRaw(wav.toBlob("audio/wav"))
    }

    private fun PlayableAudio.millis() = currentTime.state.get().toDouble(DurationUnit.MILLISECONDS)

    @Test
    fun playableAudioHoldsAClipOnlyWhilePlaying(): Unit = runBlocking(Dispatchers.Main) {
        val audio = wav(16).load()
        var completions = 0
        audio.onComplete { completions++ }
        assertTrue(FakeMixer.openClips.isEmpty(), "a loaded sound holds no clip")

        audio.play()
        assertTrue(audio.isPlaying)
        eventually { FakeMixer.openClips.size == 1 }
        assertEquals(false, FakeMixer.openClips.single().openedOnMainThread, "clips open off the main thread")
        delay(200)
        audio.isPlaying = false
        eventually { FakeMixer.openClips.isEmpty() }
        val pausedAt = audio.millis()
        assertTrue(pausedAt in 100.0..400.0, "paused at $pausedAt ms")

        audio.play()
        eventually { FakeMixer.openClips.size == 1 }
        assertTrue(audio.millis() >= pausedAt, "resumes where it paused")
        eventually { completions == 1 }
        assertFalse(audio.isPlaying)
        assertEquals(500.0, audio.millis(), 0.1)
        eventually { FakeMixer.openClips.isEmpty() }
    }

    @Test
    fun seekingAndStoppingNotifyListeners(): Unit = runBlocking(Dispatchers.Main) {
        val audio = wav(16).load()
        var notifications = 0
        audio.currentTime.addListener { notifications++ }
        audio.currentTime.set(250.milliseconds)
        assertEquals(1, notifications)
        // To the nearest frame.
        assertEquals(250.0, audio.millis(), 0.1)
        audio.stop()
        assertEquals(2, notifications)
        assertEquals(0.0, audio.millis())
    }

    @Test
    fun playableAudioLoops(): Unit = runBlocking(Dispatchers.Main) {
        val audio = wav(16).load()
        var completions = 0
        audio.onComplete { completions++ }
        audio.loop = true
        audio.play()
        delay(800)
        assertTrue(audio.isPlaying)
        assertEquals(0, completions)
        assertTrue(audio.millis() < 500, "the position stays within the sound, was ${audio.millis()} ms")
        audio.stop()
        assertEquals(0.0, audio.millis())
        eventually { FakeMixer.openClips.isEmpty() }
    }

    // Lines have no volume control for more than 16 bits, so a 24-bit sound plays as 16-bit.
    @Test
    fun volumeWorksFor24BitSound(): Unit = runBlocking(Dispatchers.Main) {
        val sound = wav(24)
        val audio = sound.load()
        audio.volume = 0.5f
        audio.play()
        eventually { FakeMixer.openClips.size == 1 }
        val gain = FakeMixer.openClips.single().getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
        assertEquals(-6.02f, gain.value, 0.01f)
        audio.stop()

        val pool = SoundEffectPool()
        val effect = pool.play(sound)
        effect.volume = 0.5f
        effect.stop()
        pool.unload(sound)
    }

    @Test
    fun soundEffectPoolKeepsToItsConcurrencyAndReusesClips(): Unit = runBlocking(Dispatchers.Main) {
        val pool = SoundEffectPool(concurrency = 2)
        val a = wav(16)
        val b = wav(16)
        val first = pool.play(a)
        assertEquals(false, FakeMixer.openClips.single().openedOnMainThread, "clips open off the main thread")
        val second = pool.play(a)
        assertEquals(2, FakeMixer.openClips.size)

        val third = pool.play(b)
        eventually { FakeMixer.openClips.size == 2 }
        assertFalse(first.isPlaying, "the oldest sound gives up its clip")
        assertTrue(second.isPlaying && third.isPlaying)

        second.stop()
        assertFalse(second.isPlaying)
        val opened = FakeMixer.clipsOpened.get()
        val fourth = pool.play(a)
        assertEquals(opened, FakeMixer.clipsOpened.get(), "an idle clip of the same sound is reused")
        delay(100)
        assertTrue(fourth.isPlaying, "stopping the clip's previous sound doesn't stop this one")
        eventually { !fourth.isPlaying && !third.isPlaying }
        assertEquals(2, FakeMixer.openClips.size, "finished sounds keep their clips for reuse")

        pool.unload(a)
        eventually { FakeMixer.openClips.size == 1 }
        pool.unload(b)
    }
}
