package com.lightningkite.kiteui.models

interface DropTargetDelegate {
    fun over(event: DragEvent): Boolean = true

    fun enter(event: DragEvent): Boolean = true
    fun exit(event: DragEvent): Boolean = true

    fun end(event: DragEvent): Boolean = true

    fun drop(event: DragEvent): Boolean
}