package com.lightningkite.kiteui.speech

import com.lightningkite.kiteui.locale.LanguageCode

internal actual suspend fun platformSpeak(text: String, language: LanguageCode?): Unit =
    throw UnsupportedOperationException("Speech isn't available during server-side rendering.")

internal actual val platformCanListen: Boolean get() = false

internal actual suspend fun platformListen(language: LanguageCode?): String? =
    throw UnsupportedOperationException("Speech isn't available during server-side rendering.")
