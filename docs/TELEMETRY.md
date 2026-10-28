# KiteUI Telemetry

KiteUI records spans and logs on the device and sends them through your own server, as Service Abstractions'
`TelemetryBatch` (from the client-safe `telemetry-shared` module). The server adds the signed-in user and
forwards everything to its configured telemetry backend (OTLP, Sentry, ...). No telemetry credentials ship in
the app.

## Setup

Server (Lightning Server):

```kotlin
val cache = setting("cache", Cache.Settings())
val telemetry = path.path("telemetry") module ClientTelemetryEndpoints(cache)
```

The server decides who records are from: `service.name` is `<projectName>-client`, `user.id` comes from the
caller's authentication, and of the client's resource attributes only `service.version`, `os.type`,
`session.id` and `app.debug` are kept. A client can't pose as the server or as another user.

Uploads are rate limited per signed-in user, else per IP, using the `cache`: a burst of about 24 uploads
(enough to drain an outbox saved while offline), then one per 5 seconds; clients normally upload every 30
seconds. A 429 is retried later like any other failure. Set the `clientTelemetryRateLimit` setting to null to
turn the limit off.

Point the server's `telemetry` setting at your backend, with its credentials kept server-side. If CORS
`limitToHeaders` isn't `*`, add `traceparent`.

Client, after regenerating the SDK so it includes `clientTelemetry.ingest`:

```kotlin
val telemetry = Telemetry(
    serviceVersion = Build.version,
    upload = telemetryUpload { api.clientTelemetry.ingest(it) },
    tracePropagationHosts = listOf("api.example.com"),
)
telemetry.install(context, navigator)
```

`upload` sends one batch: throwing keeps the batch for a retry, returning drops it. `telemetryUpload` (from
lightning-server-kiteui) drops batches the server refused for good (a 4xx other than 401, 408 or 429) and
retries everything else, including rate-limit 429s.

Upload goes through your generated client so it carries the user's `Authorization` header. Anonymous
uploads are accepted and simply have no `user.id`. The user is whoever is signed in when a batch is
*uploaded*, so call `telemetry.flush()` before signing out, or records made before sign-out (or offline) are
attributed to the next user. `flush()` waits for an upload already in progress and throws if the upload fails,
for example offline; the records stay saved and are sent by a later upload.

Install before the first `navigatorView` renders; screens shown earlier aren't recorded. During server-side
rendering `install` does nothing, so it's safe to call from `app()`.

## What's recorded

| What | How |
|---|---|
| HTTP requests | Span `http.<METHOD>` with `http.request.method`, `url.path` (no query, no credentials), `server.address`, `http.response.status_code` |
| Actions | Span `action.<title>`; requests made inside it are its children |
| Screen visits | Span `screen.load`, ending when the screen has rendered and nothing in it is loading, `Cancelled` if the user leaves first, or an `Error` after 60 s. A screen with something that stays loading, such as a `LateInitSignal` waiting for input, always ends in that error. Events `app.screen.view_start` / `app.screen.view_end` |
| App start | Span `app.start`, from constructing `Telemetry` to `install` |
| Logs | `Log` calls at `minLogLevel` and above (default `Warn`), labelled with the current screen |
| Errors | Reported exceptions and exception-handler errors, as ERROR logs with a fingerprint |
| Crashes | Uncaught exceptions, as ERROR logs with `exception.escaped = true`, saved to disk before the process dies |
| Lifecycle | Events `app.foreground` / `app.background`; connectivity issues as WARN logs |

Clients record no metrics, and the server turns none of their spans into metrics: clients are untrusted, and
metric cardinality drives cost on observability platforms. Query spans and logs instead (e.g. page views are
`screen.load` spans).

### Traces

A trace starts at an action, a screen load, or a request made outside any span. Requests inside an action are
its children, and with `tracePropagationHosts` the server's request span continues the same trace.

Sampling is decided once per session (`traceSamplingRate`); unsampled sessions record no spans. The server
links its spans to the client's but samples them by its own rules.

### Screen attributes

`navigatorView` sets `app.screen.name` and a per-visit `app.screen.id` on its screen's `ElementContext`. Every
coroutine an element launches carries them, including actions started from it, so spans recorded there are
labelled with the screen. Add app-wide attributes to the root context:

```kotlin
context.telemetryAttributes += telemetryAttributesOf(TelemetryKey.OfString("app.flavor") to "beta")
```

## Your own telemetry

Record a span with `span(owner, operation)`. It becomes a child of the current span (an action, say), and
requests made inside it become its children:

```kotlin
span("checkout", "submit", telemetryAttributesOf(TelemetryKey.OfString("checkout.method") to "card")) {
    submit()
}
```

Without an installed `Telemetry`, `span` just runs its block. There is no client metrics API.

The server rejects a whole batch if an owner or attribute name doesn't match `[A-Za-z][A-Za-z0-9_.-]{0,63}`, a
record has more than 128 attributes, or an attribute value is longer than 4096 characters. So `span` throws for an
invalid owner, and before a record is saved, attributes with invalid names or past the 128th are dropped and longer
values are cut to 4096 characters.

## Delivery

Records are batched in memory and saved to `PlatformStorage` every 30 seconds, at 100 records, when the app is
backgrounded or hidden, and on a crash. Saved batches are uploaded oldest first and removed only once
`upload` returns, so telemetry recorded offline or just before the app dies is sent on a later run. Failed
uploads back off from 30 seconds to 5 minutes.

The saved outbox is capped at 512 KB; when it overflows, the oldest batches are dropped and the next batch
carries a WARN log saying how many records were lost (`telemetry.dropped_records`).

On the web, tabs share one outbox, so concurrent tabs can occasionally duplicate or lose a batch.

## Crash fingerprints

Stack traces are normalized before hashing so the same crash groups together, even across builds that only
moved code within a file:

1. Exception messages dropped; the exception types and their cause chain are kept
2. Line and column numbers stripped: `Foo.kt:42` becomes `Foo.kt:?`, and `app.js:1:2345` becomes `app.js:?`
3. Memory addresses stripped: `0x1a2b3c` becomes `0x???`
4. Identity hashes stripped: `@deadbeef` becomes `@???`
5. Top 10 lines only
6. FNV-1a 64-bit hash of the cause chain + normalized stack, as 16 hex chars

Minified JS renames functions between builds, so web fingerprints usually change when the bundle does.

The fingerprint becomes the record's `error.fingerprint`, the same attribute server errors use.

| Platform | Crash hook | Limitation |
|---|---|---|
| Android | `Thread.setDefaultUncaughtExceptionHandler` | Chains to the previous handler |
| Desktop (JVM) | `Thread.setDefaultUncaughtExceptionHandler` | Chains to the previous handler |
| iOS | `kotlin.native.setUnhandledExceptionHook` | Only Kotlin exceptions, not ObjC/Swift crashes |
| Web | `window.error` + `window.unhandledrejection` | |

## Migrating from the OTLP exporter

1. Revoke and rotate any Grafana or OTLP token that shipped in a client build. It is public.
2. Mount `ClientTelemetryEndpoints` on the server and regenerate the SDK.
3. Replace `Telemetry(TelemetryConfig(endpoint, headers, ...))` with the constructor above. Custom
   `counter`/`histogram` calls must become spans or logs.
4. Replace `span("x") { }` with `span("owner", "x") { }`. `exceptionAttributes` is gone; `user.id`
   comes from the server, and app-wide attributes go on the root `ElementContext.telemetryAttributes`.
5. Update dashboards: `page.name` is now `app.screen.name`, `crash.fingerprint` is `error.fingerprint`, and
   the client metrics `http.client.request.duration`, `navigation.page_views`, `action.invocations` and
   `connectivity.issues` are gone; use the `http.*`, `screen.load` and `action.*` spans and the connectivity
   WARN logs.
