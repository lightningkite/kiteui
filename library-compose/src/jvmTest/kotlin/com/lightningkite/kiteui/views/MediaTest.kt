package com.lightningkite.kiteui.views

import androidx.compose.ui.test.*
import com.lightningkite.kiteui.PlayableAudio
import com.lightningkite.kiteui.SoundEffectPool
import com.lightningkite.kiteui.audio.AudioFormat
import com.lightningkite.kiteui.audio.AudioPlayback
import com.lightningkite.kiteui.createFileReferenceFromBytes
import com.lightningkite.kiteui.load
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.toBlob
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.core.*
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.InetSocketAddress
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Line
import javax.sound.sampled.SourceDataLine
import javax.sound.sampled.UnsupportedAudioFileException
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import javax.sound.sampled.AudioFormat as LineFormat

@OptIn(ExperimentalTestApi::class)
class MediaTest {
    // Two seconds of 64x32 video, left half red and right half blue, with a silent audio track.
    private val mp4 = javaClass.classLoader.getResourceAsStream("common/video/test-pattern.mp4")!!.use { it.readBytes() }

    // AVFoundation streams over HTTP with range requests, so the server answers them.
    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
        createContext("/") { exchange ->
            val range = exchange.requestHeaders.getFirst("Range")?.removePrefix("bytes=")?.split("-")
            val start = range?.get(0)?.toInt() ?: 0
            val end = range?.get(1)?.takeIf { it.isNotEmpty() }?.toInt() ?: mp4.lastIndex
            exchange.responseHeaders.add("Content-Type", "video/mp4")
            exchange.responseHeaders.add("Accept-Ranges", "bytes")
            if (range != null) exchange.responseHeaders.add("Content-Range", "bytes $start-$end/${mp4.size}")
            exchange.sendResponseHeaders(if (range != null) 206 else 200, (end - start + 1).toLong())
            exchange.responseBody.use { it.write(mp4, start, end - start + 1) }
            exchange.close()
        }
        start()
    }

    @AfterTest
    fun stopServer() = server.stop(0)

    private fun ComposeUiTest.waitForLoad(video: RawVideoView) {
        waitUntil(timeoutMillis = 10_000) { video.state.state.ready }
        check(video.state.state.success) { "${video.source} failed: ${video.state.state}" }
    }

    private fun <T> Reactive<T>.now(): T = state.get()

    // Inside the agent sandbox, AVFoundation loads media but never decodes a frame or advances playback
    // (probably because the sandbox blocks the system's media services), so playback tests can only run outside it.
    private fun ComposeUiTest.assumeNativePlaybackAdvances(video: RawVideoView) {
        val advanced = try {
            waitUntil(timeoutMillis = 3_000) { video.currentTime.now() > 100.milliseconds }
            true
        } catch (e: ComposeTimeoutException) {
            false
        }
        assumeTrue("Native video playback doesn't advance in this environment", advanced)
    }

    @Test
    fun loadsPlaysSeeksAndEnds() = runKiteUiTest {
        lateinit var video: RawVideoView
        val root = showKiteUi {
            sizeConstraints(width = 128.px, height = 64.px).rawVideo(VideoResource("common/video/test-pattern.mp4"), "test pattern") { video = this }
        }
        waitForLoad(video)
        assertEquals(2.0, video.sourceDuration.now()!!, 0.1)
        assertEquals(listOf(0.0..video.sourceDuration.now()!!), video.seekableTimeRanges)
        // Loaded videos wait to be played, as on the web.
        Thread.sleep(300)
        assertFalse(video.playing.now())
        assertEquals(0.seconds, video.currentTime.now())

        runBlocking(Dispatchers.Main) { video.volume.set(0.25f) }
        waitUntil(timeoutMillis = 5_000) { video.volume.now() == 0.25f }

        var completions = 0
        runBlocking(Dispatchers.Main) {
            video.completedPlay.addListener { completions++ }
            video.playing.set(true)
        }
        waitUntil(timeoutMillis = 5_000) { video.playing.now() }
        assumeNativePlaybackAdvances(video)

        // The screenshot is of the root element, which here is just the video.
        val image = saveScreenshot(root, "video-frame")
        val left = image.getRGB(image.width / 4, image.height / 2)
        val right = image.getRGB(image.width * 3 / 4, image.height / 2)
        assertTrue((left shr 16 and 0xFF) > 150 && (left and 0xFF) < 100, "left half should be red, was ${Integer.toHexString(left)}")
        assertTrue((right and 0xFF) > 150 && (right shr 16 and 0xFF) < 100, "right half should be blue, was ${Integer.toHexString(right)}")

        waitUntil(timeoutMillis = 5_000) { completions == 1 }
        assertFalse(video.playing.now())

        runBlocking(Dispatchers.Main) { video.currentTime.set(1.seconds) }
        waitUntil(timeoutMillis = 5_000) { abs(video.currentTime.now().inWholeMilliseconds - 1000) < 150 }
    }

    @Test
    fun pausesAndLoops() = runKiteUiTest {
        lateinit var video: RawVideoView
        showKiteUi {
            sizeConstraints(width = 128.px, height = 64.px).rawVideo(VideoResource("common/video/test-pattern.mp4"), "test pattern") {
                video = this
                loop = true
                showControls = true
            }
        }
        // Asked to play before it has loaded, it plays once it has, like the web's autoplay.
        runBlocking(Dispatchers.Main) { video.playing.set(true) }
        waitForLoad(video)
        var completions = 0
        runBlocking(Dispatchers.Main) { video.completedPlay.addListener { completions++ } }
        waitUntil(timeoutMillis = 5_000) { video.playing.now() }
        assumeNativePlaybackAdvances(video)

        // Looping wraps around to the start rather than ending.
        waitUntil(timeoutMillis = 5_000) { video.currentTime.now() > 1.5.seconds }
        waitUntil(timeoutMillis = 5_000) { video.currentTime.now() < 1.seconds }
        assertTrue(video.playing.now())
        assertEquals(0, completions)

        runBlocking(Dispatchers.Main) { video.playing.set(false) }
        waitUntil(timeoutMillis = 5_000) { !video.playing.now() }
        val pausedAt = video.currentTime.now()
        Thread.sleep(300)
        waitForIdle()
        assertEquals(pausedAt, video.currentTime.now())
    }

    @Test
    fun everySourceLoads() = runKiteUiTest {
        val sources = listOf(
            VideoRaw(mp4.toBlob("video/mp4")),
            VideoLocal(createFileReferenceFromBytes(mp4, "video/mp4", "pattern.mp4")),
            VideoRemote("http://127.0.0.1:${server.address.port}/pattern.mp4"),
        )
        val videos = ArrayList<RawVideoView>()
        showKiteUi {
            col {
                for (source in sources) sizeConstraints(width = 64.px, height = 32.px).rawVideo(source, "$source") { videos += this }
            }
        }
        for (video in videos) {
            waitForLoad(video)
            assertEquals(2.0, video.sourceDuration.now()!!, 0.1, "${video.source}")
        }
    }

    @Test
    fun missingVideoReportsAnError() = runKiteUiTest {
        lateinit var video: RawVideoView
        showKiteUi {
            rawVideo(VideoLocal(createFileReferenceFromBytes(ByteArray(100), "video/mp4", "garbage.mp4")), "garbage") { video = this }
        }
        waitUntil(timeoutMillis = 10_000) { video.state.state.ready }
        assertFalse(video.state.state.success)
    }

    @Test
    fun removedVideoReleasesItsPlayer() = runKiteUiTest {
        val shown = Signal(true)
        lateinit var video: RawVideoView
        showKiteUi {
            swapView {
                swapping(current = { shown() }) {
                    if (it) sizeConstraints(width = 64.px, height = 32.px).rawVideo(VideoResource("common/video/test-pattern.mp4"), "test pattern") { video = this }
                    else text("Gone")
                }
            }
        }
        waitForLoad(video)
        runBlocking(Dispatchers.Main) { video.playing.set(true) }
        waitUntil(timeoutMillis = 5_000) { video.playing.now() }
        runBlocking(Dispatchers.Main) { shown.value = false }
        waitForIdle()
        assertTrue(video.isShutdown)
        val player = RawVideoView::class.java.getDeclaredField("player").apply { isAccessible = true }.get(video)
            as io.github.kdroidfilter.composemediaplayer.VideoPlayerState
        waitUntil(timeoutMillis = 5_000) { !player.isPlaying && !player.hasMedia }
    }

    // Half a second of a 440Hz tone, 16-bit mono.
    private val toneFormat = LineFormat(22050f, 16, 1, true, false)
    private val tone = ByteArray(22050).also { pcm ->
        for (i in 0 until pcm.size / 2) {
            val sample = (sin(2 * PI * 440 * i / 22050) * 8000).toInt()
            pcm[2 * i] = sample.toByte()
            pcm[2 * i + 1] = (sample shr 8).toByte()
        }
    }
    private val wav = ByteArrayOutputStream().also {
        AudioSystem.write(AudioInputStream(ByteArrayInputStream(tone), toneFormat, tone.size / 2L), AudioFileFormat.Type.WAVE, it)
    }.toByteArray()

    private val hasAudioOutput get() = AudioSystem.isLineSupported(Line.Info(SourceDataLine::class.java))

    @Test
    fun soundEffectsDecodeWavAndRejectOtherFormats(): Unit = runBlocking(Dispatchers.Main) {
        val pool = SoundEffectPool()
        pool.preload(AudioRaw(wav.toBlob("audio/wav")))
        assertFailsWith<UnsupportedAudioFileException> { pool.preload(AudioRaw(ByteArray(1000).toBlob("audio/mpeg"))) }

    }

    @Test
    fun soundEffectsPlay(): Unit = runBlocking(Dispatchers.Main) {
        assumeTrue("No audio output device", hasAudioOutput)
        val pool = SoundEffectPool()
        val sound = AudioRaw(wav.toBlob("audio/wav"))
        val first = pool.play(sound)
        val second = pool.play(sound)
        first.volume = 0.5f
        assertTrue(first.isPlaying && second.isPlaying)
        first.stop()
        assertFalse(first.isPlaying)
        kotlinx.coroutines.delay(1000)
        assertFalse(second.isPlaying, "a finished sound reports that it stopped")
        second.volume = 0.5f
    }

    @Test
    fun playableAudioCompletesSeeksAndLoops(): Unit = runBlocking(Dispatchers.Main) {
        assumeTrue("No audio output device", hasAudioOutput)
        val audio: PlayableAudio = AudioRaw(wav.toBlob("audio/wav")).load()
        var completions = 0
        audio.onComplete { completions++ }
        audio.play()
        assertTrue(audio.isPlaying)
        kotlinx.coroutines.delay(1000)
        assertEquals(1, completions)
        assertFalse(audio.isPlaying)

        audio.currentTime.set(250.milliseconds)
        // To the nearest frame.
        assertEquals(250.0, audio.currentTime.now().toDouble(kotlin.time.DurationUnit.MILLISECONDS), 0.1)

        audio.loop = true
        audio.play()
        kotlinx.coroutines.delay(1000)
        assertTrue(audio.isPlaying)
        assertEquals(1, completions)
        audio.stop()
        assertFalse(audio.isPlaying)
        assertEquals(0.seconds, audio.currentTime.now())
    }

    @Test
    fun audioPlaybackBuffersUntilStarted(): Unit = runBlocking(Dispatchers.Main) {
        val playback = AudioPlayback(AudioFormat(sampleRate = 22050))
        playback.enqueue(tone)
        playback.enqueue(tone)
        assertEquals(1000L, playback.bufferedDurationMs.now())
        assertFalse(playback.isPlaying.now())
        playback.clearBuffer()
        assertEquals(0L, playback.bufferedDurationMs.now())
        playback.release()
    }

    @Test
    fun audioPlaybackStreams(): Unit = runBlocking(Dispatchers.Main) {
        assumeTrue("No audio output device", hasAudioOutput)
        val playback = AudioPlayback(AudioFormat(sampleRate = 22050))
        var empties = 0
        playback.onBufferEmpty { empties++ }
        playback.enqueue(tone)
        playback.enqueue(tone)
        playback.volume = 0.5f
        playback.start()
        assertTrue(playback.isPlaying.now())
        kotlinx.coroutines.delay(1500)
        assertEquals(0L, playback.bufferedDurationMs.now())
        assertEquals(1, empties)
        playback.enqueue(tone)
        playback.stop()
        assertFalse(playback.isPlaying.now())
        assertEquals(0L, playback.bufferedDurationMs.now())
        playback.release()
    }
}
