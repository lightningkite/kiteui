package com.lightningkite.kiteui.reactive

import com.lightningkite.kiteui.models.WindowStatistics

expect object AnimationFrame: Listenable
expect object WindowInfo: ImmediateReadable<WindowStatistics>
expect object InForeground: ImmediateReadable<Boolean>
expect object SoftInputOpen: ImmediateReadable<Boolean>