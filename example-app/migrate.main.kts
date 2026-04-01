#!/usr/bin/env kotlin

import java.io.File

fun File.migrate() = walkTopDown()
    .filter { it.name.endsWith(".kt") }
    .forEach { file ->
        val text = file.readText()
        val imports = text.lineSequence()
            .filter { it.startsWith("import ") }
            .map { it.removePrefix("import ") }

        if (imports.none { it.startsWith("com.lightningkite.", ignoreCase = true) }) return@forEach

        val fixedImports = imports
            .plus("com.lightningkite.kiteui.views.*")
            .plus("com.lightningkite.kiteui.views.direct.*")
            .minus("com.lightningkite.kiteui.views.l2.icon")
            .distinct()
            .sorted()

        val preImports = text.substringBefore("import ")
        val postImports = text.substringAfterLast("\nimport ").substringAfter('\n')
        val importCorrectedText = preImports + fixedImports.joinToString("\n") { "import $it" } + "\n" + postImports

        println("Fixed $file")
        file.writeText(
            importCorrectedText
                .replace("override fun ViewWriter.render()", "override fun ElementWriter.CanAddTheme.render()")
        )
    }

File("./src").migrate()