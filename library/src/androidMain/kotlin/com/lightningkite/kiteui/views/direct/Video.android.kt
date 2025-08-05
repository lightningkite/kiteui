package com.lightningkite.kiteui.views.direct

import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*

@InternalKiteUi
public actual class Video public actual constructor(context: RContext): RView(context) {
    override val native: PlayerView = PlayerView(context.activity).apply {
        player = ExoPlayer.Builder(context.activity).build()
    }

    public actual var source: VideoSource?
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
    public actual val time: MutableReactive<Double>
        get() = object : MutableReactive<Double> {
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
    public actual val playing: MutableReactive<Boolean>
        get() = object : MutableReactive<Boolean> {
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
    public actual val volume: MutableReactive<Float>
        get() = object : MutableReactive<Float> {
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
    public actual var showControls: Boolean
        get() = native.useController
        set(value) {
            native.useController = value
        }
    public actual var loop: Boolean
        get() = native.player!!.repeatMode == Player.REPEAT_MODE_ONE
        set(value) {
            native.player!!.repeatMode = Player.REPEAT_MODE_ONE
        }
    @get:OptIn(UnstableApi::class)
    @set:OptIn(UnstableApi::class)
    public actual var scaleType: ImageScaleType
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

//public actual fun Video.onComplete(action: () -> Unit) {
//    val l = object: Player.Listener {
//        override fun onIsPlayingChanged(isPlaying: Boolean) {
//            action()
//        }
//    }
//    native.player!!.addListener(l)
//    return { native.player!!.removeListener(l) }
//}