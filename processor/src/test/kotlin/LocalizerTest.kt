package com.lightningkite.kiteui

import org.junit.Test


class LocalizerTest {
    @Test
    fun test() {
        val out = HashSet<NeededStringTemplate>()
        val str = """
@Routable("docs/video")
object VideoElementScreen: DocScreen {
    override val covers: List<String> = listOf("video", "Video")

    override fun ViewWriter.render() {
        article {
            h1("Video")
            text("You can use the video element to render video, streamed from a remote source or locally.")
            val time = Property(0.0)
            val playing = Property(false)
            example(""${'"'}
                val time = Property(0.0)
                val playing = Property(false)
                video {
                    source = VideoRemote("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")
                    this.time bind time
                    this.playing bind playing
                }
                ""${'"'}.trimIndent()) {
                stack {
                    centered - sizeConstraints(width = 8.rem, height = 8.rem) - video {
                        source =
                            VideoRemote("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")
                        this.time bind time
                        this.playing bind playing
                        scaleType = ImageScaleType.Crop
                    }
                }
            }
            text("You can observe or control the current time via 'time'.")
            example(""${'"'}
                col {
                    text { ::content { "Time: ${'$'}{time.await()}" } }
                    button { 
                        text("Restart")
                        onClick { time set 0.0 }
                    }
                }
                ""${'"'}.trimIndent()) {
                col {
                    text { ::content { "Time: ${'$'}{time.await()}" } }
                    button {
                        text("Restart")
                        onClick { time set 0.0 }
                    }
                }
            }
            text("You can observe or control the playing state via 'playing'.")
            example(""${'"'}
                col {
                    text { ::content { if(playing.await()) "Playing" else "Paused" } }
                    button {
                        text("Play")
                        onClick { playing set true }
                    }
                    button {
                        text("Pause")
                        onClick { playing set false }
                    }
                }
                ""${'"'}.trimIndent()) {
                col {
                    text { ::content { if(playing.await()) "Playing" else "Paused" } }
                    button {
                        text("Play")
                        onClick { playing set true }
                    }
                    button {
                        text("Pause")
                        onClick { playing set false }
                    }
                }
            }
        }
    }

}
        """.trimIndent()
        str.localizer(out)
        out.forEach { println(it) }
        println("---")
        println(str.applyLocalizations(out))
    }
}

fun String.applyLocalizations(local: Set<NeededStringTemplate>): String {
    var value = this
    for (l in local) {
        value = l.pattern.replace(value) {
            l.use(it.groupValues.drop(1).map { it.trim('{', '}') })
        }
    }
    return value
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
            if(inComment) {
                if(c == '/' && raw[index-1] == '*') inComment = false
                continue
            }
            if(ignoreUntilLineEnd) {
                if(c == '\n') ignoreUntilLineEnd = false
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
                        if(index + 2 < raw.length && raw.substring(index, index + 3) != "\"\"\"") {
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
                        printlnParenLevel == -1
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
                if(c == '/' && raw.getOrNull(index + 1) == '*') {
                    inComment = true
                    continue
                }
                if(c == '/' && raw.getOrNull(index + 1) == '/') {
                    ignoreUntilLineEnd = true
                    continue
                }
                if (c == '"') {
                    val triple = index + 2 <= raw.length && raw.substring(index, index + 3) == "\"\"\""
                    if(triple) {
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
        } catch(e: Exception) {
            throw Exception("Failed parsing around ${raw.drop(index - 20).take(40)}. templateStarted: $templateStarted\n" +
                    "escaped: $escaped\n" +
                    "inString: $inString\n" +
                    "inComment: $inComment\n" +
                    "skipNext: $skipNext\n" +
                    "ignoreUntilLineEnd: $ignoreUntilLineEnd\n" +
                    "annotationParenLevel: $annotationParenLevel\n" +
                    "printlnParenLevel: $printlnParenLevel", e)
        }
    }
}

data class NeededStringTemplate(
    val content: List<String>,
    val args: List<String>,
    val name: String = "${
        content.zip(args) { a, b -> "$a ${b}" }.joinToString("")
    }${content.last()}".let { if(it.firstOrNull()?.isLowerCase() == true) "lowercase $it" else it }.split(' ').take(8).joinToString(" ").filter { it.isLetterOrDigit() || it == ' ' }.camelCase()
) {
    fun useStart() = if (args.isEmpty()) "Strings.$name" else "Strings.$name("
    fun useEnd() = if (args.isEmpty()) "" else ")"
    fun use(inputs: List<String>) = if (args.isEmpty()) "Strings.$name" else "Strings.$name(${inputs.joinToString()})"
    override fun toString(): String {
        if (args.isEmpty())
            return "val $name: String get() = \"${content.first()}\""
        return "fun $name(${args.joinToString() { "$it: Any?" }}): String = \"${
            content.zip(args) { a, b -> "$a\${${b}}" }.joinToString("")
        }${content.last()}\""
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