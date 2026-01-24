package com.lightningkite.kiteui.navigation

import com.lightningkite.kiteui.decodeURIComponent
import com.lightningkite.kiteui.encodeURIComponent
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.StringFormat
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
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

    fun <T> encodeToStringMap(serializer: SerializationStrategy<T>, value: T, out: MutableMap<String, String>) {
        require(serializer.descriptor.kind is StructureKind) {
            "Only structures can be encoded into a string map."
        }
        val e = when (serializer.descriptor.kind) {
            StructureKind.LIST -> ListEncoder("", out)
            StructureKind.MAP -> {
                ensureMapHasPrimitiveKeys(serializer.descriptor)
                MapEncoder("", out)
            }
            else -> ClassEncoder(out)
        }
        e.encodeSerializableValue(serializer, value)
    }

    fun <T> decodeFromStringMap(deserializer: DeserializationStrategy<T>, map: Map<String, String>): T {
        require(deserializer.descriptor.kind is StructureKind) {
            "Only structures can be decoded from a string map."
        }
        val e = when (deserializer.descriptor.kind) {
            StructureKind.LIST -> ListDecoder(map)
            StructureKind.MAP -> {
                ensureMapHasPrimitiveKeys(deserializer.descriptor)
                MapDecoder(map)
            }
            else -> ClassDecoder(map)
        }
        return e.decodeSerializableValue(deserializer)
    }

    // -- ENCODING --

    private inner class UriEncoder : Encoder {
        override val serializersModule: SerializersModule = this@UriFormat.serializersModule

        fun getUriOrNull(): String? =
            singleElementUri ?: encodedStructure?.let { map ->
                map.entries.joinToString("&") { "${it.key}=${encodeURIComponent(it.value)}" }
            }

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
            return when (descriptor.kind) {
                is StructureKind.LIST -> ListEncoder("", map)
                is StructureKind.MAP -> {
                    ensureMapHasPrimitiveKeys(descriptor)
                    MapEncoder("", map)
                }
                else -> ClassEncoder(map)
            }
        }
    }


    /** Mostly ripped from kotlinx.serialization's Properties.OutMapper encoder */
    @OptIn(InternalSerializationApi::class)
    private inner class ClassEncoder(
        val out: MutableMap<String, String> = mutableMapOf(),
    ) : NamedValueEncoder(), CompositeEncoder {
        constructor(map: MutableMap<String, String>, prefix: String) : this(map) { pushTag(prefix) }

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
            out[tag] = value.toString()
        }

        override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
            return when (descriptor.kind) {
                is StructureKind.LIST -> {
                    val tag = popTag()  // Pop the tag since endStructure will be called on ListEncoder, not this
                    ListEncoder(tag, out)
                }
                is StructureKind.MAP -> {
                    ensureMapHasPrimitiveKeys(descriptor)
                    val tag = popTag()  // Pop the tag since endStructure will be called on MapEncoder, not this
                    MapEncoder(tag, out)
                }
                else -> this
            }
        }

        override fun encodeTaggedNull(tag: String) {
            // ignore nulls in output
        }

        override fun encodeTaggedEnum(tag: String, enumDescriptor: SerialDescriptor, ordinal: Int) {
            out[tag] = enumDescriptor.getElementName(ordinal)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private inner class ListEncoder(
        val prefix: String,
        val map: MutableMap<String, String> = mutableMapOf()
    ) : AbstractEncoder() {
        override val serializersModule: SerializersModule = this@UriFormat.serializersModule

        private var currentIndex = 0

        private fun fullTag(index: Int): String = if (prefix.isEmpty()) "$index" else "$prefix.$index"

        override fun encodeNull() {
            map[fullTag(currentIndex++)] = "NULL"
        }

        override fun encodeValue(value: Any) {
            map[fullTag(currentIndex++)] = value.toString()
        }

        override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
            val newPrefix = fullTag(currentIndex++)
            return when (descriptor.kind) {
                is StructureKind.LIST -> ListEncoder(newPrefix, map)
                is StructureKind.MAP -> {
                    ensureMapHasPrimitiveKeys(descriptor)
                    MapEncoder(newPrefix, map)
                }
                else -> ClassEncoder(map, newPrefix)
            }
        }

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
    ) : AbstractEncoder() {
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
                map[fullTag(key)] = value.toString()
                currentKey = null
                entryCount++
            }
        }

        override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
            val key = currentKey ?: throw SerializationException("Map value must have a key")
            currentKey = null
            entryCount++
            val newPrefix = fullTag(key)
            return when (descriptor.kind) {
                is StructureKind.LIST -> ListEncoder(newPrefix, map)
                is StructureKind.MAP -> {
                    ensureMapHasPrimitiveKeys(descriptor)
                    MapEncoder(newPrefix, map)
                }
                else -> ClassEncoder(map, newPrefix)
            }
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

        private val decoded = decodeURIComponent(uri)

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
            when (descriptor.kind) {
                is StructureKind.LIST -> ListDecoder(uri)
                is StructureKind.MAP -> {
                    ensureMapHasPrimitiveKeys(descriptor)
                    MapDecoder(uri)
                }
                else -> ClassDecoder(uri)
            }
    }

    /** Decodes class structures from URI query parameter format (key=value&key2=value2) */
    @OptIn(InternalSerializationApi::class)
    private inner class ClassDecoder(
        private val map: Map<String, String>,
        private val prefix: String = ""
    ) : NamedValueDecoder() {
        override val serializersModule: SerializersModule = this@UriFormat.serializersModule

        constructor(uri: String) : this(
            uri.split('&')
                .filter { it.isNotEmpty() }
                .associate {
                    val index = it.indexOf('=')
                    if (index == -1) it to ""
                    else it.substring(0, index) to decodeURIComponent(it.substring(index + 1))
                }
        )

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
            return when (descriptor.kind) {
                is StructureKind.LIST -> ListDecoder(map, newPrefix)
                is StructureKind.MAP -> {
                    ensureMapHasPrimitiveKeys(descriptor)
                    MapDecoder(map, newPrefix)
                }
                else -> ClassDecoder(map, newPrefix)
            }
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

        override fun decodeTaggedValue(tag: String): Any {
            val full = fullTag(tag)
            return map[full] ?: throw SerializationException("Missing value for field '$full'")
        }

        override fun decodeTaggedString(tag: String): String {
            val full = fullTag(tag)
            return map[full] ?: throw SerializationException("Missing value for field '$full'")
        }

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
    private inner class ListDecoder(private val map: Map<String, String>, private val prefix: String = "") : Decoder, CompositeDecoder {
        constructor(uri: String) : this(
            uri.split('&')
                .filter { it.isNotEmpty() }
                .associate {
                    val index = it.indexOf('=')
                    if (index == -1) it to ""
                    else it.substring(0, index) to decodeURIComponent(it.substring(index + 1))
                }
        )

        override val serializersModule: SerializersModule = this@UriFormat.serializersModule

        private var currentIndex = 0
        private val size: Int by lazy {
            // Check for empty list marker first, otherwise count indexed entries
            if (map[prefix] == "~") {
                0
            } else {
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

        // Decoder interface methods for list deserialization
        override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder = this
        override fun decodeBoolean(): Boolean = throw SerializationException("List cannot decode primitive directly")
        override fun decodeByte(): Byte = throw SerializationException("List cannot decode primitive directly")
        override fun decodeChar(): Char = throw SerializationException("List cannot decode primitive directly")
        override fun decodeDouble(): Double = throw SerializationException("List cannot decode primitive directly")
        override fun decodeFloat(): Float = throw SerializationException("List cannot decode primitive directly")
        override fun decodeInt(): Int = throw SerializationException("List cannot decode primitive directly")
        override fun decodeLong(): Long = throw SerializationException("List cannot decode primitive directly")
        override fun decodeShort(): Short = throw SerializationException("List cannot decode primitive directly")
        override fun decodeString(): String = throw SerializationException("List cannot decode primitive directly")
        override fun decodeEnum(enumDescriptor: SerialDescriptor): Int = throw SerializationException("List cannot decode enum directly")
        override fun decodeInline(descriptor: SerialDescriptor): Decoder = this
        override fun decodeNotNullMark(): Boolean = true
        override fun decodeNull(): Nothing? = null

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            return if (currentIndex < size) currentIndex else CompositeDecoder.DECODE_DONE
        }

        override fun decodeCollectionSize(descriptor: SerialDescriptor): Int = size

        private fun currentItem(): String {
            val full = fullTag(currentIndex++)
            return map[full] ?: throw SerializationException("Missing value for index '$full'")
        }

        override fun decodeBooleanElement(descriptor: SerialDescriptor, index: Int): Boolean =
            currentItem().toBooleanStrict()
        override fun decodeByteElement(descriptor: SerialDescriptor, index: Int): Byte =
            currentItem().toByte()
        override fun decodeShortElement(descriptor: SerialDescriptor, index: Int): Short =
            currentItem().toShort()
        override fun decodeCharElement(descriptor: SerialDescriptor, index: Int): Char =
            currentItem().single()
        override fun decodeIntElement(descriptor: SerialDescriptor, index: Int): Int =
            currentItem().toInt()
        override fun decodeLongElement(descriptor: SerialDescriptor, index: Int): Long =
            currentItem().toLong()
        override fun decodeFloatElement(descriptor: SerialDescriptor, index: Int): Float =
            currentItem().toFloat()
        override fun decodeDoubleElement(descriptor: SerialDescriptor, index: Int): Double =
            currentItem().toDouble()
        override fun decodeStringElement(descriptor: SerialDescriptor, index: Int): String =
            currentItem()

        override fun decodeInlineElement(descriptor: SerialDescriptor, index: Int): Decoder = this

        override fun <T> decodeSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            deserializer: DeserializationStrategy<T>,
            previousValue: T?
        ): T {
            val newPrefix = fullTag(currentIndex++)
            // Check if it's a nested structure (has keys with this prefix) or a simple value
            val hasNestedKeys = map.keys.any { it.startsWith("$newPrefix.") }
            return if (hasNestedKeys || !map.containsKey(newPrefix)) {
                // Nested structure - use appropriate decoder
                when (deserializer.descriptor.kind) {
                    is StructureKind.LIST -> deserializer.deserialize(ListDecoder(map, newPrefix))
                    is StructureKind.MAP -> {
                        ensureMapHasPrimitiveKeys(deserializer.descriptor)
                        deserializer.deserialize(MapDecoder(map, newPrefix))
                    }
                    else -> deserializer.deserialize(ClassDecoder(map, newPrefix))
                }
            } else {
                // Simple value
                UriDecoder(map[newPrefix]!!).decodeSerializableValue(deserializer)
            }
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
            return if (hasNestedKeys || value == null) {
                when (deserializer.descriptor.kind) {
                    is StructureKind.LIST -> deserializer.deserialize(ListDecoder(map, newPrefix))
                    is StructureKind.MAP -> deserializer.deserialize(MapDecoder(map, newPrefix))
                    else -> deserializer.deserialize(ClassDecoder(map, newPrefix))
                }
            } else {
                UriDecoder(value).decodeSerializableValue(deserializer)
            }
        }

        override fun decodeSequentially(): Boolean = true

        override fun endStructure(descriptor: SerialDescriptor) { /* Nothing to do */ }
    }

    /** Decodes Map<String, T> from URI format using string keys directly */
    @OptIn(ExperimentalSerializationApi::class)
    private inner class MapDecoder(
        private val map: Map<String, String>,
        private val prefix: String = ""
    ) : Decoder, CompositeDecoder {
        constructor(uri: String) : this(
            uri.split('&')
                .filter { it.isNotEmpty() }
                .associate {
                    val index = it.indexOf('=')
                    if (index == -1) it to ""
                    else it.substring(0, index) to decodeURIComponent(it.substring(index + 1))
                }
        )

        override val serializersModule: SerializersModule = this@UriFormat.serializersModule

        // Discover all keys that belong to this map (have the right prefix)
        private val keys: List<String> by lazy {
            // Check for empty map marker
            if (map[prefix] == "~") {
                emptyList()
            } else {
                val prefixDot = if (prefix.isEmpty()) "" else "$prefix."
                map.keys
                    .filter { key ->
                        if (prefix.isEmpty()) {
                            // Top-level: any key without a dot, or keys that are prefixes of nested structures
                            !key.contains('.') || map.keys.none { it == key.substringBefore('.') }
                        } else {
                            key.startsWith(prefixDot)
                        }
                    }
                    .map { key ->
                        if (prefix.isEmpty()) key.substringBefore('.')
                        else key.removePrefix(prefixDot).substringBefore('.')
                    }
                    .distinct()
            }
        }

        // Current position: even = returning key index, odd = returning value index
        private var currentIndex = 0

        override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder = this

        override fun decodeBoolean(): Boolean = throw SerializationException("Map cannot decode primitive directly")
        override fun decodeByte(): Byte = throw SerializationException("Map cannot decode primitive directly")
        override fun decodeChar(): Char = throw SerializationException("Map cannot decode primitive directly")
        override fun decodeDouble(): Double = throw SerializationException("Map cannot decode primitive directly")
        override fun decodeFloat(): Float = throw SerializationException("Map cannot decode primitive directly")
        override fun decodeInt(): Int = throw SerializationException("Map cannot decode primitive directly")
        override fun decodeLong(): Long = throw SerializationException("Map cannot decode primitive directly")
        override fun decodeShort(): Short = throw SerializationException("Map cannot decode primitive directly")
        override fun decodeString(): String = throw SerializationException("Map cannot decode primitive directly")
        override fun decodeEnum(enumDescriptor: SerialDescriptor): Int = throw SerializationException("Map cannot decode enum directly")
        override fun decodeInline(descriptor: SerialDescriptor): Decoder = this
        override fun decodeNotNullMark(): Boolean = true
        override fun decodeNull(): Nothing? = null

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

        // Keys are at even indices
        override fun decodeStringElement(descriptor: SerialDescriptor, index: Int): String {
            val result = if (index % 2 == 0) {
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

        override fun decodeBooleanElement(descriptor: SerialDescriptor, index: Int): Boolean =
            decodeStringElement(descriptor, index).toBooleanStrict()
        override fun decodeByteElement(descriptor: SerialDescriptor, index: Int): Byte =
            decodeStringElement(descriptor, index).toByte()
        override fun decodeShortElement(descriptor: SerialDescriptor, index: Int): Short =
            decodeStringElement(descriptor, index).toShort()
        override fun decodeCharElement(descriptor: SerialDescriptor, index: Int): Char =
            decodeStringElement(descriptor, index).single()
        override fun decodeIntElement(descriptor: SerialDescriptor, index: Int): Int =
            decodeStringElement(descriptor, index).toInt()
        override fun decodeLongElement(descriptor: SerialDescriptor, index: Int): Long =
            decodeStringElement(descriptor, index).toLong()
        override fun decodeFloatElement(descriptor: SerialDescriptor, index: Int): Float =
            decodeStringElement(descriptor, index).toFloat()
        override fun decodeDoubleElement(descriptor: SerialDescriptor, index: Int): Double =
            decodeStringElement(descriptor, index).toDouble()

        override fun decodeInlineElement(descriptor: SerialDescriptor, index: Int): Decoder = this

        override fun <T> decodeSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            deserializer: DeserializationStrategy<T>,
            previousValue: T?
        ): T {
            val result = if (index % 2 == 0) {
                // This is a key - decode as simple value
                UriDecoder(currentKey()).decodeSerializableValue(deserializer)
            } else {
                // This is a value
                val key = currentKey()
                val newPrefix = fullTag(key)
                val hasNestedKeys = map.keys.any { it.startsWith("$newPrefix.") }
                if (hasNestedKeys || !map.containsKey(newPrefix)) {
                    // Nested structure
                    when (deserializer.descriptor.kind) {
                        is StructureKind.LIST -> deserializer.deserialize(ListDecoder(map, newPrefix))
                        is StructureKind.MAP -> {
                            ensureMapHasPrimitiveKeys(deserializer.descriptor)
                            deserializer.deserialize(MapDecoder(map, newPrefix))
                        }
                        else -> deserializer.deserialize(ClassDecoder(map, newPrefix))
                    }
                } else {
                    // Simple value
                    UriDecoder(map[newPrefix]!!).decodeSerializableValue(deserializer)
                }
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
            return if (hasNestedKeys || value == null) {
                when (deserializer.descriptor.kind) {
                    is StructureKind.LIST -> deserializer.deserialize(ListDecoder(map, newPrefix))
                    is StructureKind.MAP -> deserializer.deserialize(MapDecoder(map, newPrefix))
                    else -> deserializer.deserialize(ClassDecoder(map, newPrefix))
                }
            } else {
                UriDecoder(value).decodeSerializableValue(deserializer)
            }
        }

        override fun decodeSequentially(): Boolean = true

        override fun endStructure(descriptor: SerialDescriptor) { /* Nothing to do */ }
    }

    private fun ensureMapHasPrimitiveKeys(mapDescriptor: SerialDescriptor) =
        require(mapDescriptor.getElementDescriptor(0).kind is PrimitiveKind) {
            "UriFormat only supports maps with primitive-type keys."
        }
}