package com.lightningkite.kiteui.reactive

import com.lightningkite.kiteui.PlatformStorage
import com.lightningkite.kiteui.navigation.DefaultJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

class PersistentProperty<T>(
    private val key: String,
    defaultValue: T,
    private val serializer: KSerializer<T>,
) : ImmediateWritable<T>, BaseImmediateReadable<T>(defaultValue) {
    override var value: T
        get() = super.value
        set(value) {
            PlatformStorage.set(key, DefaultJson.encodeToString(serializer, value))
            println("Old: ${super.value} vs new: $value")
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
            }
    }
}

inline fun <reified T> PersistentProperty(
    key: String,
    defaultValue: T
): PersistentProperty<T> = PersistentProperty(key, defaultValue, serializer())