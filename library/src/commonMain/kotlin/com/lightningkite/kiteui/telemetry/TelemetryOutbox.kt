package com.lightningkite.kiteui.telemetry

import com.lightningkite.kiteui.LogRoot
import com.lightningkite.kiteui.PlatformStorage
import com.lightningkite.kiteui.suppressConnectivityIssues
import com.lightningkite.services.telemetry.MetricUnit
import com.lightningkite.services.telemetry.RecordedLog
import com.lightningkite.services.telemetry.RecordedMetric
import com.lightningkite.services.telemetry.RecordedSpan
import com.lightningkite.services.telemetry.TelemetryAttributes
import com.lightningkite.services.telemetry.TelemetryBatch
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CancellationException
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
    private val metrics = ArrayList<RecordedMetric>()
    private var droppedRecords = 0
    internal val batches: MutableList<TelemetryBatch> = load()

    private var uploading = false
    private var retryAt = Instant.DISTANT_PAST
    private var retryDelay = firstRetryDelay

    val bufferedRecords: Int get() = synchronized(lock) { spans.size + logs.size + metrics.size }

    fun add(span: RecordedSpan): Unit = synchronized(lock) { spans += span }
    fun add(log: RecordedLog): Unit = synchronized(lock) { logs += log }
    fun add(metric: RecordedMetric): Unit = synchronized(lock) { metrics += metric }

    /** Moves buffered records into a new batch and saves the outbox. [durably] for the crash path, where the process is about to die. */
    fun persist(durably: Boolean = false): Unit = synchronized(lock) {
        if (spans.isEmpty() && logs.isEmpty() && metrics.isEmpty() && droppedRecords == 0) return
        if (droppedRecords > 0) {
            metrics += RecordedMetric("telemetry", "dropped", MetricUnit.Occurrences, RecordedMetric.Kind.Counter, droppedRecords.toDouble(), TelemetryAttributes.empty)
            droppedRecords = 0
        }
        batches += TelemetryBatch(resource, Clock.System.now(), spans.toList(), logs.toList(), metrics.toList())
        spans.clear()
        logs.clear()
        metrics.clear()
        save(durably)
    }

    suspend fun upload() {
        synchronized(lock) {
            if (uploading || Clock.System.now() < retryAt) return
            uploading = true
        }
        try {
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
            synchronized(lock) { retryDelay = firstRetryDelay }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // LogRoot, not Log: Log is intercepted by telemetry, which would record this failure and retry it forever.
            LogRoot.tag("Telemetry").warn("Upload failed; retrying in $retryDelay: $e")
            synchronized(lock) {
                retryAt = Clock.System.now() + retryDelay
                retryDelay = minOf(retryDelay * 2, maxRetryDelay)
            }
        } finally {
            synchronized(lock) { uploading = false }
        }
    }

    /** Call holding [lock]. Drops the oldest batches while the outbox is over its size cap. */
    private fun save(durably: Boolean) {
        var stored = json.encodeToString(serializer, batches)
        while (stored.length > MAX_STORED_CHARS && batches.size > 1) {
            val oldest = batches.removeAt(0)
            droppedRecords += oldest.spans.size + oldest.logs.size + oldest.metrics.size
            stored = json.encodeToString(serializer, batches)
        }
        // Failing to store telemetry must not break the app code that happened to record it.
        try {
            if (durably) writeDurably(STORAGE_KEY, stored) else PlatformStorage.set(STORAGE_KEY, stored)
        } catch (e: Exception) {
            LogRoot.tag("Telemetry").warn("Couldn't save telemetry: $e")
        }
    }

    private fun load(): MutableList<TelemetryBatch> {
        val stored = PlatformStorage.get(STORAGE_KEY) ?: return ArrayList()
        return try {
            json.decodeFromString(serializer, stored).toMutableList()
        } catch (e: Exception) {
            LogRoot.tag("Telemetry").warn("Discarding unreadable stored telemetry: $e")
            ArrayList()
        }
    }

    companion object {
        const val STORAGE_KEY = "kiteui.telemetry.outbox"
        const val MAX_STORED_CHARS = 512 * 1024
        val firstRetryDelay: Duration = 30.seconds
        val maxRetryDelay: Duration = 5.minutes
        private val json = Json { ignoreUnknownKeys = true }
        private val serializer = ListSerializer(TelemetryBatch.serializer())
    }
}

/** Marks the outbox's own upload, so the fetch interceptor doesn't record a span per upload forever. */
internal object TelemetryUploading : CoroutineContext.Element, CoroutineContext.Key<TelemetryUploading> {
    override val key: CoroutineContext.Key<*> get() = this
}
