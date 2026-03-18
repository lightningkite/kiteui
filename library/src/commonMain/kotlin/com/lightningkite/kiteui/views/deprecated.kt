package com.lightningkite.kiteui.views

@Deprecated("Wrong import; this has moved", ReplaceWith("launch", "com.lightningkite.reactive.launch"), DeprecationLevel.ERROR) val launch = Unit
@Deprecated("Wrong import; this has moved", ReplaceWith("reactiveScope", "com.lightningkite.reactive.context.reactiveScope"), DeprecationLevel.ERROR) val reactiveScope = Unit

@Deprecated("Wrong import; this has moved", ReplaceWith("DropTargetDelegate", "com.lightningkite.kiteui.models.DropTargetDelegate")) typealias DropTargetDelegate = com.lightningkite.kiteui.models.DropTargetDelegate

@Deprecated("Renamed", ReplaceWith("RContextCommonCode")) typealias RContextHelper = ElementContextCommonCode

@Deprecated("Renamed", ReplaceWith("debugName"))
var RView.testId: String?
    get() = debugName
    set(value) {
        debugName = value
    }

@Deprecated("No longer supported, set debugName directly on the element", level = DeprecationLevel.ERROR)
operator fun String.minus(writer: ViewWriter): ViewWriter = writer.also {
    it.beforeNextElementSetup {
        debugName = this@minus
    }
}