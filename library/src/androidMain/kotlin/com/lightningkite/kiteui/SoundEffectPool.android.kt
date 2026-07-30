package com.lightningkite.kiteui

import android.media.MediaPlayer
import android.media.SoundPool
import android.net.Uri
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.AppState
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
import kotlin.coroutines.resume
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

public actual class SoundEffectPool actual constructor(concurrency: Int) {

    private val loadedMap = HashMap<AudioSource, Deferred<Int>>()
    private val soundPool = SoundPool.Builder().apply {
        setMaxStreams(concurrency)
    }.build()

    // SoundPool.load() returns immediately but decodes the sample on a background thread, so we
    // track pending loads by sample ID and resolve them from the pool-wide completion listener.
    private val loadCompletions = HashMap<Int, CompletableDeferred<Unit>>()

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            loadCompletions.remove(sampleId)?.let {
                if (status == 0) it.complete(Unit)
                else it.completeExceptionally(IOException("SoundPool failed to load sample (status $status)"))
            }
        }
    }

    public actual suspend fun preload(sound: AudioSource) {
        preloadInternal(sound)
    }

    private suspend fun preloadInternal(source: AudioSource): Int {
        return loadedMap.getOrPut(source) {
            AppScope.async {
                when (source) {
                    // SoundPool can only load from a resource or a file path, so remote and raw
                    // audio are first written to a cache file, the same trick used by AudioSource.load().
                    is AudioRemote -> loadBytes(fetch(source.url).blob().toByteArray())
                    is AudioRaw -> loadBytes(source.data.toByteArray())
                    is AudioLocal -> awaitLoad(soundPool.load(source.file.uri.path, 1))
                    is AudioResource -> awaitLoad(soundPool.load(AndroidAppContext.applicationCtx, source.resource, 1))
                }
            }
        }.await()
    }

    private suspend fun loadBytes(bytes: ByteArray): Int {
        val file = File.createTempFile("soundeffect", ".audio", AndroidAppContext.applicationCtx.cacheDir)
        return try {
            file.writeBytes(bytes)
            awaitLoad(soundPool.load(file.path, 1))
        } finally {
            file.delete()
        }
    }

    private suspend fun awaitLoad(soundId: Int): Int {
        // A sample ID of 0 means SoundPool rejected the load synchronously (bad data, pool full);
        // no completion event will ever arrive for it.
        if (soundId == 0) throw IOException("SoundPool failed to load sample")
        val completion = CompletableDeferred<Unit>()
        loadCompletions[soundId] = completion
        completion.await()
        return soundId
    }

    public actual suspend fun play(sound: AudioSource): PlayingSoundEffect {
        val streamId = soundPool.play(preloadInternal(sound), 1.0f, 1.0f, 0, 0, 1.0f)
        return object : PlayingSoundEffect {
            override var volume: Float = 1f
                set(value) {
                    field = value
                    soundPool.setVolume(streamId, value, value)
                }
            override var isPlaying: Boolean = true
                set(value) {
                    if (field == value) return
                    field = value
                    if (value) {
                        soundPool.resume(streamId)
                    } else {
                        soundPool.pause(streamId)
                    }
                }

            override fun stop() {
                soundPool.stop(streamId)
            }
        }
    }

    public actual fun unload(sound: AudioSource) {
        AppScope.launch {
            loadedMap[sound]?.await()?.let {
                soundPool.unload(it)
            }
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
            val file = File.createTempFile("soundeffect", ".audio", AndroidAppContext.applicationCtx.cacheDir)
            file.writeBytes(data.toByteArray())
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
