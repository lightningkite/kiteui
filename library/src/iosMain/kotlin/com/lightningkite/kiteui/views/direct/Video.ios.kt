package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.objc.UIViewWithSizeOverridesProtocol
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.AVFoundation.*
import platform.AVKit.AVPlayerViewController
import platform.AVKit.AVPlayerViewControllerDelegateProtocol
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGRectZero
import platform.CoreGraphics.CGSize
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMake
import platform.Foundation.NSBundle
import platform.Foundation.NSURL
import platform.UIKit.*
import platform.UniformTypeIdentifiers.UTTypeVideo
import platform.UniformTypeIdentifiers.loadFileRepresentationForContentType
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.ref.WeakReference

@OptIn(ExperimentalForeignApi::class)
actual class NVideo: UIView(CGRectZero.readValue()), UIViewWithSizeOverridesProtocol, AVPlayerViewControllerDelegateProtocol {
    var padding: Double
        get() = extensionPadding ?: 0.0
        set(value) { extensionPadding = value }

    private val childSizeCache: ArrayList<HashMap<Size, Size>> = ArrayList()
    override fun sizeThatFits(size: CValue<CGSize>): CValue<CGSize> = frameLayoutSizeThatFits(size, childSizeCache)
    override fun layoutSubviews() = frameLayoutLayoutSubviews(childSizeCache)
    override fun subviewDidChangeSizing(view: UIView?) = frameLayoutSubviewDidChangeSizing(view, childSizeCache)
    override fun didAddSubview(subview: UIView) {
        super.didAddSubview(subview)
        frameLayoutDidAddSubview(subview, childSizeCache)
    }
    override fun willRemoveSubview(subview: UIView) {
        if(this != null) frameLayoutWillRemoveSubview(subview, childSizeCache)
        super.willRemoveSubview(subview)
    }

    override fun hitTest(point: CValue<CGPoint>, withEvent: UIEvent?): UIView? {
        return frameLayoutHitTest(point, withEvent)
    }

    val controller = AVPlayerViewController()
    init {
        controller.delegate = this
        addSubview(controller.view)
    }

    val volume = Property(1.0f)
    val time = Property(0.0)
    val playing = Property(false)
    val controls = Property(false)
    val loop = Property(false)
    val scaleType = Property(ImageScaleType.Fit)
    private var animationFrameRateClose: (()->Unit)? = null
    private var playerRateObservationClose: (()->Unit)? = null
    private var volumeObservationClose: (()->Unit)? = null
    private var endObservationClose: (()->Unit)? = null

    @OptIn(ExperimentalNativeApi::class)
    var player: AVPlayer?
        get() = controller.player
        set(value) {
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
                    if(playing.value != value) playing.value = value
                    if (player.rate > 0f) {
                        animationFrameRateClose = AnimationFrame.addListener {
                            time.value = CMTimeGetSeconds(player.currentTime())
                        }
                    } else {
                        animationFrameRateClose?.invoke()
                        animationFrameRateClose = null
                    }
                }
                volumeObservationClose = player.observe("volume") {
                    val player = weakPlayer.get() ?: return@observe
                    val value = player.volume
                    volume.value = value
                }
//                endObservationClose =
            }
        }
}


@ViewDsl
actual inline fun ViewWriter.videoActual(crossinline setup: Video.() -> Unit): Unit = element(NVideo()) {
    handleTheme(this, viewDraws = false)
    context.addChildViewController(controller)
    calculationContext.onRemove {
        controller.removeFromParentViewController()
    }
    setup(Video(this))
}

actual inline var Video.source: VideoSource?
    get() = TODO()
    set(value) {
        when (value) {
            null -> {
                native.player = null
                native.informParentOfSizeChange()
            }
            is VideoRaw -> {
                TODO()
            }

            is VideoRemote -> {
                native.player = AVPlayer(NSURL(string = value.url))
                native.informParentOfSizeChange()
            }

            is VideoResource -> {
                native.player = AVPlayer(NSBundle.mainBundle.URLForResource(value.name, value.extension)!!)
                native.informParentOfSizeChange()
            }

            is VideoLocal -> {
                native.controller.player = null
                native.informParentOfSizeChange()
                value.file.provider.loadFileRepresentationForContentType(
                    value.file.suggestedType ?: UTTypeVideo,
                    openInPlace = true
                ) { url, b, err ->
                    if (url != null) {
                        dispatch_async(queue = dispatch_get_main_queue(), block = {
                            native.player = AVPlayer(url)
                            native.informParentOfSizeChange()
                        })
                    }
                }
            }

            else -> {}
        }
    }
@OptIn(ExperimentalForeignApi::class)
actual val Video.time: Writable<Double> get() = native.time
    .withWrite {
        native.controller.player?.seekToTime(CMTimeMake((it * 1000.0).toLong(), 1000))
    }
@OptIn(ExperimentalForeignApi::class)
actual val Video.playing: Writable<Boolean> get() = native.playing
    .withWrite {
        if(it)
            native.controller.player?.play()
        else
            native.controller.player?.pause()
    }

actual val Video.volume: Writable<Float> get() = native.volume
    .withWrite {
        native.controller.player?.volume = it
    }
actual var Video.showControls: Boolean
    get() = native.controller.showsPlaybackControls
    set(value) {
        native.controller.showsPlaybackControls = value
    }
// TODO
actual var Video.loop: Boolean
    get() = native.loop.value
    set(value) {
        native.loop.value = value
    }
actual var Video.scaleType: ImageScaleType
    get() = when(native.controller.videoGravity) {
        AVLayerVideoGravityResizeAspect -> ImageScaleType.Fit
        AVLayerVideoGravityResizeAspectFill -> ImageScaleType.Crop
        AVLayerVideoGravityResize -> ImageScaleType.Stretch
        AVLayerVideoGravityResize -> ImageScaleType.NoScale
        else -> ImageScaleType.NoScale
    }
    set(value) {
        native.controller.videoGravity = when(value) {
            ImageScaleType.Fit -> AVLayerVideoGravityResizeAspect
            ImageScaleType.Crop -> AVLayerVideoGravityResizeAspectFill
            ImageScaleType.Stretch -> AVLayerVideoGravityResize
            ImageScaleType.NoScale -> AVLayerVideoGravityResize
        }
    }