package com.lightningkite.kiteui.views.direct

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@OptIn(UnstableApi::class)
actual class RawVideoView actual constructor(
    context: RContext,
    actual val source: VideoSource,
    actual val description: String,
    @get:OptIn(UnstableApi::class)
    actual val scaleType: ImageScaleType,
    actual val preloadHint: PreloadHint,
) : RView(context) {
    override val native = PlayerView(context.activity).apply {
        player = ExoPlayer.Builder(context.activity).build()
        contentDescription = description
        resizeMode = when (scaleType) {
            ImageScaleType.Fit -> AspectRatioFrameLayout.RESIZE_MODE_FIT
            ImageScaleType.Crop -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            else -> AspectRatioFrameLayout.RESIZE_MODE_FILL
        }
    }

    private val _state = RawReactive<Unit>()
    actual val state: Reactive<Unit> = _state

    init {
        when (val value = source) {
            is VideoRemote -> native.player!!.setMediaItem(MediaItem.fromUri(value.url))
            is VideoResource -> native.player!!.setMediaItem(MediaItem.fromUri("android.resource://${native.context.packageName}/${value.resource}"))
            is VideoLocal -> native.player!!.setMediaItem(MediaItem.fromUri(value.file.uri))
            is VideoRaw -> {
                try {
                    val tmp = File.createTempFile("kiteui_video_", ".mp4", context.activity.cacheDir)
                    tmp.outputStream().use { it.write(value.data.data) }
                    native.player!!.setMediaItem(MediaItem.fromUri(Uri.fromFile(tmp)))
                } catch (e: Throwable) {
                    _state.state = ReactiveState.exception(Exception(e))
                }
            }
            else -> {}
        }
        native.player!!.prepare()
        val l = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    _state.state = ReactiveState(Unit)
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                _state.state = ReactiveState.exception(error)
            }
        }
        native.player!!.addListener(l)
    }

    actual val time: MutableReactive<Double> = object : MutableReactive<Double> {
        override suspend fun set(value: Double) {
            native.player!!.seekTo((value * 1000.0).toLong())
        }

        override val state get() = ReactiveState(native.player!!.currentPosition / 1000.0)

        override fun addListener(listener: () -> Unit): () -> Unit {
            var remover: (() -> Unit)? = null
            val l = object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (isPlaying) remover = AppState.animationFrame.addListener(listener)
                    else {
                        remover?.invoke()
                        remover = null
                    }
                }
            }
            native.player!!.addListener(l)
            return { native.player!!.removeListener(l) }
        }
    }

    actual val currentTime: MutableReactive<Duration> = object : MutableReactive<Duration> {
        override suspend fun set(value: Duration) {
            native.player!!.seekTo(value.inWholeMilliseconds)
        }

        override val state get() = ReactiveState(native.player!!.currentPosition.milliseconds)

        override fun addListener(listener: () -> Unit): () -> Unit {
            var remover: (() -> Unit)? = null
            val l = object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (isPlaying) remover = AppState.animationFrame.addListener(listener)
                    else {
                        remover?.invoke()
                        remover = null
                    }
                }
            }
            native.player!!.addListener(l)
            return { native.player!!.removeListener(l) }
        }
    }

    actual val playing: MutableReactive<Boolean> = object : MutableReactive<Boolean> {
        override suspend fun set(value: Boolean) {
            if (value) {
                native.player!!.play()
            } else {
                native.player!!.pause()
            }
        }

        override val state: ReactiveState<Boolean> get() = ReactiveState(native.player!!.isPlaying)

        override fun addListener(listener: () -> Unit): () -> Unit {
            val l = object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    listener()
                }
            }
            native.player!!.addListener(l)
            return { native.player!!.removeListener(l) }
        }
    }

    actual val volume: MutableReactive<Float> = object : MutableReactive<Float> {
        override suspend fun set(value: Float) {
            native.player!!.volume = value
        }

        override val state: ReactiveState<Float> get() = ReactiveState(native.player!!.volume)

        override fun addListener(listener: () -> Unit): () -> Unit {
            val l = object : Player.Listener {
                override fun onVolumeChanged(volume: Float) {
                    listener()
                }
            }
            native.player!!.addListener(l)
            return { native.player!!.removeListener(l) }
        }
    }

    actual val sourceDuration: Reactive<Double?> = object : Reactive<Double?> {
        override val state: ReactiveState<Double?>
            get() {
                val d = native.player!!.duration
                val seconds = if (d <= 0L) null else d / 1000.0
                return ReactiveState(seconds)
            }

        override fun addListener(listener: () -> Unit): () -> Unit {
            val l = object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    listener()
                }
            }
            native.player!!.addListener(l)
            return { native.player!!.removeListener(l) }
        }
    }

    actual var showControls: Boolean
        get() = native.useController
        set(value) {
            native.useController = value
        }

    actual var loop: Boolean
        get() = native.player!!.repeatMode == Player.REPEAT_MODE_ONE
        set(value) {
            native.player!!.repeatMode = if (value) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        }

    actual val completedPlay: Listenable = object: Listenable {
        override fun addListener(listener: () -> Unit): () -> Unit {
            val l = object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) {
                        listener()
                    }
                }
            }
            native.player!!.addListener(l)
            return { native.player!!.removeListener(l) }
        }
    }
    actual val seekableTimeRanges: List<ClosedFloatingPointRange<Double>> = listOf()
}

//actual fun Video.onComplete(action: () -> Unit) {
//    val l = object: Player.Listener {
//        override fun onIsPlayingChanged(isPlaying: Boolean) {
//            action()
//        }
//    }
//    native.player!!.addListener(l)
//    return { native.player!!.removeListener(l) }
//}