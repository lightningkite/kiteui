package com.lightningkite.kiteui

import org.apache.fontbox.ttf.OTFParser
import org.apache.fontbox.ttf.TTFParser
import java.io.File

fun String?.str() = if (this == null) "null" else "\"$this\""

sealed class Resource {
    data class Font(
        val name: String,
        val normal: Map<Int, SubFont>,
        val italics: Map<Int, SubFont>,
    ) : Resource() {
        data class SubFont(
            val source: File,
            val relativeFile: File,
            val fontSuperFamily: String,
            val fontFamily: String,
            val fontSubFamily: String,
            val postScriptName: String,
        )

        val files get() = normal.values + italics.values
    }

    data class Image(val name: String, val source: File, val relativeFile: File) : Resource()
    data class Video(val name: String, val source: File, val relativeFile: File) : Resource()
    data class Audio(val name: String, val source: File, val relativeFile: File) : Resource()
    data class Binary(val name: String, val source: File, val relativeFile: File) : Resource()
}

val boldnessNames = mapOf(
    "Thin" to 100,
    "Hairline" to 100,
    "UltraLight" to 200,
    "ExtraLight" to 200,
    "Light" to 300,
    "Normal" to 400,
    "Regular" to 400,
    "Medium" to 500,
    "SemiBold" to 600,
    "DemiBold" to 600,
    "Bold" to 700,
    "ExtraBold" to 800,
    "UltraBold" to 800,
    "Black" to 900,
    "Heavy" to 900,
).mapKeys { it.key.lowercase() }
val boldnessNamesByLength = boldnessNames.entries.sortedByDescending { it.key.length }

fun File.resources(): Map<String, Resource> {
    val out = HashMap<String, Resource>()
    walkTopDown().forEach { file ->
        if (file.name.isEmpty()) return@forEach
        if (file.nameWithoutExtension.isEmpty()) return@forEach
        if (file.isDirectory) return@forEach
        val relativeFile = file.relativeTo(this)
        val name = relativeFile.path.replace('/', ' ').replace('\\', ' ')
            .substringBeforeLast('.')
            .camelCase()
            .filter { it.isLetterOrDigit() }
        when (relativeFile.extension) {
            "png", "jpg", "webp" -> out[name] = Resource.Image(name, file, relativeFile)
            "mp4" -> out[name] = Resource.Video(name, file, relativeFile)
            "mp3", "ogg", "wav" -> out[name] = Resource.Audio(name, file, relativeFile)
            "otf", "ttf" -> {
                val font = when (relativeFile.extension) {
                    "otf" -> OTFParser().parse(file)
                    "ttf" -> TTFParser().parse(file)
                    else -> throw IllegalArgumentException()
                }
                val sf = Resource.Font.SubFont(
                    source = file,
                    relativeFile = relativeFile,
                    fontSuperFamily = font.naming.getName(16, 1, 0, 0)
                        ?: font.naming.nameRecords.find { it.nameId == 16 }?.string ?: "",
                    fontFamily = font.naming.fontFamily,
                    fontSubFamily = font.naming.fontSubFamily,
                    postScriptName = font.naming.postScriptName,
                )
                val siblings = file.parentFile.listFiles() ?: arrayOf()
                val siblingsCommonPrefix = siblings.map { it.nameWithoutExtension }.reduceOrNull { a, b -> a.commonPrefixWith(b) } ?: ""
                val siblingsAreOfSameFont = siblings.isNotEmpty() && siblings.all {
                    it.nameWithoutExtension.removePrefix(siblingsCommonPrefix)
                        .filter { it.isLetter() }
                        .lowercase()
                        .replace("italic", "")
                        .let { it.isBlank() || it in boldnessNames.keys }
                }
                val italic = file.nameWithoutExtension.contains("italic", true)
                val weight = boldnessNamesByLength.find { (key, value) -> file.nameWithoutExtension.lowercase().contains(key) }?.value ?: 400
                if (siblingsAreOfSameFont) {
                    val folderName = relativeFile.parentFile.path.replace('/', ' ').replace('\\', ' ')
                        .substringBeforeLast('.')
                        .camelCase()
                    out[folderName] = (out[folderName] as? Resource.Font)?.let {
                        if (italic) {
                            it.copy(italics = it.italics + (weight to sf))
                        } else {
                            it.copy(normal = it.normal + (weight to sf))
                        }
                    } ?: Resource.Font(
                        folderName,
                        normal = if (!italic) mapOf(weight to sf) else mapOf(),
                        italics = if (italic) mapOf(weight to sf) else mapOf(),
                    )
                } else out[name] = Resource.Font(name, normal = mapOf(400 to sf), italics = mapOf())
            }

            "" -> {}
            else -> out[name] = Resource.Binary(name, file, relativeFile)
        }
    }
    return out
}

fun String.applyLocalizations(packageName: String, local: Set<NeededStringTemplate>): String {
    var value = this
    for (l in local) {
        value = l.pattern.replace(value) {
            l.use(it.groupValues.drop(1).map { it.trim('{', '}') })
        }
    }
    return value.lines().let {
        val i = it.indexOfLast { it.startsWith("import ") }
        if (i == -1) return@let it
        it.subList(0, i) + listOf("import ${packageName}.Strings") + it.subList(i, it.size)
    }.joinToString("\n")
}

fun String.localizer(out: MutableSet<NeededStringTemplate>) {
    val raw = this
    fun argNameByIndex(index: Int) = ('a' + ((('x' - 'a') + index) % 26)).toString()
    val stringStack = ArrayList<StringLitData>()
    val codeStack = ArrayList<CodeData>()
    codeStack.add(CodeData())
    var templateStarted = false
    val buildingArgName = StringBuilder()
    var escaped = false
    var inString = false
    var inComment = false
    var skipNext = 0
    var ignoreUntilLineEnd = false
    var annotationParenLevel = -1
    var printlnParenLevel = -1
    for (index in 0 until raw.length) {
        try {
            val c = raw[index]
            if (skipNext > 0) {
                skipNext--
                continue
            }
            if (inComment) {
                if (c == '/' && raw[index - 1] == '*') inComment = false
                continue
            }
            if (ignoreUntilLineEnd) {
                if (c == '\n') ignoreUntilLineEnd = false
                continue
            }
            if (templateStarted) {
                if (c == '{') {
                    codeStack.add(CodeData(1))
                    stringStack.last().args.add("")
                    templateStarted = false
                    inString = false
                    continue
                } else if (c.isLetterOrDigit()) {
                    buildingArgName.append(c)
                    continue
                } else {
                    inString = true
                    stringStack.last().args.add(buildingArgName.toString())
                    templateStarted = false
                }
            }
            if (inString) {
                val wasEscaped = escaped
                escaped = false
                if (c == '"' && !wasEscaped) {
                    if (stringStack.last().triple) {
                        if (index + 2 < raw.length && raw.substring(index, index + 3) != "\"\"\"") {
                            // It's a fakeout
                            stringStack.lastOrNull()?.append(c)
                            continue
                        } else {
                            skipNext = 2
                        }
                    }
                    inString = false
                    val finish = stringStack.removeLast()
                    finish.finishSection()
                    val t = NeededStringTemplate(
                        content = finish.content,
                        triple = finish.triple,
                        args = finish.args.indices.map { argNameByIndex(it) },
                    )
                    val prerender = finish.content.joinToString()
                    val word = Regex("[A-Z][a-z]")
                    val bannedChars = Regex("\\<\\>\\{\\}\\[\\]_")
                    val camel = Regex("[a-z][A-Z]")
                    if (
                        prerender.contains(word) &&
                        !prerender.contains(bannedChars) &&
                        !prerender.contains(camel) &&
                        annotationParenLevel == -1 &&
                        printlnParenLevel == -1 &&
                        (prerender.length >= 2 && (prerender[0].lowercaseChar() != 'm' || !prerender[1].isDigit()))
                    ) {
                        out.add(t)
                    }
                    continue
                } else if (c == '\\') {
                    escaped = true
                } else if (c == '$' && !wasEscaped) {
                    stringStack.last().finishSection()
                    templateStarted = true
                    buildingArgName.clear()
                    continue
                }
                stringStack.lastOrNull()?.append(c)
            } else {
                if (c == '/' && raw.getOrNull(index + 1) == '*') {
                    inComment = true
                    continue
                }
                if (c == '/' && raw.getOrNull(index + 1) == '/') {
                    ignoreUntilLineEnd = true
                    continue
                }
                if (c == '"') {
                    val triple = index + 2 <= raw.length && raw.substring(index, index + 3) == "\"\"\""
                    if (triple) {
                        skipNext = 2
                    }
                    inString = true
                    stringStack.add(StringLitData(index).also {
                        it.triple = triple
                    })
                    continue
                }
                if (c == '{') {
                    codeStack.last().braceLevel++
                } else if (c == '}') {
                    if ((--codeStack.last().braceLevel) == 0) {
                        inString = true
                        codeStack.removeLast()
                    }
                }
                if (index + 7 < raw.length && raw.substring(index, index + 7) == "println") {
                    printlnParenLevel = codeStack.last().parenLevel
                }
                if (c == '@') {
                    annotationParenLevel = codeStack.last().parenLevel
                }
                if (c == '(') {
                    ++codeStack.last().parenLevel
                } else if (c == ')') {
                    val newLevel = --codeStack.last().parenLevel
                    if (newLevel == annotationParenLevel) {
                        annotationParenLevel = -1
                    }
                    if (newLevel == printlnParenLevel) {
                        printlnParenLevel = -1
                    }
                }
                if (c == ' ' && codeStack.last().parenLevel == codeStack.last().parenLevel) annotationParenLevel
            }
        } catch (e: Exception) {
            throw Exception(
                "Failed parsing around ${raw.drop(index - 20).take(40)}. templateStarted: $templateStarted\n" +
                        "escaped: $escaped\n" +
                        "inString: $inString\n" +
                        "inComment: $inComment\n" +
                        "skipNext: $skipNext\n" +
                        "ignoreUntilLineEnd: $ignoreUntilLineEnd\n" +
                        "annotationParenLevel: $annotationParenLevel\n" +
                        "printlnParenLevel: $printlnParenLevel", e
            )
        }
    }
}

data class NeededStringTemplate(
    val content: List<String>,
    val triple: Boolean,
    val args: List<String>,
    val name: String = "${
        content.zip(args) { a, b -> "$a ${b}" }.joinToString("")
    }${content.last()}".let { if (it.firstOrNull()?.isLowerCase() == true) "lowercase $it" else it }.split(' ').filter { it.isNotBlank() }.take(8).joinToString(" ")
        .filter { it.isLetterOrDigit() || it == ' ' }.camelCase()
) {
    fun useStart() = if (args.isEmpty()) "Strings.$name" else "Strings.$name("
    fun useEnd() = if (args.isEmpty()) "" else ")"
    fun use(inputs: List<String>) = if (args.isEmpty()) "Strings.$name" else "Strings.$name(${inputs.joinToString()})"
    override fun toString(): String {
        if (triple) {
            if (args.isEmpty())
                return "val $name: String get() = \"\"\"${content.first()}\"\"\""
            return "fun $name(${args.joinToString() { "$it: Any?" }}): String = \"\"\"${
                content.zip(args) { a, b -> "$a\${${b}}" }.joinToString("")
            }${content.last()}\"\"\""
        } else {
            if (args.isEmpty())
                return "val $name: String get() = \"${content.first()}\""
            return "fun $name(${args.joinToString() { "$it: Any?" }}): String = \"${
                content.zip(args) { a, b -> "$a\${${b}}" }.joinToString("")
            }${content.last()}\""
        }
    }

    val pattern = Regex(
        "\"(?:\"\")?${
            content.zip(args) { a, b -> "${Regex.escape(a)}\\\$(\\{[^{}]*(?:\\{[^}]*\\}[^}]*)*\\}|[\\w\\d]+)" }
                .joinToString("")
        }${Regex.escape(content.last())}\"(?:\"\")?"
    )
}

class CodeData(var braceLevel: Int = 1, var parenLevel: Int = 1) {
}

class StringLitData(
    val start: Int,
) {
    var triple = false
    val builder = StringBuilder()
    val content = ArrayList<String>()
    fun append(c: Char) = builder.append(c)
    fun finishSection() {
        content += builder.toString()
        builder.clear()
    }

    val args = ArrayList<String>()
}


val `casing separator regex` = Regex("([-_\\s]+([A-Z]*[a-z0-9]+))|([-_\\s]*[A-Z]+)")
inline fun String.caseAlter(crossinline update: (after: String) -> String): String =
    `casing separator regex`.replace(this) {
        if (it.range.start == 0) it.value
        else update(it.value.filter { !(it == '-' || it == '_' || it.isWhitespace()) })
    }


fun String.titleCase() = caseAlter { " " + it.capitalize() }.capitalize()
fun String.spaceCase() = caseAlter { " " + it }.decapitalize()
fun String.kabobCase() = caseAlter { "-$it" }.toLowerCase()
fun String.snakeCase() = caseAlter { "_$it" }.toLowerCase()
fun String.screamingSnakeCase() = caseAlter { "_$it" }.toUpperCase()
fun String.camelCase() = caseAlter { it.capitalize() }.decapitalize()
fun String.pascalCase() = caseAlter { it.capitalize() }.capitalize()