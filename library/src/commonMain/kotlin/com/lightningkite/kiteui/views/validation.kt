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

fun ReactiveContext.issues(readables: List<Readable<*>>): List<ReadableState.Issue> =
    readables.mapNotNull { r -> r.state { it.issue } }
fun ReactiveContext.issues(vararg readables: Readable<*>) = issues(readables.toList())

fun RView.validates(readables: List<Readable<*>>) {
    dynamicTheme {
        val errors = issues(readables)
        if (errors.any { it is ReadableState.Exception }) DangerSemantic
        else if (errors.any { it is ReadableState.Invalid<*> }) InvalidSemantic
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
            if (it is ReadableState.Issue) it.summary
            else null
        }
    }
fun ReactiveContext.errorMessages(vararg readables: Readable<*>) = errorMessages(readables.toList())