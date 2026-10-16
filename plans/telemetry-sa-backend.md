# KiteUI telemetry on Service Abstractions, shipped through the app's own server

Status: implemented on the `telemetry-*` worktree branches (SA, LS, KiteUI); LSK needs no change. Open questions below were not answered, so the proposals stand. Where implementation differs from this text, see "Implementation notes" at the end.

## Goal

Replace KiteUI's private OTLP client with:
- recording through SA's own API (`telemetryTrace`, ambient attributes, `TelemetryBackend`);
- a client `TelemetryBackend` that queues records durably and POSTs them to an ingest endpoint on the app's own Lightning Server.

The server then adds `user.id` from the session and forwards the records to its configured backend (OTLP today, a self-hosted monitor later).

The changes fix the review findings:
- No write token ships in clients.
- There are no page-long spans or page-long traces.
- Telemetry recorded while offline survives.
- Client and server share one vocabulary.
- Spans get `user.id`.

## Order of work

The work spans three repos, and each step depends on the one before it:

1. SA: basis and otel-jvm (and telemetry-sentry), published to mavenLocal.
2. LS: core and typed.
3. KiteUI.
4. LSK: no code change is expected. Apps regenerate their SDK so it includes the new endpoint.

## 1. Service Abstractions changes

### Does SA's common telemetry work on JS, Android and iOS as it is?

Verified on JS and Android at compile level.
- I temporarily added `api("com.lightningkite.services:basis:1.3.0-2-52932054-1-local")` to KiteUI `library` commonMain.
- I compiled a probe file against it. The probe used `SettingContext`, `SharedResources`, `Namespaced`, `telemetryAttributes`, `telemetryTrace`, `enrich`, `currentTelemetryAttributes` and `TelemetryKeys`.
- `:library:compileKotlinJs`, `:library:compileKotlinJvmSsr` and `:library:compileAndroidMain` all passed, offline.
- I then reverted the probe.

What I did not verify:
- iOS: KiteUI's iOS targets need cinterop and Xcode, which the sandbox doesn't have. SA does publish basis, data and kfile for `iosArm64`, `iosSimulatorArm64` and `iosX64`, and kotlin-logging for iOS is in the Gradle cache.
- The runtime on JS.

Notes:
- `basis` brings in `data`, `kfile` (which uses kotlinx-io) and `kotlin-logging` 8.0.4. KiteUI already uses the same versions of serialization 1.11.0, coroutines 1.11.0 and datetime 0.8.0.
- `TelemetryBackend.flush()` is an uncommitted SA change. It is present in the `1.3.0-2-52932054-1-local` jar but not in `1.3.0-local`.

There is no "common backend base" class. The client driver implements `TelemetryBackend` directly.

The SA changes we do need:

### 1a. Make the ambient attribute element public
File: `basis/src/commonMain/kotlin/com/lightningkite/services/telemetry/telemetryCoroutines.kt`.

The problem: `TelemetryAttributeElement` is `internal`. The only public way to add ambient attributes is to wrap a call in `telemetryAttributes(attrs) { }`.

KiteUI needs to place the attribute bag directly into coroutine contexts that it builds itself:
- every element's `coroutineContext`, built once in `NativeElement.kt:456`, which has no parent job chain;
- the `launch` of an `Action`.

The change: make the class and its constructor `public`. Callers that start a fresh chain pass `parent = null`.

### 1b. Wire model
New file: `basis/src/commonMain/kotlin/com/lightningkite/services/telemetry/TelemetryBatch.kt`.

The model has to live in SA. KiteUI has to produce it and LS has to consume it, and KiteUI doesn't depend on LS.

```kotlin
@Serializable data class TelemetryBatch(
    val resource: TelemetryAttributes,      // service.name/version, os.type, session.id, telemetry.sdk.name="kiteui", app.debug
    val sentAt: Instant,                    // stamped at upload, for clock-skew correction
    val spans: List<RecordedSpan> = listOf(),
    val logs: List<RecordedLog> = listOf(),
    val metrics: List<RecordedMetric> = listOf(),
)
@Serializable data class RecordedSpan(
    val traceId: String, val spanId: String, val parentSpanId: String? = null,
    val owner: String, val operation: String,        // exported name "$owner.$operation", same as OtelTelemetryBackend
    val start: Instant, val end: Instant,
    val attributes: TelemetryAttributes,             // ambient bag + enriched
    val dimensions: Set<String>,                     // RED dimension key names, resolved from attributes
    val outcome: Outcome,                            // Ok, Error, Cancelled
    val error: RecordedError? = null,
    val sampled: Boolean,
)
@Serializable data class RecordedLog(val time: Instant, val level: LogLevel, val message: String,
    val attributes: TelemetryAttributes, val traceId: String? = null, val spanId: String? = null,
    val error: RecordedError? = null)
@Serializable data class RecordedError(val type: String, val message: String, val stacktrace: String, val fingerprint: String)
@Serializable data class RecordedMetric(val owner: String, val name: String, val unit: MetricUnit,
    val kind: Kind /* Counter, Histogram */, val value: Double, val attributes: TelemetryAttributes)
```

Serializing `TelemetryAttributes`:
- `TelemetryAttributes` gets a `KSerializer`.
- The JSON form groups values by type, so `TelemetryKey.Type` survives the round trip: `{"s":{"app.screen.name":"Home"},"l":{...},"d":{...},"b":{...},"sl":{...},...}`.
- Type can't be inferred from a JSON number, because Kotlin/JS writes `2.0` as `2`.

Metric points are sent raw rather than aggregated on the client.
- The server's OTel SDK aggregates them, which deletes KiteUI's aggregators.
- Client metrics are rare. Request, action and screen RED all come from spans.

### 1c. Ingesting client records
- Add `fun ingest(batch: TelemetryBatch)` to `TelemetryBackend`. It is abstract, like `flush()`.
  - `Noop` ignores the batch.
  - `LoggingTelemetryBackend` logs one line per record.
  - `SentryTelemetryBackend` delegates, and also reports `RecordedLog`s that carry an `error` to Sentry.
  - Test fakes implement it.
- `OtelTelemetryBackend` (`otel-jvm/.../OtelTelemetryBackend.kt`) cannot replay a client span through its public API.
  - Spans are always timestamped "now" and get SDK-generated ids.
  - The client's span id must survive, because the server's request span has it as its parent through `traceparent`.
  - So `ingest` builds `SpanData` and `LogRecordData` (small private implementations of those SDK interfaces). They carry the client's ids, timestamps and the client `Resource`, and go straight to the backend's `SpanExporter` and `LogRecordExporter`.
  - Spans are exported only when `sampled` is true.
  - Every span, sampled or not, also records RED through the existing `redFor(owner)`, with the client span's duration. That keeps metrics unbiased by sampling.
  - `RecordedMetric`s go through the SDK meter for `owner`.
  - Timestamps are shifted by `serverNow - batch.sentAt`.
- Constructor change: `OtelTelemetryBackend(sdk, sanitization, spanExporter: SpanExporter?, logExporter: LogRecordExporter?)`.
  - The `register(...)` blocks already hold their exporters, so they pass them in.
  - If the backend was built without exporters (for example, a Java-agent `OpenTelemetry`), `ingest` throws `UnsupportedOperationException`. It fails fast and does not drop data silently.

### 1d. Remote parent for the server's request span
- Today nothing extracts `traceparent`. I grepped SA and LS core and the engines.
- Add `public class TraceParent(val header: String) : AbstractCoroutineContextElement` to basis.
- `OtelTelemetryBackend.span` uses it as the parent only when no span is current. It parses with the `W3CTraceContextPropagator` the SDK is already built with.
- LS sets it (section 2).

## 2. Lightning Server changes

### Server span parented to the client
- File: `core/.../runtime/implementationHelpers.kt`, in `instrumentHttpRequest` (around line 470).
- When the request has a `traceparent` header, run `runtime.telemetryTrace("$method $route", ...)` inside `withContext(TraceParent(header))`.
- The WebSocket connect path gets the same treatment.
- The server already enriches the request span with `user.id`: `SessionManager.read` calls `currentRequestTrace()?.enrich(...)`. That change is uncommitted.

### Ingest endpoint
- New file: `typed/src/main/kotlin/com/lightningkite/lightningserver/typed/ClientTelemetryEndpoints.kt`.
  - It is a `ServerBuilder`, following the `FunnelEndpoints` precedent.
  - It lives in `typed` because it needs `ApiHttpHandler` and `authOrNull`, and `core` already depends on `otel-jvm`.
  - The model is in SA basis, so no `-shared` change is needed.

```kotlin
public class ClientTelemetryEndpoints : ServerBuilder() {
    public val ingest = path.path("ingest").post bind ApiHttpHandler(
        summary = "Ingest client telemetry",
        auth = anyAuth or noAuth,
        implementation = { batch: TelemetryBatch ->
            if (batch.spans.size + batch.logs.size + batch.metrics.size > 2000) throw BadRequestException("Batch too large")
            val verified = TelemetryAttributes {
                putIfNotNull(TelemetryKeys.User.id, authOrNull?.rawId?.toString())
            }
            telemetryBackend.ingest(batch.withoutUserAttributes().let { it.copy(resource = it.resource + verified) })
        }
    )
}
```

- Anonymous clients are accepted, and `user.id` is omitted for them.
- The server strips any client-supplied `user.*` keys, so the client cannot spoof them.
- Apps mount it as `path("telemetry") module ClientTelemetryEndpoints()`.
- Rate limiting uses the existing server-wide `ratelimit` `RateLimitInterceptor`. The endpoint caps record count itself, and engines already cap body size with `maxBodySize`.
- **Bounded names (decided during implementation).** `OtelTelemetryBackend` keeps a meter, tracer and RED instruments per owner name, and the OTel SDK keeps an instrument per metric name. Client-chosen names would otherwise grow those without bound. The fix lives in the endpoint, not the backend:
  - Owner and metric names must match `[A-Za-z][A-Za-z0-9_.-]{0,63}`; otherwise 400.
  - Attribute key names must match the same pattern: the backend caches an OTel key per name.
  - The endpoint remembers the distinct owners, owner/metric pairs and attribute key names it has accepted (in memory, per process, registered atomically) and rejects a batch that would push the count past 1000, with 400. Names already accepted keep working.
  - Also rejected: more than 128 attributes on a record, and trace/span ids that aren't 32/16 lowercase hex.
  - Operation names and dimension values are not capped here: they only become RED attribute values, which the SDK already caps per instrument (2000 series, then an overflow series).
  - Trade-off: a hostile client can fill the 1000 names until restart, after which new legitimate names are rejected (and the client drops those batches). A fixed allowlist would avoid that, but would make every app list KiteUI's internal owner names on the server. Revisit if that ever happens in practice.
- Watch out: `SessionManager.read` throws `UnauthorizedException` for a bad token even on a `noAuth` endpoint. The client therefore treats 401 as retryable (section 3).

## 3. KiteUI client

### Dependency
- `gradle/libs.versions.toml`: add `serviceAbstractions` and `services-basis`.
- `library/build.gradle.kts` commonMain: `api(libs.services.basis)`. It is `api` because `TelemetryAttributes` and `TelemetryBackend` appear in KiteUI's public API.

### Setup (app code)
```kotlin
val telemetry = Telemetry(
    serviceName = "my-app", serviceVersion = Build.version,
    upload = { api.telemetry.ingest(it) },          // app's generated LS client: auth header + URL + serialization
    traceSamplingRate = 1.0,
    tracePropagationHosts = listOf("api.example.com"),
    minLogLevel = LogLevel.Warn,
)
telemetry.install(context, navigator)
```

- Upload goes through the app's own LS client, which is how it gets authenticated.
  - Lightning-server-kiteui (LSK) attaches the access token as an `Authorization` header via its header calculator, not as a cookie.
  - A raw `fetch` from KiteUI would therefore be anonymous.
- `ClientContext` is a new `public object ClientContext : SettingContext`. Its `telemetryBackend` is set by `install` and reset by `shutdown`.
  - It gives every client `Namespaced` owner a context: KiteUI's own owners, and apps' `object Checkout : Namespaced { name = "checkout"; context = ClientContext }`.
  - `publicUrl` and `internalSerializersModule` are unused on the client; they are empty values.
  - The name is an open question.

### `Telemetry` as a `TelemetryBackend`
File: `library/src/commonMain/.../telemetry/Telemetry.kt`, rewritten.

- `span(owner, op, attrs, dims, action)`:
  - Reads its own internal `ClientSpan(traceId, spanId)` coroutine element.
  - If there is a `ClientSpan`, the new span is its child. If not, the new span starts a new trace.
  - Runs `action` under `withContext(ClientSpan(...))`.
  - Enqueues one `RecordedSpan` in `finally`. A cancelled span is recorded with outcome `Cancelled`; for example, a screen load the user left.
- The `TelemetryTrace` handed to `action` works like this:
  - `enrich` adds attributes to the span.
  - `log` enqueues a `RecordedLog` carrying the span's ids.
  - `isLoggable` is `level >= minLogLevel`.
- `counter` and `histogram` enqueue `RecordedMetric`. They project `currentTelemetryAttributes()` to the instrument's dimensions.
- `gauge` and `inFlight` throw `UnsupportedOperationException`. Nothing on the client uses them, and they are not built speculatively.
- `reportError` enqueues an ERROR `RecordedLog`:
  - `RecordedError.fingerprint = CrashFingerprint.generate(t)`.
  - The attribute key becomes `error.fingerprint`, matching the server; it replaces `crash.fingerprint`.
- `flush` drains the buffer into the outbox and uploads (see below).
- Sampling is decided once per session, as today. It sets `RecordedSpan.sampled` and the `traceparent` flags.

### Durable queue (common code, one implementation)
- **Write point.** Records accumulate in memory. When the outbox is written (every 30s, at 100 records, on background, or on crash), the buffer becomes one `TelemetryBatch`, the batch is appended to the outbox, and the outbox is persisted with `PlatformStorage.set("kiteui.telemetry.outbox", json)`. `PlatformStorage` exists on JS (localStorage), Android (SharedPreferences) and iOS (NSUserDefaults).
- **Upload.** Batches are uploaded oldest first, each with `sentAt` stamped at send time. A batch is removed and re-persisted when its upload succeeds.
- **Retry.** Network errors, 5xx, 401, 408 and 429 keep the batch, with backoff from 30s doubling to 5 min. Any other 4xx drops the batch, logged with `LogRoot` so the log interceptor doesn't re-capture it.
- **Size cap.** The outbox is capped at 512 KB serialized. When over the cap, the oldest batches are dropped, and the next batch carries a `telemetry.dropped` counter. Nothing is lost silently.
- **Recursion guard.** The upload runs under `suppressConnectivityIssues { }` plus an internal `TelemetryUploading` coroutine element. The fetch interceptor passes it straight through, which avoids recording a span per upload forever. Today this is done by calling `platformFetch` directly; the upload now goes through the app's client, so that trick no longer works.

Per platform:
- **JS.** `AppState.inForeground` already follows `visibilitychange` (`AppState.js.kt:164`).
  - On hide, the buffer is persisted synchronously to localStorage, then an async upload is attempted.
  - No `sendBeacon` or keepalive is needed: whatever doesn't send is still in localStorage for the next load.
  - This deletes the `sendBeacon` path, which couldn't send auth headers anyway.
- **Android.**
  - Background (`KiteUiActivity.kt:283`) triggers the same persist-then-upload.
  - `PlatformStorage.set` uses `apply()`, which is asynchronous, and the crash path runs just before process death. So `TelemetryCrashHook.android.kt` writes the outbox with `edit().commit()` itself.
- **iOS.** The crash path calls `NSUserDefaults.standardUserDefaults.synchronize()` after persisting, in `TelemetryCrashHook.ios.kt`. Background works as on the other platforms.
- **JVM (Compose desktop).** It uses `library-compose`'s `PlatformStorage.jvm.kt`. The crash hook in `commonJvmMain` persists. Whether compose picks up `commonJvmMain`'s crash hook still needs checking during implementation.
- **SSR (`jvmSsr`).** `PlatformStorage` is a no-op there and telemetry isn't installed; no change.
- **Web, multiple tabs.** All tabs share one key, so concurrent tabs can race and duplicate or lose a batch. This is accepted for now (see the open questions).

### Screen visits
- New `ElementContext.telemetryAttributes: TelemetryAttributes` addon. It inherits down the `ChainMap`, like `pageNavigator`.
- `NativeElement.kt:483` replaces `TelemetryContext(element = ...)` with `TelemetryAttributeElement(context.telemetryAttributes, null)`. Every coroutine an element launches, including reactive loads and the fetches inside them, then carries the screen's attributes.
- `navigatorView` (`views/l2/NavigatorView.kt`) sets the addon inside `with(split())`, next to `context.pageNavigator = navigator`: `TelemetryKeys.App.screenName` = page class simple name, and `TelemetryKeys.App.screenId` = fresh random id per visit.
- What a visit records, all short:
  - **Screen load.**
    - Owner `screen`, op `load`, launched in the screen's scope: `telemetryTrace("load") { loaded.await() }`.
    - It ends when the screen is usable, or as `Cancelled` if the user navigates away first.
    - "Usable" means the screen subtree has no background process that is not ready. `Processes` in `NativeElement.kt:632` is per element and not hierarchical. So a small `ScreenLoad` counter in the screen's context addons is incremented and decremented by each element's background `Processes` when its not-ready count crosses zero.
    - Capped at 60s. This is open question 2.
  - **View started / view ended.** Info-level `RecordedLog`s with `event.name` = `app.screen.view_start` / `app.screen.view_end`. They are written on screen element startup and on job completion, best effort.
  - **Actions and requests.** Each is its own span (below).
- Logs from the non-suspend `Log` interceptor have no coroutine context. They take the current top screen's attributes, which the driver tracks from the main navigator's stack. They carry no trace ids, which fixes the overlapping-action mis-attribution.

### Hook mapping

| Today | After |
|---|---|
| `fetchInterceptors` → `instrumentFetch` | Kept. Runs `http.telemetryTrace(method.name, attrs) { proceed(...) }`. URL credentials and query are still stripped (`withoutUserInfo` is kept). `traceparent` comes from `ClientSpan` and is sent only to allowlisted hosts. Inside an action the request is a child of that action; otherwise it starts its own trace. Attributes use `TelemetryKeys.Http/Url/Server` keys. The `http.client.request.duration` histogram is replaced by RED from the span. |
| `actionInstrumentors` + `ActionInstrumentation(context, onStart, onEnd)` | The type is replaced by an interceptor `suspend (scope, title, proceed) -> Unit`, which is needed to wrap `telemetryTrace` around the body. It has no other users (grep). It runs at both call sites, `Action.kt:222` and `Action.kt:315`. Telemetry's interceptor copies `scope.coroutineContext[TelemetryAttributeElement]` into the action, because actions launch in `AppScope` by default. Then it runs `action.telemetryTrace(title, viewPath attr) { proceed() }`. The `action.invocations` counter is replaced by RED. |
| Navigation listener: `page: X` span, trace rotation, `navigation.page_views` counter | Screen-visit model above. `page_views` = RED count of `screen.load`. |
| `Throwable_report` hook, `ExceptionHandler` interceptor | Kept. Both call `reportError`. The `ExceptionHandler` path adds the `meta.source` element's ambient attributes and `view.path`. |
| `Log.interceptors` | Kept, as described above. `setVerboseLogging` is kept if used, otherwise deleted. |
| Connectivity listener | Kept. `connectivity.issues` counter plus a WARN log. |
| Cold start span, foreground session span, foreground counter | Cold start: short `app.start` span. The long foreground span is deleted; foreground and background become info events, and background triggers persist and upload. |
| Crash hook + `blockingFlush` (3s network) | The hook is kept. It records the throwable as ERROR with `exception.escaped=true`, then persists synchronously. No network. |
| `CrashFingerprint` | Kept, unchanged. |
| Public `span(name, attrs) {}` | Deleted. Use `Owner.telemetryTrace(...)` / `telemetryAttributes(...)`. |
| `exceptionAttributes` config | Deleted. `user.id` comes from the server; app-wide attributes go in the root `ElementContext.telemetryAttributes`. |

### Deleted
- From `library/src/commonMain/.../telemetry/`:
  - `OtlpModels.kt`
  - `TelemetryExporter.kt`
  - `TelemetryMetrics.kt`
  - `Span.kt`
  - `TelemetryContext.kt`
  - `TelemetryConfig.kt`
- The JS `sendBeacon` `blockingFlush`, and `blockingFlush` on every platform. It is replaced by `persistNow()`.
- The `activeTraceId` and `activeSpanId` globals.
- The `try { exceptionAttributes() } catch (_: Exception) {}`.
- `ActionInstrumentation`.
- Tests: `OtlpModelsTest`, `TelemetryMetricsTest`, `TracePerPageTest`, `SpanTest`, `TelemetryContextTest`, `TelemetryExporterTest`, `InteractionSpanTest`, `TelemetryConfigureTest`. Their replacements are listed in section 5.
- `docs/TELEMETRY.md` is rewritten, removing the Grafana-token setup and the PromQL for the old metric names.
- `example-app/.../TelemetryTestPage.kt` is updated to `telemetryTrace`.

## 4. Migration for existing apps
1. **Revoke and rotate any Grafana or OTLP token that shipped in a client build.** It is public now.
2. Server:
   - Upgrade LS.
   - Mount `ClientTelemetryEndpoints`.
   - Point `telemetry` settings at OTLP, with credentials kept server-side.
   - If CORS `limitToHeaders` isn't `*`, add `traceparent`.
3. Client:
   - Upgrade KiteUI.
   - Regenerate the SDK so it includes `telemetry.ingest`.
   - Replace `Telemetry(TelemetryConfig(endpoint, headers, ...))` with the constructor shown in section 3.
4. Code:
   - `span("x") { }` becomes `SomeOwner.telemetryTrace("x") { }`.
   - `exceptionAttributes` is removed.
   - Dashboards change too: `page.name` becomes `app.screen.name`, `crash.fingerprint` becomes `error.fingerprint`, and the old histogram and counters become RED metrics `<owner>.client.operation.{count,duration}`.

## 5. Test plan

**SA**
- basis `commonTest`:
  - `TelemetryAttributes` serializer round trip for all 8 types, including whole-valued Doubles.
  - `TelemetryBatch` round trip.
  - These should run on JS as well as JVM.
- otel-jvm, with an in-memory exporter:
  - `ingest` keeps the client trace id, span id, parent and timestamps.
  - Unsampled spans are not exported but still record RED.
  - Logs are exported with trace ids.
  - A span with `TraceParent` and no current span gets the remote parent; nested spans still parent locally.
  - Building without exporters makes `ingest` throw.

**LS** (`typed` test, `ServerRuntime` test harness)
- Anonymous ingest has no `user.id`.
- Authenticated ingest gets `user.id`.
- A client-sent `user.id` is stripped.
- A batch over the record cap is rejected.
- Client-to-server linking: a request with `traceparent` produces a server span whose parent is the client span id.

**KiteUI** (`commonTest`, run on `jvmSsrTest` like today's telemetry tests)
- Span nesting:
  - No enclosing span means a new trace.
  - A request inside an action is the action's child.
  - A request in an element scope is a root.
- `traceparent` only goes to allowlisted hosts, and its flags follow sampling.
- URL redaction (keep `TelemetryUrlRedactionTest`).
- The action interceptor carries the screen's ambient attributes even though the action launches in `AppScope`.
- Screen visit:
  - The element coroutine context has `screenName` and `screenId`.
  - The load span ends when background processes settle.
  - It is `Cancelled` on navigate-away.
- `reportError` includes the fingerprint (keep `CrashFingerprintTest`).
- The log interceptor gets the current screen and no trace ids (adapt `TelemetryLogTest`).
- The upload recursion guard means no span is recorded for an upload.

**Outbox**
- `PlatformStorage` is a no-op on `jvmSsr`, so outbox tests go in `jsTest`, which has real localStorage.
- Cases: persist then reload, retry and backoff, drop on 4xx, size cap with `telemetry.dropped`.
- I have not checked that `jsTest` runs in this sandbox.

**Manual**
- Example app on web plus `test-server` with the ingest endpoint and the `dev` telemetry backend:
  - Batches arrive with `user.id`.
  - Client spans and server spans share trace ids.
  - Records made while DevTools is offline, followed by a reload, are delivered.
  - Hiding the tab persists the buffer.
- Android and iOS: the user runs them, since the sandbox has no iOS toolchain or emulator.

## 6. Open questions
1. **Requests inside an action.** The direction said "each request its own trace". This spec makes a request inside an action a child of the action, so the trace is action-sized: one click's action span, its requests, and their server spans. Requests outside any action are their own trace. Is that acceptable, or should every request be a root, with a link or attribute pointing back to the action?
2. **What "usable" means for screen load.** The proposal is that no background (loading) process is pending in the screen subtree, capped at 60s. It needs a small hook in `NativeElement.Processes`. The alternative is to end at first render (render returned plus one frame), which is simpler but measures less.
3. **Raw metric points versus client-side aggregation.** Raw points are proposed because they are simpler and client metrics are rare. Is that acceptable?
4. **The client `SettingContext` object.** It needs a name (`ClientContext`?). Should it live in KiteUI, or in SA basis so non-KiteUI clients could share it? I recommend KiteUI.
5. **Multi-tab web outbox race.** Accept it, or key the outbox per tab and have the next load adopt orphaned keys? Adopting would need key listing, which `PlatformStorage` lacks.
6. **Clock-skew correction via `sentAt`.** It is in the spec because device clocks are often off, which breaks client and server span alignment. Keep it?
7. **Ingest request span.** Each ingest POST creates its own server request span, which is noise. Leave it, or have LS skip tracing that route?
8. **Fingerprints.** SA has a JVM-only `Throwable.errorFingerprint()`. Should KiteUI's common `CrashFingerprint` move into SA basis and replace it, so client and server share one fingerprint? Suggested as a follow-up, not part of this work.
9. **Upload failure classification (new).** See implementation note 1. Should LSK ship the `LsErrorException` mapping as a helper so apps don't each copy it?

## Implementation notes

Where the implementation differs from the text above:

1. **Who decides retry versus drop.** KiteUI can't see HTTP statuses through the app's generated client, so the `upload` lambda decides: throwing keeps the batch and retries with backoff; returning forgets it. The documented setup catches `LsErrorException` and returns for permanent 4xx (not 401/408/429). KiteUI itself does no status classification.
2. **SDK name.** The generated client exposes the endpoint as `api.clientTelemetry.ingest(batch)` (named after the class), not `api.telemetry.ingest`.
3. **SA additions.** `TelemetryAttributes.filterKeys` (LS uses it to strip `user.*`; the map constructor is private). `OtelTelemetryBackend` takes `spanExporter`/`logExporter`; every registered URL scheme passes the same rate-limited/safe exporters its SDK uses. `TraceParent` is parsed with `W3CTraceContextPropagator` directly, since the `otlp-*` SDKs are built without propagators.
4. **No `view.path` on action and request spans.** The per-element `TelemetryContext` that carried the element is gone; spans carry the screen's name and visit id instead. Exception-handler errors still get `view.path` from the handler's source element.
5. **Screen visit.** `navigatorView` starts the visit in the swap view's scope and cancels it on the next swap. Usability is tracked per element: `Processes.notReadyCount` crossing zero calls `ScreenLoad.startedLoading`/`finishedLoading`, which also covers an element shut down mid-load (its processes are released). The 60 s cap ends the span as `Error` (a `ScreenLoadTimeout`); the visit itself continues. View start/end events are recorded regardless of `minLogLevel`.
6. **Connectivity metric.** Recorded as owner `app`, metric `connectivity.issues`.
7. **Tests not run.** `jsTest` (needs Karma/Chrome via npm, unreachable from the sandbox), so outbox persistence across reloads is untested; iOS; the manual end-to-end run.
8. **Sampling.** The server's sampler links a remote parent (the client's `traceparent`) but ignores its sampled flag: a client could otherwise hide the server's spans (`-00`) or bypass its ratio (`-01`). Server request spans under an unsampled client span therefore have a parent that was never exported.
9. **Thread safety.** The outbox is guarded by an atomicfu lock (new KiteUI dependency `org.jetbrains.kotlinx:atomicfu`), since records arrive from background dispatchers and crash hooks. Storage write failures are logged, not thrown into app code.
10. **Known gaps (from review, not fixed).**
    - Client owners share RED instruments with server owners of the same name, so a client could add points to server dashboards. Prefixing ingested owners would rename the documented metrics.
    - Batches are attributed to whoever is signed in at upload, not at recording; the docs say to `flush()` before sign-out.
    - The retry/drop policy lives in each app's `upload` lambda (open question 9).
    - Ingested spans bypass the server SDK's `SpanLimits` (the 128-attribute cap bounds count, not value length).
