package com.lightningkite.kiteui

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.lightningkite.kiteui.views.AndroidAppContext
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

public actual object SecurePlatformStorage {
    private const val name = "KiteUI.SecurePlatformStorage"
    private const val ivBytes = 12
    private val log = LogRoot.tag("SecurePlatformStorage")

    private val preferences: SharedPreferences by lazy {
        AndroidAppContext.applicationCtx.getSharedPreferences(name, Context.MODE_PRIVATE)
    }

    // The key never leaves the Keystore, so the stored values are useless to anyone who copies the preferences file.
    private val secretKey: SecretKey by lazy {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        keyStore.getKey(name, null) as SecretKey? ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(
                KeyGenParameterSpec.Builder(name, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
            )
            generateKey()
        }
    }

    public actual fun get(key: String): String? {
        val stored = Base64.decode(preferences.getString(key, null) ?: return null, Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, stored, 0, ivBytes))
        // Binding the value to its key stops one stored value being swapped in for another.
        cipher.updateAAD(key.encodeToByteArray())
        return try {
            cipher.doFinal(stored, ivBytes, stored.size - ivBytes).decodeToString()
        } catch (e: GeneralSecurityException) {
            // Expected after a backup is restored onto another device: the preferences come along, the Keystore key
            // does not. The value can never be read again, so it is gone, not corrupt.
            log.warn("Discarding '$key', which this device's key cannot decrypt", e)
            remove(key)
            null
        }
    }

    public actual fun set(key: String, value: String, ensureOnDisk: Boolean) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        cipher.updateAAD(key.encodeToByteArray())
        val stored = cipher.iv + cipher.doFinal(value.encodeToByteArray())
        val editor = preferences.edit().putString(key, Base64.encodeToString(stored, Base64.NO_WRAP))
        if (ensureOnDisk) editor.commit() else editor.apply()
    }

    public actual fun remove(key: String) {
        preferences.edit().remove(key).apply()
    }
}
