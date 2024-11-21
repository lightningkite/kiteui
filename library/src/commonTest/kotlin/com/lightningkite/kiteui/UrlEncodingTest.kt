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
import kotlin.test.assertTrue

class UrlEncodingTest {
    @Serializable
    data class Sample(val a: Int, val y: String? = null)

    data class TestCase<T>(val name: String, val serializer: KSerializer<T>, val sample: T)
    val testCases = listOf(
        TestCase("int", Int.serializer(), 18),
        TestCase("string_test", String.serializer(), "test"),
        TestCase("string_blank", String.serializer(), ""),
        TestCase("string_n_test", String.serializer().nullable, "test"),
        TestCase("string_n_null", String.serializer().nullable, null),
        TestCase("string_n_blank", String.serializer().nullable, ""),
        TestCase("sample", Sample.serializer(), Sample(42, "The Answer")),
    )

    @Test
    fun testMap() {
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

    inline fun <reified T> stringSubtest(item: T) {
        println("Testing '$item' (${item?.let { it::class }})")
        assertEquals(
            item,
            Properties.decodeFromString(Properties.encodeToString(item).also(::println))
        )
    }
    @Test fun testString() {
        stringSubtest(42)
        stringSubtest("String")
        stringSubtest("")
        stringSubtest<String?>(null)
        stringSubtest<String?>("")
        stringSubtest<String?>("null")
        stringSubtest(Sample(42, "The Answer"))
    }
}