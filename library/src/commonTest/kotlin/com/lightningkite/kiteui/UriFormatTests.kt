@file:OptIn(ExperimentalSerializationApi::class)

package com.lightningkite.kiteui

import com.lightningkite.kiteui.navigation.UriFormat
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.modules.EmptySerializersModule
import kotlin.jvm.JvmInline
import kotlin.test.Test
import kotlin.test.assertEquals

// Test data classes
@Serializable
data class SimpleClass(val name: String, val age: Int)

@Serializable
data class ClassWithOptional(val required: String, val optional: String? = null)

@Serializable
data class ClassWithAllPrimitives(
    val boolVal: Boolean,
    val byteVal: Byte,
    val shortVal: Short,
    val intVal: Int,
    val longVal: Long,
    val floatVal: Float,
    val doubleVal: Double,
    val charVal: Char,
    val stringVal: String
)

@Serializable
data class NestedClass(val inner: SimpleClass, val value: Int)

@Serializable
enum class TestEnum { FIRST, SECOND, THIRD }

@Serializable
data class ClassWithEnum(val status: TestEnum, val name: String)

@Serializable
@JvmInline
value class UserId(val id: Int)

@Serializable
data class ClassWithListElem(val list: List<Int>)

@Serializable
@JvmInline
value class WrappedSimpleClass(val wrapped: SimpleClass)

@Serializable
@JvmInline
value class UserName(val name: String)

@Serializable
data class ClassWithValueClass(val userId: UserId, val userName: UserName)

@Serializable
data class KitchenSink(
    val name: String = "kitchen sink",
    val nested: List<KitchenSink> = listOf(
        KitchenSink(
            "nested 1",
            listOf(
                KitchenSink("nested 1.1", emptyList(), NestedClass(SimpleClass("nested 1.4", 1), 0)),
                KitchenSink("nested 1.2",
                    listOf(
                        KitchenSink("nested 1.2.1", emptyList(), NestedClass(SimpleClass("", 0), 0))
                    ),
                    NestedClass(SimpleClass("", 0), 0)
                )
            ),
            NestedClass(SimpleClass("", 0), 0)
        )
    ),
    val clazz: NestedClass = NestedClass(SimpleClass("outermost", 1000), 1000)
)

class UriFormatTests {

    private val format = UriFormat(EmptySerializersModule())

    private fun <T> roundTrip(serializer: KSerializer<T>, value: T) {
        val encoded = format.encodeToString(serializer, value)
        println("Encoded '$value' -> '$encoded'")
        val decoded = format.decodeFromString(serializer, encoded)
        assertEquals(value, decoded, "Round-trip failed for value: $value")
    }

    // === Primitive Tests ===

    @Test
    fun testBoolean() {
        roundTrip(Boolean.serializer(), true)
        roundTrip(Boolean.serializer(), false)
    }

    @Test
    fun testByte() {
        roundTrip(Byte.serializer(), 0.toByte())
        roundTrip(Byte.serializer(), Byte.MAX_VALUE)
        roundTrip(Byte.serializer(), Byte.MIN_VALUE)
        roundTrip(Byte.serializer(), 42.toByte())
    }

    @Test
    fun testShort() {
        roundTrip(Short.serializer(), 0.toShort())
        roundTrip(Short.serializer(), Short.MAX_VALUE)
        roundTrip(Short.serializer(), Short.MIN_VALUE)
        roundTrip(Short.serializer(), 1234.toShort())
    }

    @Test
    fun testInt() {
        roundTrip(Int.serializer(), 0)
        roundTrip(Int.serializer(), Int.MAX_VALUE)
        roundTrip(Int.serializer(), Int.MIN_VALUE)
        roundTrip(Int.serializer(), 42)
        roundTrip(Int.serializer(), -123456)
    }

    @Test
    fun testLong() {
        roundTrip(Long.serializer(), 0L)
        roundTrip(Long.serializer(), Long.MAX_VALUE)
        roundTrip(Long.serializer(), Long.MIN_VALUE)
        roundTrip(Long.serializer(), 123456789012345L)
    }

    @Test
    fun testFloat() {
        roundTrip(Float.serializer(), 0.0f)
        roundTrip(Float.serializer(), 3.14159f)
        roundTrip(Float.serializer(), -123.456f)
        roundTrip(Float.serializer(), Float.MAX_VALUE)
        roundTrip(Float.serializer(), Float.MIN_VALUE)
    }

    @Test
    fun testDouble() {
        roundTrip(Double.serializer(), 0.0)
        roundTrip(Double.serializer(), 3.141592653589793)
        roundTrip(Double.serializer(), -123.456789)
        roundTrip(Double.serializer(), Double.MAX_VALUE)
        roundTrip(Double.serializer(), Double.MIN_VALUE)
    }

    @Test
    fun testChar() {
        roundTrip(Char.serializer(), 'A')
        roundTrip(Char.serializer(), 'z')
        roundTrip(Char.serializer(), '0')
        roundTrip(Char.serializer(), ' ')
    }

    @Test
    fun testString() {
        roundTrip(String.serializer(), "hello")
        roundTrip(String.serializer(), "")
        roundTrip(String.serializer(), "Hello World!")
        roundTrip(String.serializer(), "special chars: &=?#")
        roundTrip(String.serializer(), "unicode: 你好世界")
    }

    @Test
    fun testNullableString() {
        roundTrip(String.serializer().nullable, "hello")
        roundTrip(String.serializer().nullable, null)
        roundTrip(String.serializer().nullable, "")
    }

    @Test
    fun testNullableInt() {
        roundTrip(Int.serializer().nullable, 42)
        roundTrip(Int.serializer().nullable, null)
        roundTrip(Int.serializer().nullable, 0)
    }

    // === Enum Tests ===

    @Test
    fun testEnum() {
        roundTrip(TestEnum.serializer(), TestEnum.FIRST)
        roundTrip(TestEnum.serializer(), TestEnum.SECOND)
        roundTrip(TestEnum.serializer(), TestEnum.THIRD)
    }

    // === Class Tests ===

    @Test
    fun testSimpleClass() {
        roundTrip(SimpleClass.serializer(), SimpleClass("John", 30))
        roundTrip(SimpleClass.serializer(), SimpleClass("", 0))
        roundTrip(SimpleClass.serializer(), SimpleClass("Special & chars = here", 999))
    }

    @Test
    fun testClassWithOptional() {
        roundTrip(ClassWithOptional.serializer(), ClassWithOptional("required", "optional"))
        roundTrip(ClassWithOptional.serializer(), ClassWithOptional("required", null))
        roundTrip(ClassWithOptional.serializer(), ClassWithOptional("required"))
    }

    @Test
    fun testClassWithAllPrimitives() {
        roundTrip(
            ClassWithAllPrimitives.serializer(),
            ClassWithAllPrimitives(
                boolVal = true,
                byteVal = 127,
                shortVal = 1000,
                intVal = 123456,
                longVal = 9876543210L,
                floatVal = 3.14f,
                doubleVal = 2.718281828,
                charVal = 'X',
                stringVal = "test string"
            )
        )
    }

    @Test
    fun testClassWithEnum() {
        roundTrip(ClassWithEnum.serializer(), ClassWithEnum(TestEnum.FIRST, "first"))
        roundTrip(ClassWithEnum.serializer(), ClassWithEnum(TestEnum.SECOND, "second"))
    }

    @Test
    fun testNestedClass() {
        roundTrip(
            NestedClass.serializer(),
            NestedClass(SimpleClass("inner", 10), 42)
        )
    }

    // === List Tests ===

    @Test
    fun testListOfInts() {
        roundTrip(ListSerializer(Int.serializer()), listOf(1, 2, 3, 4, 5))
        roundTrip(ListSerializer(Int.serializer()), listOf())
        roundTrip(ListSerializer(Int.serializer()), listOf(42))
    }

    @Test
    fun testListOfStrings() {
        roundTrip(ListSerializer(String.serializer()), listOf("a", "b", "c"))
        roundTrip(ListSerializer(String.serializer()), listOf())
        roundTrip(ListSerializer(String.serializer()), listOf("single"))
        roundTrip(ListSerializer(String.serializer()), listOf("hello", "world"))
    }

    @Test
    fun testListOfBooleans() {
        roundTrip(ListSerializer(Boolean.serializer()), listOf(true, false, true))
        roundTrip(ListSerializer(Boolean.serializer()), listOf())
    }

    @Test
    fun testListOfDoubles() {
        roundTrip(ListSerializer(Double.serializer()), listOf(1.1, 2.2, 3.3))
    }

    @Test
    fun testListWithNullables() {
        roundTrip(
            ListSerializer(String.serializer().nullable),
            listOf("a", null, "b", null)
        )
        roundTrip(
            ListSerializer(Int.serializer().nullable),
            listOf(1, null, 3)
        )
    }

    // === Value Class (Inline Class) Tests ===

    @Test
    fun testValueClassUserId() {
        roundTrip(UserId.serializer(), UserId(123))
        roundTrip(UserId.serializer(), UserId(0))
        roundTrip(UserId.serializer(), UserId(-1))
    }

    @Test
    fun testClassWrappedInValueClass() {
        roundTrip(WrappedSimpleClass.serializer(), WrappedSimpleClass(SimpleClass("simple", 1)))
        roundTrip(WrappedSimpleClass.serializer(), WrappedSimpleClass(SimpleClass("here # are & = special characters", 60)))
        roundTrip(WrappedSimpleClass.serializer(), WrappedSimpleClass(SimpleClass("blah", 6000)))
    }

    @Test
    fun testValueClassUserName() {
        roundTrip(UserName.serializer(), UserName("Alice"))
        roundTrip(UserName.serializer(), UserName(""))
        roundTrip(UserName.serializer(), UserName("Special & Name"))
    }

    @Test
    fun testClassWithValueClasses() {
        roundTrip(
            ClassWithValueClass.serializer(),
            ClassWithValueClass(UserId(42), UserName("Bob"))
        )
    }

    @Test
    fun testListOfValueClasses() {
        roundTrip(
            ListSerializer(UserId.serializer()),
            listOf(UserId(1), UserId(2), UserId(3))
        )
        roundTrip(
            ListSerializer(UserName.serializer()),
            listOf(UserName("Alice"), UserName("Bob"))
        )
    }

    @Test
    fun testClassWithNestedList() {
        roundTrip(
            ClassWithListElem.serializer(),
            ClassWithListElem(listOf(1, 2, 3, 4, 5))
        )
        roundTrip(
            ClassWithListElem.serializer(),
            ClassWithListElem(listOf())
        )
    }

    // === Edge Cases ===

    @Test
    fun testSpecialCharactersInStrings() {
        // Characters that need URL encoding
        roundTrip(String.serializer(), "hello world")  // space
        roundTrip(String.serializer(), "a&b")          // ampersand
        roundTrip(String.serializer(), "a=b")          // equals
        roundTrip(String.serializer(), "a?b")          // question mark
        roundTrip(String.serializer(), "a#b")          // hash
        roundTrip(String.serializer(), "a/b")          // slash
        roundTrip(String.serializer(), "a%b")          // percent
    }

    @Test
    fun testClassWithSpecialCharsInValues() {
        roundTrip(
            SimpleClass.serializer(),
            SimpleClass("Name with & special = chars", 42)
        )
    }

    @Test
    fun testMaps() {
        roundTrip(
            MapSerializer(String.serializer(), SimpleClass.serializer()),
            mapOf(
                "first" to SimpleClass("first", 0),
                "second" to SimpleClass("second", 1)
            )
        )
        roundTrip(
            MapSerializer(Int.serializer(), SimpleClass.serializer()),
            mapOf(
                1 to SimpleClass("first", 0),
                2 to SimpleClass("second", 1)
            )
        )
        var failedCorrectly = false  // only support primitive keys
        try {
            roundTrip(
                MapSerializer(SimpleClass.serializer(), SimpleClass.serializer()),
                mapOf(
                    SimpleClass("first", 0) to SimpleClass("first", 0),
                    SimpleClass("second", 1) to SimpleClass("second", 1)
                )
            )
        } catch (e: Exception) {
            if (e.message == "UriFormat only supports maps with primitive-type keys.") {
                failedCorrectly = true
            }
            else throw e
        }
        if (!failedCorrectly) throw IllegalStateException("Did not fail")
    }

    @Test
    fun theKitchenSink() {
        roundTrip(KitchenSink.serializer(), KitchenSink())
    }
}
