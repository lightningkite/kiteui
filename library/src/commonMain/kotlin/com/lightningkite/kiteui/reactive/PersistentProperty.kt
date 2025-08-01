package com.lightningkite.kiteui.reactive

import com.lightningkite.kiteui.PlatformStorage
import com.lightningkite.kiteui.navigation.DefaultJson
import com.lightningkite.signal.BaseImmediateReadable
import com.lightningkite.signal.ImmediateWritable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

public class PersistentProperty<T>(
    private val key: String,
    defaultValue: T,
    private val serializer: KSerializer<T>,
) : ImmediateWritable<T>, BaseImmediateReadable<T>(defaultValue) {
    public override var value: T
        get() = super.value
        set(value) {
            PlatformStorage.set(key, DefaultJson.encodeToString(serializer, value))
//            println("Old: ${super.value} vs new: $value")
            super.value = value
        }

    public override suspend infix fun set(value: T) {
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

public inline fun <reified T> PersistentProperty(
    key: String,
    defaultValue: T
): PersistentProperty<T> = PersistentProperty(key, defaultValue, serializer())