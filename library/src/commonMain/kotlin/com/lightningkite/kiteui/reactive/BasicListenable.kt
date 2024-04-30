package com.lightningkite.kiteui.reactive

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
}