package com.lightningkite.kiteui

import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

// Writes go straight to the Keychain, so ensureOnDisk has nothing to add here.
public actual object SecurePlatformStorage {
    private const val service = "KiteUI.SecurePlatformStorage"

    public actual fun get(key: String): String? = memScoped {
        val query = itemQuery(key)
        CFDictionaryAddValue(query, kSecReturnData, kCFBooleanTrue)
        CFDictionaryAddValue(query, kSecMatchLimit, kSecMatchLimitOne)
        val result = alloc<CFTypeRefVar>()
        val status = SecItemCopyMatching(query, result.ptr)
        CFRelease(query)
        when (status) {
            errSecSuccess -> NSString.create(data = CFBridgingRelease(result.value) as NSData, encoding = NSUTF8StringEncoding).toString()
            errSecItemNotFound -> null
            else -> throw IllegalStateException("Keychain read of '$key' failed with OSStatus $status")
        }
    }

    public actual fun set(key: String, value: String, ensureOnDisk: Boolean) {
        remove(key)
        val query = itemQuery(key)
        // ThisDeviceOnly keeps the item out of backups and iCloud Keychain; AfterFirstUnlock lets background work read it.
        CFDictionaryAddValue(query, kSecAttrAccessible, kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly)
        query.addBridged(kSecValueData, NSString.create(string = value).dataUsingEncoding(NSUTF8StringEncoding)!!)
        val status = SecItemAdd(query, null)
        CFRelease(query)
        check(status == errSecSuccess) { "Keychain write of '$key' failed with OSStatus $status" }
    }

    public actual fun remove(key: String) {
        val query = itemQuery(key)
        val status = SecItemDelete(query)
        CFRelease(query)
        check(status == errSecSuccess || status == errSecItemNotFound) { "Keychain delete of '$key' failed with OSStatus $status" }
    }

    private fun itemQuery(key: String): CFMutableDictionaryRef? {
        val query = CFDictionaryCreateMutable(null, 0, kCFTypeDictionaryKeyCallBacks.ptr, kCFTypeDictionaryValueCallBacks.ptr)
        CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
        query.addBridged(kSecAttrService, service)
        query.addBridged(kSecAttrAccount, key)
        return query
    }

    // The dictionary retains what it is given, so the reference made to hand it over is released straight away.
    private fun CFMutableDictionaryRef?.addBridged(key: CFStringRef?, value: Any) {
        val bridged = CFBridgingRetain(value)
        CFDictionaryAddValue(this, key, bridged)
        CFRelease(bridged)
    }
}
