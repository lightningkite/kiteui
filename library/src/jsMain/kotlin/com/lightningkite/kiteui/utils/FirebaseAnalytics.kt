@file:JsModule("firebase/analytics")
@file:JsNonModule

package com.lightningkite.kiteui.utils

import kotlin.js.Json

external interface Analytics

external fun getAnalytics(app: FirebaseApp?): Analytics
external fun logEvent(analyticsInstance: Analytics, eventName: String, eventParams: Json)
external fun setUserId(analyticsInstance: Analytics, userId: String)