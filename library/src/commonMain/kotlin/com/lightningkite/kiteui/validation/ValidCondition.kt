package com.lightningkite.kiteui.validation

import com.lightningkite.kiteui.reactive.ImmediateReadable
import com.lightningkite.kiteui.reactive.Readable
import com.lightningkite.kiteui.reactive.reactiveScope

// A Validated object that can be used as a general-purpose shared condition
class ValidCondition<T>(
    private val validation: suspend ValidationBuilder.() -> ValidationBuilder.Result
): Validated {
    private val context = SelfCancellingContext()

    private val _errors = SignallingSet<String>()
    override val errors: ImmediateReadable<Set<String>> = _errors.trackWith(context)

    override fun addError(error: String): Boolean = _errors.add(error)
    override fun removeError(error: String): Boolean = _errors.remove(error)
    override fun clearErrors() = _errors.clear()

    private val builder = ValidationBuilder(this)

    init {
        context.onStartup {
            context.reactiveScope {
                builder.validation()
            }
        }
    }
}