package com.lightningkite.kiteui.reactive.mapping

import com.lightningkite.kiteui.reactive.Writable
import kotlin.reflect.typeOf

// Helper function to translate writables into a form that can be used by number fields
inline fun <reified T: Number> Writable<T>.toDouble(): Writable<Double?> {
    val conversion: (Double?) -> Number = when(typeOf<T>()) {
        typeOf<Int>() -> { value: Double? -> value?.toInt() ?: 0 }
        typeOf<Float>() -> { value: Double? -> value?.toFloat() ?: 0f }
        typeOf<Double>() -> { value: Double? -> value ?: 0.0 }
        typeOf<Long>() -> { value: Double? -> value?.toLong() ?: 0L }
        typeOf<Short>() -> { value: Double? -> value?.toInt()?.toShort() ?: 0 }
        typeOf<Byte>() -> { value: Double? -> value?.toInt()?.toByte() ?: 0 }
        else -> throw IllegalArgumentException("Unsupported type")
    }

    return transform(
        get = { it.toDouble() },
        set = { conversion(it) as T }
    )
}