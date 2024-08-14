package com.lightningkite.kiteui.validation

import com.lightningkite.kiteui.reactive.*

/**
 * Tracks validation through a hierarchy of sub-validators and tracked validates
* */
interface Validator {
    val subValidators: Readable<List<Validator>>
    val validations: Readable<List<Readable<Any?>>>

    fun validate(readable: Readable<Any?>)
    fun remove(readable: Readable<Any?>)
    fun clearValidations()

    /** Creates a sub-validator */
    fun sub(): Validator
    fun remove(validator: Validator)

    val errors: Readable<List<ErrorState.Invalid>> get() = shared {
        val subErrors = subValidators().flatMap { it.errors() }
        val validationErrors = validations().mapNotNull { it.isInvalid() }
        subErrors + validationErrors
    }

    val allValid: Readable<Boolean> get() = shared { errors().isEmpty() }

    open class Basic: Validator {
        private val _children = SignalingList<Basic>()
        override val subValidators: ImmediateReadable<List<Validator>> get() = _children

        private val _validations = SignalingList<Readable<Any?>>()
        override val validations: ImmediateReadable<List<Readable<Any?>>> get() = _validations

        override fun validate(readable: Readable<Any?>) { _validations.add(readable) }

        override fun remove(readable: Readable<Any?>) { _validations.remove(readable) }

        override fun clearValidations() { _validations.clear() }

        override fun sub(): Validator = Basic().also { _children.add(it) }

        override fun remove(validator: Validator) {
            if (validator is Basic) {
                _children.remove(validator)
            }
        }
    }

    open class Lazy: Validator {
        private val _children by lazy { SignalingList<Validator>() }
        override val subValidators: ImmediateReadable<List<Validator>> get() = _children

        private val _validations by lazy { SignalingList<Readable<Any?>>() }
        override val validations: ImmediateReadable<List<Readable<Any?>>> get() = _validations

        override fun validate(readable: Readable<Any?>) { _validations.add(readable) }

        override fun remove(readable: Readable<Any?>) { _validations.remove(readable) }

        override fun clearValidations() { _validations.clear() }

        override fun sub(): Validator = Lazy().also { _children.add(it) }

        override fun remove(validator: Validator) {
            if (validator is Lazy) {
                _children.remove(validator)
            }
        }

        override val errors: Readable<List<ErrorState.Invalid>> by lazy {
            shared {
                val subErrors = subValidators().flatMap { it.errors() }
                val validationErrors = validations().mapNotNull { it.isInvalid() }
                subErrors + validationErrors
            }
        }

        override val allValid: Readable<Boolean> by lazy { shared { errors().isEmpty() } }
    }
}
