package com.lightningkite.kiteui.views

public actual fun ElementContext.bestGuessAtAppName(): String? =
    activityOrNull?.title?.toString()
