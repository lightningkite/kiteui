package com.lightningkite.kiteui

import androidx.compose.ui.test.ExperimentalTestApi
import com.lightningkite.kiteui.views.runKiteUiTest
import com.lightningkite.kiteui.views.showKiteUi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

@OptIn(ExperimentalTestApi::class)
class PlatformStorageTest {
    @Test
    fun storesPerApp() = runKiteUiTest {
        showKiteUi {}
        assertNull(PlatformStorage.get("key"))
        PlatformStorage.set("key", "value")
        assertEquals("value", PlatformStorage.get("key"))
        PlatformStorage.remove("key")
        assertNull(PlatformStorage.get("key"))
    }

    @Test
    fun refusesToSwitchApps() = runKiteUiTest {
        showKiteUi {}
        assertFailsWith<IllegalStateException> { PlatformStorage.appId = "some-other-app" }
    }
}
