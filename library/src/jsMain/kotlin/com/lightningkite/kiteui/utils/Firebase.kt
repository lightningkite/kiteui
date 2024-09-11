@file:JsModule("firebase/app")
@file:JsNonModule

package com.lightningkite.kiteui.utils

external interface FirebaseOptions {
    var apiKey: String?
    var appId: String?
    var measurementId: String?
    var messagingSenderId: String?
    var projectId: String?
    var storageBucket: String?
}

external interface FirebaseApp

external fun initializeApp(options: FirebaseOptions, name: String?): FirebaseApp