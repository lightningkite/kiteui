package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.printStackTrace2
import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.ref.WeakReference
import kotlinx.cinterop.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.AVFoundation.*
import platform.AVKit.AVPlayerViewController
import platform.AVKit.AVPlayerViewControllerDelegateProtocol
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMake
import platform.Foundation.NSBundle
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSURLAuthenticationChallenge
import platform.Foundation.NSUUID.Companion.UUID
import platform.Foundation.writeToFile
import platform.UIKit.*
import platform.UniformTypeIdentifiers.UTTypeVideo
import platform.UniformTypeIdentifiers.loadFileRepresentationForContentType
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.darwin.sel_registerName
import kotlin.collections.mapOf
import kotlin.time.Duration.Companion.seconds


actual class RawVideoView actual constructor(
    context: RContext,
    actual val source: VideoSource,
    actual val description: String,
    actual val scaleType: ImageScaleType,
) : RView(context) {

    inner class IosDelegate: NSObject(), AVPlayerViewControllerDelegateProtocol {

    }
    val ios = IosDelegate()

    val controller = AVPlayerViewController().apply {
        delegate = ios

        // Most UIViews use UIViewAutoresizingNone by default, but AVPlayerViewController does not and causes
        // contention with size and positioning logic in KiteUI
        view.autoresizingMask = UIViewAutoresizingNone

        // AVPlayerViewController sets black as the default background color which interferes with the background color
        // set by the KiteUI theme
        view.backgroundColor = UIColor.colorWithWhite(0.0, 0.0)
    }
    override val native = controller.view

        private val _state = RawReactive<Unit>()
        actual val state: Reactive<Unit> = _state

    private val _playing = Signal(false)
    private val _volume = Signal(0f)
    private val _time = Signal(0.0)
    private var animationFrameRateClose: (() -> Unit)? = null
    private var playerRateObservationClose: (() -> Unit)? = null
    private var volumeObservationClose: (() -> Unit)? = null
    private var endObservationClose: (() -> Unit)? = null
    private var playerStatusObservationClose: (() -> Unit)? = null
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
            playerStatusObservationClose?.invoke()
            playerStatusObservationClose = null
            controller.player = value
            value?.let { player ->
                val weakPlayer = WeakReference(player)
                playerRateObservationClose = player.observe("rate") {
                    val player = weakPlayer.get() ?: return@observe
                    val value = player.rate > 0f
                    if (shouldPlay && !value) _completedPlay.invokeAll()
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
                playerStatusObservationClose = player.observe("status") {
                    val p = weakPlayer.get() ?: return@observe
                    when (p.status) {
                        AVPlayerStatusReadyToPlay -> {
                            _state.state = ReactiveState(Unit)
                        }
                        AVPlayerStatusFailed -> {
                            val message = p.error?.localizedDescription ?: "Video failed to load"
                            _state.state = ReactiveState.exception(Exception(message))
                        }
                        else -> {}
                    }
                }
//                endObservationClose =
            }
        }

    
    val playerCallbackHolder: AVAssetResourceLoaderDelegateProtocol = object: NSObject(), AVAssetResourceLoaderDelegateProtocol {
        @ObjCAction
        fun playerItemDidReachEnd(notification: NSNotification?) {
            if (player?.rate == 0f) {
                onComplete?.invoke()
            }
        }
        @ObjCAction
        fun handleAVPlayerAccess(notification: NSNotification) {
            val playerItem = notification.`object` as? AVPlayerItem ?: return
            val lastEvent = playerItem.accessLog()?.events?.lastOrNull() as? AVPlayerItemAccessLogEvent ?: return

            val indicatedBitrate = lastEvent.indicatedBitrate

            println("--------------PLAYER LOG--------------")
            println("EVENT: ${lastEvent}")
            println("INDICATED BITRATE: ${indicatedBitrate}")
            println("PLAYBACK RELATED LOG EVENTS")
            println("PLAYBACK START DATE: ${lastEvent.playbackStartDate}")
            println("PLAYBACK SESSION ID: ${lastEvent.playbackSessionID}")
            println("PLAYBACK START OFFSET: ${lastEvent.playbackStartOffset}")
            println("PLAYBACK TYPE: ${lastEvent.playbackType}")
            println("STARTUP TIME: ${lastEvent.startupTime}")
            println("DURATION WATCHED: ${lastEvent.durationWatched}")
            println("NUMBER OF DROPPED VIDEO FRAMES: ${lastEvent.numberOfDroppedVideoFrames}")
            println("NUMBER OF STALLS: ${lastEvent.numberOfStalls}")
            println("SEGMENTS DOWNLOADED DURATION: ${lastEvent.segmentsDownloadedDuration}")
            println("DOWNLOAD OVERDUE: ${lastEvent.downloadOverdue}")
            println("--------------------------------------")
        }

        override fun resourceLoader(
            resourceLoader: AVAssetResourceLoader,
            didCancelLoadingRequest: AVAssetResourceLoadingRequest
        ) {
            println("resourceLoader.didCancelLoadingRequest(${didCancelLoadingRequest})")
        }

        override fun resourceLoader(
            resourceLoader: AVAssetResourceLoader,
            didCancelAuthenticationChallenge: NSURLAuthenticationChallenge
        ) {
            println("resourceLoader.didCancelAuthenticationChallenge(${didCancelAuthenticationChallenge})")
        }
    }

    init {
        NSNotificationCenter.defaultCenter.addObserver(
            observer = playerCallbackHolder,
            selector = sel_registerName("playerItemDidReachEnd:"),
            name = AVPlayerItemDidPlayToEndTimeNotification,
            `object` = null
        )
        NSNotificationCenter.defaultCenter.addObserver(
            observer = playerCallbackHolder,
            selector = sel_registerName("handleAVPlayerAccess:"),
            name = AVPlayerItemNewAccessLogEntryNotification,
            `object` = null
        )
        launch {
            println("AVPlayerStatusUnknown: $AVPlayerStatusUnknown")
            println("AVPlayerStatusReadyToPlay: $AVPlayerStatusReadyToPlay")
            println("AVPlayerStatusFailed: $AVPlayerStatusFailed")
            while(true) {
                delay(10.seconds)
//                if(player?.status == AVPlayerStatusFailed) {
                    println("Player: $player")
                    println("Player status: ${player?.status}")
                    println("ERR: ${player?.error}")
                    println("Error from player: " + player?.error?.localizedDescription)
//                }
            }
        }
    }


    init {
        // Accessibility description
        native.accessibilityLabel = description

        // Apply scale type to video gravity
        controller.videoGravity = when (scaleType) {
            ImageScaleType.Fit -> AVLayerVideoGravityResizeAspect
            ImageScaleType.Crop -> AVLayerVideoGravityResizeAspectFill
            ImageScaleType.Stretch -> AVLayerVideoGravityResize
            ImageScaleType.NoScale -> AVLayerVideoGravityResize
        }

        // Load the provided source
        when (val value = source) {
            is VideoRaw -> {
                val filePath = NSTemporaryDirectory() + "/" + UUID().UUIDString() + ".mp4"
                value.data.data.writeToFile(filePath, true)
                val url = NSURL.fileURLWithPath(filePath)
                val reread = url.filePathURL!!
                player = AVPlayer(uRL = reread)
                native.informParentOfSizeChange()
            }

            is VideoRemote -> {
                val asset = AVURLAsset(NSURL(string = value.url), mapOf<Any?, Any?>())
                asset.resourceLoader.setDelegate(playerCallbackHolder, dispatch_get_main_queue())
                player = AVPlayer(AVPlayerItem(asset)).also {
                    println("Player: $it")
                    println("Url is being set to ${value.url}")
                }
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
                    _state.state = ReactiveState.exception(e)
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

    
    actual val time: MutableReactive<Double>
        get() = _time
            .withWrite {
                controller.player?.seekToTime(CMTimeMake((it * 1000.0).toLong(), 1000))
            }

    
    actual val playing: MutableReactive<Boolean>
        get() = _playing
            .withWrite {
                shouldPlay = it
                if (it)
                    controller.player?.play()
                else
                    controller.player?.pause()
            }

    actual val volume: MutableReactive<Float>
        get() = _volume
            .withWrite {
                controller.player?.volume = it
            }
    actual var showControls: Boolean
        get() = controller.showsPlaybackControls
        set(value) {
            controller.showsPlaybackControls = value
            controller.updatesNowPlayingInfoCenter = value
        }
    actual var loop: Boolean = false
    private val _completedPlay = BasicListenable()
    actual val completedPlay: Listenable get() = _completedPlay
}