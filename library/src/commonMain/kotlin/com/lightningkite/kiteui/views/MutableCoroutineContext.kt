package com.lightningkite.kiteui.views

import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * A [CoroutineContext] implementation that uses an [ArrayList] to manage its internal elements.
 *
 * This is *much* faster than the standard `kotlinx-coroutines` implementation, which uses a linked list.
 * */
public class MutableCoroutineContext: CoroutineContext {
    public val list = ArrayList<CoroutineContext.Element>()

    public fun add(context: CoroutineContext) {
        context.fold(Unit) { _, element -> add(element) }
    }
    public fun add(element: CoroutineContext.Element) {
        list.add(element)
    }

    override fun <R> fold(initial: R, operation: (R, CoroutineContext.Element) -> R): R {
        return list.fold(initial, operation)
    }

    override fun <E : CoroutineContext.Element> get(key: CoroutineContext.Key<E>): E? {
        for (index in list.lastIndex downTo 0) {
            return list[index][key] ?: continue
        }
        return null
    }

    override fun minusKey(key: CoroutineContext.Key<*>): CoroutineContext {
        val elements = this.list
        return buildCoroutineContext {
            elements.forEach { if (it[key] == null) add(it) }
        }
    }

    override fun plus(context: CoroutineContext): CoroutineContext {
        return if (context === EmptyCoroutineContext) this
        else buildCoroutineContext build@{
            this@build.list.addAll(this@MutableCoroutineContext.list)
            add(context)
        }
    }
}

public fun coroutineContextOf(vararg elements: CoroutineContext.Element): CoroutineContext {
    return MutableCoroutineContext().apply {
        list.addAll(elements)
    }
}

public fun mutableCoroutineContextOf(vararg elements: CoroutineContext.Element): MutableCoroutineContext {
    return MutableCoroutineContext().apply {
        list.addAll(elements)
    }
}

public fun coroutineContextOf(vararg elements: CoroutineContext): CoroutineContext {
    return MutableCoroutineContext().apply {
        for (e in elements) add(e)
    }
}

public inline fun buildCoroutineContext(setup: MutableCoroutineContext.() -> Unit): CoroutineContext {
    return MutableCoroutineContext().apply(setup)
}