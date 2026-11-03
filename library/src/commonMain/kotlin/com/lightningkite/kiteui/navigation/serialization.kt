@file:OptIn(ExperimentalSerializationApi::class)

package com.lightningkite.kiteui.navigation

import com.lightningkite.kotlinx.serialization.uri.UriFormat
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.properties.Properties

public var DefaultSerializersModule: SerializersModule = EmptySerializersModule()
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

public val DefaultJson: Json get() = DefaultJsonCurrent
private var UrlPropertiesCurrent: Properties = Properties(DefaultSerializersModule)
public val UrlProperties: Properties get() = UrlPropertiesCurrent

private var DefaultUriFormatCurrent: UriFormat = UriFormat(DefaultSerializersModule)
public val DefaultUriFormat: UriFormat get() = DefaultUriFormatCurrent