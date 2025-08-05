package com.lightningkite.kiteui.navigation

import kotlinx.serialization.Serializable


@Serializable
public data class Wrapper<T>(val value: T)