package com.lightningkite.kiteui.views

actual fun ElementContext.bestGuessAtAppName(): String? =
    activityOrNull?.title?.toString()
