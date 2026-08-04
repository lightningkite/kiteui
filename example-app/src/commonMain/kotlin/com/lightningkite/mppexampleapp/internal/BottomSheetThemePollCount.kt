package com.lightningkite.mppexampleapp.internal

import com.lightningkite.reactive.core.Reactive

/**
 * Live count of currently-running bottom-sheet theme-poll loops
 * (`CoordinatorFrame.ios.kt`'s `BottomSheetThemePolling.activeCount`) - iOS only, since only iOS
 * needs that poll (see the comment on `startBottomSheetThemePoll` for why). Always 0 on every
 * other platform. Used by [IosMiscFixesPage] so the poll-loop-leak check doesn't rely on watching
 * CPU usage by eye.
 */
internal expect val bottomSheetThemePollCount: Reactive<Int>
