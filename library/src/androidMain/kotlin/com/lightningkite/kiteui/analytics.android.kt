package com.lightningkite.kiteui

import android.annotation.SuppressLint
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.logEvent
import com.google.firebase.ktx.Firebase
import com.lightningkite.kiteui.navigation.mainScreenNavigator
import com.lightningkite.kiteui.reactive.CalculationContext
import com.lightningkite.kiteui.reactive.Readable
import com.lightningkite.kiteui.reactive.awaitOnce
import com.lightningkite.kiteui.reactive.invoke
import com.lightningkite.kiteui.reactive.reactiveScope
import com.lightningkite.kiteui.views.AndroidAppContext
import com.lightningkite.kiteui.views.ViewWriter

var firebaseApp: FirebaseApp? = null

@SuppressLint("MissingPermission")
actual fun ViewWriter.setupAnalytics(
    apiKey: Map<Platform, String>,
    applicationId: Map<Platform, String>,
    gcmSenderId: String,
    storageBucket: String,
    projectId: String,
    measurementId: Map<Platform, String>?,
    userId: Readable<String>?
) {
    val options = FirebaseOptions.Builder().apply {
        apiKey[Platform.Android]?.let(this::setApiKey)
        applicationId[Platform.Android]?.let(this::setApplicationId)
        setGcmSenderId(gcmSenderId)
        setStorageBucket(storageBucket)
        setProjectId(projectId)
        measurementId?.get(Platform.Android)?.let(this::setGaTrackingId)
    }.build()
    firebaseApp = FirebaseApp.initializeApp(AndroidAppContext.applicationCtx, options)

    // Automatic event logging
    CalculationContext.NeverEnds.reactiveScope {
        val screenName = mainScreenNavigator.currentScreen()?.title?.awaitOnce() ?: return@reactiveScope
        Firebase.analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
        }
    }
    userId?.let {
        CalculationContext.NeverEnds.reactiveScope {
            Firebase.analytics.setUserId(it())
        }
    }
}