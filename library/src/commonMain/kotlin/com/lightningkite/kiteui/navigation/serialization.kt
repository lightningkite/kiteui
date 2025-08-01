package com.lightningkite.kiteui.navigation

import kotlinx.serialization.Serializable


@Serializable
data class Wrapper<T>(val value: T)