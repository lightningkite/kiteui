package com.lightningkite.kiteui.telemetry

/**
 * Generates stable fingerprints from exceptions for crash grouping.
 *
 * Normalizes stack traces by stripping volatile parts (line numbers, memory addresses,
 * identity hashes) so that the same logical crash produces the same fingerprint even
 * across builds where line numbers shift.
 */
object CrashFingerprint {

    private val addressPattern = Regex("0x[0-9a-fA-F]+")
    private val identityHashPattern = Regex("@[0-9a-f]{4,}")
    private val lineNumberPattern = Regex("(\\.[a-zA-Z]+:)\\d+")

    /** Generates a 16-hex-char fingerprint for the given throwable. */
    fun generate(throwable: Throwable): String {
        val type = throwable::class.simpleName ?: "Unknown"
        val normalized = normalize(throwable.stackTraceToString())
        return fnv1a64("$type\n$normalized")
    }

    /** Strips volatile parts from a stack trace to produce a stable representation. */
    internal fun normalize(stackTrace: String): String {
        return stackTrace.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { line ->
                line
                    .replace(addressPattern, "0x???")
                    .replace(identityHashPattern, "@???")
                    .replace(lineNumberPattern, "$1?")
            }
            .take(10)
            .joinToString("\n")
    }

    /** FNV-1a 64-bit hash, returned as 16 hex chars. */
    internal fun fnv1a64(input: String): String {
        var hash = 0xcbf29ce484222325uL
        val prime = 0x100000001b3uL
        for (c in input) {
            hash = hash xor c.code.toULong()
            hash *= prime
        }
        return hash.toString(16).padStart(16, '0')
    }
}
