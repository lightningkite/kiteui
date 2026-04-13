package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.HttpMethod
import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.fetch
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.reactive.invoke
import com.lightningkite.kiteui.telemetry.span
import com.lightningkite.kiteui.views.ElementWriter
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.important
import com.lightningkite.reactive.core.Constant
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.Signal
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Routable("/internal/telemetry-test")
object TelemetryTestPage : Page {
    override val title: Reactive<String> = Constant("Telemetry Test")
    override fun ElementWriter.CanAddTheme.render() {
        val result = Signal("")
        scrolling.col {
            h1("Telemetry Test")
            text("Buttons for manually testing telemetry traces, spans, and metrics. Check Grafana after clicking.")
            separator()

            h2("Action Spans")
            text("Each button click creates an interaction span as a child of the page span.")
            row {
                important.button {
                    text("+")
                    action = Action("Increment", Icon.add, frequencyCap = 0.milliseconds) {
                        result.value = "incremented"
                    }
                }
                important.button {
                    text("-")
                    action = Action("Decrement", Icon.remove, frequencyCap = 0.milliseconds) {
                        result.value = "decremented"
                    }
                }
            }

            separator()
            h2("HTTP Spans")
            text("HTTP calls become children of their triggering action span.")
            button {
                text("HTTP Success (same-origin)")
                onClick("Fetch Self", frequencyCap = 0.milliseconds) {
                    val response = fetch("/", method = HttpMethod.GET)
                    result.value = "HTTP ${response.status}"
                }
            }
            button {
                text("HTTP Cross-Origin (will fail CORS)")
                onClick("Fetch External", frequencyCap = 0.milliseconds) {
                    val response = fetch("https://httpstat.us/200", method = HttpMethod.GET)
                    result.value = "HTTP ${response.status}"
                }
            }

            separator()
            h2("Custom span()")
            text("Sequential custom spans become children of the action span.")
            button {
                text("3 Sequential Spans")
                onClick("Multi-Step Work", frequencyCap = 0.milliseconds) {
                    span("validate") { delay(50) }
                    span("transform") { delay(100) }
                    span("save") { delay(75) }
                    result.value = "3 spans done"
                }
            }

            separator()
            h2("Nested span()")
            text("Nested spans form a parent-child hierarchy. HTTP inside a span becomes its child.")
            button {
                text("Nested Spans + HTTP")
                onClick("Nested Work", frequencyCap = 0.milliseconds) {
                    span("outer-work") {
                        span("inner-fetch") {
                            fetch("/", method = HttpMethod.GET)
                        }
                        span("inner-compute") {
                            delay(30)
                        }
                    }
                    result.value = "nested spans done"
                }
            }

            separator()
            h2("Error Spans")
            text("Errors set STATUS_CODE_ERROR on the span and propagate up through nested spans.")
            button {
                text("Span With Error")
                onClick("Failing Work", frequencyCap = 0.milliseconds) {
                    try {
                        span("will-fail") {
                            error("intentional test error")
                        }
                    } catch (_: Exception) {
                        // caught
                    }
                    result.value = "error span done"
                }
            }

            separator()
            text { ::content { "Result: ${result()}" } }
        }
    }
}
