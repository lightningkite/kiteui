package com.lightningkite.kiteui.validation

import com.lightningkite.kiteui.reactive.*

/**
 * Tracks validation through a hierarchy of sub-validators and tracked validates
* */
interface Validator {
    val subValidators: Readable<List<Validator>>
    val validations: Readable<List<Validated>>

    fun validate(validated: Validated)
    fun remove(validated: Validated)
    fun clearValidations()

    /** Creates a sub-validator */
    fun sub(): Validator
    fun remove(validator: Validator)

    val errors: Readable<Set<String>> get() = shared {
        (subValidators.await().flatMap { it.errors() } + validations.await().flatMap { it.errors() }).toSet()
    }

    val allValid: Readable<Boolean> get() = shared {
        subValidators.await().all { it.allValid() } and validations.await().all { it.valid() }
    }

    open class Basic: Validator {
        private val _children = SignalingList<Basic>()
        override val subValidators: ImmediateReadable<List<Validator>> get() = _children

        private val _validations = SignalingList<Validated>()
        override val validations: ImmediateReadable<List<Validated>> get() = _validations

        override fun validate(validated: Validated) { _validations.add(validated) }

        override fun remove(validated: Validated) { _validations.remove(validated) }

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

        private val _validations by lazy { SignalingList<Validated>() }
        override val validations: ImmediateReadable<List<Validated>> get() = _validations

        override fun validate(validated: Validated) { _validations.add(validated) }

        override fun remove(validated: Validated) { _validations.remove(validated) }

        override fun clearValidations() { _validations.clear() }

        override fun sub(): Validator = Lazy().also { _children.add(it) }

        override fun remove(validator: Validator) {
            if (validator is Lazy) {
                _children.remove(validator)
            }
        }

        override val errors: Readable<Set<String>> by lazy {
            shared {
                (subValidators.await().flatMap { it.errors() } + validations.await().flatMap { it.errors() }).toSet()
            }
        }

        override val allValid: Readable<Boolean> by lazy {
            shared {
                subValidators.await().all { it.allValid() } and validations.await().all { it.valid() }
            }
        }
    }
}
