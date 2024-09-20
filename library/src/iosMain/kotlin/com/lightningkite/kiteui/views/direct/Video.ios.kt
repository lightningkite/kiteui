package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.objc.UIViewWithSizeOverridesProtocol
import com.lightningkite.kiteui.printStackTrace2
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import kotlinx.cinterop.*
import platform.AVFoundation.*
import platform.AVKit.AVPlayerViewController
import platform.AVKit.AVPlayerViewControllerDelegateProtocol
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGRectZero
import platform.CoreGraphics.CGSize
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMake
import platform.Foundation.NSBundle
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSURL
import platform.UIKit.*
import platform.UniformTypeIdentifiers.UTTypeVideo
import platform.UniformTypeIdentifiers.loadFileRepresentationForContentType
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.darwin.sel_registerName
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.ref.WeakReference


actual class Video actual constructor(context: RContext) : RView(context) {

    inner class IosDelegate: NSObject(), AVPlayerViewControllerDelegateProtocol {

    }
    val ios = IosDelegate()

    val controller = AVPlayerViewController().apply {
        delegate = ios
    }
    override val native = controller.view

    private val _playing = Property(false)
    private val _volume = Property(0f)
    private val _time = Property(0.0)
    private var animationFrameRateClose: (() -> Unit)? = null
    private var playerRateObservationClose: (() -> Unit)? = null
    private var volumeObservationClose: (() -> Unit)? = null
    private var endObservationClose: (() -> Unit)? = null
    internal var onComplete: (() -> Unit)? = null
    internal var shouldPlay = false

    @OptIn(ExperimentalNativeApi::class)
    private var player: AVPlayer?
        get() = controller.player
        set(value) {
            native.hidden = value == null
            playerRateObservationClose?.invoke()
            playerRateObservationClose = null
            volumeObservationClose?.invoke()
            volumeObservationClose = null
            endObservationClose?.invoke()
            endObservationClose = null
            controller.player = value
            value?.let { player ->
                val weakPlayer = WeakReference(player)
                playerRateObservationClose = player.observe("rate") {
                    val player = weakPlayer.get() ?: return@observe
                    val value = player.rate > 0f
                    if (!value && loop && shouldPlay) {
                        controller.player?.seekToTime(CMTimeMake(0.toLong(), 1000))
                        controller.player?.play()
                        if (!_playing.value) _playing.value = true
                    } else {
                        if (_playing.value != value) _playing.value = value
                        if (player.rate > 0f) {
                            animationFrameRateClose = AppState.animationFrame.addListener {
                                _time.value = CMTimeGetSeconds(player.currentTime())
                            }
                        } else {
                            animationFrameRateClose?.invoke()
                            animationFrameRateClose = null
                        }
                    }
                }
                volumeObservationClose = player.observe("volume") {
                    val player = weakPlayer.get() ?: return@observe
                    val value = player.volume
                    _volume.value = value
                }
//                endObservationClose =
            }
        }

    
    val playerCallbackHolder = object: NSObject() {
        @ObjCAction
        fun playerItemDidReachEnd(notification: NSNotification?) {
            if (player?.rate == 0f) {
                onComplete?.invoke()
            }
        }
    }

    init {
        NSNotificationCenter.defaultCenter.addObserver(
            observer = playerCallbackHolder,
            selector = sel_registerName("playerItemDidReachEnd:"),
            name = AVPlayerItemDidPlayToEndTimeNotification,
            `object` = null
        )
    }


    actual var source: VideoSource? = null
        set(value) {
            field = value
            when (value) {
                null -> {
                    player = null
                    native.informParentOfSizeChange()
                }

                is VideoRaw -> {
                    TODO()
                }

                is VideoRemote -> {
                    player = AVPlayer(NSURL(string = value.url))
                    native.informParentOfSizeChange()
                }

                is VideoResource -> {
                    try {
                        player = AVPlayer(
                            NSBundle.mainBundle.URLForResource(value.name, value.extension)
                                ?: throw Exception("Could not find the video in the bundle ${value.name} / ${value.extension}")
                        )
                        native.informParentOfSizeChange()
                    } catch (e: Exception) {
                        e.printStackTrace2()
                    }
                }

                is VideoLocal -> {
                    controller.player = null
                    native.informParentOfSizeChange()
                    value.file.provider.loadFileRepresentationForContentType(
                        value.file.suggestedType ?: UTTypeVideo,
                        openInPlace = true
                    ) { url, b, err ->
                        if (url != null) {
                            dispatch_async(queue = dispatch_get_main_queue(), block = {
                                player = AVPlayer(url)
                                native.informParentOfSizeChange()
                            })
                        }
                    }
                }

                else -> {}
            }
        }

    
    actual val time: Writable<Double>
        get() = _time
            .withWrite {
                controller.player?.seekToTime(CMTimeMake((it * 1000.0).toLong(), 1000))
            }

    
    actual val playing: Writable<Boolean>
        get() = _playing
            .withWrite {
                shouldPlay = it
                if (it)
                    controller.player?.play()
                else
                    controller.player?.pause()
            }

    actual val volume: Writable<Float>
        get() = _volume
            .withWrite {
                controller.player?.volume = it
            }
    actual var showControls: Boolean
        get() = controller.showsPlaybackControls
        set(value) {
            controller.showsPlaybackControls = value
        }
    actual var loop: Boolean = false
    actual var scaleType: ImageScaleType = ImageScaleType.Crop
        set(value) {
            field = value
            controller.videoGravity = when (value) {
                ImageScaleType.Fit -> AVLayerVideoGravityResizeAspect
                ImageScaleType.Crop -> AVLayerVideoGravityResizeAspectFill
                ImageScaleType.Stretch -> AVLayerVideoGravityResize
                ImageScaleType.NoScale -> AVLayerVideoGravityResize
            }
        }

}