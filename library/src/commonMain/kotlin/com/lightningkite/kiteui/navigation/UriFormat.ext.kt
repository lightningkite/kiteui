package com.lightningkite.kiteui.navigation

import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.serializer

inline fun <reified T> UriFormat.encodeToString(value: T): String =
    encodeToString(serializersModule.serializer(), value)

inline fun <reified T> UriFormat.decodeFromString(string: String): T =
    decodeFromString(serializersModule.serializer(), string)

fun <T> UriFormat.encodeToStringMap(serializer: SerializationStrategy<T>, value: T): Map<String, String> {
    val out = mutableMapOf<String, String>()
    encodeToStringMap(serializer, value, out)
    return out
}

inline fun <reified T> UriFormat.encodeToStringMap(value: T): Map<String, String> =
    encodeToStringMap(serializersModule.serializer(), value)

inline fun <reified T> UriFormat.decodeFromStringMap(map: Map<String, String>): T =
    decodeFromStringMap(serializersModule.serializer(), map)