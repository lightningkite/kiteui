package com.lightningkite.kiteui.validation

import com.lightningkite.kiteui.reactive.Readable

interface Validated {
    val errors: Readable<Set<String>>

    /**
     * Adds an error
     * @return `true` if the error was added, `false` if the error was already in the set
     * */
    fun addError(error: String): Boolean

    /**
     * Removes the specified error
     * @return `true` if the error was removed, or `false` if it was not present in the set.
     * */
    fun removeError(error: String): Boolean

    fun clearErrors()

    suspend fun valid(): Boolean {
        var valid = true
        return errors.awaitCondition { newErrors ->
            if (valid and newErrors.isNotEmpty()) {
                valid = false
                true
            }
            else if (!valid and newErrors.isEmpty()) {
                valid = true
                true
            }
            else false
        }.isEmpty()
    }
}

suspend fun Validated.invalid(): Boolean = !valid()