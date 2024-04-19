package com.lightningkite.kiteui

import org.junit.Test


class LocalizerTest {
    @Test
    fun test() {
        val out = HashSet<NeededStringTemplate>()
        val str = """
            fun someCode() {
                println("Some test code")
                println("With argument ${'$'}x and after")
                println("With complex argument ${'$'}{complex + expression}")
                println("With string in complex ${'$'}{"asdf"}")
                println(${"\"\"\""}OH SHIT${"\"\"\""})
            }
        """.trimIndent()
        str.localizer(out)
        out.forEach { println(it) }
        println("---")
        println(str.applyLocalications(out))
    }
}

fun String.applyLocalications(local: Set<NeededStringTemplate>): String {
    var value = this
    for(l in local) {
        value = l.pattern.replace(value) {
            l.use(it.groupValues.drop(1).map { it.trim('{', '}') })
        }
    }
    return value
}

fun String.localizer(out: MutableSet<NeededStringTemplate>) {
    fun argNameByIndex(index: Int) = ('a' + ((('x' - 'a') + index) % 26)).toString()
    val raw = this
    val stringStack = ArrayList<StringLitData>()
    val codeStack = ArrayList<CodeData>()
    codeStack.add(CodeData())
    var templateStarted = false
    val buildingArgName = StringBuilder()
    var escaped = false
    var inString = false
    for(index in 0 until raw.length) {
        val c = raw[index]
        if(templateStarted) {
            if(c == '{') {
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
        if(inString) {
            val wasEscaped = escaped
            escaped = false
            if(c == '"' && !wasEscaped) {
                inString = false
                val finish = stringStack.removeLast()
                finish.finishSection()
                val t = NeededStringTemplate(
                    content = finish.content,
                    args = finish.args.indices.map { argNameByIndex(it) },
                )
                out.add(t)
                continue
            } else if(c == '\\') {
                escaped = true
            } else if(c == '$' && !wasEscaped) {
                stringStack.last().finishSection()
                templateStarted = true
                buildingArgName.clear()
                continue
            }
            stringStack.lastOrNull()?.append(c)
        } else {
            if(c == '"') {
                inString = true
                stringStack.add(StringLitData(index))
                continue
            }
            if(c == '{') {
                codeStack.last().braceLevel++
            } else if(c == '}') {
                if((--codeStack.last().braceLevel) == 0) {
                    inString = true
                }
            }
        }
    }
}
data class NeededStringTemplate(val content: List<String>, val args: List<String>, val name: String = "${content.zip(args) { a, b -> "$a ${b}" }.joinToString("") }${content.last()}".camelCase()) {
    fun useStart() = if(args.isEmpty()) "Strings.$name" else "Strings.$name("
    fun useEnd() = if(args.isEmpty()) "" else ")"
    fun use(inputs: List<String>) = if(args.isEmpty()) "Strings.$name" else "Strings.$name(${inputs.joinToString()})"
    override fun toString(): String {
        if(args.isEmpty())
            return "val $name: String get() = \"${content.first()}\""
        return "fun $name(${args.joinToString(){ "$it: Any?" }}): String = \"${content.zip(args) { a, b -> "$a\${${b}}" }.joinToString("") }${content.last()}\""
    }

    val pattern = Regex("\"${content.zip(args) { a, b -> "${Regex.escape(a)}\\\$(\\{[^{}]*(?:\\{[^}]*\\}[^}]*)*\\}|[\\w\\d]+)" }.joinToString("") }${Regex.escape(content.last())}\"")
}
class CodeData(var braceLevel: Int = 0) {
}
class StringLitData(
    val start: Int,
) {
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