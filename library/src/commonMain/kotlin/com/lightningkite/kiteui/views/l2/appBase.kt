package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.navigation.bindToPlatform
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.kiteui.views.ContainerElement
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.ElementWriter
import com.lightningkite.kiteui.views.direct.CoordinatorFrame
import com.lightningkite.kiteui.views.direct.coordinatorFrame
import com.lightningkite.kiteui.views.lazyContextAddon

public var ElementContext.overlayFrame: ContainerElement by lazyContextAddon {
    throw IllegalStateException(
        "'ElementContext.overlayFrame' has not been initialized, it must be set before dialogs, overlays, and popovers can be used. " +
            "It is recommended to call 'appBase { ... }' at the root of your app to properly initialize overlayFrame, as well as many other properties."
    )
}
public var ElementContext.coordinatorFrame: CoordinatorFrame by lazyContextAddon {
    throw IllegalStateException(
        "'ElementContext.coordinatorFrame' has not been initialized. " +
            "It is recommended to call 'appBase { ... }' at the root of your app to properly initialize coordinatorFrame, as well as many other properties."
    )
}

public inline fun ElementWriter.appBase(navigator: PageNavigator, mainLayout: ContainerElement.() -> Unit) {
    coordinatorFrame {
        debugName = "appBase"
        context.pageNavigator = navigator
        navigator.bindToPlatform(context)
        context.overlayFrame = this
        context.coordinatorFrame = this
        mainLayout()
    }
}