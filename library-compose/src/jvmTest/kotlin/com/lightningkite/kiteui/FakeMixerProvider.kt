package com.lightningkite.kiteui

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.sound.sampled.*
import javax.sound.sampled.spi.MixerProvider
import javax.swing.SwingUtilities
import kotlin.math.min

// A stand-in for sound hardware, which the agent sandbox and CI machines don't have.
// AudioSystem finds it as a service (META-INF/services), but it offers its mixer only while the
// `javax.sound.sampled.Clip` property names it, so it never takes the place of a real device.
class FakeMixerProvider : MixerProvider() {
    override fun getMixerInfo(): Array<Mixer.Info> =
        if (System.getProperty("javax.sound.sampled.Clip") == javaClass.name) arrayOf(FakeMixer.info) else arrayOf()

    override fun getMixer(info: Mixer.Info?): Mixer =
        if (info == FakeMixer.info) FakeMixer else throw IllegalArgumentException("Not the fake mixer: $info")
}

object FakeMixer : Mixer {
    val info = object : Mixer.Info("Fake mixer", "KiteUI tests", "Silent clips for tests", "1") {}
    val openClips = CopyOnWriteArrayList<FakeClip>()
    val clipsOpened = AtomicInteger()

    internal val events = Executors.newSingleThreadExecutor { Thread(it, "Fake audio events").apply { isDaemon = true } }
    internal val timers = Executors.newSingleThreadScheduledExecutor { Thread(it, "Fake audio playback").apply { isDaemon = true } }

    override fun getMixerInfo() = info
    override fun isLineSupported(info: Line.Info) = info.lineClass == Clip::class.java
    override fun getLine(info: Line.Info): Line =
        if (isLineSupported(info)) FakeClip() else throw IllegalArgumentException("The fake mixer has clips only, not $info")

    override fun getSourceLineInfo(): Array<Line.Info> = arrayOf(Line.Info(Clip::class.java))
    override fun getTargetLineInfo(): Array<Line.Info> = arrayOf()
    override fun getSourceLineInfo(info: Line.Info): Array<Line.Info> = if (isLineSupported(info)) sourceLineInfo else arrayOf()
    override fun getTargetLineInfo(info: Line.Info): Array<Line.Info> = arrayOf()
    override fun getMaxLines(info: Line.Info) = AudioSystem.NOT_SPECIFIED
    override fun getSourceLines(): Array<Line> = openClips.toTypedArray()
    override fun getTargetLines(): Array<Line> = arrayOf()
    override fun synchronize(lines: Array<out Line>?, maintainSync: Boolean) = throw UnsupportedOperationException()
    override fun unsynchronize(lines: Array<out Line>?) = throw UnsupportedOperationException()
    override fun isSynchronizationSupported(lines: Array<out Line>?, maintainSync: Boolean) = false

    override fun getLineInfo() = Line.Info(Mixer::class.java)
    override fun open() {}
    override fun close() {}
    override fun isOpen() = true
    override fun getControls(): Array<Control> = arrayOf()
    override fun isControlSupported(control: Control.Type?) = false
    override fun getControl(control: Control.Type?) = throw IllegalArgumentException("Unsupported control type: $control")
    override fun addLineListener(listener: LineListener?) {}
    override fun removeLineListener(listener: LineListener?) {}
}

// Plays silently in real time, and behaves like the JDK's DirectAudioDevice clips where KiteUI depends on them:
// events arrive on another thread, the frame position counts every frame played since it was set (so it passes
// the end when looping), and only PCM of at most 16 bits and two channels gets a gain control.
class FakeClip : Clip {
    var openedOnMainThread: Boolean? = null
        private set

    private val listeners = CopyOnWriteArrayList<LineListener>()
    private var format: AudioFormat? = null
    private var frames = 0
    private var controls: List<Control> = listOf()

    private var running = false
    private var looping = false

    // The frame position when last started, stopped or set, and when that was.
    private var basePosition = 0L
    private var baseNanos = 0L

    // Where playback stops if it isn't looping, and the timer for it.
    private var endPosition = Long.MAX_VALUE
    private var end: ScheduledFuture<*>? = null

    @Synchronized
    override fun open(format: AudioFormat, data: ByteArray, offset: Int, bufferSize: Int) {
        check(!isOpen) { "Already open" }
        this.format = format
        frames = bufferSize / format.frameSize
        val pcm = format.encoding == AudioFormat.Encoding.PCM_SIGNED || format.encoding == AudioFormat.Encoding.PCM_UNSIGNED
        controls = if (pcm && format.sampleSizeInBits <= 16 && format.channels <= 2) {
            listOf(object : FloatControl(FloatControl.Type.MASTER_GAIN, -80f, 6.0206f, 0.0001f, -1, 0f, "dB") {})
        } else listOf()
        basePosition = 0
        openedOnMainThread = SwingUtilities.isEventDispatchThread()
        FakeMixer.openClips.add(this)
        FakeMixer.clipsOpened.incrementAndGet()
        send(LineEvent.Type.OPEN)
    }

    override fun open(stream: AudioInputStream) {
        val data = stream.readAllBytes()
        open(stream.format, data, 0, data.size)
    }

    override fun open() = throw IllegalArgumentException("illegal call to open() in interface Clip")

    @Synchronized
    override fun close() {
        if (!isOpen) return
        stop()
        format = null
        FakeMixer.openClips.remove(this)
        send(LineEvent.Type.CLOSE)
    }

    override fun isOpen() = format != null

    @Synchronized
    override fun getLongFramePosition(): Long {
        if (!running) return basePosition
        val played = ((System.nanoTime() - baseNanos) * format!!.frameRate / 1e9).toLong()
        return min(basePosition + played, endPosition)
    }

    override fun getFramePosition() = longFramePosition.toInt()

    @Synchronized
    override fun setFramePosition(frames: Int) {
        basePosition = frames.coerceIn(0, this.frames).toLong()
        baseNanos = System.nanoTime()
        scheduleEnd()
    }

    @Synchronized
    override fun start() {
        if (running || !isOpen) return
        running = true
        baseNanos = System.nanoTime()
        scheduleEnd()
        send(LineEvent.Type.START)
    }

    @Synchronized
    override fun stop() {
        if (!running) return
        basePosition = longFramePosition
        running = false
        looping = false
        scheduleEnd()
        send(LineEvent.Type.STOP)
    }

    @Synchronized
    override fun loop(count: Int) {
        require(count == 0 || count == Clip.LOOP_CONTINUOUSLY) { "The fake clip loops forever or not at all" }
        if (running) {
            basePosition = longFramePosition
            baseNanos = System.nanoTime()
        }
        looping = count == Clip.LOOP_CONTINUOUSLY
        if (running) scheduleEnd() else start()
    }

    private fun scheduleEnd() {
        end?.cancel(false)
        end = null
        endPosition = Long.MAX_VALUE
        if (!running || looping) return
        val positionInSound = if (basePosition <= frames) basePosition else basePosition % frames
        endPosition = basePosition + (frames - positionInSound)
        val delayNanos = ((endPosition - basePosition) / format!!.frameRate * 1e9).toLong()
        lateinit var task: ScheduledFuture<*>
        task = FakeMixer.timers.schedule({
            synchronized(this) {
                if (end !== task) return@synchronized
                basePosition = endPosition
                running = false
                end = null
                send(LineEvent.Type.STOP)
            }
        }, delayNanos, TimeUnit.NANOSECONDS)
        end = task
    }

    private fun send(type: LineEvent.Type) {
        val event = LineEvent(this, type, longFramePosition)
        FakeMixer.events.execute { listeners.forEach { it.update(event) } }
    }

    override fun getFrameLength() = frames
    override fun getMicrosecondLength() = (frames / format!!.frameRate * 1e6).toLong()
    override fun getMicrosecondPosition() = (longFramePosition / format!!.frameRate * 1e6).toLong()
    override fun setMicrosecondPosition(microseconds: Long) = setFramePosition((microseconds * format!!.frameRate / 1e6).toInt())
    override fun setLoopPoints(start: Int, end: Int) = throw UnsupportedOperationException()
    override fun drain() = throw UnsupportedOperationException()
    override fun flush() {}
    override fun isRunning() = running
    override fun isActive() = running
    override fun getFormat() = format!!
    override fun getBufferSize() = frames * format!!.frameSize
    override fun available() = 0
    override fun getLevel() = AudioSystem.NOT_SPECIFIED.toFloat()

    override fun getLineInfo() = DataLine.Info(Clip::class.java, format)
    override fun getControls() = controls.toTypedArray()
    override fun isControlSupported(type: Control.Type) = controls.any { it.type == type }
    override fun getControl(type: Control.Type) = controls.find { it.type == type } ?: throw IllegalArgumentException("Unsupported control type: $type")
    override fun addLineListener(listener: LineListener) { listeners.add(listener) }
    override fun removeLineListener(listener: LineListener) { listeners.remove(listener) }
}
