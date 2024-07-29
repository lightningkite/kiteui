package com.lightningkite.kiteui.reactive

import kotlin.random.Random

class BasicListenable : Listenable {
    private val listeners = ArrayList<() -> Unit>()
    override fun addListener(listener: () -> Unit): () -> Unit {
        listeners.add(listener)
        return {
            val pos = listeners.indexOfFirst { it === listener }
            if (pos != -1) {
                listeners.removeAt(pos)
            }
        }
    }

    fun invokeAll() {
        listeners.invokeAllSafe()
    }

    private var id = Random.nextInt(0, 100000)
    override fun toString(): String {
        return "BasicListenable($id)"
    }
}

abstract class BaseListenable : Listenable {
    private val listeners = ArrayList<() -> Unit>()
    override fun addListener(listener: () -> Unit): () -> Unit {
        listeners.add(listener)
        return {
            val pos = listeners.indexOfFirst { it === listener }
            if (pos != -1) {
                listeners.removeAt(pos)
            }
        }
    }

    fun invokeAll() {
        listeners.invokeAllSafe()
    }
}