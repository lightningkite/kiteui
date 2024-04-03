package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.AudioSource

expect class UIAudioPool() {
    suspend fun play(sound: UIAudioSegment, gain: Float = 1F)
    suspend fun load(source: AudioSource): UIAudioSegment
    suspend fun unload(sound: UIAudioSegment)
}

expect class UIAudioSegment