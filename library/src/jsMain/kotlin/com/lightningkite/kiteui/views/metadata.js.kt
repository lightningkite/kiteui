package com.lightningkite.kiteui.views

import kotlinx.browser.window

actual fun ElementContext.bestGuessAtAppName(): String? =
    window.location.hostname.removeSuffix(".com")