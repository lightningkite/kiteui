package com.lightningkite.kiteui

import cocoapods.FirebaseAnalytics.FIRAnalytics
import cocoapods.FirebaseAnalytics.kFIREventScreenView
import cocoapods.FirebaseAnalytics.kFIRParameterScreenName
import cocoapods.FirebaseCore.FIRApp
import cocoapods.FirebaseCore.FIROptions
import com.lightningkite.kiteui.navigation.mainScreenNavigator
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.ViewWriter
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
actual fun ViewWriter.setupAnalytics(
    apiKey: Map<Platform, String>,
    applicationId: Map<Platform, String>,
    gcmSenderId: String,
    storageBucket: String,
    projectId: String,
    measurementId: Map<Platform, String>?,
    userId: Readable<String>?
) {
    FIRApp.configureWithOptions(FIROptions().apply {
        setAPIKey(apiKey[Platform.iOS])
        setGoogleAppID(applicationId[Platform.iOS] ?: return)
        setGCMSenderID(gcmSenderId)
        setStorageBucket(storageBucket)
        setProjectID(projectId)
        measurementId?.get(Platform.iOS)?.let(this::setTrackingID)
    })

    // Automatic event logging
    CalculationContext.NeverEnds.reactiveScope {
        val screenName = mainScreenNavigator.currentScreen()?.title?.awaitOnce() ?: return@reactiveScope
        FIRAnalytics.logEventWithName(kFIREventScreenView!!, mapOf(
            kFIRParameterScreenName to screenName
        ))
    }
    userId?.let {
        CalculationContext.NeverEnds.reactiveScope {
            FIRAnalytics.setUserID(it())
        }
    }
}