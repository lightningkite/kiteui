package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.DragEvent

interface DropTargetDelegate {
    fun over(event: DragEvent): Boolean = true
    fun drop(event: DragEvent): Boolean
}