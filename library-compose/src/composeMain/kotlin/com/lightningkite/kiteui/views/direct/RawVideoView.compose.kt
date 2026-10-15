package com.lightningkite.kiteui.views.direct

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.onRemove
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.withWrite
import io.github.kdroidfilter.composemediaplayer.VideoPlayerSurface
import io.github.kdroidfilter.composemediaplayer.createVideoPlayerState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

// preloadHint is ignored, as on Android: the player always loads the media up front.
public actual class RawVideoView actual constructor(
    context: ElementContext,
    public actual val source: VideoSource,
    public actual val description: String,
    public actual val scaleType: ImageScaleType,
    public actual val preloadHint: PreloadHint,
) : NativeElement(context) {
    private val player = createVideoPlayerState()

    private val loadState = RawReactive<Unit>()
    public actual val state: Reactive<Unit> = loadState

    private val position = Signal(Duration.ZERO)
    private val isPlaying = Signal(false)
    private val duration = Signal<Double?>(null)
    private val currentVolume = Signal(player.volume)
    private val completed = BasicListenable()

    // Like the web's autoplay: asking to play before the media has loaded plays it once it has.
    private var playWhenReady = false

    init {
        onRemove { player.dispose() }
        launch {
            try {
                player.openUri(source.toPlayerUri())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                loadState.state = ReactiveState.exception(e)
            }
        }
        // Not element.launch: see ScrollingBehaviorImpl.  This scope still ends with the element.
        val scope = CoroutineScope(Dispatchers.Main.immediate + Job(coroutineContext[Job]))
        scope.launch {
            snapshotFlow { player.error }.collect { error ->
                if (error != null) loadState.state = ReactiveState.exception(Exception("Failed to load video $source: $error"))
            }
        }
        scope.launch {
            snapshotFlow { player.hasMedia && !player.isLoading }.collect { ready ->
                if (!ready) return@collect
                loadState.state = ReactiveState(Unit)
                if (playWhenReady) {
                    playWhenReady = false
                    player.play()
                }
            }
        }
        scope.launch { snapshotFlow { player.currentTime }.collect { position.value = it.seconds } }
        scope.launch { snapshotFlow { player.isPlaying }.collect { isPlaying.value = it } }
        scope.launch { snapshotFlow { player.duration.takeIf { it > 0 } }.collect { duration.value = it } }
        scope.launch { snapshotFlow { player.volume }.collect { currentVolume.value = it } }
        // The macOS player never calls onPlaybackEnded, so watch for playback stopping at the end instead.
        // With loop on, the player seeks back to the start without stopping, so like the web's "ended" this doesn't fire.
        scope.launch {
            snapshotFlow { !player.isPlaying && player.duration > 0 && player.currentTime >= player.duration }.collect { ended ->
                if (ended) completed.invokeAll()
            }
        }
    }

    public actual val currentTime: MutableReactive<Duration> = position.withWrite { to ->
        // The player seeks to a fraction of the duration, in thousandths.
        val total = player.duration
        check(total > 0) { "Can't seek $source before its duration is known; wait for state to be ready" }
        player.seekTo((to.toDouble(DurationUnit.SECONDS) / total * 1000).toFloat())
    }

    @Deprecated("Use currentTime instead")
    public actual val time: MutableReactive<Double> = currentTime.lens(get = { it.toDouble(DurationUnit.SECONDS) }, set = { it.seconds })

    public actual val sourceDuration: Reactive<Double?> = duration

    public actual val playing: MutableReactive<Boolean> = isPlaying.withWrite {
        if (!loadState.state.success) playWhenReady = it
        else if (it) player.play()
        else player.pause()
    }

    public actual val volume: MutableReactive<Float> = currentVolume.withWrite { player.volume = it }

    public actual var showControls: Boolean by mutableStateOf(false)

    public actual var loop: Boolean
        get() = player.loop
        set(value) {
            player.loop = value
        }

    public actual val completedPlay: Listenable = completed

    public actual val seekableTimeRanges: List<ClosedFloatingPointRange<Double>>
        get() = if (player.duration > 0) listOf(0.0..player.duration) else listOf()

    @Composable
    override fun Content(modifier: Modifier) {
        VideoPlayerSurface(
            playerState = player,
            // Sized like the web's <video>: its own aspect ratio, unless the parent fixes both axes.
            modifier = modifier.aspectRatio(player.aspectRatio).semantics { contentDescription = description },
            contentScale = when (scaleType) {
                ImageScaleType.Fit -> ContentScale.Fit
                ImageScaleType.Crop -> ContentScale.Crop
                ImageScaleType.Stretch -> ContentScale.FillBounds
                ImageScaleType.NoScale -> ContentScale.None
            },
        ) {
            if (showControls) Controls()
        }
    }

    // A minimal stand-in for the browser's controls: play/pause and a seek bar.
    @Composable
    private fun Controls() {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Row(
                Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.5f)).padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(
                    Modifier
                        .size(20.dp)
                        .clickable { launch { playing.set(!player.isPlaying) } }
                        .drawBehind {
                            if (player.isPlaying) {
                                val bar = Size(size.width / 3, size.height)
                                drawRect(Color.White, Offset.Zero, bar)
                                drawRect(Color.White, Offset(size.width - bar.width, 0f), bar)
                            } else {
                                drawPath(Path().apply {
                                    moveTo(0f, 0f)
                                    lineTo(size.width, size.height / 2)
                                    lineTo(0f, size.height)
                                    close()
                                }, Color.White)
                            }
                        }
                )
                Spacer(
                    Modifier
                        .weight(1f)
                        .height(20.dp)
                        .pointerInput(Unit) {
                            fun seekTo(x: Float) = player.seekTo((x / size.width).coerceIn(0f, 1f) * 1000)
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                seekTo(down.position.x)
                                drag(down.id) {
                                    seekTo(it.position.x)
                                    it.consume()
                                }
                            }
                        }
                        .drawBehind {
                            val trackHeight = 4.dp.toPx()
                            val top = (size.height - trackHeight) / 2
                            val fraction = if (player.duration > 0) (player.currentTime / player.duration).toFloat() else 0f
                            drawRect(Color.White.copy(alpha = 0.3f), Offset(0f, top), Size(size.width, trackHeight))
                            drawRect(Color.White, Offset(0f, top), Size(size.width * fraction, trackHeight))
                        }
                )
            }
        }
    }
}

internal expect suspend fun VideoSource.toPlayerUri(): String
