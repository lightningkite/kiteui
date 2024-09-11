package com.lightningkite.kiteui

import com.lightningkite.kiteui.utils.FirebaseOptions
import com.lightningkite.kiteui.utils.getAnalytics
import com.lightningkite.kiteui.utils.initializeApp

actual fun setupAnalytics(
    apiKey: Map<Platform, String>,
    applicationId: Map<Platform, String>,
    gcmSenderId: String,
    storageBucket: String,
    projectId: String,
    measurementId: Map<Platform, String>?
) {
    println("Getting ready to initialize analytics on web")
    val firebaseConfig: dynamic = object {}
    firebaseConfig["apiKey"] = apiKey[Platform.Web]
    firebaseConfig["appId"] = applicationId[Platform.Web]
    firebaseConfig["measurementId"] = measurementId?.get(Platform.Web)
    firebaseConfig["messagingSenderId"] = gcmSenderId
    firebaseConfig["projectId"] = projectId
    firebaseConfig["storageBucket"] = storageBucket
    println("Initializing Firebase with $firebaseConfig")
    val app = initializeApp(firebaseConfig.unsafeCast<FirebaseOptions>(), null)
    println("Initializing analytics")
    val analytics = getAnalytics(app)
    println("Analytics successfully initialized")
}