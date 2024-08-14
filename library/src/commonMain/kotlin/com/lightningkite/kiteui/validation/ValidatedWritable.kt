package com.lightningkite.kiteui.validation

import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.centered
import com.lightningkite.kiteui.views.direct.*

class ValidationBuilder(private val target: Validated) {
    sealed interface Result {
        data object Invalid: Result
        data object Valid: Result
    }

    fun invalid(reason: String): Result {
        target.addError(reason)
        return Result.Invalid
    }
    fun valid(): Result {
        target.clearErrors()
        return Result.Valid
    }
}

class ValidatedWritable<T>(
    private val wraps: Writable<T>,
    private val validation: suspend ValidationBuilder.(T) -> ValidationBuilder.Result
): Validated, Writable<T> by wraps {
    private val internalErrors = SignallingSet<String>()
    override val errors: Readable<Set<String>> get() = internalErrors

    override fun addError(error: String): Boolean = internalErrors.add(error)
    override fun removeError(error: String): Boolean = internalErrors.remove(error)
    override fun clearErrors() = internalErrors.clear()

    private val builder = ValidationBuilder(this)

    override suspend fun set(value: T) {
        builder.validation(value)
        wraps set value
    }
}

fun <T> Writable<T>.validate(
    validation: suspend ValidationBuilder.(T) -> ValidationBuilder.Result
) = ValidatedWritable(this, validation)

fun <T, V> Writable<T>.withTransform(
    inputTransform: suspend (V) -> T,
    outputTransform: suspend (T) -> V
): Writable<V> = shared {
    outputTransform(this@withTransform.await())
}.withWrite {
    this@withTransform set inputTransform(it)
}


fun RView.example() {
    val prop = Property(10)
        .withTransform<Int, Double?>(
            inputTransform = { it?.toInt() ?: 0 },
            outputTransform = { it.toDouble() }
        )
        .validate {
            if (it == null) invalid("Cannot be empty")
            else if (it.toString().contains('.')) invalid("Must be an integer")
            else if (it.toInt() % 2 != 0) invalid("Must be an even number")
            else valid()
        }

    row {
        val failedSubmission = Property(false)

        numberField {
            hint = "Enter an even number"

            validate(prop)

            ::displayValidation { failedSubmission() }

            content bind prop
        }

        button {
            centered - text("Submit")

            ::enabled {
                if (failedSubmission()) {
                    prop.valid()
                }
                else true
            }

            onClick {
                if (prop.isInvalid()) {
                    failedSubmission.value = true
                    return@onClick
                }

                // Does something when valid
            }
        }
    }
}