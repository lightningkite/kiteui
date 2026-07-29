package com.lightningkite.kiteui.telemetry

/**
 * Generates stable fingerprints from exceptions for crash grouping.
 *
 * Uses an adaptive strategy: when the stack trace contains meaningful function/class names
 * (JVM, Android, iOS debug), it's included in the hash for fine-grained grouping. When the
 * trace is garbage (iOS release with only memory addresses, or empty), falls back to
 * exception cause chain + normalized message.
 */
public object CrashFingerprint {

    private val addressPattern = Regex("0x[0-9a-fA-F]+")
    private val identityHashPattern = Regex("@[0-9a-f]{4,}")
    private val lineNumberPattern = Regex("(\\.[a-zA-Z]+:)\\d+")
    private val numberPattern = Regex("\\b\\d+\\b")
    private val hexLiteralPattern = Regex("\\b0x[0-9a-fA-F]+\\b")
    private val uuidPattern = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")

    /** Generates a 16-hex-char fingerprint for the given throwable. */
    public fun generate(throwable: Throwable): String {
        val chain = causeChain(throwable)
        val normalized = normalize(throwable.stackTraceToString())

        val input = if (isUsableTrace(normalized)) {
            "$chain\n$normalized"
        } else {
            val msg = normalizeMessage(throwable.message ?: "")
            "$chain\n$msg"
        }
        return fnv1a64(input)
    }

    /**
     * Builds a stable cause chain string like "IllegalStateException > IOException".
     * Works identically on all platforms since it only uses class names.
     */
    public fun causeChain(throwable: Throwable): String = buildString {
        var t: Throwable? = throwable
        var depth = 0
        while (t != null && depth < 10) {
            if (depth > 0) append(" > ")
            append(t::class.simpleName ?: "Unknown")
            t = t.cause
            depth++
        }
    }

    /**
     * Returns true if the normalized stack trace has at least 2 lines with recognizable
     * class/method names (not just `0x???` address placeholders).
     */
    internal fun isUsableTrace(normalized: String): Boolean {
        val meaningfulLines = normalized.lineSequence()
            .filter { it.isNotBlank() }
            .count { line -> !line.all { c -> c == '0' || c == 'x' || c == '?' || c.isWhitespace() } }
        return meaningfulLines >= 2
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

    /**
     * Normalizes an exception message by stripping volatile values (numbers, hex, UUIDs)
     * so that different instances of the same error group together.
     * e.g. "User 42 not found" → "User ? not found"
     */
    internal fun normalizeMessage(message: String): String {
        return message
            .replace(uuidPattern, "?")
            .replace(hexLiteralPattern, "?")
            .replace(numberPattern, "?")
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
