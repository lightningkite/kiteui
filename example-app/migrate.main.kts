#!/usr/bin/env kotlin

import java.io.File

val scrollingModifiers = listOf(
    "scrolling",
    "scrolls",
    "scrollingHorizontally",
    "scrollsHorizontally",
)

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

        val withReplacements = importCorrectedText
            .replace("override fun ViewWriter.render()", "override fun ElementWriter.CanAddTheme.render()")

        val fixed = withReplacements.let { str ->
            var str = str

            // god help you if you have to read this

            while (true) {
                val match = Regex("dynamicTheme\\s*?\\{").find(str) ?: break

                println("Match (${match.range}) = ${match.value}")

                var scopeCount = 0
                val calculation = str.substring(match.range.last + 1).takeWhile {
                    if (it == '{') scopeCount++
                    else if (it == '}') {
                        if (scopeCount == 0) return@takeWhile false
                        else scopeCount -= 1
                    }
                    true
                }

                println("Calculation: $calculation")

                val matchStart = match.range.first
                val matchEnd = match.range.last + calculation.length

                val before = str.substring(0..matchStart)
                str = before.dropLast(1) + str.substring(matchEnd + 2)

                scopeCount = 0
                var idx = matchStart
                for (c in before.reversed()) {
                    idx--
                    if (c == '}') scopeCount++
                    else if (c == '{') {
                        if (scopeCount == 0) break
                        else scopeCount -= 1
                    }
                }

                val upToElementDecl = str.substring(0..idx).trimEnd()

                println("upToElementDecl:\n${upToElementDecl.takeLast(100)}\n\n")

                var atCount = 0
                val upToModifiers = upToElementDecl
                    .dropLastWhile {
                        if (it == '@') {
                            atCount++
                            return@dropLastWhile true
                        }
                        if (it == ' ') {
                            atCount--
                            return@dropLastWhile true
                        }
                        it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9'
                    }

                val element = str.substring(upToModifiers.length-1..upToElementDecl.length)
                println("element: $element")

                println("upToModifiers:\n${upToModifiers.takeLast(100)}\n\n")

                var insert = upToModifiers
                for (m in scrollingModifiers) {
                    insert = insert.removeSuffix("$m.")
                }

                println("insert:\n${insert.takeLast(100)}\n\n")

                str = str.substring(0..<insert.length) + "dynamicThemed {$calculation}." + str.substring(insert.length)
            }

            str
        }

        println("Fixed $file")
        file.writeText(fixed)
    }

//File("./src/commonMain/kotlin/com/lightningkite/mppexampleapp/internal/TestMigration.kt").migrate()
//File("./src/commonMain/kotlin/com/lightningkite/mppexampleapp/docs/ThemeTesterPage.kt").migrate()
//File("./src").migrate()