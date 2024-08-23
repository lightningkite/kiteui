package com.lightningkite.kiteui.views.direct

import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.AnimationFrame
import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.kiteui.reactive.ReadableState
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.views.*

actual class Video actual constructor(context: RContext): RView(context) {
    override val native = PlayerView(context.activity).apply {
        player = ExoPlayer.Builder(context.activity).build()
    }

    actual var source: VideoSource?
        get() = TODO()
        set(value) {
            when (value) {
                null -> {
                    native.player!!.stop()
                    native.player!!.clearMediaItems()
                }

                is VideoRemote -> {
                    native.player!!.setMediaItem(MediaItem.fromUri(value.url))
                    native.player!!.prepare()
                }

                is VideoRaw -> {
                    TODO()
                }

                is VideoResource -> {
                    native.player!!.setMediaItem(MediaItem.fromUri("android.resource://${native.context.packageName}/${value.resource}"))
                    native.player!!.prepare()
                }

                is VideoLocal -> {
                    native.player!!.setMediaItem(MediaItem.fromUri(value.file.uri))
                    native.player!!.prepare()
                }

                else -> {}
            }
        }
    actual val time: Writable<Double>
        get() = object : Writable<Double> {
            override suspend fun set(value: Double) {
                native.player!!.seekTo((value * 1000.0).toLong())
            }

            override val state get() = ReadableState(native.player!!.currentPosition / 1000.0)

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
    actual val playing: Writable<Boolean>
        get() = object : Writable<Boolean> {
            override suspend fun set(value: Boolean) {
                if (value) {
                    native.player!!.play()
                } else {
                    native.player!!.pause()
                }
            }

            override val state: ReadableState<Boolean> get() = ReadableState(native.player!!.isPlaying)

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
    actual val volume: Writable<Float>
        get() = object : Writable<Float> {
            override suspend fun set(value: Float) {
                native.player!!.volume = value
            }

            override val state: ReadableState<Float> get() = ReadableState(native.player!!.volume)

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
    actual var showControls: Boolean
        get() = native.useController
        set(value) {
            native.useController = value
        }
    actual var loop: Boolean
        get() = native.player!!.repeatMode == Player.REPEAT_MODE_ONE
        set(value) {
            native.player!!.repeatMode = Player.REPEAT_MODE_ONE
        }
    @get:OptIn(UnstableApi::class)
    @set:OptIn(UnstableApi::class)
    actual var scaleType: ImageScaleType
        get() = when (native.resizeMode) {
            AspectRatioFrameLayout.RESIZE_MODE_FIT -> ImageScaleType.Fit
            AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> ImageScaleType.Crop
            else -> ImageScaleType.NoScale
        }
        set(value) {
            native.resizeMode = when (value) {
                ImageScaleType.Fit -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                ImageScaleType.Crop -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                else -> AspectRatioFrameLayout.RESIZE_MODE_FILL
            }
        }
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