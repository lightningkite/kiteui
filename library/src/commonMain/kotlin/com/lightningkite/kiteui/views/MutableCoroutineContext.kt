package com.lightningkite.kiteui.views

import kotlin.coroutines.CoroutineContext

class MutableCoroutineContext: CoroutineContext {
    val list = ArrayList<CoroutineContext.Element>()
    fun add(context: CoroutineContext) {
        context.fold(Unit) { _, element -> add(element) }
    }
    fun add(element: CoroutineContext.Element) {
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

    override fun minusKey(key: CoroutineContext.Key<*>): CoroutineContext {
        return MutableCoroutineContext().apply {
            this@MutableCoroutineContext.list.forEach { if(it[key] == null) this@apply.add(it) }
        }
    }
}