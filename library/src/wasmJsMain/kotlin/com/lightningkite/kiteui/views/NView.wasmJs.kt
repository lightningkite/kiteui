package com.lightningkite.kiteui.views

import kotlinx.browser.window

actual val NContext.darkMode: Boolean? get() = window.matchMedia("(prefers-color-scheme: dark)").matches