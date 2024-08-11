package com.lightningkite.kiteui.validation

import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.centered
import com.lightningkite.kiteui.views.direct.button
import com.lightningkite.kiteui.views.direct.numberField
import com.lightningkite.kiteui.views.direct.textField
import kotlin.jvm.JvmInline

fun <T, V> Writable<T>.withTransform(
    inputTransform: suspend (V) -> T,
    outputTransform: suspend (T) -> V
): Writable<V> = shared {
    outputTransform(this@withTransform.await())
}.withWrite {
    this@withTransform set inputTransform(it)
}

sealed interface ValidationResult {
    data object Valid: ValidationResult
    class Invalid(val reason: String): ValidationResult
}

@JvmInline
value class ValidationUtils(private val validator: Validator) {
    val errors get() = validator.errors

    fun invalid(reason: String): ValidationResult.Invalid {
        validator.addError(reason)
        return ValidationResult.Invalid(reason)
    }

    fun valid(): ValidationResult.Valid {
        validator.clearErrors()
        return ValidationResult.Valid
    }
}

interface Validated {
    val errors: Readable<Set<String>>
    var validator: Validator

    fun bindTo(validator: Validator) {
        this.validator = validator
    }
}

class ValidatedWritable<T>(
    private val wraps: Writable<T>,
    validator: Validator,
    private val validation: ValidationUtils.(T) -> ValidationResult
): Validated, Writable<T> by wraps {
    private var checker = ValidationUtils(validator)
    override var validator: Validator = validator
        set(value) {
            field = value
            checker = ValidationUtils(value)
        }

    override val errors: Readable<Set<String>> get() = checker.errors

    override suspend fun set(value: T) {
        checker.validation(value)
        wraps.set(value)
    }
}

fun <T> Writable<T>.validate(
    validator: Validator,
    validation: ValidationUtils.(T) -> ValidationResult
) = ValidatedWritable(this, validator, validation)

fun RView.example() {



    val prop = Property(10)
        .withTransform<Int, Double?>(
            inputTransform = {
                if (it == null) throw Invalid("Cannot be empty")
                else if (it.toString().contains('.')) throw Invalid("Must be an integer")
                else if (it.toInt() % 2 != 0) throw Invalid("Must be an even number")
                else it.toInt()
             },
            outputTransform = { it.toDouble() }
        )
//        .validate {
//            if (it == null) invalid("Cannot be empty")
//            else if (it.toString().contains('.')) invalid("Must be an integer")
//            else if (it.toInt() % 2 != 0) invalid("Must be an even number")
//            else valid()
//        }

    numberField {
        hint = "Enter an even number"

        content bind prop
    }
}