package com.lightningkite.kiteui.telemetry

import com.lightningkite.kiteui.LogRoot
import com.lightningkite.kiteui.PlatformStorage
import com.lightningkite.kiteui.suppressConnectivityIssues
import com.lightningkite.services.telemetry.LogLevel
import com.lightningkite.services.telemetry.RecordedLog
import com.lightningkite.services.telemetry.RecordedSpan
import com.lightningkite.services.telemetry.TelemetryAttributes
import com.lightningkite.services.telemetry.TelemetryBatch
import com.lightningkite.services.telemetry.TelemetryKey
import com.lightningkite.services.telemetry.telemetryAttributesOf
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.coroutines.CoroutineContext
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Records accumulate in memory; [persist] turns them into one [TelemetryBatch] and saves every pending
 * batch to [PlatformStorage], so telemetry recorded offline or just before the app dies is sent on a
 * later run. [upload] sends batches oldest first and forgets each one only once [send] returns.
 */
internal class TelemetryOutbox(
    private val resource: TelemetryAttributes,
    private val send: suspend (TelemetryBatch) -> Unit,
) {
    // Records arrive from any thread: requests on background dispatchers, crash hooks on the crashing thread.
    private val lock = SynchronizedObject()
    private val spans = ArrayList<RecordedSpan>()
    private val logs = ArrayList<RecordedLog>()
    internal val batches: MutableList<TelemetryBatch> = load()

    private val uploading = Mutex()
    // Claude: only touched while holding uploading, so no lock.
    private var retryAt = Instant.DISTANT_PAST
    private var retryDelay = firstRetryDelay

    val bufferedRecords: Int get() = synchronized(lock) { spans.size + logs.size }

    fun add(span: RecordedSpan): Unit = synchronized(lock) { spans += span.copy(attributes = span.attributes.withinServerLimits()) }
    fun add(log: RecordedLog): Unit = synchronized(lock) { logs += log.copy(attributes = log.attributes.withinServerLimits()) }

    /** Moves buffered records into a new batch and saves the outbox. [durably] for the crash path, where the process is about to die. */
    fun persist(durably: Boolean = false): Unit = synchronized(lock) {
        if (spans.isEmpty() && logs.isEmpty()) return
        batches += TelemetryBatch(resource, Clock.System.now(), spans.toList(), logs.toList())
        spans.clear()
        logs.clear()
        save(durably)
    }

    /** Forgets every record and batch that hasn't been sent, here and on the device. */
    fun clear(): Unit = synchronized(lock) {
        spans.clear()
        logs.clear()
        batches.clear()
        save(durably = false)
    }

    /** Sends batches oldest first, unless an upload is already running or a failed one is still backing off. Failures are retried later. */
    suspend fun upload() {
        if (!uploading.tryLock()) return
        try {
            if (Clock.System.now() < retryAt) return
            sendAll()
        } catch (e: Exception) {
            // Claude: a CancellationException from inside send, such as a withTimeout there, is a failed upload;
            // only our own cancellation should end the caller's upload loop.
            if (e is CancellationException) currentCoroutineContext().ensureActive()
            // LogRoot, not Log: Log is intercepted by telemetry, which would record this failure and retry it forever.
            LogRoot.tag("Telemetry").warn("Upload failed; retrying in $retryDelay: $e")
            retryAt = Clock.System.now() + retryDelay
            retryDelay = minOf(retryDelay * 2, maxRetryDelay)
        } finally {
            uploading.unlock()
        }
    }

    /** Sends every batch now, waiting for a running upload to finish and ignoring any backoff. Throws what [send] throws. */
    suspend fun flush(): Unit = uploading.withLock { sendAll() }

    /** Call holding [uploading]. */
    private suspend fun sendAll() {
        withContext(TelemetryUploading) {
            suppressConnectivityIssues {
                while (true) {
                    val batch = synchronized(lock) { batches.firstOrNull() } ?: break
                    send(batch.copy(sentAt = Clock.System.now()))
                    synchronized(lock) {
                        // By identity: persist() may have dropped this batch for size while it was in flight.
                        batches.removeAll { it === batch }
                        save(durably = false)
                    }
                }
            }
        }
        retryAt = Instant.DISTANT_PAST
        retryDelay = firstRetryDelay
    }

    /** Call holding [lock]. Drops the oldest batches while the outbox is over its size cap, noting the drop in the next batch. */
    private fun save(durably: Boolean) {
        var stored = json.encodeToString(serializer, batches)
        var dropped = 0
        while (stored.length > MAX_STORED_CHARS && batches.size > 1) {
            val oldest = batches.removeAt(0)
            dropped += oldest.spans.size + oldest.logs.size
            stored = json.encodeToString(serializer, batches)
        }
        if (dropped > 0) {
            logs += RecordedLog(
                Clock.System.now(),
                LogLevel.Warn,
                "Dropped $dropped telemetry records: the device's outbox was full",
                telemetryAttributesOf(droppedRecordsKey to dropped.toLong())
            )
        }
        // Failing to store telemetry must not break the app code that happened to record it.
        // Claude: Throwable, because a browser's DOMException (storage blocked or full) isn't a Kotlin Exception.
        try {
            PlatformStorage.set(STORAGE_KEY, stored, ensureOnDisk = durably)
        } catch (e: Throwable) {
            LogRoot.tag("Telemetry").warn("Couldn't save telemetry: $e")
        }
    }

    // Claude: Throwable for the same reason as in save; reading blocked browser storage throws too.
    private fun load(): MutableList<TelemetryBatch> = try {
        PlatformStorage.get(STORAGE_KEY)?.let { json.decodeFromString(serializer, it).toMutableList() } ?: ArrayList()
    } catch (e: Throwable) {
        LogRoot.tag("Telemetry").warn("Discarding unreadable stored telemetry: $e")
        ArrayList()
    }

    // Claude: the server refuses a whole batch for one record over its limits (Lightning Server's ClientTelemetryEndpoints),
    // so attributes with invalid names or past the count are dropped and long values are cut short.
    private fun TelemetryAttributes.withinServerLimits(): TelemetryAttributes {
        val kept = map.entries.filter { NAME_PATTERN.matches(it.key.name) }.take(MAX_ATTRIBUTES)
        if (kept.size == map.size && kept.all { it.value.fits() }) return this
        @Suppress("UNCHECKED_CAST")
        return TelemetryAttributes(kept.map { (key, value) -> TelemetryKey.Entry(key as TelemetryKey<Any>, value.cut()) })
    }

    private fun Any.fits(): Boolean = when (this) {
        is String -> length <= MAX_VALUE_LENGTH
        is List<*> -> all { it !is String || it.length <= MAX_VALUE_LENGTH }
        else -> true
    }

    private fun Any.cut(): Any = when (this) {
        is String -> take(MAX_VALUE_LENGTH)
        is List<*> -> map { if (it is String) it.take(MAX_VALUE_LENGTH) else it }
        else -> this
    }

    companion object {
        const val STORAGE_KEY = "kiteui.telemetry.outbox"
        const val MAX_ATTRIBUTES = 128
        const val MAX_VALUE_LENGTH = 4096
        val NAME_PATTERN = Regex("[A-Za-z][A-Za-z0-9_.-]{0,63}")
        const val MAX_STORED_CHARS = 512 * 1024
        val firstRetryDelay: Duration = 30.seconds
        val maxRetryDelay: Duration = 5.minutes
        private val droppedRecordsKey = TelemetryKey.OfLong("telemetry.dropped_records")
        private val json = Json { ignoreUnknownKeys = true }
        private val serializer = ListSerializer(TelemetryBatch.serializer())
    }
}

/** Marks the outbox's own upload, so the fetch interceptor doesn't record a span per upload forever. */
internal object TelemetryUploading : CoroutineContext.Element, CoroutineContext.Key<TelemetryUploading> {
    override val key: CoroutineContext.Key<*> get() = this
}
