package com.lightningkite.kiteui

import cocoapods.FirebaseCore.FIRApp
import cocoapods.FirebaseCore.FIROptions
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
actual fun setupAnalytics(
    apiKey: Map<Platform, String>,
    applicationId: Map<Platform, String>,
    gcmSenderId: String,
    storageBucket: String,
    projectId: String,
    measurementId: Map<Platform, String>?
) {
    FIRApp.configureWithOptions(FIROptions().apply {
        setAPIKey(apiKey[Platform.iOS])
        setGoogleAppID(applicationId[Platform.iOS] ?: return)
        setGCMSenderID(gcmSenderId)
        setStorageBucket(storageBucket)
        setProjectID(projectId)
        measurementId?.get(Platform.iOS)?.let(this::setTrackingID)
    })
}