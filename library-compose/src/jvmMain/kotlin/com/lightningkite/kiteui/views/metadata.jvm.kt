package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.PlatformStorage

internal actual fun ElementContext.bestGuessAtAppName(): String? = PlatformStorage.appId
