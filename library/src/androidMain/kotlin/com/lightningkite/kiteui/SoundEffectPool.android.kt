package com.lightningkite.kiteui

import android.media.MediaPlayer
import android.media.SoundPool
import android.net.Uri
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.kiteui.utils.SuspendCache
import com.lightningkite.kiteui.views.AndroidAppContext
import com.lightningkite.reactive.core.AppScope
import com.lightningkite.reactive.core.BaseListenable
import com.lightningkite.reactive.core.MutableReactive
import com.lightningkite.reactive.core.ReactiveState
import com.lightningkite.reactive.extensions.invokeAllSafe
import kotlinx.coroutines.*
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Backed by `SoundPool`, which is built for short, overlapping effects and imposes two limits worth
 * knowing before choosing a clip:
 *
 * - **About one megabyte of *decoded* audio per sound.** Decoded, not encoded - so the ceiling is
 *   set by sample rate and channel count, not by file size. At 44.1kHz stereo 16-bit that is
 *   roughly 5.7 seconds, and a longer clip is silently truncated rather than rejected. A
 *   compressed file that looks small on disk is no help: an 800KB MP3 decodes to far more.
 * - **No completion callback**, which is why [PlayingSoundEffect.isPlaying] cannot report a sound
 *   reaching its own end here. See the property for details.
 *
 * For anything longer than a few seconds - music, voice, a full sound bed - use
 * [AudioSource.load] instead. It is backed by `MediaPlayer`, which streams rather than decoding
 * into memory, has no length limit, and reports completion properly.
 */
public actual class SoundEffectPool actual constructor(concurrency: Int) {

    private val soundPool = SoundPool.Builder().apply {
        setMaxStreams(concurrency)
    }.build()

    private val loaded = SuspendCache<AudioSource, Int> { source ->
        when (source) {
            // SoundPool can only load from a resource or a file path, so remote and raw audio are
            // first staged in a cache file, the same trick used by AudioSource.load().
            is AudioRemote -> loadBytes(fetch(source.url).blob().toByteArray())
            is AudioRaw -> loadBytes(source.data.toByteArray())
            is AudioLocal -> awaitLoad(soundPool.load(source.file.uri.path, 1))
            is AudioResource -> awaitLoad(soundPool.load(AndroidAppContext.applicationCtx, source.resource, 1))
        }
    }

    // SoundPool.load() returns immediately and decodes on a background thread, reporting every
    // result through one pool-wide listener. Rather than have the waiter register and the listener
    // look up - which breaks if the listener gets there first - both sides go through
    // completionFor(), so whichever arrives first creates the CompletableDeferred and the other
    // finds it. That has to be atomic: the listener is delivered on the Looper of the thread that
    // built the pool, which need not be the thread staging the load. putIfAbsent gives exactly that
    // guarantee, and unlike computeIfAbsent it is not desugared on API 23.
    private val loadCompletions = ConcurrentHashMap<Int, CompletableDeferred<Unit>>()

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            val completion = completionFor(sampleId)
            if (status == 0) completion.complete(Unit)
            else completion.completeExceptionally(IOException("SoundPool failed to load sample (status $status)"))
        }
    }

    private fun completionFor(sampleId: Int): CompletableDeferred<Unit> {
        val fresh = CompletableDeferred<Unit>()
        return loadCompletions.putIfAbsent(sampleId, fresh) ?: fresh
    }

    private suspend fun awaitLoad(soundId: Int): Int {
        // A sample ID of 0 means SoundPool rejected the load synchronously (bad data, pool full);
        // no completion event will ever arrive for it.
        if (soundId == 0) throw IOException("SoundPool failed to load sample")
        try {
            completionFor(soundId).await()
        } finally {
            loadCompletions.remove(soundId)
        }
        return soundId
    }

    private suspend fun loadBytes(bytes: ByteArray): Int {
        // AppScope runs on the main dispatcher, so staging has to move off it: writing a
        // multi-megabyte clip would otherwise block the UI thread for the whole write.
        val file = withContext(Dispatchers.IO) {
            File.createTempFile("soundeffect", ".audio", AndroidAppContext.applicationCtx.cacheDir)
        }
        return try {
            // Inside the try, so a write that fails partway (a full cache partition) still leaves a
            // deletable file behind rather than an orphan.
            withContext(Dispatchers.IO) { file.writeBytes(bytes) }
            awaitLoad(soundPool.load(file.path, 1))
        } finally {
            // NonCancellable so the staging file is still cleaned up when the load is cancelled.
            withContext(NonCancellable + Dispatchers.IO) { file.delete() }
        }
    }

    public actual suspend fun preload(sound: AudioSource) {
        loaded.get(sound)
    }

    public actual suspend fun play(sound: AudioSource): PlayingSoundEffect {
        val streamId = soundPool.play(loaded.get(sound), 1.0f, 1.0f, 0, 0, 1.0f)
        return object : PlayingSoundEffect {
            override var volume: Float = 1f
                set(value) {
                    field = value
                    soundPool.setVolume(streamId, value, value)
                }

            /** Backing state for [isPlaying], so [stop] can clear it without re-entering the setter. */
            private var isPlayingBacking: Boolean = true

            /**
             * **Reports pausing and stopping, but not a sound reaching its own end.**
             *
             * `SoundPool` exposes no completion callback of any kind - there is no listener, no
             * stream-state query, nothing to observe - so a clip that simply finishes leaves this
             * reading `true`. Deriving it from the clip's duration would be the only alternative,
             * and it would be wrong the moment a stream is paused, resumed or pre-empted when the
             * pool runs out of channels.
             *
             * The other platforms do report a natural end, so treat a `true` here as "not
             * explicitly stopped" rather than "still audible".
             */
            override var isPlaying: Boolean
                get() = isPlayingBacking
                set(value) {
                    if (isPlayingBacking == value) return
                    isPlayingBacking = value
                    if (value) {
                        soundPool.resume(streamId)
                    } else {
                        soundPool.pause(streamId)
                    }
                }

            override fun stop() {
                soundPool.stop(streamId)
                // Has to be assigned through the backing field, not the setter: the setter would
                // see the change to false and call pause() on a stream that has just been stopped.
                isPlayingBacking = false
            }
        }
    }

    public actual fun unload(sound: AudioSource) {
        val loading = loaded.peek(sound) ?: return
        // Dropped from the cache as well as from the pool. Leaving it cached meant a later play()
        // handed SoundPool a sample ID it had already freed, which plays nothing at all.
        loaded.forget(sound)
        AppScope.launch {
            // A load that ended up failing has no sample to free, and evicted itself already.
            val id = try {
                loading.await()
            } catch (e: Exception) {
                return@launch
            }
            soundPool.unload(id)
        }
    }
}

private val runningMediaPlayers = ArrayList<MediaPlayer>()
public actual suspend fun AudioSource.load(): PlayableAudio {
    val player = MediaPlayer()
    var toClose: Closeable? = null
    when (this) {
        is AudioLocal -> player.setDataSource(AndroidAppContext.applicationCtx, file.uri)
        is AudioRaw -> {
            // MediaPlayer has no API to play from an in-memory buffer, so stage it in a cache file.
            // Deleting the file once it is opened (see toClose below) is safe: the file stays
            // readable through its open descriptor in the media server until playback finishes.
            // Written off the caller's dispatcher, which is the main thread in practice.
            val file = withContext(Dispatchers.IO) {
                File.createTempFile("soundeffect", ".audio", AndroidAppContext.applicationCtx.cacheDir)
                    .apply { writeBytes(data.toByteArray()) }
            }
            player.setDataSource(file.path)
            toClose = Closeable { file.delete() }
        }
        is AudioRemote -> player.setDataSource(AndroidAppContext.applicationCtx, Uri.parse(url))
        is AudioResource -> {
            val afd = AndroidAppContext.applicationCtx.resources.openRawResourceFd(this.resource)
                ?: throw IllegalStateException("No such resource found")
            player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            toClose = afd
        }
    }
    val onCompletes = ArrayList<() -> Unit>()
    player.setOnCompletionListener {
        runningMediaPlayers.remove(player)
        onCompletes.invokeAllSafe()
    }
    val audio = object : PlayableAudio {
        override var volume: Float = 1f
            set(value) {
                field = value; player.setVolume(value, value)
            }
        override var loop: Boolean = false
            set(value) {
                player.isLooping = value
            }
        override var isPlaying: Boolean
            get() = player.isPlaying
            set(value) {
                if (value == player.isPlaying) return
                if (value) {
                    player.start()
                    runningMediaPlayers.add(player)
                } else {
                    runningMediaPlayers.remove(player)
                    player.pause()
                }
            }

        override fun onComplete(action: () -> Unit) {
            onCompletes.add(action)
        }

        override fun stop() {
            if (player.isPlaying) {
                player.pause()
                runningMediaPlayers.remove(player)
            }
        }

        override val currentTime: MutableReactive<Duration> = object : BaseListenable(), MutableReactive<Duration> {
            override suspend fun set(value: Duration) {
                player.seekTo(value.inWholeMilliseconds.toInt())
            }

            override val state get() = ReactiveState(player.currentPosition.milliseconds)

            var remover: (() -> Unit)? = null

            override fun activate() {
                remover = AppState.animationFrame.addListener {
                    if (player.isPlaying) invokeAllListeners()
                }
            }

            override fun deactivate() {
                remover?.invoke()
                remover = null
            }
        }

    }
    return suspendCancellableCoroutine { cont ->
        player.setOnPreparedListener {
            toClose?.close()
            cont.resume(audio)
        }
        player.prepareAsync()
        cont.invokeOnCancellation {
            player.release()
        }
    }
}
