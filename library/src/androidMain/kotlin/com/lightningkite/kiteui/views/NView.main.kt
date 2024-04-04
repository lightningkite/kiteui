package com.lightningkite.kiteui.views

import android.content.res.Configuration

actual val NContext.darkMode: Boolean?
    get() = when(resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
        Configuration.UI_MODE_NIGHT_NO -> false
        Configuration.UI_MODE_NIGHT_YES -> true
        else -> null
    }