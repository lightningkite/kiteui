package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.setupAnalytics

fun setupExampleAppAnalytics() {
    // See the google-services-example.json file for where to pull these values from
    return
    setupAnalytics(
        apiKey = "API_KEY",
        applicationId = "APPLICATION_ID",
        gcmSenderId = "GCM_SENDER_ID",
        storageBucket = "STORAGE_BUCKET",
        projectId = "PROJECT_ID"
    )
}