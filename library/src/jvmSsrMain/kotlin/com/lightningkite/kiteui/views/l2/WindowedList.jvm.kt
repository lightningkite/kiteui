package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.ExperimentalKiteUi
import com.lightningkite.kiteui.views.ElementContext

@ExperimentalKiteUi
internal actual fun platformWindowedList(context: ElementContext): WindowedList = Recycler2WindowedList(Recycler2(context))
