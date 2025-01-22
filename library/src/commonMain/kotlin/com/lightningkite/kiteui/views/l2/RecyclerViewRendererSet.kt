package com.lightningkite.kiteui.views.l2

interface RecyclerViewRendererSet<in T, out ID> {
    fun id(item: T): ID
    fun renderer(item: T): RecyclerViewRenderer<T>

    object Empty : RecyclerViewRendererSet<Any?, Unit> {
        override fun id(item: Any?): Unit = Unit
        override fun renderer(item: Any?): RecyclerViewRenderer<Any?> = RecyclerViewRenderer.Blank
    }
}