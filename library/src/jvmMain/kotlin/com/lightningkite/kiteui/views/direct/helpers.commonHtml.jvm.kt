package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.reactive.Listenable
import com.lightningkite.kiteui.views.HtmlElementLike


actual fun HtmlElementLike.resizeObserver(): Listenable = Listenable.Never
