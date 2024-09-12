package com.lightningkite.kiteui

import com.lightningkite.kiteui.navigation.mainScreenNavigator
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.utils.*
import com.lightningkite.kiteui.views.ViewWriter
import kotlin.js.json

actual fun ViewWriter.setupAnalytics(
    apiKey: Map<Platform, String>,
    applicationId: Map<Platform, String>,
    gcmSenderId: String,
    storageBucket: String,
    projectId: String,
    measurementId: Map<Platform, String>?,
    userId: Readable<String>?
) {
    val firebaseConfig: dynamic = object {}
    firebaseConfig["apiKey"] = apiKey[Platform.Web]
    firebaseConfig["appId"] = applicationId[Platform.Web]
    firebaseConfig["measurementId"] = measurementId?.get(Platform.Web)
    firebaseConfig["messagingSenderId"] = gcmSenderId
    firebaseConfig["projectId"] = projectId
    firebaseConfig["storageBucket"] = storageBucket
    val app = initializeApp(firebaseConfig.unsafeCast<FirebaseOptions>(), null)
    val analytics = getAnalytics(app)

    // Automatic event logging
    CalculationContext.NeverEnds.reactiveScope {
        val screenName = mainScreenNavigator.currentScreen()?.title?.awaitOnce() ?: return@reactiveScope
        logEvent(analytics, "screen_view", json(
            "firebase_screen" to screenName
        ))
    }
    userId?.let {
        CalculationContext.NeverEnds.reactiveScope {
            setUserId(analytics, it())
        }
    }
}