// by Claude - Automatically sets up `adb reverse` for all connected Android devices
package com.lightningkite.kiteui.aidriver.server

import kotlinx.coroutines.*
import java.io.IOException

/**
 * Monitors connected Android devices via ADB and automatically sets up port forwarding
 * so that `localhost:<port>` on the device reaches the daemon on the host.
 */
object AdbReverse {
    private val knownDevices = mutableSetOf<String>()
    private var job: Job? = null

    /**
     * Starts periodic ADB device monitoring. Runs `adb reverse` for each newly detected device.
     * Silently no-ops if ADB is not installed.
     */
    fun start(scope: CoroutineScope, port: Int = 7474) {
        if (!isAdbAvailable()) {
            println("ADB not found on PATH — skipping automatic adb reverse setup")
            return
        }
        println("ADB reverse monitor started — will forward tcp:$port for all connected devices")
        job = scope.launch {
            while (isActive) {
                try {
                    refreshDevices(port)
                } catch (e: Exception) {
                    // Don't crash the monitor on transient ADB errors
                    if (e !is CancellationException) {
                        println("ADB reverse monitor error: ${e.message}")
                    } else throw e
                }
                delay(5_000)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        knownDevices.clear()
    }

    private fun refreshDevices(port: Int) {
        val currentDevices = listDevices()
        val newDevices = currentDevices - knownDevices
        for (serial in newDevices) {
            setupReverse(serial, port)
        }
        // Remove devices that disconnected so we re-setup if they reconnect
        knownDevices.retainAll(currentDevices)
    }

    /** Parses `adb devices` output to get serial numbers of connected devices/emulators. */
    private fun listDevices(): Set<String> {
        val process = ProcessBuilder("adb", "devices")
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText()
        process.waitFor()
        if (process.exitValue() != 0) return emptySet()

        return output.lineSequence()
            .drop(1) // skip "List of devices attached" header
            .filter { it.contains("\tdevice") }
            .map { it.substringBefore("\t").trim() }
            .filter { it.isNotEmpty() }
            .toSet()
    }

    /** Runs `adb -s <serial> reverse tcp:<port> tcp:<port>` for a single device. */
    private fun setupReverse(serial: String, port: Int) {
        val process = ProcessBuilder("adb", "-s", serial, "reverse", "tcp:$port", "tcp:$port")
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText().trim()
        process.waitFor()
        if (process.exitValue() == 0) {
            knownDevices.add(serial)
            println("ADB reverse tcp:$port set up for device $serial")
        } else {
            println("ADB reverse failed for device $serial: $output")
        }
    }

    private fun isAdbAvailable(): Boolean {
        return try {
            val process = ProcessBuilder("adb", "version")
                .redirectErrorStream(true)
                .start()
            process.inputStream.bufferedReader().readText()
            process.waitFor()
            process.exitValue() == 0
        } catch (_: IOException) {
            false
        }
    }
}
