// by Claude - CLI arg parser: decodes List<String> tokens into CliCommand instances
package com.lightningkite.kiteui.aidriver

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer

/**
 * Encodes/decodes [CliCommand] to/from a list of CLI argument tokens.
 *
 * Decoding (parse args → CliCommand):
 * - First token is the subcommand name (the @SerialName of the sealed class branch)
 * - Required fields (no default value) are consumed positionally in declaration order
 * - Optional fields (has default) are matched by `--fieldName value`
 * - Boolean optional flags: `--flagName` alone sets the field to `true`
 *
 * Encoding (CliCommand → canonical CLI strings):
 * - Produces the subcommand name followed by required args, then optional `--key value` pairs
 */
@OptIn(ExperimentalSerializationApi::class)
object CliFormat {

    /**
     * Parses [args] into a [CliCommand].
     * Throws [CliParseException] on invalid input.
     */
    fun parse(args: List<String>): CliCommand {
        if (args.isEmpty()) throw CliParseException("No command specified")
        val serializer = serializer<CliCommand>()
        return decode(serializer, args)
    }

    /**
     * Encodes a [CliCommand] as a list of CLI tokens.
     */
    fun encode(command: CliCommand): List<String> {
        val serializer = serializer<CliCommand>()
        return encodeCommand(serializer, command)
    }

    // ---- Decoding ----

    fun <T> decode(deserializer: DeserializationStrategy<T>, args: List<String>): T {
        val decoder = CliDecoder(args.toMutableList())
        return deserializer.deserialize(decoder)
    }

    private class CliDecoder(
        private val tokens: MutableList<String>,
        override val serializersModule: SerializersModule = EmptySerializersModule()
    ) : AbstractDecoder() {

        // Index into descriptor elements being decoded
        private var elementIndex = 0

        // Positional queue: tokens not yet consumed as named args
        private val positional = ArrayDeque<String>()

        // Named args: --key value or --flag (boolean)
        private val named = mutableMapOf<String, String?>()

        init {
            parseTokens()
        }

        private fun parseTokens() {
            var i = 0
            while (i < tokens.size) {
                val tok = tokens[i]
                if (tok.startsWith("--")) {
                    val key = tok.removePrefix("--")
                    val next = tokens.getOrNull(i + 1)
                    if (next != null && !next.startsWith("--")) {
                        named[key] = next
                        i += 2
                    } else {
                        named[key] = null // boolean flag
                        i++
                    }
                } else {
                    positional.addLast(tok)
                    i++
                }
            }
        }

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            // Advance through descriptor elements
            while (elementIndex < descriptor.elementsCount) {
                val idx = elementIndex++
                val name = descriptor.getElementName(idx)
                val hasDefault = descriptor.isElementOptional(idx)
                if (hasDefault) {
                    // Only decode if explicitly supplied as --name
                    if (named.containsKey(name)) return idx
                    // Otherwise skip (use default)
                } else {
                    // Required: consume next positional
                    return idx
                }
            }
            return CompositeDecoder.DECODE_DONE
        }

        override fun decodeString(): String {
            // Named args have already been looked up; this is called for positionals
            return positional.removeFirstOrNull()
                ?: throw CliParseException("Not enough arguments")
        }

        override fun decodeBoolean(): Boolean {
            val posVal = positional.firstOrNull()
            return if (posVal == "true" || posVal == "false") {
                positional.removeAt(0)
                posVal.toBoolean()
            } else {
                // Boolean flag: presence means true
                true
            }
        }

        override fun decodeInt(): Int = decodeString().toIntOrNull()
            ?: throw CliParseException("Expected integer")

        override fun decodeLong(): Long = decodeString().toLongOrNull()
            ?: throw CliParseException("Expected long integer")

        override fun decodeFloat(): Float = decodeString().toFloatOrNull()
            ?: throw CliParseException("Expected float")

        override fun decodeDouble(): Double = decodeString().toDoubleOrNull()
            ?: throw CliParseException("Expected double")

        override fun decodeNotNullMark(): Boolean {
            // Named optional null: if key present with no value or "null"
            return true
        }

        override fun decodeNull(): Nothing? = null

        @OptIn(ExperimentalSerializationApi::class)
        override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
            return when (descriptor.kind) {
                StructureKind.CLASS -> {
                    // Build a new decoder scoped to this class's named args
                    StructureDecoder(descriptor, positional, named, serializersModule)
                }
                else -> this
            }
        }

        // For sealed class dispatch: first token is the subcommand name
        // by Claude - fixed to match kotlinx.serialization 1.7+ AbstractDecoder API
        override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T {
            return deserializer.deserialize(this)
        }

        override fun decodeInline(descriptor: SerialDescriptor): CliDecoder = this
    }

    /**
     * Decoder for a specific struct (non-sealed class branch).
     * Handles positional and named arguments for its fields.
     */
    @OptIn(ExperimentalSerializationApi::class)
    private class StructureDecoder(
        private val descriptor: SerialDescriptor,
        private val positional: ArrayDeque<String>,
        private val named: Map<String, String?>,
        override val serializersModule: SerializersModule
    ) : AbstractDecoder() {

        private var elementIndex = 0
        private var currentName: String? = null

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            while (elementIndex < descriptor.elementsCount) {
                val idx = elementIndex++
                val name = descriptor.getElementName(idx)
                val hasDefault = descriptor.isElementOptional(idx)
                if (hasDefault) {
                    if (named.containsKey(name)) {
                        currentName = name
                        return idx
                    }
                } else {
                    currentName = null
                    return idx
                }
            }
            return CompositeDecoder.DECODE_DONE
        }

        private fun nextValue(): String {
            val name = currentName
            return if (name != null) {
                named[name] ?: throw CliParseException("Flag --$name requires a value")
            } else {
                positional.removeFirstOrNull()
                    ?: throw CliParseException("Not enough positional arguments for ${descriptor.serialName}")
            }
        }

        override fun decodeString(): String = nextValue()

        override fun decodeBoolean(): Boolean {
            val name = currentName
            return if (name != null) {
                val v = named[name]
                v?.toBooleanStrictOrNull() ?: true // flag-only → true
            } else {
                positional.removeFirstOrNull()?.toBooleanStrictOrNull()
                    ?: throw CliParseException("Expected boolean")
            }
        }

        override fun decodeInt(): Int = nextValue().toIntOrNull()
            ?: throw CliParseException("Expected integer")

        override fun decodeLong(): Long = nextValue().toLongOrNull()
            ?: throw CliParseException("Expected long integer")

        override fun decodeFloat(): Float = nextValue().toFloatOrNull()
            ?: throw CliParseException("Expected float")

        override fun decodeDouble(): Double = nextValue().toDoubleOrNull()
            ?: throw CliParseException("Expected double")

        // by Claude - required: AbstractDecoder.decodeEnum calls decodeValue() which always throws
        override fun decodeEnum(enumDescriptor: SerialDescriptor): Int =
            enumDescriptor.getElementIndex(decodeString())

        override fun decodeNotNullMark(): Boolean = true
        override fun decodeNull(): Nothing? = null

        // by Claude - create fresh decoders for nested types so they get their own element index
        override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder =
            when (descriptor.kind) {
                StructureKind.CLASS -> StructureDecoder(descriptor, positional, named, serializersModule)
                else -> SealedDecoder(positional, named, serializersModule)
            }

        override fun endStructure(descriptor: SerialDescriptor) {}
    }

    /**
     * Decoder for a nested sealed class encountered inside a [StructureDecoder].
     * Reads the type discriminator from the next positional token, then delegates
     * to a fresh [StructureDecoder] for the concrete type's fields.
     * Shares the same positional/named queues so tokens are consumed in sequence.
     */
    // by Claude
    @OptIn(ExperimentalSerializationApi::class)
    private class SealedDecoder(
        private val positional: ArrayDeque<String>,
        private val named: Map<String, String?>,
        override val serializersModule: SerializersModule
    ) : AbstractDecoder() {

        private var elementIndex = 0

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int =
            if (elementIndex < descriptor.elementsCount) elementIndex++
            else CompositeDecoder.DECODE_DONE

        // Reads the sealed type discriminator (next unconsumed positional token)
        override fun decodeString(): String =
            positional.removeFirstOrNull() ?: throw CliParseException("Not enough arguments")

        override fun decodeEnum(enumDescriptor: SerialDescriptor): Int =
            enumDescriptor.getElementIndex(decodeString())

        override fun decodeBoolean(): Boolean = decodeString().toBooleanStrictOrNull()
            ?: throw CliParseException("Expected boolean")
        override fun decodeInt(): Int = decodeString().toIntOrNull()
            ?: throw CliParseException("Expected integer")
        override fun decodeLong(): Long = decodeString().toLongOrNull()
            ?: throw CliParseException("Expected long integer")
        override fun decodeFloat(): Float = decodeString().toFloatOrNull()
            ?: throw CliParseException("Expected float")
        override fun decodeDouble(): Double = decodeString().toDoubleOrNull()
            ?: throw CliParseException("Expected double")

        override fun decodeNotNullMark(): Boolean = true
        override fun decodeNull(): Nothing? = null

        override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder =
            when (descriptor.kind) {
                StructureKind.CLASS -> StructureDecoder(descriptor, positional, named, serializersModule)
                else -> SealedDecoder(positional, named, serializersModule)
            }

        override fun endStructure(descriptor: SerialDescriptor) {}
    }

    // ---- Encoding ----

    @Suppress("UNCHECKED_CAST")
    private fun <T> encodeCommand(serializer: KSerializer<T>, value: T): List<String> {
        val result = mutableListOf<String>()
        val encoder = CliEncoder(result)
        serializer.serialize(encoder, value)
        return result
    }

    private class CliEncoder(private val out: MutableList<String>) :
        kotlinx.serialization.encoding.Encoder,
        kotlinx.serialization.encoding.CompositeEncoder {

        override val serializersModule: SerializersModule = EmptySerializersModule()

        private var fieldName: String? = null
        private var isOptional = false

        override fun encodeString(value: String) {
            emitValue(value)
        }

        override fun encodeBoolean(value: Boolean) {
            val name = fieldName
            if (name != null && isOptional) {
                if (value) out.add("--$name")
            } else {
                emitValue(value.toString())
            }
        }

        override fun encodeInt(value: Int) = emitValue(value.toString())
        override fun encodeLong(value: Long) = emitValue(value.toString())
        override fun encodeFloat(value: Float) = emitValue(value.toString())
        override fun encodeDouble(value: Double) = emitValue(value.toString())
        override fun encodeNull() {} // omit nulls
        override fun encodeNotNullMark() {}

        override fun encodeInline(descriptor: SerialDescriptor): kotlinx.serialization.encoding.Encoder = this

        private fun emitValue(s: String) {
            val name = fieldName
            if (name != null && isOptional) {
                out.add("--$name")
                out.add(s)
            } else {
                out.add(s)
            }
        }

        @OptIn(ExperimentalSerializationApi::class)
        override fun beginStructure(descriptor: SerialDescriptor): kotlinx.serialization.encoding.CompositeEncoder = this

        override fun endStructure(descriptor: SerialDescriptor) {}

        @OptIn(ExperimentalSerializationApi::class)
        override fun <T> encodeSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            serializer: SerializationStrategy<T>,
            value: T
        ) {
            fieldName = descriptor.getElementName(index)
            isOptional = descriptor.isElementOptional(index)
            serializer.serialize(this, value)
        }

        override fun encodeBooleanElement(descriptor: SerialDescriptor, index: Int, value: Boolean) {
            fieldName = descriptor.getElementName(index)
            isOptional = descriptor.isElementOptional(index)
            encodeBoolean(value)
        }

        override fun encodeByteElement(descriptor: SerialDescriptor, index: Int, value: Byte) {
            fieldName = descriptor.getElementName(index)
            isOptional = descriptor.isElementOptional(index)
            emitValue(value.toString())
        }

        override fun encodeShortElement(descriptor: SerialDescriptor, index: Int, value: Short) {
            fieldName = descriptor.getElementName(index)
            isOptional = descriptor.isElementOptional(index)
            emitValue(value.toString())
        }

        override fun encodeIntElement(descriptor: SerialDescriptor, index: Int, value: Int) {
            fieldName = descriptor.getElementName(index)
            isOptional = descriptor.isElementOptional(index)
            emitValue(value.toString())
        }

        override fun encodeLongElement(descriptor: SerialDescriptor, index: Int, value: Long) {
            fieldName = descriptor.getElementName(index)
            isOptional = descriptor.isElementOptional(index)
            emitValue(value.toString())
        }

        override fun encodeFloatElement(descriptor: SerialDescriptor, index: Int, value: Float) {
            fieldName = descriptor.getElementName(index)
            isOptional = descriptor.isElementOptional(index)
            emitValue(value.toString())
        }

        override fun encodeDoubleElement(descriptor: SerialDescriptor, index: Int, value: Double) {
            fieldName = descriptor.getElementName(index)
            isOptional = descriptor.isElementOptional(index)
            emitValue(value.toString())
        }

        override fun encodeStringElement(descriptor: SerialDescriptor, index: Int, value: String) {
            fieldName = descriptor.getElementName(index)
            isOptional = descriptor.isElementOptional(index)
            emitValue(value)
        }

        override fun encodeCharElement(descriptor: SerialDescriptor, index: Int, value: Char) {
            fieldName = descriptor.getElementName(index)
            isOptional = descriptor.isElementOptional(index)
            emitValue(value.toString())
        }

        override fun <T : Any> encodeNullableSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            serializer: SerializationStrategy<T>,
            value: T?
        ) {
            if (value != null) {
                encodeSerializableElement(descriptor, index, serializer, value)
            }
            // else: omit nulls from CLI output
        }

        // by Claude - required by CompositeEncoder in kotlinx.serialization 1.7+
        override fun encodeInlineElement(descriptor: SerialDescriptor, index: Int): kotlinx.serialization.encoding.Encoder {
            fieldName = descriptor.getElementName(index)
            isOptional = descriptor.isElementOptional(index)
            return this
        }

        override fun encodeByte(value: Byte) = emitValue(value.toString())
        override fun encodeShort(value: Short) = emitValue(value.toString())
        override fun encodeChar(value: Char) = emitValue(value.toString())
        override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) =
            emitValue(enumDescriptor.getElementName(index))
    }
}

class CliParseException(message: String) : SerializationException(message)
