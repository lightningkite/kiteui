# KiteUI Telemetry

KiteUI has built-in OpenTelemetry integration that exports traces, metrics, and logs via OTLP/JSON over HTTP. Zero additional dependencies — it uses KiteUI's existing `fetch()` and `kotlinx.serialization`.

## Quick Start

```kotlin
// In your App.kt or equivalent, at startup:
Telemetry.configure(
    endpoint = "https://otlp-gateway-prod-us-central-0.grafana.net/otlp",
    headers = mapOf("Authorization" to "Basic ${Base64.encode("instanceId:apiToken")}")
)
```

That's it. Everything below is automatic.

## What's Auto-Instrumented

When configured, KiteUI automatically tracks:

| Signal | What | Details |
|--------|------|---------|
| **Traces** | HTTP requests | Client-perceived latency, method, host, URL. Span hierarchy links to current page. |
| **Traces** | Page views | Time spent on each page, page name from `Page::class.simpleName`. |
| **Traces** | App cold start | Time from process start to `Telemetry.configure()`. |
| **Traces** | Foreground sessions | Duration of each foreground session (background → foreground → background). |
| **Metrics** | `http.client.request.duration` | Histogram of HTTP latency in ms, bucketed by method and host. |
| **Metrics** | `navigation.page_views` | Counter of page views by page name. |
| **Metrics** | `connectivity.issues` | Counter of connectivity failures by status code. |
| **Metrics** | `app.foreground_count` | Counter of foreground transitions. |
| **Logs** | Warn/Error logs | All `Log.warn()` and `Log.error()` calls, with tag and session ID. |
| **Logs** | Exception reports | Full stack traces from `Throwable.report()`, with exception type, context, and crash fingerprint. |
| **Logs** | Uncaught exceptions | FATAL-severity log with crash fingerprint, flushed before process death. |
| **Headers** | `traceparent` | W3C trace context injected into all HTTP requests for end-to-end correlation with server traces. |

### Trace Context Propagation

Every outgoing HTTP request includes a `traceparent` header ([W3C Trace Context](https://www.w3.org/TR/trace-context/)):

```
traceparent: 00-{traceId}-{spanId}-{sampled}
```

If your server picks up this header (most OpenTelemetry-instrumented servers do automatically), you get end-to-end traces:

```
page: ItemDetailPage (client)
  └── HTTP GET (client)
        └── GET /api/items/123 (server)
              └── database query (server)
```

#### Coroutine Safety

Trace context is captured **before** any coroutine suspension in `connectivityFetch()`. This means even if the user navigates to a different page while an HTTP request is in-flight, the span is correctly attributed to the page that initiated the request — not the page that's visible when it completes.

For background coroutines launched with a custom scope, use `TelemetryContext.current()` to carry the caller's trace context:

```kotlin
// Inside a page action:
launch(TelemetryContext.current()) {
    // connectivityFetch() calls here inherit the parent's trace context
    val data = connectivityFetch(url = "...", body = null)
}
```

## Volume Control

### Logs

By default, only `WARN` and `ERROR` level logs are shipped. This keeps volume low during normal operation.

To temporarily enable verbose logging for debugging a production issue:

```kotlin
// Ship all log levels (DEBUG and above)
Telemetry.setVerboseLogging(true)

// Restore default (WARN and above)
Telemetry.setVerboseLogging(false)
```

### Traces

All traces are exported by default. For high-traffic apps, reduce with:

```kotlin
Telemetry.configure(
    TelemetryConfig(
        endpoint = "...",
        traceSamplingRate = 0.1,  // Export 10% of traces
    )
)
```

Metrics are always exported regardless of sampling — they're aggregated client-side into counters and histograms, so the volume is tiny.

### Batching

Data is buffered and flushed:
- Every **30 seconds** (configurable via `flushIntervalMs`)
- When any buffer reaches **100 items** (configurable via `maxBatchSize`)
- **Immediately when the app goes to background** (critical — the OS may kill the app)

Buffers cap at **500 items** (configurable via `maxQueueSize`). Oldest entries are dropped when exceeded.

## Custom Metrics

Record your own counters and histograms:

```kotlin
// Count events
Telemetry.counter("checkout.completed")
Telemetry.counter("search.queries", attributes = listOf(
    OtlpKeyValue("search.category", OtlpAnyValue(stringValue = "products"))
))

// Record values into histograms
Telemetry.histogram("image.load_time", durationMs, unit = "ms")
```

## Full Configuration

```kotlin
Telemetry.configure(
    TelemetryConfig(
        // Required
        endpoint = "https://otlp-gateway-prod-us-central-0.grafana.net/otlp",

        // Authentication
        headers = mapOf("Authorization" to "Basic ${Base64.encode("instanceId:apiToken")}"),

        // Identity
        serviceName = "my-app",        // Identifies this app in dashboards
        serviceVersion = "1.2.3",      // Attached to all exported data

        // Batching
        flushIntervalMs = 30_000L,     // Flush every 30s (default)
        maxBatchSize = 100,            // Flush when buffer hits 100 (default)
        maxQueueSize = 500,            // Drop oldest beyond 500 (default)

        // Sampling
        traceSamplingRate = 1.0,       // Export all traces (default)

        // Logs
        logMinSeverity = OtlpSeverity.WARN,  // Only WARN+ (default)
    )
)
```

## Noop Mode

When `Telemetry.configure()` is not called, all instrumentation is gated behind a single `if (Telemetry.isActive)` boolean check. No buffers are allocated, no coroutines are launched, no listeners are registered. Effectively zero overhead.

## Grafana Cloud Setup

1. Go to your Grafana Cloud portal → **OpenTelemetry** tile
2. Note your **zone URL** (e.g., `https://otlp-gateway-prod-us-central-0.grafana.net/otlp`)
3. Note your **instance ID** (numeric) and create an **API token** (starts with `glc_`)
4. Configure:

```kotlin
val instanceId = "123456"
val apiToken = "glc_..."
val auth = Base64.encode("$instanceId:$apiToken".encodeToByteArray())

Telemetry.configure(
    endpoint = "https://otlp-gateway-prod-us-central-0.grafana.net/otlp",
    headers = mapOf("Authorization" to "Basic $auth"),
    serviceName = "my-app"
)
```

Grafana Cloud automatically routes:
- **Traces** → Tempo
- **Metrics** → Mimir (Prometheus-compatible)
- **Logs** → Loki

### JS/Browser Note

Sending directly from the browser to Grafana Cloud exposes the API token in client-side code. For production web apps, proxy telemetry through your backend:

```kotlin
// Browser app points at your backend's telemetry proxy
Telemetry.configure(endpoint = "https://your-backend.com/otlp")
```

Your backend forwards to Grafana Cloud with the real credentials. Android and iOS apps can send directly.

## Crash Tracking

Uncaught exceptions are automatically captured at `FATAL` severity with a best-effort flush before the process dies. Each crash includes a `crash.fingerprint` attribute for grouping identical crashes in Grafana.

### How Fingerprinting Works

Stack traces are normalized before hashing to produce stable fingerprints across builds:

1. **Line numbers stripped** — `Foo.kt:42` becomes `Foo.kt:?`
2. **Memory addresses stripped** — `0x1a2b3c` becomes `0x???`
3. **Identity hashes stripped** — `@deadbeef` becomes `@???`
4. **Top 10 frames** — limits sensitivity to deep call stack variation
5. **FNV-1a 64-bit hash** — exception type + normalized stack → 16 hex chars

The same logical crash produces the same fingerprint even when line numbers shift between releases.

### Platform Hooks

| Platform | Hook | Limitation |
|----------|------|------------|
| **Android** | `Thread.setDefaultUncaughtExceptionHandler` | Chains to previous handler |
| **iOS** | `kotlin.native.setUnhandledExceptionHook` | Only catches Kotlin exceptions, not ObjC/Swift crashes |
| **JS/Web** | `window.error` + `window.unhandledrejection` | Page stays alive; async flush |
| **JVM** | `Thread.setDefaultUncaughtExceptionHandler` | Chains to previous handler |

### Attributes on Exception Logs

All exception log records (both caught ERROR and uncaught FATAL) include:

| Attribute | Description |
|-----------|-------------|
| `exception.type` | Exception class name |
| `exception.message` | Exception message |
| `exception.stacktrace` | Full stack trace (OTel semantic convention) |
| `exception.context` | Reporting context (e.g., "uncaught") |
| `crash.fingerprint` | 16-char hex fingerprint for grouping |
| `session.id` | Session identifier |
| `page.name` | Page the user was on when the exception occurred |
| `os.type` | Platform (android, ios, web, jvmssr) |
| `app.version` | `Build.version` |
| `app.debug` | Whether the app is a debug build |

Custom attributes can be added via `TelemetryConfig.exceptionAttributes`:

```kotlin
Telemetry(TelemetryConfig(
    endpoint = "...",
    exceptionAttributes = {
        listOf(
            OtlpKeyValue("user.id", OtlpAnyValue(stringValue = currentUser?.id ?: "anonymous")),
        )
    }
))
```

### Grafana Alert Example

Alert on new crash types using Loki:

```
sum by (crash_fingerprint, service_name) (
  count_over_time({severity="FATAL"} | json | crash_fingerprint != "" [5m])
)
```

Group alerts by `crash.fingerprint` and route to Grafana IRM for incident management.

## Architecture

```
library/src/commonMain/kotlin/com/lightningkite/kiteui/telemetry/
├── Telemetry.kt              # Public API singleton
├── TelemetryConfig.kt        # Configuration data class
├── OtlpModels.kt             # @Serializable OTLP JSON wire format
├── IdGenerator.kt            # Trace/span ID generation
├── TelemetryExporter.kt      # Batching + HTTP export
├── TelemetryLog.kt           # Log interceptor (decorator pattern)
├── TelemetryNavigation.kt    # Page view tracking
├── TelemetryLifecycle.kt     # App lifecycle tracking
├── TelemetryMetrics.kt       # Counter + histogram aggregators
├── CrashFingerprint.kt       # Stack trace normalization + FNV-1a hashing
└── TelemetryCrashHook.kt     # expect declarations for platform crash hooks

library/src/{android,ios,js,jvmSsr}Main/.../telemetry/
└── TelemetryCrashHook.*.kt   # Platform-specific crash hook actuals
```

### Integration Points

- **`fetch-connectivity.kt`** — HTTP spans, latency histograms, and `traceparent` injection happen here
- **`debugger.kt`** — `logInterceptor` var enables the log interceptor chain
- **`AppNavV2.kt`** — Navigation telemetry auto-binds in `appBase()`
- **`Throwable_report`** — Exception capture hooks into the global error handler

### Export Path

Telemetry export uses `suppressConnectivityIssues { fetch(...) }` internally. This means:
- Export failures never trigger the app's `ConnectivityGate` or retry UI
- Export failures are silently logged, never crash the app
- The export path is completely isolated from app networking
