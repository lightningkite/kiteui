package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.HtmlElementLike
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*


public actual fun HtmlElementLike.resizeObserver(): Listenable = Listenable.Never
public actual fun HtmlElementLike.mutationObserver(recursive: Boolean): Listenable = Listenable.Never