package com.lightningkite.kiteui.utils

import com.lightningkite.reactive.core.Release

public class LiveRegistry<T>(list: List<T> = emptyList()) {
    private val registry = ArrayList<T>(list)

    public fun register(value: T): Release {
        registry.add(value)
        return {
            val i = registry.indexOf(value)
            if (i != -1) registry.removeAt(i)
        }
    }

    public fun snapshot(): List<T> = registry.toList()
}