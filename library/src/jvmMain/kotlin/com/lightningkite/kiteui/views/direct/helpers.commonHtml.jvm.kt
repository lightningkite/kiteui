package com.lightningkite.kiteui.views.direct

import com.lightningkite.signal.Listenable
import com.lightningkite.kiteui.views.HtmlElementLike


actual fun HtmlElementLike.resizeObserver(): Listenable = Listenable.Never
actual fun HtmlElementLike.mutationObserver(recursive: Boolean): Listenable = Listenable.Never