@file:OptIn(ExperimentalSerializationApi::class)

package com.lightningkite.kiteui.navigation

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.properties.Properties

var DefaultSerializersModule: SerializersModule = EmptySerializersModule()
    set(value) {
        field = value
        DefaultJsonCurrent = Json {
            serializersModule = DefaultSerializersModule
            ignoreUnknownKeys = true
        }
        UrlPropertiesCurrent = Properties(DefaultSerializersModule)
        DefaultUriFormatCurrent = UriFormat(DefaultSerializersModule)
    }

private var DefaultJsonCurrent: Json = Json {
    serializersModule = DefaultSerializersModule
    ignoreUnknownKeys = true
}

val DefaultJson: Json get() = DefaultJsonCurrent
private var UrlPropertiesCurrent: Properties = Properties(DefaultSerializersModule)
val UrlProperties: Properties get() = UrlPropertiesCurrent

private var DefaultUriFormatCurrent: UriFormat = UriFormat(DefaultSerializersModule)
val DefaultUriFormat: UriFormat get() = DefaultUriFormatCurrent

fun <K, V> mapOfNotNull(vararg entries: Pair<K, V>?): Map<K, V> =
    entries.filterNotNull().toMap()