package com.lightningkite.kiteui.reactive

import com.lightningkite.kiteui.Log
import com.lightningkite.kiteui.PlatformStorage
import com.lightningkite.kiteui.navigation.DefaultJson
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

public class PersistentProperty<T>(
    private val key: String,
    defaultValue: T,
    private val serializer: KSerializer<T>,
) : MutableReactiveValue<T>, BaseReactiveValue<T>(defaultValue) {
    override var value: T
        get() = super.value
        set(value) {
            PlatformStorage.set(key, DefaultJson.encodeToString(serializer, value))
            super.value = value
        }

    override suspend infix fun set(value: T) {
        this.value = value
    }

    init {
        val stored = PlatformStorage.get(key)
        if (stored != null)
            try {
                super.value = DefaultJson.decodeFromString(serializer, stored)
            } catch (e: Exception) {
                Log.error("Failed to deserialize PersistentProperty '$key': ${e.message}", e)
            }
    }
}

public inline fun <reified T> PersistentProperty(
    key: String,
    defaultValue: T
): PersistentProperty<T> = PersistentProperty(key, defaultValue, serializer())