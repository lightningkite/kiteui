package com.lightningkite.kiteui.navigation

import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.serializer

/**
 * Encodes the given [value] to a URI query string using the serializer resolved from the type parameter.
 *
 * This is a convenience overload that uses reified type parameters to automatically
 * resolve the serializer from the [serializersModule].
 *
 * @param T The type of value to encode. Must be serializable.
 * @param value The value to encode.
 * @return A URI query string representation of the value.
 */
inline fun <reified T> UriFormat.encodeToString(value: T): String =
    encodeToString(serializersModule.serializer(), value)

/**
 * Decodes a value of type [T] from a URI query [string] using the serializer resolved from the type parameter.
 *
 * This is a convenience overload that uses reified type parameters to automatically
 * resolve the deserializer from the [serializersModule].
 *
 * @param T The type of value to decode. Must be serializable.
 * @param string The URI query string to decode.
 * @return The decoded value.
 */
inline fun <reified T> UriFormat.decodeFromString(string: String): T =
    decodeFromString(serializersModule.serializer(), string)

/**
 * Encodes a structure value into a new string map and returns it.
 *
 * This is a convenience overload that creates the destination map internally.
 *
 * @param serializer The serialization strategy for type [T]. Must be a structure type.
 * @param value The value to encode.
 * @return A new map containing the encoded key-value pairs.
 */
fun <T> UriFormat.encodeToStringMap(serializer: SerializationStrategy<T>, value: T): Map<String, String> {
    val out = mutableMapOf<String, String>()
    encodeToStringMap(serializer, value, out)
    return out
}

/**
 * Encodes a structure value into a new string map using the serializer resolved from the type parameter.
 *
 * This is a convenience overload that uses reified type parameters and creates
 * the destination map internally.
 *
 * @param T The type of value to encode. Must be a serializable structure type.
 * @param value The value to encode.
 * @return A new map containing the encoded key-value pairs.
 */
inline fun <reified T> UriFormat.encodeToStringMap(value: T): Map<String, String> =
    encodeToStringMap(serializersModule.serializer(), value)

/**
 * Decodes a structure value from a string map using the serializer resolved from the type parameter.
 *
 * This is a convenience overload that uses reified type parameters to automatically
 * resolve the deserializer from the [serializersModule].
 *
 * @param T The type of value to decode. Must be a serializable structure type.
 * @param map The source map containing the encoded key-value pairs.
 * @return The decoded value.
 */
inline fun <reified T> UriFormat.decodeFromStringMap(map: Map<String, String>): T =
    decodeFromStringMap(serializersModule.serializer(), map)

/**
 * Encodes a value into a mutable string map under a specified key prefix,
 * using the serializer resolved from the type parameter.
 *
 * This is a convenience overload that uses reified type parameters.
 *
 * @param T The type of value to encode. Must be serializable.
 * @param key The key or prefix under which to store the encoded value.
 * @param value The value to encode.
 * @param dest The destination map to receive the encoded key-value pairs.
 */
inline fun <reified T> UriFormat.encodeToStringMap(key: String, value: T, dest: MutableMap<String, String>) =
    encodeToStringMap(serializersModule.serializer(), key, value, dest)

/**
 * Decodes a value from a string map using a specified key prefix,
 * using the serializer resolved from the type parameter.
 *
 * This is a convenience overload that uses reified type parameters.
 *
 * @param T The type of value to decode. Must be serializable.
 * @param key The key or prefix from which to read the encoded value.
 * @param source The source map containing the encoded key-value pairs.
 * @return The decoded value.
 */
inline fun <reified T> UriFormat.decodeFromStringMap(key: String, source: Map<String, String>): T =
    decodeFromStringMap(serializersModule.serializer(), key, source)