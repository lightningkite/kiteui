package com.lightningkite.kiteui.reactive.lensing

import com.lightningkite.kiteui.reactive.*


fun <T> Writable<T>.vet(vetter: (T) -> T): Writable<T> = validationLens(get = { it }, set = vetter)

fun <T> Writable<T>.validate(validation: (T) -> String?) = vet {
    validation(it)?.let { msg ->
        throw InvalidException(msg)
    }
    it
}

// General Helpers
fun <T: Any> Writable<T>.nullable(): Writable<T?> =
    object : Writable<T?>, Readable<T?> by this {
        override suspend fun set(value: T?) {
            if (value != null) this@nullable.set(value)
        }
    }

fun <T: Any> Writable<T>.nullableValidated(errorSummary: String? = null, errorDescription: String? = null): Writable<T?> =
    validationLens(
        get = { it },
        set = { it ?: throw InvalidException(errorSummary ?: "Cannot be null", errorDescription ?: "This field cannot be null") }
    )

fun <T: Any> Writable<T?>.notNull(default: T): Writable<T> = lens(
    get = { it ?: default },
    set = { it }
)

fun <T: Any> Writable<T?>.validateNotNull(fieldName: String? = null): Writable<T?> =
    vet {
        it ?: throw InvalidException(
            "Cannot be empty",
            fieldName?.let { "$it cannot be empty" } ?: "Cannot be empty"
        )
    }


// Number field helpers
fun Writable<Double>.nullableValidated(fieldName: String? = null): Writable<Double?> =
    validationLens(
        get = { it },
        set = { value ->
            value ?: throw InvalidException(
                "Cannot be empty",
                fieldName?.let { "$it cannot be empty" } ?: "Cannot be empty"
            )
        }
    )

fun Writable<Int>.toNullableDouble(): Writable<Double?> =
    lens(
        get = { it.toDouble() },
        set = { it?.toInt() ?: 0 }
    )

fun Writable<Int>.toNullableDoubleValidated(fieldName: String? = null): Writable<Double?> =
    validationLens(
        get = { it.toDouble() },
        set = { value ->
            if (value == null) throw InvalidException(
                "Cannot be empty",
                fieldName?.let { "$it cannot be empty" } ?: "Cannot be empty"
            )
            else {
                val converted = value.toInt()
                if (converted.toDouble() != value) throw InvalidException(
                    "Must be an integer",
                    fieldName?.let { "$it must be an integer" } ?: "Must be an integer"
                )

                converted
            }
        }
    )

// Text field helpers
fun Writable<String?>.nullToBlank(): Writable<String> = lens(
    get = { it ?: "" },
    set = { it.takeUnless { it.isBlank() } }
)

fun Writable<String>.validateNotBlank(): Writable<String> = validate {
    if (it.isBlank()) "Cannot be blank" else null
}
fun Writable<String?>.validateNotBlank(): Writable<String> = validationLens(
    get = { it ?: "" },
    set = { if (it.isBlank()) "Cannot be blank" else null }
)



