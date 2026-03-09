// by Claude - records CLI commands for replay and test export
package com.lightningkite.kiteui.aidriver.server

import com.lightningkite.kiteui.aidriver.CliCommand
import com.lightningkite.kiteui.aidriver.CliFormat
import com.lightningkite.kiteui.aidriver.UiAction

// by Claude - added @Synchronized/@Volatile for thread safety (JVM-only module)
class Recording {
    private val commands = mutableListOf<CliCommand>()
    @Volatile var isActive = false

    @Synchronized fun record(command: CliCommand) {
        if (isActive) commands.add(command)
    }

    @Synchronized fun export(): String = commands.joinToString("\n") { cmd ->
        CliFormat.encode(cmd).joinToString(" ")
    }

    // by Claude - generates UiTestScope code for automated test integration
    @Synchronized fun exportKotlin(): String = buildString {
        appendLine("// Recorded test - paste inside a uiTest { } block")
        appendLine("// by Claude")
        for (cmd in commands) {
            val line = cmd.toUiTestScope()
            if (line != null) appendLine(line)
        }
    }

    @Synchronized fun stop() { isActive = false }
    @Synchronized fun start() { isActive = true; commands.clear() }
    @Synchronized fun clear() { commands.clear() }
}

// by Claude - escapes a string for embedding in Kotlin string literals
private fun String.escapeKotlin(): String = this
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")
    .replace("\$", "\\\$")
    .replace("\n", "\\n")
    .replace("\r", "\\r")
    .replace("\t", "\\t")

// by Claude - generates UiTestScope-style Kotlin for each recorded command
private fun CliCommand.toUiTestScope(): String? = when (this) {
    is CliCommand.Perform -> action.toUiTestScope()
    is CliCommand.Wait -> {
        when {
            page != null -> "waitForPage(\"${page!!.escapeKotlin()}\")"
            component != null -> "waitForComponent(\"${component!!.escapeKotlin()}\")"
            componentGone != null -> "waitForComponentGone(\"${componentGone!!.escapeKotlin()}\")"
            enabled != null -> "waitForEnabled(\"${enabled!!.escapeKotlin()}\")"
            else -> "// wait (unhandled condition)"
        }
    }
    is CliCommand.Snapshot -> "dumpSnapshot()"
    else -> null  // skip non-test commands like start, stop, list, etc.
}

private fun UiAction.toUiTestScope(): String = when (this) {
    is UiAction.Click -> "click(\"${targetId.escapeKotlin()}\")"
    is UiAction.LongClick -> "perform(UiAction.LongClick(\"${targetId.escapeKotlin()}\"))"
    is UiAction.SetValue -> "setValue(\"${targetId.escapeKotlin()}\", \"${value.escapeKotlin()}\")"
    is UiAction.Scroll -> "perform(UiAction.Scroll(\"${targetId.escapeKotlin()}\", ${dx}f, ${dy}f))"
    is UiAction.Navigate -> "navigate(\"${route.escapeKotlin()}\")"
    is UiAction.Back -> "back()"
    is UiAction.Forward -> "perform(UiAction.Forward)"
    is UiAction.Screenshot -> "// screenshot (not applicable in tests)"
    // by Claude - drag-and-drop action export
    is UiAction.DragAndDrop -> "perform(UiAction.DragAndDrop(\"${targetId.escapeKotlin()}\", toTargetId = \"${toTargetId.escapeKotlin()}\"))"
}
