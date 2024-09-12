package com.lightningkite.kiteui

import com.lightningkite.kiteui.reactive.Readable
import com.lightningkite.kiteui.views.ViewWriter

expect fun ViewWriter.setupAnalytics(
    apiKey: Map<Platform, String>,
    applicationId: Map<Platform, String>,
    gcmSenderId: String,
    storageBucket: String,
    projectId: String,
    measurementId: Map<Platform, String>? = null,
    userId: Readable<String>? = null
)