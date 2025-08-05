package com.lightningkite.kiteui.views

import kotlin.coroutines.CoroutineContext

public class MutableCoroutineContext: CoroutineContext {
    public val list: ArrayList<CoroutineContext.Element> = ArrayList<CoroutineContext.Element>()
    public fun add(context: CoroutineContext) {
        context.fold(Unit) { _, element -> add(element) }
    }
    public fun add(element: CoroutineContext.Element) {
        list.add(element)
    }
    override fun <R> fold(initial: R, operation: (R, CoroutineContext.Element) -> R): R {
        return list.fold(initial, operation)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <E : CoroutineContext.Element> get(key: CoroutineContext.Key<E>): E? {
        for(index in list.lastIndex downTo 0) {
            return list[index][key] ?: continue
        }
        return null
    }

    public override fun minusKey(key: CoroutineContext.Key<*>): CoroutineContext {
        return MutableCoroutineContext().apply {
            this@MutableCoroutineContext.list.forEach { if(it[key] == null) this@apply.add(it) }
        }
    }
}