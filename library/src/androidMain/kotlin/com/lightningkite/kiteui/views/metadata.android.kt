package com.lightningkite.kiteui.views

private actual fun ElementContext.bestGuessAtAppName(): String? =
    activityOrNull?.title?.toString()
