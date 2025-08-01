@file:OptIn(ExperimentalSerializationApi::class)

package com.lightningkite.kiteui

import com.lightningkite.kiteui.navigation.decodeFromString
import com.lightningkite.kiteui.navigation.decodeFromStringMap
import com.lightningkite.kiteui.navigation.encodeToString
import com.lightningkite.kiteui.navigation.encodeToStringMap
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.properties.Properties
import kotlin.test.Test
import kotlin.test.assertEquals

@Serializable
public data class SampleComplexType(val a: Int, val y: String? = null)

public class UrlEncodingTest {

    public data class TestCase<T>(val name: String, val serializer: KSerializer<T>, val sample: T)
    public val testCases = listOf(
        TestCase("int", Int.serializer(), 18),
        TestCase("string_test", String.serializer(), "test"),
        TestCase("string_blank", String.serializer(), ""),
        TestCase("string_n_test", String.serializer().nullable, "test"),
        TestCase("string_n_null", String.serializer().nullable, null),
        TestCase("string_n_blank", String.serializer().nullable, ""),
        TestCase("sample", SampleComplexType.serializer(), SampleComplexType(42, "The Answer")),
    )

    @Test
    public fun testMap() {
        val out = HashMap<String, String>()
        val properties = Properties
        testCases.forEach {
            properties.encodeToStringMap(it.serializer as KSerializer<Any?>, it.sample, it.name, out)
        }
        println(out)
        testCases.forEach {
            assertEquals(it.sample, properties.decodeFromStringMap(it.serializer, it.name, out))
        }
    }

    fun <T> stringSubtest(serializer: KSerializer<T>, item: T) {
        println("Testing '$item' (${item?.let { it::class }})")
        assertEquals(
            item,
            Properties.decodeFromString(serializer, Properties.encodeToString(serializer, item).also(::println))
        )
    }
    @Test fun testString() {
        stringSubtest(Int.serializer(), 42)
        stringSubtest(String.serializer(), "String")
        stringSubtest(String.serializer(), "")
        stringSubtest(String.serializer().nullable, null)
        stringSubtest(String.serializer().nullable, "")
        stringSubtest(String.serializer().nullable, "null")
        stringSubtest(SampleComplexType.serializer(), SampleComplexType(42, "The Answer"))
    }
}