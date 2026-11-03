package com.lightningkite.kiteui

import androidx.compose.ui.test.ExperimentalTestApi
import com.lightningkite.kiteui.views.runKiteUiTest
import com.lightningkite.kiteui.views.showKiteUi
import kotlin.io.path.createTempDirectory
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

    // PersistentProperty stores whole JSON documents under descriptive keys.
    @Test
    fun storesLongKeysAndValues() = runKiteUiTest {
        showKiteUi {}
        val key = "k".repeat(200)
        val value = "v".repeat(100_000)
        PlatformStorage.set(key, value)
        assertEquals(value, PlatformStorage.get(key))
    }

    @Test
    fun survivesRestart() {
        val file = createTempDirectory().resolve("storage.json").toFile()
        JsonFileStorage(file).apply {
            set("kept", "value")
            set("removed", "value")
            remove("removed")
            save()
        }
        val restarted = JsonFileStorage(file)
        assertEquals("value", restarted.get("kept"))
        assertNull(restarted.get("removed"))
    }

    @Test
    fun savesInTheBackground() {
        val file = createTempDirectory().resolve("storage.json").toFile()
        JsonFileStorage(file).set("key", "value")
        val deadline = System.currentTimeMillis() + 5_000
        while (!file.exists() && System.currentTimeMillis() < deadline) Thread.sleep(10)
        assertEquals("value", JsonFileStorage(file).get("key"))
    }

    @Test
    fun refusesToSwitchApps() = runKiteUiTest {
        showKiteUi {}
        assertFailsWith<IllegalStateException> { PlatformStorage.appId = "some-other-app" }
    }
}
