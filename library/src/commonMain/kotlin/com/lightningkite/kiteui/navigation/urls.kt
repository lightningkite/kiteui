@file:OptIn(ExperimentalSerializationApi::class)

package com.lightningkite.kiteui.navigation

import com.lightningkite.kiteui.decodeURIComponent
import com.lightningkite.kiteui.encodeURIComponent
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.serializer

var DefaultSerializersModule: SerializersModule = EmptySerializersModule()
    set(value) {
        field = value
        DefaultJsonCurrent = Json {
            serializersModule = DefaultSerializersModule
            ignoreUnknownKeys = true
        }
        UrlPropertiesCurrent = Properties(DefaultSerializersModule)
    }
private var DefaultJsonCurrent: Json = Json {
    serializersModule = DefaultSerializersModule
    ignoreUnknownKeys = true
}
val DefaultJson: Json get() = DefaultJsonCurrent
private var UrlPropertiesCurrent: Properties = Properties(DefaultSerializersModule)
val UrlProperties: Properties get() = UrlPropertiesCurrent

@Serializable
private data class Wrapper<T>(val value: T)
fun <T> Properties.encodeToStringMap(serializer: KSerializer<T>, value: T, key: String, out: MutableMap<String, String>) {
    if(value == null) return
    out += encodeToStringMap(Wrapper.serializer(serializer), Wrapper(value)).mapKeys { it.key.replaceFirst("value", key) }
}
fun <T> Properties.decodeFromStringMap(serializer: KSerializer<T>, key: String, source: Map<String, String>): T? {
    val filtered = source.filterKeys { it.startsWith(key) }.mapKeys { it.key.replaceFirst(key, "value") }
    if(filtered.isEmpty()) return null
    return decodeFromStringMap(Wrapper.serializer(serializer), filtered).value
}
inline fun <reified T> Properties.encodeToStringMap(value: T, key: String, out: MutableMap<String, String>) = encodeToStringMap(UrlProperties.serializersModule.serializer<T>(), value, key, out)
inline fun <reified T> Properties.decodeFromStringMap(key: String, source: Map<String, String>): T? = decodeFromStringMap(UrlProperties.serializersModule.serializer<T>(), key, source)


fun <T> Properties.encodeToString(serializer: KSerializer<T>, value: T): String {
    return if(serializer.descriptor.kind is StructureKind) {
        encodeToStringMap(serializer, value).entries.joinToString("&") { "${it.key}=${encodeURIComponent(it.value)}" }
    } else {
        encodeURIComponent(encodeToStringMap(Wrapper.serializer(serializer), Wrapper(value))["value"] ?: "NULL")
    }
}
fun <T> Properties.decodeFromString(serializer: KSerializer<T>, value: String): T {
    if(serializer.descriptor.kind is StructureKind) {
        return decodeFromStringMap(serializer, value.split('&').associate {
            val index = it.indexOf('=')
            it.substring(0, index) to decodeURIComponent(it.substring(index + 1))
        })
    } else {
        @Suppress("UNCHECKED_CAST")
        if(value == "NULL" && serializer.descriptor.isNullable) return null as T
        return decodeFromStringMap(Wrapper.serializer(serializer), mapOf("value" to decodeURIComponent(value))).value
    }
}

inline fun <reified T> Properties.encodeToString(value: T): String = encodeToString(serializersModule.serializer(), value)
inline fun <reified T> Properties.decodeFromString(value: String): T = decodeFromString(serializersModule.serializer(), value)