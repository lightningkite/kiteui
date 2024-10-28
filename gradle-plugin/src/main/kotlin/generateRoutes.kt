package com.lightningkite.kiteui

import org.gradle.api.Task
import java.io.File
import kotlin.math.min

private fun String.indexOf(startIndex: Int, vararg chars: Char): Int {
    return chars.asSequence().map {
        indexOf(it, startIndex).let { if(it == -1) length else it }
    }.minOrNull() ?: length
}
private val blockComment = Regex("/\\*[^*]*\\*+(?:[^/*][^*]*\\*+)*/")

fun generateAutoroutes(sources: File, out: File) {

    val allRoutables = sources.walkTopDown()
        .filter { it.extension == "kt" }
        .flatMap {
            val out = ArrayList<ScreenData>()
            val text = it.readLines().map { it.trim() }.filter { !it.startsWith("//") }.joinToString("\n").replace(blockComment, "")
            val packageName = text.substringAfter("package ").substringBefore("\n").trim()
            var index = 0
            while (true) {
                val next = text.indexOf("@Routable", index)
                if (next == -1) break
                index = next + 1

                val quoteStart = text.indexOf('"', next)
                if (quoteStart == -1) break
                val quoteEnd = text.indexOf('"', quoteStart + 1)
                if (quoteEnd == -1) break
                val url = text.substring(quoteStart + 1, quoteEnd)
                val urlParts = url.split('/').map { it.trim() }.filter { it.isNotBlank() }.map {
                    if(it.startsWith('{'))
                        Segment.Variable(it.trim('{', '}'))
                    else
                        Segment.Constant(it)
                }

                val classOrObjectMark = min(
                    text.indexOf("class ", next).let { if (it == -1) Int.MAX_VALUE else it },
                    text.indexOf("object ", next).let { if (it == -1) Int.MAX_VALUE else it },
                )
                if (classOrObjectMark == Int.MAX_VALUE) break
                val nameStart = text.indexOf(' ', classOrObjectMark) + 1
                val name = text.substring(nameStart, text.indexOf(nameStart, ' ', '(', ':', '<')).trim().trim(':')
                val constructorParamsStart = text.indexOf('(', classOrObjectMark)
                val bodyStart = text.indexOf('{', classOrObjectMark)
                val constructorParams = if(constructorParamsStart == -1 || constructorParamsStart > bodyStart) listOf() else text.splitParens(startingAt = constructorParamsStart)

                val upperIndex = index
                val queryParams = run {
                    val out = HashMap<String, String>()
                    var index = upperIndex
                    while(true) {
                        val next = text.indexOf("@QueryParameter", index)
                        if (next == -1) break
                        index = next + 1
                        val argStart = text.indexOf('(', next).let { if(it == -1) text.length else it }
                        val hasExplicitName = (next + 15..argStart).none { !text[it].isWhitespace() }
                        if(argStart == text.length) continue
                        var annoArgs: List<String>? = null
                        val beginLoookingForVa = if(hasExplicitName) {
                            annoArgs = text.splitParens(startingAt = argStart)
                            text.afterParens(startingAt = argStart)
                        } else {
                            index
                        }
                        val declstart = text.indexOf("va", beginLoookingForVa)
                        if(declstart == -1) continue
                        val nameStart = text.indexOf(' ', declstart) + 1
                        if(nameStart == -1) continue
                        val nameEnd = text.indexOf(' ', nameStart)
                        if(nameEnd == -1) continue
                        val codename = text.substring(nameStart, nameEnd)
                        out[codename] = annoArgs?.getOrNull(0) ?: codename
                    }
                    out
                }

                out.add(ScreenData(
                    packageName = packageName,
                    name = name,
                    params = constructorParams.map { it.substringBefore(':').removePrefix("val ").removePrefix("var ").trim() },
                    url = urlParts,
                    isObject = text[classOrObjectMark] == 'o',
                    queryParams = queryParams
                ))
            }
            out
        }
        .toList()

    val topPackage = allRoutables
        .takeIf { it.isNotEmpty() }
        ?.map { it.packageName }
        ?.reduce { a, b -> a.commonPrefixWith(b) }
        ?.removeSuffix(".")
        ?: ""

    out.writer().use {
        TabAppendable(it).run {
            appendLine("package $topPackage")
            appendLine("")
            appendLine("import com.lightningkite.kiteui.navigation.*")
            allRoutables.forEach {
                appendLine("import ${it.packageName}.${it.name}")
            }
            appendLine("")
            appendLine("")
                        appendLine("val AutoRoutes = Routes(")
            tab {
                appendLine("parsers = listOf(")
                tab {
                    for (routable in allRoutables) {
                        val route = routable.url
                        appendLine("label@{ ")
                        tab {
                            appendLine("if (it.segments.size != ${route.size}) return@label null")
                            for ((index, part) in route.withIndex()) {
                                when (part) {
                                    is Segment.Constant -> {
                                        appendLine("if (it.segments[$index] != \"${part.value}\") return@label null")
                                    }

                                    else -> {}
                                }
                            }
                            if (routable.isObject) {
                                appendLine("${routable.name}")
                                appendLine(".apply {")
                                tab {
                                    for (qp in routable.queryParams) {
                                        appendLine("UrlProperties.decodeFromStringMap(\"${qp.value}\", it.parameters, this.${qp.key})")
                                    }
                                }
                                appendLine("}")
                            } else {
                                appendLine("${routable.name}(")
                                tab {
                                    for ((index, part) in route.withIndex()) {
                                        when (part) {
                                            is Segment.Variable -> {
                                                appendLine("${part.name} = UrlProperties.decodeFromString(it.segments[$index]),")
                                            }

                                            else -> {}
                                        }
                                    }
                                }
                                appendLine(").apply {")
                                tab {
                                    for (qp in routable.queryParams) {
                                        appendLine("UrlProperties.decodeFromStringMap(\"${qp.value}\", it.parameters, this.${qp.key})")
                                    }
                                }
                                appendLine("}")
                            }
                        }
                        appendLine("},")
                    }
                }
                appendLine("),")
                appendLine("renderers = mapOf(")
                tab {
                    for (routable in allRoutables) {
                        val route = routable.url
                        val rendered = route.joinToString(", ") {
                            when (it) {
                                is Segment.Constant -> "\"${it.value}\""
                                is Segment.Variable -> "UrlProperties.encodeToString(it.${it.name})"
                                else -> throw Exception()
                            }
                        }
                        appendLine("${routable.name}::class to label@{")
                        tab {
                            appendLine("if (it !is ${routable.name}) return@label null")
                            appendLine("val p = HashMap<String, String>()")
                            routable.queryParams.forEach {
                                appendLine("UrlProperties.encodeToStringMap(it.${it.key}.value, \"${it.value}\", p)")
                            }
                            appendLine("RouteRendered(UrlLikePath(")
                            tab {
                                appendLine("segments = listOf($rendered),")
                                appendLine("parameters = p")
                            }
                            appendLine("), listOf(${routable.queryParams.keys.joinToString { "it.${it}" }}))")
                        }
                        appendLine("},")
                    }
                }
                appendLine("),")
//                if (fallbackRoute.classKind == ClassKind.OBJECT)
//                    appendLine("fallback = ${fallbackRoute.simpleName!!.asString()}")
//                else
//                    appendLine("fallback = ${fallbackRoute.simpleName!!.asString()}()")
            }
            appendLine(")")
        }
    }
}


data class ScreenData(
    val packageName: String,
    val name: String,
    val params: List<String>,
    val url: List<Segment>,
    val isObject: Boolean,
    val queryParams: Map<String, String>,
)
sealed class Segment {
    data class Constant(val value: String) : Segment()
    data class Variable(val name: String) : Segment()
}