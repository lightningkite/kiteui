package com.lightningkite.kiteui

import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.logEvent
import com.google.firebase.ktx.Firebase
import com.lightningkite.kiteui.views.AndroidAppContext

var firebaseApp: FirebaseApp? = null

actual fun setupAnalytics(apiKey: String, applicationId: String, gcmSenderId: String, storageBucket: String, projectId: String) {
    val options = FirebaseOptions.Builder().apply {
        setApiKey(apiKey)
        setApplicationId(applicationId)
        setGcmSenderId(gcmSenderId)
        setStorageBucket(storageBucket)
        setProjectId(projectId)
    }.build()
    println("Initializing Firebase")
    firebaseApp = FirebaseApp.initializeApp(AndroidAppContext.applicationCtx, options)
    val firebaseAnalytics = Firebase.analytics
    firebaseAnalytics.logEvent("my_custom_event") {
        param(FirebaseAnalytics.Param.VALUE, 837)
    }
}