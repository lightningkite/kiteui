package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.DangerSemantic
import com.lightningkite.kiteui.models.InvalidSemantic
import com.lightningkite.kiteui.models.WarningSemantic
import com.lightningkite.kiteui.reactive.ErrorState
import com.lightningkite.kiteui.reactive.Readable
import com.lightningkite.kiteui.reactive.ReadableState
import com.lightningkite.kiteui.reactive.state

suspend fun allValid(readables: List<Readable<*>>): Boolean =
    readables.all { r -> r.state { it.invalid == null } }

suspend fun allValid(vararg readables: Readable<*>): Boolean = allValid(readables.toList())

suspend fun errors(readables: List<Readable<*>>): List<ErrorState> =
    readables.mapNotNull { r -> r.state { it.error } }

fun RView.validates(readables: List<Readable<*>>) {
    dynamicTheme {
        val errors = errors(readables)
        if (errors.any { it is ErrorState.ThrownException }) DangerSemantic
        else if (errors.any { it is ErrorState.Invalid<*> }) InvalidSemantic
        else if (errors.any { it is ErrorState.Warning<*> }) WarningSemantic
        else null
    }
}

fun RView.validates(vararg readables: Readable<*>) = validates(readables.toList())

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