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

class UriFormat(
    override val serializersModule: SerializersModule
) : StringFormat {
    override fun <T> encodeToString(serializer: SerializationStrategy<T>, value: T): String {
        val e = UriEncoder()
        e.encodeSerializableValue(serializer, value)
        return e.getUri()
    }

    override fun <T> decodeFromString(deserializer: DeserializationStrategy<T>, string: String): T {
        val d = UriDecoder(string)
        return d.decodeSerializableValue(deserializer)
    }

    fun encodeToString(map: Map<String, String>): String =
        map.entries.joinToString("&") { "${it.key}=${it.value}" }

    fun <T> encodeToStringMap(serializer: SerializationStrategy<T>, value: T, dest: MutableMap<String, String>) {
        require(serializer.descriptor.unwrap().kind.isStructure()) {
            "Only structures can be encoded into a string map."
        }
        StringMapEncoder(prefix = "", dest).encodeSerializableValue(serializer, value)
    }

    fun <T> encodeToStringMap(serializer: SerializationStrategy<T>, key: String, value: T, dest: MutableMap<String, String>) {
        if (serializer.descriptor.unwrap().kind.isNonStructure()) {
            dest[key] = encodeToString(serializer, value)
        }
        else StringMapEncoder(prefix = key, dest).encodeSerializableValue(serializer, value)
    }

    fun <T> decodeFromStringMap(deserializer: DeserializationStrategy<T>, source: Map<String, String>): T {
        require(deserializer.descriptor.unwrap().kind.isStructure()) {
            "Only structures can be decoded from a string map."
        }
        return StringMapDecoder(source, prefix = "").decodeSerializableValue(deserializer)
    }

    fun <T> decodeFromStringMap(deserializer: DeserializationStrategy<T>, key: String, source: Map<String, String>): T =
        if (deserializer.descriptor.unwrap().kind.isNonStructure()) {
            decodeFromString(deserializer, source[key] ?: throw SerializationException("Missing key $key"))
        }
        else StringMapDecoder(source, prefix = key).decodeSerializableValue(deserializer)

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
            StructureKind.OBJECT -> throw UnsupportedOperationException("UriFormat does not support objects yet")
            else -> throw IllegalArgumentException("Only structure types can be encoded as a string map. Got ${descriptor.kind} (${descriptor.serialName}).")
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
            StructureKind.OBJECT -> throw UnsupportedOperationException("UriFormat does not support objects yet")
            else -> throw IllegalArgumentException("Only structure types can be encoded as a string map. Got ${descriptor.kind} (${descriptor.serialName}).")
        }
    }

    // -- ENCODING --

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

    @OptIn(ExperimentalSerializationApi::class)
    private inner class StringMapEncoder(val prefix: String, val dest: MutableMap<String, String>) : AbstractEncoder() {
        override val serializersModule: SerializersModule = this@UriFormat.serializersModule

        override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder =
            getProperStructureEncoder(descriptor, dest, prefix)
    }


    /** Mostly ripped from kotlinx.serialization's Properties.OutMapper encoder */
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

    /** Encodes Map<String, T> using string keys directly as field names */
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



    // -- DECODING --

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

    @OptIn(ExperimentalSerializationApi::class)
    private inner class StringMapDecoder(val source: Map<String, String>, val prefix: String) : AbstractDecoder() {
        override val serializersModule: SerializersModule = this@UriFormat.serializersModule

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int = throw UnsupportedOperationException()

        override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder =
            getProperStructureDecoder(descriptor, prefix, source)
    }

    /** Decodes class structures from URI query parameter format (key=value&key2=value2) */
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
                val typeName = map[fullTag("type")]
                    ?: throw SerializationException("Missing 'type' field for polymorphic deserialization")

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

    /** Decodes list structures from URI format using indexed keys (0=item1&1=item2&2=item3) */
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

    /** Decodes Map<String, T> from URI format using string keys directly */
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