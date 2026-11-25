package com.lightningkite.kiteui

import android.content.Context
import android.util.Base64
import androidx.test.platform.app.InstrumentationRegistry
import com.lightningkite.kiteui.views.AndroidAppContext
import java.io.File
import java.security.KeyStore
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SecurePlatformStorageDeviceTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    // SecurePlatformStorage's own file and key alias; these tests look at what it leaves on disk.
    private val storeName = "KiteUI.SecurePlatformStorage"
    private val preferences get() = context.getSharedPreferences(storeName, Context.MODE_PRIVATE)

    @BeforeTest
    fun setUp() {
        AndroidAppContext.applicationCtx = context.applicationContext
    }

    @Test
    fun setGetRemove() {
        SecurePlatformStorage.set("round-trip", "first")
        assertEquals("first", SecurePlatformStorage.get("round-trip"))
        SecurePlatformStorage.set("round-trip", "second")
        assertEquals("second", SecurePlatformStorage.get("round-trip"))
        SecurePlatformStorage.remove("round-trip")
        assertNull(SecurePlatformStorage.get("round-trip"))
        assertNull(SecurePlatformStorage.get("never-written"))
    }

    @Test
    fun ensureOnDiskWritesTheCiphertextBeforeReturning() {
        val value = "on-disk-secret-${System.nanoTime()}"
        SecurePlatformStorage.set("on-disk", value, ensureOnDisk = true)

        val file = File(context.applicationInfo.dataDir, "shared_prefs/$storeName.xml").readText()
        assertTrue("name=\"on-disk\"" in file, "the entry is in the file already")
        assertFalse(value in file, "the value is not stored in plain text")
        // What a restarted process needs besides the file: the key, still in the Keystore under the same alias.
        assertTrue(KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.containsAlias(storeName))
    }

    @Test
    fun tamperedCiphertextReadsAsAbsentAndIsRemoved() {
        SecurePlatformStorage.set("tampered", "original", ensureOnDisk = true)
        val stored = Base64.decode(preferences.getString("tampered", null)!!, Base64.NO_WRAP)
        stored[stored.size - 1] = (stored[stored.size - 1].toInt() xor 1).toByte()
        preferences.edit().putString("tampered", Base64.encodeToString(stored, Base64.NO_WRAP)).commit()

        assertNull(SecurePlatformStorage.get("tampered"))
        assertFalse(preferences.contains("tampered"))
    }

    @Test
    fun aValueMovedToAnotherKeyDoesNotDecrypt() {
        SecurePlatformStorage.set("owner", "owner's secret", ensureOnDisk = true)
        val ciphertext = assertNotNull(preferences.getString("owner", null))
        preferences.edit().putString("intruder", ciphertext).commit()

        assertNull(SecurePlatformStorage.get("intruder"))
        assertEquals("owner's secret", SecurePlatformStorage.get("owner"))
    }
}
