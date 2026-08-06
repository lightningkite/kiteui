package com.lightningkite.mppexampleapp.internal

import com.lightningkite.reactive.core.Constant
import com.lightningkite.reactive.core.Reactive

// Android's CoordinatorFrame doesn't need a theme-poll loop (it gets real theme-change
// notifications through the normal view tree), so there's nothing to count here.
internal actual val bottomSheetThemePollCount: Reactive<Int> = Constant(0)
