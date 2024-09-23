package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.DangerSemantic
import com.lightningkite.kiteui.models.InvalidSemantic
import com.lightningkite.kiteui.models.WarningSemantic
import com.lightningkite.kiteui.reactive.*

suspend fun allValid(readables: List<Readable<*>>): Boolean =
    readables.all { r -> r.state { it.invalid == null } }
suspend fun allValid(vararg readables: Readable<*>): Boolean = allValid(readables.toList())

fun ReactiveContext.allValid(readables: List<Readable<*>>): Boolean =
    readables.all { r -> r.state { it.invalid == null } }
fun ReactiveContext.allValid(vararg readables: Readable<*>): Boolean = allValid(readables.toList())

fun ReactiveContext.errors(readables: List<Readable<*>>): List<ErrorState> =
    readables.mapNotNull { r -> r.state { it.error } }
fun ReactiveContext.errors(vararg readables: Readable<*>) = errors(readables.toList())

fun RView.validates(readables: List<Readable<*>>) {
    dynamicTheme {
        val errors = errors(readables)
        if (errors.any { it is ErrorState.ThrownException }) DangerSemantic
        else if (errors.any { it is ErrorState.Invalid<*> }) InvalidSemantic
        else if (errors.any { it is ErrorState.Warning<*> }) WarningSemantic
        else null
    }
}
fun RView.validates(vararg readables: Readable<*>) {
    if (readables.isNotEmpty()) validates(readables.toList())
}

fun <T> Readable<Readable<T>>.flattenState(): Readable<T> =
    object : Readable<T> {
        override val state: ReadableState<T>
            get() {
                val source = this@flattenState.state
                return if (source.success) source.get().state
                else source as ReadableState<T>
            }
        override fun addListener(listener: () -> Unit): () -> Unit = this@flattenState.addListener(listener)
    }

fun ReactiveContext.errorMessages(readables: List<Readable<*>>): List<String> =
    readables.mapNotNull { r ->
        r.state {
            val raw = it.raw
            when (raw) {
                is ErrorState.Invalid<*> -> raw.errorDescription
                is ErrorState.Warning<*> -> raw.errorDescription
                else -> null
            }
        }
    }
fun ReactiveContext.errorMessages(vararg readables: Readable<*>) = errorMessages(readables.toList())