package com.lightningkite.kiteui.exceptions

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.exceptions.ExceptionHandler.Companion.messageDialog
import com.lightningkite.kiteui.locale.RenderSize
import com.lightningkite.kiteui.locale.renderToString
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.dialog
import com.lightningkite.kiteui.views.l2.toast
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.Release
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

/**
 * A hierarchical tree of exception handlers and exception-to-message converters.
 *
 * Handlers and converters are stored in priority order, with specific exception types
 * taking precedence over generic handlers. When handling an exception, the tree first
 * checks local handlers, then falls back to parent handlers if no match is found.
 *
 * @param parent Optional parent tree to fall back to when no local handler matches
 */
class ExceptionHandlersTree(private val parent: ExceptionHandlersTree? = null) {
    private val handlers = ArrayList<ExceptionHandler>()
    private val messages = ArrayList<ExceptionToMessage>()

    fun add(handler: ExceptionHandler) {
        handlers.add(handler)
        handlers.sortWith(handlerComparator)
    }

    fun add(message: ExceptionToMessage) {
        messages.add(message)
        messages.sortWith(messageComparator)
    }

    operator fun plusAssign(handler: ExceptionHandler) = add(handler)

    operator fun plusAssign(message: ExceptionToMessage) = add(message)

    /**
     * Attempts to find and invoke a matching [ExceptionHandler] in this tree to handle the exception, short-circuiting.
     *
     * @return A [Release] if a matching handler was found, `null` if no match. The returned [Release] should be called to release the handler and return everything to its original state once the error has been handled/changed.
     */
    fun handle(context: ElementContext, exception: Exception, metadata: ExceptionHandler.Metadata?): Release? =
        handlers.firstNotNullOfOrNull { it.handle(context, exception, metadata) } ?: parent?.handle(context, exception, metadata)

    /**
     * Attempts to find a matching [ExceptionToMessage] converter in this tree to convert the exception, short-circuiting.
     *
     * @return An [ExceptionMessage] if a handler was found matching the exception, or `null` if no handler was found.
     */
    fun message(context: ElementContext, exception: Exception, metadata: ExceptionHandler.Metadata?): ExceptionMessage? =
        messages.firstNotNullOfOrNull { it.message(context, exception, metadata) } ?: parent?.message(context, exception, metadata)

    operator fun contains(handler: ExceptionHandler): Boolean {
        return handlers.contains(handler) || parent?.contains(handler) == true
    }

    operator fun contains(message: ExceptionToMessage): Boolean {
        return messages.contains(message) || parent?.contains(message) == true
    }
}

private val handlerComparator = compareByDescending<ExceptionHandler> { it.forSpecificException }.thenByDescending { it.priority }

private val messageComparator = compareByDescending<ExceptionToMessage> { it.forSpecificException }.thenByDescending { it.priority }


/**
 * Installs
 * - [ExceptionToMessage.plainTextException]
 * - [ExceptionHandler.messageDialog]
 * - [ExceptionToMessage.unexpectedError]
 * */
fun ExceptionHandlersTree.installStandardHandlers() {
    add(ExceptionHandler.messageDialog)
    add(ExceptionToMessage.plainTextException)
    add(ExceptionToMessage.unexpectedError)
}

/**
 * Installs
 * - [ExceptionToMessage.plainTextException]
 * - [ExceptionHandler.stacktraceDialog]
 * - [ExceptionToMessage.debugInformation]
 * */
fun ExceptionHandlersTree.installDebugHandlers() {
    add(ExceptionHandler.stacktraceDialog)
    add(ExceptionToMessage.plainTextException)
    add(ExceptionToMessage.debugInformation)
}

/**
 * Calls [installDebugHandlers] if `Platform.isDevelopment == true`, otherwise calls [installStandardHandlers].
 * */
fun ExceptionHandlersTree.installSmartHandlers() {
    if (Platform.isDevelopment) installDebugHandlers()
    else installStandardHandlers()
}


/**
 * Handles exceptions by performing some action (e.g., showing a dialog, logging, etc.).
 *
 * Handlers are tried in order of specificity and priority. Specific exception type handlers
 * are tried before generic handlers, and within each category, higher priority handlers are
 * tried first.
 */
interface ExceptionHandler {
    /**
     * Priority value for ordering handlers. Higher values are tried first.
     * Default priority is 0.5 when using factory functions.
     */
    val priority: Float

    /**
     * Whether this handler is for a specific exception type (true) or all exceptions (false).
     * Specific handlers are always tried before generic handlers.
     */
    val forSpecificException get() = false

    /**
     * Attempts to handle the given exception.
     *
     * @param context The element context where the exception occurred
     * @param exception The exception to handle
     * @return A Release to clean up resources when done, or `null` if this handler can't handle it
     */
    fun handle(context: ElementContext, exception: Exception, metadata: Metadata?): Release?

    data class Metadata(
        val source: Element?,
        val process: Reactive<*>?,
        val foregroundProcess: Boolean?,
        val context: Map<String, String> = emptyMap()
    )

    companion object {
        /**
         * Standard fallback exception handler that displays a dialog with the exception message.
         * Displays title, body, any associated actions, and a close button.
         *
         * Requires an [ExceptionToMessage] handler for the exception.
         *
         * Suitable for production use.
         */
        val messageDialog = ExceptionHandler(0f) { exception, meta ->
            val message = exceptionMessage(exception, meta) ?: return@ExceptionHandler null

            dialog { close ->
                col {
                    h1(message.title)
                    text(message.body)

                    space(0.5)

                    row {
                        for (action in message.actions) centered.important.buttonTheme.button {
                            text(action.title)
                            this.action = action
                        }
                        centered.card.buttonTheme.button {
                            text("Close")
                            onClick { close() }
                        }
                    }
                }
            }

            return@ExceptionHandler {}
        }

        /**
         * Debug exception handler that shows a dialog with the exception message and full stacktrace.
         * Similar to [messageDialog] but includes a scrollable stacktrace view.
         *
         * Requires an [ExceptionToMessage] handler for the exception.
         *
         * Suitable for development/debugging.
         */
        val stacktraceDialog = ExceptionHandler(0f) { exception, meta ->
            val message = exceptionMessage(exception, meta) ?: return@ExceptionHandler null

            dialog { close ->
                col {
                    h1(message.title)
                    text(message.body)

                    sizeConstraints(maxHeight = 15.rem).scrolling.subtext(exception.stackTraceToString())

                    space(0.5)

                    row {
                        for (action in message.actions) centered.important.buttonTheme.button {
                            text(action.title)
                            this.action = action
                        }
                        centered.card.buttonTheme.button {
                            text("Close")
                            onClick { close() }
                        }
                    }
                }
            }

            return@ExceptionHandler {}
        }
    }
}

/**
 * Converts exceptions into user-friendly messages that can be displayed to users.
 *
 * Converters are tried in order of specificity and priority. Specific exception type converters
 * are tried before generic converters, and within each category, higher priority converters are
 * tried first.
 *
 * @see ExceptionMessage
 */
interface ExceptionToMessage {
    /**
     * Priority value for ordering converters. Higher values are tried first.
     * Default priority is 0.5 (or 0.6 for specific types) when using factory functions.
     */
    val priority: Float

    /**
     * Whether this converter is for a specific exception type (true) or all exceptions (false).
     * Specific converters are always tried before generic converters.
     */
    val forSpecificException get() = false

    /**
     * Attempts to convert the given exception into a user-friendly message.
     *
     * @param context The element context where the exception occurred
     * @param exception The exception to convert
     * @return An ExceptionMessage with title, body, and actions, or null if not convertible
     */
    fun message(context: ElementContext, exception: Exception, metadata: ExceptionHandler.Metadata?): ExceptionMessage?

    companion object {
        /**
         * Converter for [PlainTextException] that extracts the title, message, and actions.
         */
        val plainTextException = ExceptionToMessage<PlainTextException>(0f) {
            ExceptionMessage(it.title, it.message, it.actions)
        }

        /**
         * Fallback converter that produces a generic error message for any exception.
         * Always returns a message, making it suitable as a last-resort converter.
         *
         * Suitable for production use.
         */
        val unexpectedError = ExceptionToMessage(0f) {
            ExceptionMessage(
                "Error",
                "An unexpected error occurred."
            )
        }

        /**
         * Debug converter that includes detailed exception information including message,
         * cause, and optionally stacktrace. The stacktrace is included unless the
         * [ExceptionHandler.stacktraceDialog] handler is being used (to avoid redundancy). Also
         * provides actions to export a detailed report of the error.
         *
         * Suitable for development/debugging.
         */
        val debugInformation = ExceptionToMessage(0f) { e, meta ->       // TODO: Most of this functionality should live in the stacktraceDialog handler an be reported to Otel
            val usingStacktraceDialog = exceptionHandlers.contains(ExceptionHandler.stacktraceDialog)

            fun report(): String = buildString {
                appendLine(appName?.let { "# Error Report for $it" } ?: "# Error Report")
                appendLine()
                appendLine("## Metadata")
                appendLine("__Timestamp__: ${Clock.System.now().renderToString(RenderSize.Full)} ${TimeZone.currentSystemDefault().id}")
                meta?.source?.let { s ->
                    val options = Element.DriverSnapshotOptions(
                        includeThemes = false,
                        includeActions = false
                    )

                    val path = generateSequence(s) { it.parent }
                        .map { it.driverDisplay(options) }
                        .toList()
                        .reversed()
                        .joinToString("/")

                    val display = s.driverDisplay(Element.DriverSnapshotOptions(includeThemes = true, includeActions = true))

                    appendLine("__Source Element__: $s ($display) @ $path")
                }
                meta?.process?.let { p ->
                    append("__Process__: $p")
                    meta.foregroundProcess?.let { if (it) append(" (Foreground Process)") else append(" (Background Process)") }
                    appendLine()
                }
                meta?.context?.takeUnless { it.isEmpty() }?.let { c ->
                    appendLine("__Context__: $c")
                }
                appendLine()
                appendLine("## Summary")
                appendLine("__Error__: $e")
                e.cause?.let { appendLine("__Caused By__: $it") }
                appendLine("__Stacktrace__")
                appendLine(e.stackTraceToString())
            }

            ExceptionMessage(
                "Error: $e",
                listOfNotNull(
                    e.message,
                    e.cause?.let { "Caused By: $it" },
                    if (usingStacktraceDialog) null else "Stacktrace: ${e.stackTraceToString()}"
                ).joinToString("\n"),
                actions = listOfNotNull(
                    if (Platform.probablyAppleUser) null
                    else Action(
                        if (AppState.windowInfo.value.width >= 40.rem) "Copy to Clipboard"
                        else "Copy"
                    ) {
                        setClipboardText(report())
                        toast("Copied to Clipboard!")
                    },
                    Action("Export") {
                        download("error_export.md", report().toBlob())
                    }
                )
            )
        }
    }
}

/**
 * A user-friendly representation of an exception with a title, body text, and optional actions.
 *
 * @property title The error title to display prominently
 * @property body The detailed error message body
 * @property actions Optional list of actions the user can take in response to the error
 *
 * @see ExceptionToMessage
 */
data class ExceptionMessage(
    val title: String,
    val body: String,
    val actions: List<Action> = emptyList()
)

/**
 * An exception that carries user-friendly display information.
 *
 * This is an easy way to display an error to a user using the built-in [ExceptionToMessage.plainTextException] handler.
 *
 * @property message The detailed error message body
 * @property title The error title (defaults to "Error")
 * @property actions Optional list of actions the user can take to resolve the error
 */
open class PlainTextException(
    override val message: String,
    val title: String = "Error",
    val actions: List<Action> = emptyList()
): Exception(message)


fun ExceptionHandler(priority: Float = 0.5f, handler: ElementContext.(Exception) -> Release?): ExceptionHandler =
    object : ExceptionHandler {
        override val priority: Float = priority
        override val forSpecificException: Boolean = false
        override fun handle(context: ElementContext, exception: Exception, metadata: ExceptionHandler.Metadata?): Release? = context.handler(exception)
    }

fun ExceptionHandler(priority: Float = 0.5f, handler: ElementContext.(Exception, ExceptionHandler.Metadata?) -> Release?): ExceptionHandler =
    object : ExceptionHandler {
        override val priority: Float = priority
        override val forSpecificException: Boolean = false
        override fun handle(context: ElementContext, exception: Exception, metadata: ExceptionHandler.Metadata?): Release? = context.handler(exception, metadata)
    }

inline fun <reified T : Exception> ExceptionHandler(priority: Float = 0.5f, crossinline handler: ElementContext.(T) -> Release?): ExceptionHandler =
    object : ExceptionHandler {
        override val priority: Float = priority
        override val forSpecificException: Boolean = true
        override fun handle(context: ElementContext, exception: Exception, metadata: ExceptionHandler.Metadata?): Release? {
            if (exception !is T) return null
            return context.handler(exception)
        }
    }

inline fun <reified T : Exception> ExceptionHandler(priority: Float = 0.5f, crossinline handler: ElementContext.(T, ExceptionHandler.Metadata?) -> Release?): ExceptionHandler =
    object : ExceptionHandler {
        override val priority: Float = priority
        override val forSpecificException: Boolean = true
        override fun handle(context: ElementContext, exception: Exception, metadata: ExceptionHandler.Metadata?): Release? {
            if (exception !is T) return null
            return context.handler(exception, metadata)
        }
    }


fun ExceptionToMessage(priority: Float = 0.5f, message: ElementContext.(Exception) -> ExceptionMessage?): ExceptionToMessage =
    object : ExceptionToMessage {
        override val priority: Float = priority
        override val forSpecificException: Boolean = false
        override fun message(context: ElementContext, exception: Exception, metadata: ExceptionHandler.Metadata?): ExceptionMessage? = message(context, exception)
    }

fun ExceptionToMessage(priority: Float = 0.5f, message: ElementContext.(Exception, ExceptionHandler.Metadata?) -> ExceptionMessage?): ExceptionToMessage =
    object : ExceptionToMessage {
        override val priority: Float = priority
        override val forSpecificException: Boolean = false
        override fun message(context: ElementContext, exception: Exception, metadata: ExceptionHandler.Metadata?): ExceptionMessage? = message(context, exception, metadata)
    }

inline fun <reified T : Exception> ExceptionToMessage(priority: Float = 0.6f, crossinline message: ElementContext.(T) -> ExceptionMessage?): ExceptionToMessage =
    object : ExceptionToMessage {
        override val priority: Float = priority
        override val forSpecificException: Boolean = true
        override fun message(context: ElementContext, exception: Exception, metadata: ExceptionHandler.Metadata?): ExceptionMessage? {
            if (exception !is T) return null
            return message(context, exception)
        }
    }


inline fun <reified T : Exception> ExceptionToMessage(priority: Float = 0.6f, crossinline message: ElementContext.(T, ExceptionHandler.Metadata?) -> ExceptionMessage?): ExceptionToMessage =
    object : ExceptionToMessage {
        override val priority: Float = priority
        override val forSpecificException: Boolean = true
        override fun message(context: ElementContext, exception: Exception, metadata: ExceptionHandler.Metadata?): ExceptionMessage? {
            if (exception !is T) return null
            return message(context, exception, metadata)
        }
    }