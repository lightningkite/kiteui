package com.lightningkite.kiteui.exceptions

import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.dialog
import com.lightningkite.reactive.core.*

class ExceptionHandlersTree(private val parent: ExceptionHandlersTree? = null) {
    private val handlers = ArrayList<ExceptionHandler>()
    private val messages = ArrayList<ExceptionToMessage>()

    fun add(handler: ExceptionHandler) {
        handlers.add(handler)
        handlers.sortByDescending { it.priority }
    }
    fun add(message: ExceptionToMessage) {
        messages.add(message)
        messages.sortByDescending { it.priority }
    }

    operator fun plusAssign(handler: ExceptionHandler) = add(handler)
    operator fun plusAssign(message: ExceptionToMessage) = add(message)

    fun handle(context: ElementContext, exception: Exception): Release? =
        handlers.firstNotNullOfOrNull { it.handle(context, exception) } ?: parent?.handle(context, exception)

    fun message(context: ElementContext, exception: Exception): ExceptionMessage? =
        messages.firstNotNullOfOrNull { it.message(context, exception) } ?: parent?.message(context, exception)
}

interface ExceptionHandler {
    val priority: Float
    fun handle(context: ElementContext, exception: Exception): Release?

    companion object {
        val openDialog = ExceptionHandler(0f) { exception ->
            val message = exceptionMessage(exception) ?: return@ExceptionHandler null

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

        val stacktraceDialog = ExceptionHandler(0f) { exception ->
            val message = exceptionMessage(exception) ?: return@ExceptionHandler null

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

interface ExceptionToMessage {
    val priority: Float
    fun message(context: ElementContext, exception: Exception): ExceptionMessage?

    companion object {
        val unexpectedError = ExceptionToMessage(0f) {
            ExceptionMessage(
                "Error",
                "An unexpected error occurred."
            )
        }

        val debug = ExceptionToMessage(0f) { e ->
            ExceptionMessage(
                "Error: $e",
                listOfNotNull(
                    e.message,
                    e.cause?.let { "Caused By: $it" }
                ).joinToString("\n")
            )
        }
    }
}

data class ExceptionMessage(
    val title: String,
    val body: String,
    val actions: List<Action> = emptyList()
)



fun ExceptionHandler(priority: Float = 0.5f, handler: ElementContext.(Exception) -> Release?): ExceptionHandler =
    object : ExceptionHandler {
        override val priority: Float = priority
        override fun handle(context: ElementContext, exception: Exception): Release? = context.handler(exception)
    }

inline fun <reified T : Exception> ExceptionHandler(priority: Float = 0.5f, crossinline handler: ElementContext.(T) -> Release?): ExceptionHandler =
    object : ExceptionHandler {
        override val priority: Float = priority
        override fun handle(context: ElementContext, exception: Exception): Release? {
            if (exception !is T) return null
            return context.handler(exception)
        }
    }

fun ExceptionToMessage(priority: Float = 0.5f, message: ElementContext.(Exception) -> ExceptionMessage?): ExceptionToMessage =
    object : ExceptionToMessage {
        override val priority: Float = priority
        override fun message(context: ElementContext, exception: Exception): ExceptionMessage? = message(context, exception)
    }

inline fun <reified T : Exception> ExceptionToMessage(priority: Float = 0.6f, crossinline message: ElementContext.(T) -> ExceptionMessage?): ExceptionToMessage =
    object : ExceptionToMessage {
        override val priority: Float = priority
        override fun message(context: ElementContext, exception: Exception): ExceptionMessage? {
            if (exception !is T) return null
            return message(context, exception)
        }
    }