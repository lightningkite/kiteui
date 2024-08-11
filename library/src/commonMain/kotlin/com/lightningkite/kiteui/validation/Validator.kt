package com.lightningkite.kiteui.validation

import com.lightningkite.kiteui.reactive.*

/**
 * Tracks validation through a hierarchy of sub-validators.
 *
 * A Validator should relay all of its sub-validator errors
* */
interface Validator {
    val errors: Readable<Set<String>>

    fun addError(cause: String)
    fun removeError(cause: String)
    fun clearErrors()

    /**
     * Creates a sub-validator
    * */
    fun sub(): Validator


    suspend fun allValid(): Boolean = errors.await().isEmpty()

    fun <T> Writable<T>.validate(
        validation: ValidationUtils.(T) -> ValidationResult
    ) = validate(this@Validator, validation)


    open class Basic: Validator {
        private val children = Property(emptySet<Basic>())

        private val myErrors = Property(emptySet<String>())

        override val errors: Readable<Set<String>> = shared {
            myErrors() + children().flatMap { it.errors() }
        }

        override fun addError(cause: String) { myErrors.value += cause }
        override fun removeError(cause: String) {
            myErrors.value -= cause
            children.value.forEach { it.myErrors.value -= cause }
        }
        override fun clearErrors() {
            myErrors.value = emptySet()
            children.value.forEach { it.clearErrors() }
        }

        override fun sub(): Validator = Basic().also { children.value += it }
    }

    class NonSuspending(parent: NonSuspending? = null): Validator {
        private val children = ArrayList<NonSuspending>()

        private val myErrors = mutableSetOf<String>()
        override val errors = Property(emptySet<String>())

        override fun addError(cause: String) {
            myErrors.add(cause)
            errors.value += cause
        }
        override fun removeError(cause: String) {
            myErrors.remove(cause)
            errors.value -= cause
            children.forEach { it.removeError(cause) }
        }
        override fun clearErrors() {
            myErrors.clear()
            errors.value = emptySet()
        }

        private fun recalculateValidity() {
            val childErrors = mutableSetOf<String>()
            for (child in children) {
                childErrors.addAll(child.errors.value)
            }

            errors.value = myErrors + childErrors
        }

        override fun sub(): Validator =
            NonSuspending(parent = this).also {
                children.add(it)
                recalculateValidity()
            }

        init {
            parent?.let {
                errors.addListener { it.recalculateValidity() }
            }
        }
    }
}