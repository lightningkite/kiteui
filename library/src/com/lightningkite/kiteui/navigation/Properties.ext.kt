@file:OptIn(ExperimentalSerializationApi::class)
@file:Suppress("DEPRECATION")

package com.lightningkite.kiteui.navigation

import com.lightningkite.kiteui.LogRoot
import com.lightningkite.kiteui.decodeURIComponent
import com.lightningkite.kiteui.encodeURIComponent
import com.lightningkite.reactive.core.MutableValue
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.serializer

@Serializable
private data class Wrapper<T>(val value: T)

@Deprecated("Properties cannot serialize value classes.")
public fun <T> Properties.encodeToStringMap(
    serializer: KSerializer<T>,
    value: T,
    key: String,
    out: MutableMap<String, String>
) {
    if (value == null) return
    out += encodeToStringMap(Wrapper.serializer(serializer), Wrapper(value)).mapKeys {
        it.key.replaceFirst(
            "value",
            key
        )
    }
}

@Deprecated("Properties cannot serialize value classes.")
public fun <T> Properties.decodeFromStringMap(serializer: KSerializer<T>, key: String, source: Map<String, String>): T? {
    try {
        val filtered = source.filterKeys { it.startsWith(key) }.mapKeys { it.key.replaceFirst(key, "value") }
        if (filtered.isEmpty()) return null
        return decodeFromStringMap(Wrapper.serializer(serializer), filtered).value
    } catch (e: Exception) {
        LogRoot.warn("Could not parse query parameter '$key': ${e.message}")
        return null
    }
}

@Deprecated("Properties cannot serialize value classes.")
public inline fun <reified T> Properties.decodeFromStringMap(
    key: String,
    source: Map<String, String>,
    into: MutableValue<T>
) {
    decodeFromStringMap(serializersModule.serializer<T>(), key, source)?.let { into.valueSet(it) }
}

@Deprecated("Properties cannot serialize value classes.")
public inline fun <reified T> Properties.encodeToStringMap(value: T, key: String, out: MutableMap<String, String>): Unit =
    encodeToStringMap(UrlProperties.serializersModule.serializer<T>(), value, key, out)

@Deprecated("Properties cannot serialize value classes.")
public inline fun <reified T> Properties.decodeFromStringMap(key: String, source: Map<String, String>): T? =
    decodeFromStringMap(UrlProperties.serializersModule.serializer<T>(), key, source)


@Deprecated("Properties cannot serialize value classes.")
public fun <T> Properties.encodeToString(serializer: KSerializer<T>, value: T): String {
    return if (serializer.descriptor.kind is StructureKind) {
        encodeToStringMap(serializer, value).entries.joinToString("&") { "${it.key}=${encodeURIComponent(it.value)}" }
    } else {
        encodeURIComponent(encodeToStringMap(Wrapper.serializer(serializer), Wrapper(value))["value"] ?: "NULL")
    }
}

@Deprecated("Properties cannot serialize value classes.")
public fun <T> Properties.decodeFromString(serializer: KSerializer<T>, value: String): T {
    if (serializer.descriptor.kind is StructureKind) {
        return decodeFromStringMap(serializer, value.split('&').associate {
            val index = it.indexOf('=')
            it.substring(0, index) to decodeURIComponent(it.substring(index + 1))
        })
    } else {
        @Suppress("UNCHECKED_CAST")
        if (value == "NULL" && serializer.descriptor.isNullable) return null as T
        return decodeFromStringMap(Wrapper.serializer(serializer), mapOf("value" to decodeURIComponent(value))).value
    }
}

@Deprecated("Properties cannot serialize value classes.")
public inline fun <reified T> Properties.encodeToString(value: T): String =
    encodeToString(serializersModule.serializer(), value)

@Deprecated("Properties cannot serialize value classes.")
public inline fun <reified T> Properties.decodeFromString(value: String): T =
    decodeFromString(serializersModule.serializer(), value)