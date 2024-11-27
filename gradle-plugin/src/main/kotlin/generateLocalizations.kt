package com.lightningkite.kiteui

import java.io.File


fun generateLocalizations(toRead: List<File>, outKt: File, ext: KiteUiPluginExtension) {
    val localizations = HashSet<NeededStringTemplate>()
    toRead.filterNotNull().asSequence()
        .flatMap { it.walkTopDown() }
        .filter { it.extension == "kt" && it.isFile }
        .forEach {
            try {
                it.readText().localizer(localizations)
            } catch (e: Exception) {
                println("WARNING: Could not parse $it")
                e.printStackTrace()
            }
        }
    localizations.groupBy { it.name }
        .filter { it.value.size > 1 }
        .forEach { t, u ->
            localizations.removeAll(u)
            localizations.addAll(u.mapIndexed { index, it ->
                it.copy(name = it.name + (index + 1))
            })
        }
    val wordRegex = Regex("[A-Z][a-z]")
    val commaWithoutSpace = Regex(",[^ ]")
    outKt.writeText(
        """
    package ${ext.packageName}
    
    interface StringsBase {
        ${localizations.joinToString("\n    ")}
    }
    
    object Strings: StringsBase
                        """.trimIndent()
    )
    toRead.filterNotNull().asSequence()
        .flatMap { it.walkTopDown() }
        .filter { it.extension == "kt" && it.isFile }
        .forEach {
            it.writeText(it.readText().applyLocalizations(ext.packageName, localizations))
        }
}
