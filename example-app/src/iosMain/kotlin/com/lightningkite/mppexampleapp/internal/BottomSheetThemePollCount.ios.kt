package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.views.direct.BottomSheetThemePolling
import com.lightningkite.reactive.core.Reactive

internal actual val bottomSheetThemePollCount: Reactive<Int> get() = BottomSheetThemePolling.activeCount
