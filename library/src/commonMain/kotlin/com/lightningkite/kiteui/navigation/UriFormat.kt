package com.lightningkite.kiteui.navigation

import com.lightningkite.kiteui.decodeURIComponent
import com.lightningkite.kiteui.encodeURIComponent
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.StringFormat
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.getContextualDescriptor
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.findPolymorphicSerializer
import kotlinx.serialization.internal.AbstractPolymorphicSerializer
import kotlinx.serialization.internal.NamedValueDecoder
import kotlinx.serialization.internal.NamedValueEncoder
import kotlinx.serialization.modules.SerializersModule
import kotlin.collections.set

/**
 * A kotlinx.serialization [StringFormat] that encodes Kotlin objects to and from URI query string format.
 *
 * URI query format uses `key=value` pairs separated by `&`, with nested structures represented using
 * dot notation (e.g., `parent.child=value`). Values are URL-encoded to handle special characters.
 *
 * ## Supported Types
 * - All primitive types (Boolean, Byte, Short, Int, Long, Float, Double, Char, String)
 * - Enums (serialized by name)
 * - Nullable types (null represented as "NULL")
 * - Data classes and structures (fields become key-value pairs)
 * - Lists (indexed as `list.0=first&list.1=second`)
 * - Maps with primitive/enum keys (keys become field names)
 * - Value classes (automatically unwrapped)
 * - Polymorphic types (discriminator stored as `type` field)
 * - Contextual serializers (via [serializersModule])
 *
 * ## Examples
 * ```kotlin
 * @Serializable
 * data class User(val name: String, val age: Int)
 *
 * val format = UriFormat(EmptySerializersModule())
 * val encoded = format.encodeToString(User.serializer(), User("Alice", 30))
 * // Result: "name=Alice&age=30"
 *
 * val decoded = format.decodeFromString(User.serializer(), "name=Bob&age=25")
 * // Result: User(name="Bob", age=25)
 * ```
 *
 * ## Nested Structures
 * ```kotlin
 * @Serializable
 * data class Address(val city: String)
 * @Serializable
 * data class Person(val name: String, val address: Address)
 *
 * // Encodes as: "name=Alice&address.city=Seattle"
 * ```
 *
 * ## Empty Collections
 * Empty lists and maps are marked with `~` to distinguish them from missing values:
 * - Empty list: `listField=~`
 * - Empty map: `mapField=~`
 *
 * @param serializersModule The module providing contextual and polymorphic serializers.
 */
class UriFormat(
    override val serializersModule: SerializersModule
) : StringFormat {

    /**
     * Encodes the given [value] to a URI query string using the provided [serializer].
     *
     * @param serializer The serialization strategy for type [T].
     * @param value The value to encode.
     * @return A URI query string representation of the value.
     */
    override fun <T> encodeToString(serializer: SerializationStrategy<T>, value: T): String {
        val e = UriEncoder()
        e.encodeSerializableValue(serializer, value)
        return e.getUri()
    }

    /**
     * Decodes a value of type [T] from a URI query [string] using the provided [deserializer].
     *
     * @param deserializer The deserialization strategy for type [T].
     * @param string The URI query string to decode.
     * @return The decoded value.
     * @throws SerializationException If the string cannot be decoded to the expected type.
     */
    override fun <T> decodeFromString(deserializer: DeserializationStrategy<T>, string: String): T {
        val d = UriDecoder(string)
        return d.decodeSerializableValue(deserializer)
    }

    /**
     * Encodes a pre-built map of key-value pairs into a URI query string.
     *
     * This is a utility function for converting an already-flattened map into query string format.
     * Values should already be URI-encoded if necessary.
     *
     * @param map The map of key-value pairs to encode.
     * @return A URI query string in the format `key1=value1&key2=value2`.
     */
    fun encodeToString(map: Map<String, String>): String =
        map.entries.joinToString("&") { "${it.key}=${it.value}" }

    /**
     * Encodes a structure value into a mutable string map.
     *
     * The structure's fields are flattened into key-value pairs and added to [dest].
     * This is useful for building up a query string from multiple values.
     *
     * @param serializer The serialization strategy for type [T]. Must be a structure type.
     * @param value The value to encode.
     * @param dest The destination map to receive the encoded key-value pairs.
     * @throws IllegalArgumentException If the serializer describes a non-structure type.
     */
    fun <T> encodeToStringMap(serializer: SerializationStrategy<T>, value: T, dest: MutableMap<String, String>) {
        require(serializer.descriptor.unwrap().kind.isStructure()) {
            "Only structures can be encoded into a string map."
        }
        StringMapEncoder(prefix = "", dest).encodeSerializableValue(serializer, value)
    }

    /**
     * Encodes a value into a mutable string map under a specified key prefix.
     *
     * For primitive/enum types, the value is stored directly at [key].
     * For structure types, fields are stored with [key] as a prefix (e.g., `key.field=value`).
     *
     * This enables heterogeneous encoding where multiple different types can be stored
     * in the same map under different keys.
     *
     * @param serializer The serialization strategy for type [T].
     * @param key The key or prefix under which to store the encoded value.
     * @param value The value to encode.
     * @param dest The destination map to receive the encoded key-value pairs.
     */
    fun <T> encodeToStringMap(serializer: SerializationStrategy<T>, key: String, value: T, dest: MutableMap<String, String>) {
        if (serializer.descriptor.unwrap().kind.isNonStructure()) {
            dest[key] = encodeToString(serializer, value)
        }
        else StringMapEncoder(prefix = key, dest).encodeSerializableValue(serializer, value)
    }

    /**
     * Decodes a structure value from a string map.
     *
     * The map's key-value pairs are interpreted as flattened structure fields.
     *
     * @param deserializer The deserialization strategy for type [T]. Must be a structure type.
     * @param source The source map containing the encoded key-value pairs.
     * @return The decoded value.
     * @throws IllegalArgumentException If the deserializer describes a non-structure type.
     * @throws SerializationException If required fields are missing or values are malformed.
     */
    fun <T> decodeFromStringMap(deserializer: DeserializationStrategy<T>, source: Map<String, String>): T {
        require(deserializer.descriptor.unwrap().kind.isStructure()) {
            "Only structures can be decoded from a string map."
        }
        return StringMapDecoder(source, prefix = "").decodeSerializableValue(deserializer)
    }

    /**
     * Decodes a value from a string map using a specified key prefix.
     *
     * For primitive/enum types, the value is read directly from [key].
     * For structure types, fields are read with [key] as a prefix (e.g., `key.field`).
     *
     * This enables heterogeneous decoding where multiple different types were stored
     * in the same map under different keys.
     *
     * @param deserializer The deserialization strategy for type [T].
     * @param key The key or prefix from which to read the encoded value.
     * @param source The source map containing the encoded key-value pairs.
     * @return The decoded value.
     * @throws SerializationException If the key is missing or values are malformed.
     */
    fun <T> decodeFromStringMap(deserializer: DeserializationStrategy<T>, key: String, source: Map<String, String>): T =
        if (deserializer.descriptor.unwrap().kind.isNonStructure()) {
            decodeFromString(deserializer, source[key] ?: throw SerializationException("Missing key $key"))
        }
        else StringMapDecoder(source, prefix = key).decodeSerializableValue(deserializer)

    /**
     * Checks if a map contains any keys that start with the given prefix.
     *
     * Useful for determining if a nested structure or collection exists in the map.
     *
     * @param map The map to search.
     * @param key The key prefix to look for.
     * @return `true` if any key in the map starts with [key], `false` otherwise.
     */
    fun mapContainsKey(map: Map<String, String>, key: String) = map.keys.any { it.startsWith(key) }

    // -- Implementation Helpers --

    @OptIn(ExperimentalSerializationApi::class)
    private tailrec fun SerialDescriptor.unwrap(): SerialDescriptor = when {
        kind == SerialKind.CONTEXTUAL -> serializersModule.getContextualDescriptor(this)?.unwrap() ?: this
        isInline -> getElementDescriptor(0).unwrap()
        else     -> this
    }

    private fun SerialKind.isNonStructure() = this is PrimitiveKind || this == SerialKind.ENUM

    @OptIn(ExperimentalSerializationApi::class)
    private fun SerialKind.isStructure() = this is StructureKind || this is PolymorphicKind

    private interface StructureEncoder : Encoder, CompositeEncoder
    private interface StructureDecoder : Decoder, CompositeDecoder

    private fun ensureMapHasEncodableKeys(mapDescriptor: SerialDescriptor) {
        require(mapDescriptor.getElementDescriptor(0).kind.isNonStructure()) {
            "UriFormat only supports maps with primitive and enum-type keys."
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun getProperStructureEncoder(
        descriptor: SerialDescriptor,
        into: MutableMap<String, String>,
        prefix: String = "",
    ): StructureEncoder {
        val descriptor = descriptor.unwrap()
        return when (descriptor.kind) {
            StructureKind.CLASS, is PolymorphicKind -> ClassEncoder(prefix, into)
            StructureKind.LIST -> ListEncoder(prefix, into)
            StructureKind.MAP -> {
                ensureMapHasEncodableKeys(descriptor)
                MapEncoder(prefix, into)
            }
            StructureKind.OBJECT -> ObjectEncoder(prefix, into)
            else -> throw SerializationException("Only structure types can be encoded as a string map. Got ${descriptor.kind} (${descriptor.serialName}).")
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun getProperStructureDecoder(
        descriptor: SerialDescriptor,
        prefix: String,
        source: Map<String, String>,
    ): StructureDecoder {
        val descriptor = descriptor.unwrap()
        return when (descriptor.kind) {
            StructureKind.CLASS, is PolymorphicKind -> ClassDecoder(prefix, source)
            StructureKind.LIST -> ListDecoder(prefix, source)
            StructureKind.MAP -> {
                ensureMapHasEncodableKeys(descriptor)
                MapDecoder(prefix, source)
            }
            StructureKind.OBJECT -> ObjectDecoder(prefix, source)
            else -> throw SerializationException("Only structure types can be encoded as a string map. Got ${descriptor.kind} (${descriptor.serialName}).")
        }
    }

    // -- ENCODING --

    /**
     * Top-level encoder for serializing a single value to URI query string format.
     *
     * Handles the distinction between primitive values (encoded directly) and
     * structure types (delegated to appropriate structure encoders). Ensures only
     * one value is encoded per instance.
     */
    private inner class UriEncoder : Encoder {
        override val serializersModule: SerializersModule = this@UriFormat.serializersModule

        fun getUriOrNull(): String? =
            singleElementUri ?: encodedStructure?.let(::encodeToString)

        fun getUri(): String = getUriOrNull() ?: "NULL"

        private var singleElementUri: String? = null
            set(value) {
                if (encodedStructure != null || field != null) throw IllegalStateException("UriFormat.UriEncoder can only encode a single primitive or structure")
                field = value
            }

        private var encodedStructure: MutableMap<String, String>? = null
            set(value) {
                if (singleElementUri != null || field != null) throw IllegalStateException("UriFormat.UriEncoder can only encode a single primitive or structure")
                field = value
            }

        @ExperimentalSerializationApi
        override fun encodeNull() { /*Ignored*/ }

        private fun encodePrimitive(value: Any) { singleElementUri = encodeURIComponent(value.toString()) }

        override fun encodeBoolean(value: Boolean) = encodePrimitive(value)
        override fun encodeByte(value: Byte) = encodePrimitive(value)
        override fun encodeShort(value: Short) = encodePrimitive(value)
        override fun encodeChar(value: Char) = encodePrimitive(value)
        override fun encodeInt(value: Int) = encodePrimitive(value)
        override fun encodeLong(value: Long) = encodePrimitive(value)
        override fun encodeFloat(value: Float) = encodePrimitive(value)
        override fun encodeDouble(value: Double) = encodePrimitive(value)
        override fun encodeString(value: String) = encodePrimitive(value)

        override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
            singleElementUri = encodeURIComponent(enumDescriptor.getElementName(index))
        }

        override fun encodeInline(descriptor: SerialDescriptor): Encoder = this

        override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
            val map = mutableMapOf<String, String>()
            encodedStructure = map
            return getProperStructureEncoder(descriptor, map, "")
        }
    }

    /**
     * Encoder for serializing directly into a string map with an optional key prefix.
     *
     * Used by [encodeToStringMap] to encode values into an existing mutable map.
     * Delegates structure encoding to the appropriate specialized encoder.
     */
    @OptIn(ExperimentalSerializationApi::class)
    private inner class StringMapEncoder(val prefix: String, val dest: MutableMap<String, String>) : AbstractEncoder() {
        override val serializersModule: SerializersModule = this@UriFormat.serializersModule

        override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder =
            getProperStructureEncoder(descriptor, dest, prefix)
    }


    /**
     * Encoder for class/object structures using named fields.
     *
     * Encodes each field as a key-value pair using the field name as the key.
     * Nested structures use dot notation (e.g., `parent.child=value`).
     * Handles polymorphic types by encoding a `type` discriminator field.
     *
     * Based on kotlinx.serialization's Properties.OutMapper encoder.
     */
    @OptIn(InternalSerializationApi::class)
    private inner class ClassEncoder(
        val map: MutableMap<String, String> = mutableMapOf(),
    ) : NamedValueEncoder(), StructureEncoder {
        constructor(prefix: String, map: MutableMap<String, String>) : this(map) { pushTag(prefix) }

        override val serializersModule: SerializersModule = this@UriFormat.serializersModule

        @Suppress("UNCHECKED_CAST")
        override fun <T> encodeSerializableValue(serializer: SerializationStrategy<T>, value: T) {
            if (serializer is AbstractPolymorphicSerializer<*>) {
                val casted = serializer as AbstractPolymorphicSerializer<Any>
                val actualSerializer = casted.findPolymorphicSerializer(this, value as Any)
                encodeTaggedString(nested("type"), actualSerializer.descriptor.serialName)

                return actualSerializer.serialize(this, value)
            }

            return serializer.serialize(this, value)
        }

        override fun encodeTaggedValue(tag: String, value: Any) {
            map[tag] = encodeURIComponent(value.toString())
        }

        override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder =
            getProperStructureEncoder(descriptor, map, popTag())

        override fun encodeTaggedNull(tag: String) {
            // ignore nulls in output
        }

        override fun encodeTaggedEnum(tag: String, enumDescriptor: SerialDescriptor, ordinal: Int) {
            map[tag] = encodeURIComponent(enumDescriptor.getElementName(ordinal))
        }
    }

    /**
     * Encoder for list/array structures using indexed keys.
     *
     * Elements are encoded with their index as the key (e.g., `list.0=first&list.1=second`).
     * Empty lists are marked with `~` at the prefix key to distinguish from missing values.
     * Nested structures within the list use the index as part of their key prefix.
     */
    @OptIn(ExperimentalSerializationApi::class)
    private inner class ListEncoder(
        val prefix: String,
        val map: MutableMap<String, String> = mutableMapOf()
    ) : AbstractEncoder(), StructureEncoder {
        override val serializersModule: SerializersModule = this@UriFormat.serializersModule

        private var currentIndex = 0

        private fun fullTag(index: Int): String = if (prefix.isEmpty()) "$index" else "$prefix.$index"

        override fun encodeNull() {
            map[fullTag(currentIndex++)] = "NULL"
        }

        override fun encodeValue(value: Any) {
            map[fullTag(currentIndex++)] = encodeURIComponent(value.toString())
        }

        override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder =
            getProperStructureEncoder(descriptor, map, fullTag(currentIndex++))

        override fun endStructure(descriptor: SerialDescriptor) {
            // Only output marker for empty lists
            if (currentIndex == 0) {
                map[prefix] = "~"
            }
        }
    }

    /**
     * Encoder for map structures using primitive/enum keys directly as field names.
     *
     * Map entries are encoded with the key's string representation as the field name
     * (e.g., `Map("a" to 1, "b" to 2)` becomes `a=1&b=2`).
     * Empty maps are marked with `~` at the prefix key.
     * Only primitive and enum key types are supported; structure keys will throw.
     */
    @OptIn(ExperimentalSerializationApi::class)
    private inner class MapEncoder(
        val prefix: String,
        val map: MutableMap<String, String> = mutableMapOf()
    ) : AbstractEncoder(), StructureEncoder {
        override val serializersModule: SerializersModule = this@UriFormat.serializersModule

        private var currentKey: String? = null
        private var entryCount = 0

        private fun fullTag(key: String): String = if (prefix.isEmpty()) key else "$prefix.$key"

        override fun encodeNull() {
            val key = currentKey
            if (key != null) {
                map[fullTag(key)] = "NULL"
                currentKey = null
            }
        }

        override fun encodeValue(value: Any) {
            val key = currentKey
            if (key == null) {
                // This is a key - store it
                currentKey = value.toString()
            } else {
                // This is a value - write it with the stored key
                map[fullTag(key)] = encodeURIComponent(value.toString())
                currentKey = null
                entryCount++
            }
        }

        override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) =
            encodeValue(enumDescriptor.getElementName(index))

        override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
            val key = currentKey ?: throw SerializationException("Map value must have a key")
            currentKey = null
            entryCount++
            return getProperStructureEncoder(descriptor, map, fullTag(key))
        }

        override fun endStructure(descriptor: SerialDescriptor) {
            // Only output marker for empty maps
            if (entryCount == 0) {
                map[prefix] = "~"
            }
        }
    }

    /**
     * Encoder for Kotlin object singletons.
     *
     * Objects have no properties to encode. A marker (`~`) is written at the prefix
     * to indicate the object's presence, which is important when objects are nested
     * inside other structures.
     */
    @OptIn(ExperimentalSerializationApi::class)
    private inner class ObjectEncoder(
        val prefix: String,
        val map: MutableMap<String, String>
    ) : AbstractEncoder(), StructureEncoder {
        override val serializersModule: SerializersModule = this@UriFormat.serializersModule

        override fun encodeValue(value: Any) {
            // Objects have no properties, this should not be called
        }

        override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder = this

        override fun endStructure(descriptor: SerialDescriptor) {
            // Write a marker to indicate the object exists (important for nested objects)
            if (prefix.isNotEmpty()) {
                map[prefix] = "~"
            }
        }
    }



    // -- DECODING --

    /**
     * Top-level decoder for deserializing a single value from URI query string format.
     *
     * Handles primitive values directly and delegates structure types to specialized
     * decoders. Parses the URI string into a key-value map for structure decoding.
     */
    private inner class UriDecoder(private val uri: String) : Decoder {
        override val serializersModule: SerializersModule = this@UriFormat.serializersModule

        private val decoded get() = decodeURIComponent(uri)

        @ExperimentalSerializationApi
        override fun decodeNull(): Nothing? = null

        @ExperimentalSerializationApi
        override fun decodeNotNullMark(): Boolean = uri != "NULL"

        override fun decodeBoolean(): Boolean = decoded.toBooleanStrict()
        override fun decodeByte(): Byte = decoded.toByte()
        override fun decodeShort(): Short = decoded.toShort()
        override fun decodeChar(): Char = decoded.single()
        override fun decodeInt(): Int = decoded.toInt()
        override fun decodeLong(): Long = decoded.toLong()
        override fun decodeFloat(): Float = decoded.toFloat()
        override fun decodeDouble(): Double = decoded.toDouble()
        override fun decodeString(): String = decoded

        override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
            return enumDescriptor.getElementIndex(decoded)
        }

        override fun decodeInline(descriptor: SerialDescriptor): Decoder = this

        override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder =
            getProperStructureDecoder(
                descriptor,
                prefix = "",
                uri.split('&')
                    .filter { it.isNotEmpty() }
                    .associate {
                        val index = it.indexOf('=')
                        if (index == -1) it to ""
                        else it.substring(0, index) to decodeURIComponent(it.substring(index + 1))
                    }
            )
    }

    /**
     * Decoder for deserializing directly from a string map with an optional key prefix.
     *
     * Used by [decodeFromStringMap] to decode values from an existing map.
     * Delegates structure decoding to the appropriate specialized decoder.
     */
    @OptIn(ExperimentalSerializationApi::class)
    private inner class StringMapDecoder(val source: Map<String, String>, val prefix: String) : AbstractDecoder() {
        override val serializersModule: SerializersModule = this@UriFormat.serializersModule

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int = throw UnsupportedOperationException()

        override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder =
            getProperStructureDecoder(descriptor, prefix, source)
    }

    /**
     * Decoder for class/object structures using named fields.
     *
     * Decodes fields by looking up their names as keys in the map (with optional prefix).
     * Handles polymorphic types by reading the `type` discriminator field.
     * Supports nullable fields by checking for key presence.
     * Nested structures are decoded by creating a new decoder with an extended prefix.
     */
    @OptIn(InternalSerializationApi::class)
    private inner class ClassDecoder(
        private val prefix: String = "",
        private val map: Map<String, String>,
    ) : NamedValueDecoder(), StructureDecoder {
        override val serializersModule: SerializersModule = this@UriFormat.serializersModule

        private var currentIndex = 0

        private fun fullTag(tag: String): String = if (prefix.isEmpty()) tag else "$prefix.$tag"

        /** Check if there's any key that matches this tag exactly or starts with this tag as a prefix */
        private fun hasKeyOrPrefix(tag: String): Boolean {
            val full = fullTag(tag)
            return map.containsKey(full) ||
                    map.keys.any { it.startsWith("$full.") }
        }

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            while (currentIndex < descriptor.elementsCount) {
                val name = descriptor.getElementName(currentIndex)
                if (hasKeyOrPrefix(name)) return currentIndex++
                currentIndex++
            }
            return CompositeDecoder.DECODE_DONE
        }

        override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
            // When beginStructure is called on ClassDecoder, it's always for a NESTED structure.
            // The ClassDecoder itself was already created at the right scope by UriDecoder.beginStructure.
            // Use currentTag (the field name being decoded) as the new prefix for the nested structure.
            // If currentTag is null (edge case), fall back to current prefix.
            val newPrefix = currentTagOrNull?.let { fullTag(it) } ?: prefix
            return getProperStructureDecoder(descriptor, newPrefix, map)
        }

        @OptIn(ExperimentalSerializationApi::class)
        override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T {
            if (deserializer is AbstractPolymorphicSerializer<*>) {
                // For polymorphic types, the type discriminator is nested under the field name
                // e.g., for field "item", the type is at "item.type"
                val typeKey = currentTagOrNull?.let { fullTag("$it.type") } ?: fullTag("type")
                val typeName = map[typeKey]
                    ?: throw SerializationException("Missing 'type' field for polymorphic deserialization at '$typeKey'")

                @Suppress("UNCHECKED_CAST")
                val actualDeserializer = deserializer.findPolymorphicSerializer(this, typeName)
                @Suppress("UNCHECKED_CAST")
                return actualDeserializer.deserialize(this) as T
            }
            return deserializer.deserialize(this)
        }

        override fun decodeTaggedValue(tag: String): Any =
            map[fullTag(tag)] ?: throw SerializationException("Missing value for field '${fullTag(tag)}'")

        override fun decodeTaggedString(tag: String): String =
            map[fullTag(tag)] ?: throw SerializationException("Missing value for field '${fullTag(tag)}'")

        override fun decodeTaggedBoolean(tag: String): Boolean = decodeTaggedString(tag).toBooleanStrict()
        override fun decodeTaggedByte(tag: String): Byte = decodeTaggedString(tag).toByte()
        override fun decodeTaggedShort(tag: String): Short = decodeTaggedString(tag).toShort()
        override fun decodeTaggedChar(tag: String): Char = decodeTaggedString(tag).single()
        override fun decodeTaggedInt(tag: String): Int = decodeTaggedString(tag).toInt()
        override fun decodeTaggedLong(tag: String): Long = decodeTaggedString(tag).toLong()
        override fun decodeTaggedFloat(tag: String): Float = decodeTaggedString(tag).toFloat()
        override fun decodeTaggedDouble(tag: String): Double = decodeTaggedString(tag).toDouble()

        override fun decodeTaggedEnum(tag: String, enumDescriptor: SerialDescriptor): Int {
            val name = decodeTaggedString(tag)
            return enumDescriptor.getElementIndex(name)
        }

        @ExperimentalSerializationApi
        override fun decodeTaggedNotNullMark(tag: String): Boolean = hasKeyOrPrefix(tag)
    }

    /**
     * Decoder for list/array structures using indexed keys.
     *
     * Decodes elements by looking up sequential indices (e.g., `prefix.0`, `prefix.1`).
     * The list size is determined by counting consecutive existing indices.
     * Empty lists are recognized by the `~` marker at the prefix key.
     * Supports both primitive elements and nested structures.
     */
    @OptIn(ExperimentalSerializationApi::class)
    private inner class ListDecoder(
        private val prefix: String = "",
        private val map: Map<String, String>,
    ) : SequentialDecoder() {
        override val serializersModule: SerializersModule = this@UriFormat.serializersModule

        private var currentIndex = 0
        private val size: Int by lazy {
            // Check for empty list marker first, otherwise count indexed entries
            if (map[prefix] == "~") 0
            else {
                var count = 0
                while (hasKeyOrPrefix(count)) count++
                count
            }
        }

        private fun fullTag(index: Int): String = if (prefix.isEmpty()) "$index" else "$prefix.$index"

        /** Check if there's any key that matches this index exactly or starts with this index as a prefix */
        private fun hasKeyOrPrefix(index: Int): Boolean {
            val full = fullTag(index)
            return map.containsKey(full) || map.keys.any { it.startsWith("$full.") }
        }

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            return if (currentIndex < size) currentIndex else CompositeDecoder.DECODE_DONE
        }

        override fun decodeCollectionSize(descriptor: SerialDescriptor): Int = size

        override fun decodeNextStringElement(): String {
            val full = fullTag(currentIndex++)
            return map[full] ?: throw SerializationException("Missing value for index '$full'")
        }

        override fun decodeNotNullMark(): Boolean =
            map[fullTag(currentIndex)].let { it != null && it != "NULL" }

        override fun <T> decodeSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            deserializer: DeserializationStrategy<T>,
            previousValue: T?
        ): T {
            val newPrefix = fullTag(currentIndex++)
            // Check if it's a nested structure (has keys with this prefix) or a simple value
            val hasNestedKeys = map.keys.any { it.startsWith("$newPrefix.") }

            return deserializer.deserialize(
                if (hasNestedKeys || !map.containsKey(newPrefix)) getProperStructureDecoder(
                    deserializer.descriptor,
                    newPrefix,
                    map
                )
                else UriDecoder(map[newPrefix]!!)
            )
        }

        override fun <T : Any> decodeNullableSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            deserializer: DeserializationStrategy<T?>,
            previousValue: T?
        ): T? {
            val newPrefix = fullTag(currentIndex++)
            val value = map[newPrefix]
            if (value == "NULL") return null
            val hasNestedKeys = map.keys.any { it.startsWith("$newPrefix.") }
            if (!hasNestedKeys && value == null) return null
            return deserializer.deserialize(
                if (hasNestedKeys || value == null) getProperStructureDecoder(
                    deserializer.descriptor,
                    newPrefix,
                    map
                )
                else UriDecoder(value)
            )
        }
    }

    /**
     * Decoder for map structures using primitive/enum keys.
     *
     * Discovers map entries by finding all keys with the given prefix and extracts
     * the first path segment as the map key. Returns key-value pairs in sequence.
     * Empty maps are recognized by the `~` marker at the prefix key.
     * Only primitive and enum key types are supported.
     */
    @OptIn(ExperimentalSerializationApi::class)
    private inner class MapDecoder(
        private val prefix: String = "",
        private val map: Map<String, String>,
    ) : SequentialDecoder() {
        override val serializersModule: SerializersModule = this@UriFormat.serializersModule

        // Discover all keys that belong to this map (have the right prefix)
        private val keys: List<String> by lazy {
            // Check for empty map marker
            if (map[prefix] == "~") return@lazy emptyList()

            val prefixDot = if (prefix.isEmpty()) "" else "$prefix."
            map.keys
                .asSequence()
                .let { seq ->
                    if (prefix.isEmpty()) seq   // top level
                    else seq.filter { it.startsWith(prefixDot) }
                }
                .map { key ->
                    if (prefix.isEmpty()) key.substringBefore('.')
                    else key.removePrefix(prefixDot).substringBefore('.')
                }
                .distinct()
                .toList()
        }

        // Current position: even = returning key index, odd = returning value index
        private var currentIndex = 0

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            // Maps encode as key0, value0, key1, value1, ...
            // So we return indices 0, 1, 2, 3, ... up to keys.size * 2
            return if (currentIndex < keys.size * 2) currentIndex else CompositeDecoder.DECODE_DONE
        }

        override fun decodeCollectionSize(descriptor: SerialDescriptor): Int = keys.size

        private fun fullTag(key: String): String = if (prefix.isEmpty()) key else "$prefix.$key"

        private fun currentKey(): String {
            val keyIndex = currentIndex / 2
            return keys[keyIndex]
        }

        override fun decodeNextStringElement(): String {
            val result = if (currentIndex % 2 == 0) {
                // This is a key
                currentKey()
            } else {
                // This is a value
                val key = currentKey()
                map[fullTag(key)] ?: throw SerializationException("Missing value for key '$key'")
            }
            currentIndex++
            return result
        }

        override fun decodeNotNullMark(): Boolean = map.containsKey(currentKey())

        override fun <T> decodeSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            deserializer: DeserializationStrategy<T>,
            previousValue: T?
        ): T {
            val result = if (index % 2 == 0) {
                // This is a key - decode as simple value
                deserializer.deserialize(UriDecoder(currentKey()))
            } else {
                // This is a value
                val key = currentKey()
                val newPrefix = fullTag(key)
                val hasNestedKeys = map.keys.any { it.startsWith("$newPrefix.") }

                deserializer.deserialize(
                    if (hasNestedKeys || !map.containsKey(newPrefix)) getProperStructureDecoder(
                        deserializer.descriptor,
                        newPrefix,
                        map
                    )
                    else UriDecoder(map[newPrefix]!!)
                )
            }
            currentIndex++
            return result
        }

        override fun <T : Any> decodeNullableSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            deserializer: DeserializationStrategy<T?>,
            previousValue: T?
        ): T? {
            if (index % 2 == 0) {
                // Keys can't be null in our format
                currentIndex++
                return UriDecoder(currentKey()).decodeSerializableValue(deserializer)
            }
            val key = currentKey()
            val newPrefix = fullTag(key)
            val value = map[newPrefix]
            currentIndex++
            if (value == "NULL") return null
            val hasNestedKeys = map.keys.any { it.startsWith("$newPrefix.") }
            if (!hasNestedKeys && value == null) return null
            return deserializer.deserialize(
                if (hasNestedKeys || value == null) getProperStructureDecoder(
                    deserializer.descriptor,
                    newPrefix,
                    map
                )
                else UriDecoder(value)
            )
        }

        override fun decodeSequentially(): Boolean = true

        override fun endStructure(descriptor: SerialDescriptor) { /* Nothing to do */ }
    }

    /**
     * Decoder for Kotlin object singletons.
     *
     * Objects have no properties to decode. This decoder immediately signals
     * that decoding is complete, allowing the deserializer to return the singleton instance.
     */
    @OptIn(ExperimentalSerializationApi::class)
    private inner class ObjectDecoder(
        private val prefix: String,
        private val map: Map<String, String>
    ) : AbstractDecoder(), StructureDecoder {
        override val serializersModule: SerializersModule = this@UriFormat.serializersModule

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int = CompositeDecoder.DECODE_DONE

        override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder = this

        override fun endStructure(descriptor: SerialDescriptor) { /* Nothing to do */ }
    }


    /**
     * Base class for sequential decoders (lists and maps) that iterate through elements in order.
     *
     * Provides common infrastructure for decoders that process elements sequentially
     * rather than by named fields. Subclasses implement [decodeNextStringElement] to
     * provide the next string value, and [decodeSerializableElement] for complex types.
     */
    @OptIn(ExperimentalSerializationApi::class)
    private abstract class SequentialDecoder : StructureDecoder {
        override fun decodeBoolean(): Boolean = throw SerializationException("${this::class} cannot decode primitive directly")
        override fun decodeByte(): Byte = throw SerializationException("${this::class} cannot decode primitive directly")
        override fun decodeChar(): Char = throw SerializationException("${this::class} cannot decode primitive directly")
        override fun decodeDouble(): Double = throw SerializationException("${this::class} cannot decode primitive directly")
        override fun decodeFloat(): Float = throw SerializationException("${this::class} cannot decode primitive directly")
        override fun decodeInt(): Int = throw SerializationException("${this::class} cannot decode primitive directly")
        override fun decodeLong(): Long = throw SerializationException("${this::class} cannot decode primitive directly")
        override fun decodeShort(): Short = throw SerializationException("${this::class} cannot decode primitive directly")
        override fun decodeString(): String = throw SerializationException("${this::class} cannot decode primitive directly")
        override fun decodeEnum(enumDescriptor: SerialDescriptor): Int = throw SerializationException("${this::class} cannot decode enum directly")
        override fun decodeInline(descriptor: SerialDescriptor): Decoder = this
        override fun decodeNull(): Nothing? = null

        override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder = this


        abstract fun decodeNextStringElement(): String

        override fun decodeBooleanElement(descriptor: SerialDescriptor, index: Int): Boolean =
            decodeNextStringElement().toBooleanStrict()
        override fun decodeByteElement(descriptor: SerialDescriptor, index: Int): Byte =
            decodeNextStringElement().toByte()
        override fun decodeShortElement(descriptor: SerialDescriptor, index: Int): Short =
            decodeNextStringElement().toShort()
        override fun decodeCharElement(descriptor: SerialDescriptor, index: Int): Char =
            decodeNextStringElement().single()
        override fun decodeIntElement(descriptor: SerialDescriptor, index: Int): Int =
            decodeNextStringElement().toInt()
        override fun decodeLongElement(descriptor: SerialDescriptor, index: Int): Long =
            decodeNextStringElement().toLong()
        override fun decodeFloatElement(descriptor: SerialDescriptor, index: Int): Float =
            decodeNextStringElement().toFloat()
        override fun decodeDoubleElement(descriptor: SerialDescriptor, index: Int): Double =
            decodeNextStringElement().toDouble()
        override fun decodeStringElement(descriptor: SerialDescriptor, index: Int): String =
            decodeNextStringElement()

        override fun decodeSequentially(): Boolean = true

        override fun decodeInlineElement(descriptor: SerialDescriptor, index: Int): Decoder = this

        override fun endStructure(descriptor: SerialDescriptor) { }
    }
}