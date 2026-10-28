package com.lightningkite.kiteui

import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/** Per-app storage in a file in the OS's app-data folder.  [KiteUiRoot] sets the app id it's keyed by. */
public actual object PlatformStorage {
    internal var appId: String? = null
        set(value) {
            check(field == null || field == value) { "PlatformStorage already uses app id '$field'; a process runs one app, so it can't switch to '$value'." }
            if (field == null) storage = JsonFileStorage(appDataFolder().resolve("$value/storage.json"))
            field = value
        }

    internal var storage: JsonFileStorage? = null
        private set

    private fun storage() = checkNotNull(storage) { "PlatformStorage used before a KiteUiRoot set the app id." }

    public actual fun get(key: String): String? = storage().get(key)
    public actual fun set(key: String, value: String): Unit = storage().set(key, value)
    public actual fun remove(key: String): Unit = storage().remove(key)

    private fun appDataFolder(): File {
        // Lets tests keep their data out of the developer's real app-data folder.
        System.getProperty("kiteui.dataDirectory")?.let { return File(it) }
        val os = System.getProperty("os.name")
        val home = File(System.getProperty("user.home"))
        return when {
            os.startsWith("Mac") -> home.resolve("Library/Application Support")
            os.startsWith("Windows") -> File(checkNotNull(System.getenv("APPDATA")) { "APPDATA is not set, so there's nowhere to store app data." })
            else -> System.getenv("XDG_DATA_HOME")?.let(::File) ?: home.resolve(".local/share")
        }
    }
}

internal class JsonFileStorage(private val file: File) {
    private val values = ConcurrentHashMap<String, String>(
        if (file.exists()) Json.decodeFromString<Map<String, String>>(file.readText()) else mapOf()
    )
    private val unsaved = AtomicBoolean(false)

    init {
        Runtime.getRuntime().addShutdownHook(thread(start = false, name = "Save $file") { save() })
    }

    fun get(key: String): String? = values[key]

    fun set(key: String, value: String) {
        values[key] = value
        changed()
    }

    fun remove(key: String) {
        values.remove(key)
        changed()
    }

    fun clear() {
        values.clear()
        changed()
    }

    private fun changed() {
        if (unsaved.compareAndSet(false, true)) writer.execute(::save)
    }

    // The writer thread and the shutdown hook can both get here.
    fun save(): Unit = synchronized(this) {
        if (!unsaved.getAndSet(false)) return
        file.parentFile.mkdirs()
        val temporary = File(file.parentFile, "${file.name}.tmp")
        temporary.writeText(Json.encodeToString(values.toMap()))
        // Atomic, so a crash mid-write leaves the previous file rather than half of this one.
        Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    }

    private companion object {
        val writer = Executors.newSingleThreadExecutor { Thread(it, "PlatformStorage writer").apply { isDaemon = true } }
    }
}
