package com.lightningkite.mppexampleapp.internal

import com.lightningkite.reactive.core.Constant
import com.lightningkite.reactive.core.Reactive

// JS and JVM/SSR don't have a bottom-sheet theme-poll loop at all - nothing to count here.
internal actual val bottomSheetThemePollCount: Reactive<Int> = Constant(0)
