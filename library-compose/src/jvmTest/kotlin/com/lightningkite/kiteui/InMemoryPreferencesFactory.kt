package com.lightningkite.kiteui

import java.util.prefs.AbstractPreferences
import java.util.prefs.Preferences
import java.util.prefs.PreferencesFactory

// The jvmTest task installs this (see build.gradle.kts), so tests never touch the developer's real preferences.
class InMemoryPreferencesFactory : PreferencesFactory {
    private val user = InMemoryPreferences(null, "")
    private val system = InMemoryPreferences(null, "")
    override fun userRoot(): Preferences = user
    override fun systemRoot(): Preferences = system
}

class InMemoryPreferences(parent: InMemoryPreferences?, name: String) : AbstractPreferences(parent, name) {
    private val values = HashMap<String, String>()
    override fun putSpi(key: String, value: String) {
        values[key] = value
    }
    override fun getSpi(key: String): String? = values[key]
    override fun removeSpi(key: String) {
        values.remove(key)
    }
    override fun keysSpi(): Array<String> = values.keys.toTypedArray()
    // AbstractPreferences keeps the children it made; there are no others.
    override fun childrenNamesSpi(): Array<String> = arrayOf()
    override fun childSpi(name: String): AbstractPreferences = InMemoryPreferences(this, name)
    override fun removeNodeSpi() {}
    override fun syncSpi() {}
    override fun flushSpi() {}
}
