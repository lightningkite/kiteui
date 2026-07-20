package com.lightningkite.kiteui.testing

import androidx.test.core.app.ApplicationProvider
import com.lightningkite.kiteui.views.AndroidAppContext
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
actual abstract class BaseUiTest actual constructor() {
    init {
        AndroidAppContext.applicationCtx = ApplicationProvider.getApplicationContext()
    }
}