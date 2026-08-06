#!/usr/bin/env kotlin

import java.io.File

private val actionImport = "com.lightningkite.kiteui.views.AiDriver.Actions"

fun File.addImports() = walkTopDown()
    .filter { it.extension == "kt" }
    .forEach { file ->
        val text = file.readText()
        val imports = text.lineSequence()
            .filter { it.startsWith("import ") }
            .map { it.removePrefix("import ") }
            .toList()

        if (imports.none { it.startsWith("com.lightningkite.", ignoreCase = true) }) return@forEach
        if (!text.contains("AiDriver.Actions")) return@forEach
        if (imports.any { it == actionImport }) return@forEach


        val fixedImports = imports
            .plus(actionImport)
            .distinct()

        val preImports = text.substringBefore("import ")
        val postImports = text.substringAfterLast("\nimport ").substringAfter('\n')

        file.writeText(preImports + fixedImports.joinToString("\n") { "import $it" } + "\n" + postImports)

        println("Fixed $file")
    }

File(".").addImports()