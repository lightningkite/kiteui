package com.lightningkite.kiteui.reactive.lensing

import com.lightningkite.kiteui.reactive.ErrorState
import com.lightningkite.kiteui.reactive.Readable
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.reactive.waitForNotNull

// Validation lens shortcuts
typealias InvalidException = ErrorState.Invalid

fun <T> Writable<T>.vet(vetter: (T) -> T): Writable<T> = validationLens(get = { it }, set = vetter)

fun <T> Writable<T>.validate(
    errorSummary: String? = null,
    errorDescription: String? = null,
    validation: (T) -> Boolean
): Writable<T> =
    vet {
        if (validation(it)) it
        else throw InvalidException(it, errorSummary ?: "Condition not met", errorDescription ?: "Condition not met")
    }

fun <T> Writable<T>.validate(validation: (T) -> String?) =
    vet {
        val error = validation(it)
        if (error != null) throw InvalidException(it, error)
        else it
    }


// General Helpers
fun <T: Any> Writable<T>.nullable(): Writable<T?> =
    lens(
        get = { it },
        modify = { old, it -> it ?: old }
    )

fun <T: Any> Writable<T>.asNullable(errorSummary: String? = null, errorDescription: String? = null): Writable<T?> =
    validationLens(
        get = { it },
        set = { it ?: throw InvalidException(it, errorSummary ?: "Cannot be null", errorDescription ?: "This field cannot be null") }
    )

fun <T: Any> Writable<T?>.validateNotNull(fieldName: String? = null): Writable<T?> =
    validationLens(
        get = { it },
        set = { value ->
            value ?: throw InvalidException(
                value,
                "Cannot be empty",
                fieldName?.let { "$it cannot be empty" } ?: "Cannot be empty"
            )
        }
    )


// Number field helpers
fun Writable<Double>.asNullable(fieldName: String? = null): Writable<Double?> =
    validationLens(
        get = { it },
        set = { value ->
            value ?: throw InvalidException(
                value,
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

fun Writable<Int>.asDouble(fieldName: String? = null): Writable<Double?> =
    validationLens(
        get = { it.toDouble() },
        set = { value ->
            if (value == null) throw InvalidException(
                value,
                "Cannot be empty",
                fieldName?.let { "$it cannot be empty" } ?: "Cannot be empty"
            )
            else {
                val converted = value.toInt()
                if (converted.toDouble() != value) throw InvalidException(
                    value,
                    "Must be an integer",
                    fieldName?.let { "$it must be an integer" } ?: "Must be an integer"
                )

                converted
            }
        }
    )

// Text field helpers
fun Writable<String>.nullable(): Writable<String?> =
    lens(
        get = { it },
        set = { it ?: "" }
    )

fun Writable<String>.asNullable(fieldName: String? = null): Writable<String?> =
    validationLens(
        get = { it },
        set = { value ->
            value ?: throw InvalidException(
                value,
                "Cannot be blank",
                fieldName?.let { "$it cannot be blank" } ?: "Cannot be blank"
            )
        }
    )

