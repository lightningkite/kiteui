package com.lightningkite.kiteui.reactive.lensing

import com.lightningkite.kiteui.reactive.ErrorState
import com.lightningkite.kiteui.reactive.Writable

// Validation lens shortcuts
fun <T> Writable<T>.vet(vetter: (T) -> T): Writable<T> = validationLens(get = { it }, set = vetter)

fun <T> Writable<T>.validate(
    errorSummary: String? = null,
    errorDescription: String? = null,
    validation: (T) -> Boolean
): Writable<T> =
    vet {
        if (validation(it)) it
        else throw ErrorState.Invalid(it, errorSummary ?: "Condition not met", errorDescription ?: "Condition not met")
    }

fun <T> Writable<T>.validate(validation: (T) -> String?) =
    vet {
        val error = validation(it)
        if (error != null) throw ErrorState.Invalid(it, error)
        else it
    }


// General Helpers
fun <T: Any> Writable<T>.nullable(): Writable<T?> =
    lens(
        get = { it },
        modify = { old, it -> it ?: old }
    )

fun <T: Any> Writable<T>.asNullable(errorSummary: String?, errorDescription: String?): Writable<T?> =
    validationLens(
        get = { it },
        set = { it ?: throw ErrorState.Invalid(it, errorSummary ?: "Cannot be null", errorDescription ?: "This field cannot be null") }
    )


// Number field helpers
fun Writable<Double>.asNullable(fieldName: String?): Writable<Double?> =
    validationLens(
        get = { it },
        set = { value ->
            value ?: throw ErrorState.Invalid(
                value,
                "Cannot be empty",
                fieldName?.let { "$it cannot be empty" } ?: "Cannot be empty"
            )
        }
    )

fun Writable<Int>.toDouble(): Writable<Double?> =
    lens(
        get = { it.toDouble() },
        set = { it?.toInt() ?: 0 }
    )

fun Writable<Int>.asDouble(fieldName: String? = null): Writable<Double?> =
    validationLens(
        get = { it.toDouble() },
        set = { value ->
            if (value == null) throw ErrorState.Invalid(
                value,
                "Cannot be empty",
                fieldName?.let { "$it cannot be empty" } ?: "Cannot be empty"
            )
            else {
                val converted = value.toInt()
                if (converted.toDouble() != value) throw ErrorState.Invalid(
                    value,
                    "Must be an integer",
                    fieldName?.let { "$it must be an integer" } ?: "Must be an integer"
                )

                converted
            }
        }
    )