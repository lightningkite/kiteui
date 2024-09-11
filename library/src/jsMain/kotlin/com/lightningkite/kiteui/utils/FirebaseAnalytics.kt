@file:JsModule("firebase/analytics")
@file:JsNonModule

package com.lightningkite.kiteui.utils

external interface Analytics

external fun getAnalytics(app: FirebaseApp?): Analytics