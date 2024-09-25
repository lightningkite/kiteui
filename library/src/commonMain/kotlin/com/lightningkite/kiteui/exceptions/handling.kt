package com.lightningkite.kiteui.exceptions

import com.lightningkite.kiteui.debugMode
import com.lightningkite.kiteui.models.Action
import com.lightningkite.kiteui.reactive.onRemove
import com.lightningkite.kiteui.report
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.dialog


class ExceptionHandlers {
    companion object {
        val root = object: ExceptionHandler {
            override val priority: Float get() = 0f
            var open = false
            override fun handle(view: RView, exception: Exception): (() -> Unit)? {
                if(open) return {}
                open = true
                view.closePopovers()
                val message = view.exceptionToMessage(exception)!!
                view.dialog {
                    onRemove { open = false }
                    col {
                        h2(message.title)
                        text(message.body)
                        if(debugMode) {
                            subtext(exception.stackTraceToString())
                        }
                        row {
                            expanding - space()
                            for(action in message.actions) {
                                button {
                                    text(action.title)
                                    onClick { action.onSelect() }
                                }
                            }
                            buttonTheme - button {
                                text("OK")
                                onClick { closePopovers() }
                            }
                        }
                    }
                }
                return { }
            }
        }
    }
    private val handlers: ArrayList<ExceptionHandler> = arrayListOf()
    fun handle(view: RView, exception: Exception): (() -> Unit)? = handlers.firstNotNullOfOrNull { it.handle(view, exception) }
    operator fun plusAssign(other: ExceptionHandler) {
        handlers.add(other)
        handlers.sortByDescending { it.priority }
    }
}
interface ExceptionHandler {
    val priority: Float
    fun handle(view: RView, exception: Exception): (() -> Unit)?
}
class ExceptionToMessages {
    companion object {
        val root = ExceptionToMessages().apply {
            this += object: ExceptionToMessage {
                override val priority: Float
                    get() = 0f

                override fun handle(view: RView, exception: Exception): ExceptionMessage? {
                    exception.report()
                    return ExceptionMessage(
                        title = "Error",
                        body = "An unknown error occurred.  If this issue persists, please contact the developers."
                    )
                }
            }
            this += ExceptionToMessage<PlainTextException> { ExceptionMessage(it.title, it.message!!, it.actions) }
        }
    }
    private val handlers: ArrayList<ExceptionToMessage> = arrayListOf()
    fun handle(view: RView, exception: Exception): ExceptionMessage? = handlers.firstNotNullOfOrNull { it.handle(view, exception) }
    operator fun plusAssign(other: ExceptionToMessage) {
        handlers.add(other)
        handlers.sortByDescending { it.priority }
    }
}
interface ExceptionToMessage {
    val priority: Float
    fun handle(view: RView, exception: Exception): ExceptionMessage?

    companion object {
        inline operator fun <reified E: Exception> invoke(priority: Float = 2f, crossinline additionalCondition: (E)->Boolean = { true }, crossinline handler: RView.(E)->ExceptionMessage): ExceptionToMessage {
            return object: ExceptionToMessage {
                override val priority: Float = priority
                override fun handle(view: RView, exception: Exception): ExceptionMessage? {
                    if(exception !is E) return null
                    if(!additionalCondition(exception)) return null
                    return handler(view, exception)
                }
            }
        }
        inline operator fun <reified E: Exception> invoke(priority: Float = 1f, crossinline handler: RView.(E)->ExceptionMessage): ExceptionToMessage {
            return object: ExceptionToMessage {
                override val priority: Float = priority
                override fun handle(view: RView, exception: Exception): ExceptionMessage? {
                    if(exception !is E) return null
                    return handler(view, exception)
                }
            }
        }
    }
}
data class ExceptionMessage(val title: String, val body: String, val actions: List<Action> = listOf())

class PlainTextException(message: String, val title: String = "Error", val actions: List<Action> = listOf()): Exception(message)