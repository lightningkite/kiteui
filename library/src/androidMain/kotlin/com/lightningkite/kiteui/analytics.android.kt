package com.lightningkite.kiteui

import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.lightningkite.kiteui.views.AndroidAppContext

var firebaseApp: FirebaseApp? = null

actual fun setupAnalytics(
    apiKey: Map<Platform, String>,
    applicationId: Map<Platform, String>,
    gcmSenderId: String,
    storageBucket: String,
    projectId: String,
    measurementId: Map<Platform, String>?
) {
    val options = FirebaseOptions.Builder().apply {
        setApiKey(apiKey[Platform.Android] ?: return)
        setApplicationId(applicationId[Platform.Android] ?: return)
        setGcmSenderId(gcmSenderId)
        setStorageBucket(storageBucket)
        setProjectId(projectId)
        measurementId?.get(Platform.Android)?.let(this::setGaTrackingId)
    }.build()
    firebaseApp = FirebaseApp.initializeApp(AndroidAppContext.applicationCtx, options)
    val firebaseAnalytics = Firebase.analytics
}