package com.lightningkite.kiteui

import android.media.MediaPlayer
import android.media.SoundPool
import android.net.Uri
import com.lightningkite.kiteui.models.*
import com.lightningkite.signal.invokeAllSafe
import com.lightningkite.kiteui.views.AndroidAppContext
import com.lightningkite.signal.AppScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.Closeable
import kotlin.coroutines.resume

public actual class SoundEffectPool public actual constructor(concurrency: Int) {

    private val loadedMap = HashMap<AudioSource, Deferred<Int>>()
    private val soundPool = SoundPool.Builder().apply {
        setMaxStreams(concurrency)
    }.build()

    public actual suspend fun preload(sound: AudioSource) {
        preloadInternal(sound)
    }

    private suspend fun preloadInternal(source: AudioSource): Int {
        return loadedMap.getOrPut(source) {
            AppScope.async {
                when (source) {
                    is AudioRemote -> TODO()
                    is AudioRaw -> TODO()
                    is AudioLocal -> {
                        soundPool.load(source.file.uri.path, 1)
                    }

                    is AudioResource -> {
                        soundPool.load(AndroidAppContext.applicationCtx, source.resource, 1)
                    }

                    else -> TODO()  // Not sure why this else branch is necessary as AudioSource is a sealed class
                }
            }
        }.await()
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
                    if(field == value) return
                    field = value
                    if(value) {
                        soundPool.pause(streamId)
                    } else {
                        soundPool.resume(streamId)
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
    when(this) {
        is AudioLocal -> player.setDataSource(AndroidAppContext.applicationCtx, file.uri)
        is AudioRaw -> TODO()
        is AudioRemote -> player.setDataSource(AndroidAppContext.applicationCtx, Uri.parse(url))
        is AudioResource -> {
            val afd = AndroidAppContext.applicationCtx.resources.openRawResourceFd(this.resource) ?: throw IllegalStateException("No such resource found")
            player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            toClose = afd
        }
        else -> TODO()
    }
    val onCompletes = ArrayList<()->Unit>()
    player.setOnCompletionListener {
        runningMediaPlayers.remove(player)
        onCompletes.invokeAllSafe()
    }
    val audio = object: PlayableAudio {
        override var volume: Float = 1f
            set(value) { field = value; player.setVolume(value, value) }
        override var loop: Boolean = false
            set(value) { player.isLooping = value }
        override var isPlaying: Boolean
            get() = player.isPlaying
            set(value) {
                if (value == player.isPlaying) return
                if(value) {
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
