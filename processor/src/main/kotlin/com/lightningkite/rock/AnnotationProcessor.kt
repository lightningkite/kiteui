package com.lightningkite.rock

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import java.io.BufferedWriter
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

lateinit var comparable: KSClassDeclaration
var khrysalisUsed = false

class RouterGeneration(
    val codeGenerator: CodeGenerator,
    val logger: KSPLogger,
) : CommonSymbolProcessor(codeGenerator) {
    override fun process2(resolver: Resolver) {
        val allRoutables = resolver.getAllFiles()
            .flatMap { it.declarations }
            .filterIsInstance<KSClassDeclaration>()
            .filter { it.annotation("Routable") != null }
            .toList()
            .map { ParsedRoutable(it) }
        if (allRoutables.isEmpty()) return
        val fallbackRoute = resolver.getAllFiles()
            .flatMap { it.declarations }
            .filterIsInstance<KSClassDeclaration>()
            .filter { it.annotation("FallbackRoute") != null }
            .toList()
            .singleOrNull()
            ?: resolver.getClassDeclarationByName("com.lightningkite.rock.navigation.RockScreen.Empty")!!

        val topPackage = allRoutables
            .takeIf { it.isNotEmpty() }
            ?.map { it.source.packageName.asString() }
            ?.reduce { a, b -> a.commonPrefixWith(b) }
            ?: ""

        createNewFile(
            dependencies = Dependencies.ALL_FILES,
            packageName = topPackage,
            fileName = "AutoRoutes",
        )
            .use {
                with(TabAppendable(it)) {
                    appendLine("package $topPackage")
                    appendLine("")
                    appendLine("import com.lightningkite.rock.navigation.*")
                    for (r in allRoutables) appendLine("import ${r.source.qualifiedName!!.asString()}")
                    appendLine("import ${fallbackRoute.qualifiedName!!.asString()}")
                    if (allRoutables.any { it.routes.any { it.any { it is ParsedRoutable.Segment.Variable && it.type.declaration.simpleName?.asString() == "UUID" } } }) {
                        appendLine("import com.lightningkite.uuid")
                    }
                    appendLine("")
                    appendLine("")
                    appendLine("val AutoRoutes = Routes(")
                    tab {
                        appendLine("parsers = listOf(")
                        tab {
                            for (routable in allRoutables) {
                                for (route in routable.routes) {
                                    appendLine("label@{ ")
                                    tab {
                                        appendLine("if (it.segments.size != ${route.size}) return@label null")
                                        for ((index, part) in route.withIndex()) {
                                            when (part) {
                                                is ParsedRoutable.Segment.Constant -> {
                                                    appendLine("if (it.segments[$index] != \"${part.value}\") return@label null")
                                                }

                                                else -> {}
                                            }
                                        }
                                        if (routable.source.classKind == ClassKind.OBJECT)
                                            appendLine("${routable.source.simpleName!!.asString()}")
                                        else {
                                            appendLine("${routable.source.simpleName!!.asString()}(")
                                            tab {
                                                for ((index, part) in route.withIndex()) {
                                                    when (part) {
                                                        is ParsedRoutable.Segment.Variable -> {
                                                            appendLine("${part.name} = UrlProperties.decodeFromString(it.segments[$index]),")
                                                        }

                                                        else -> {}
                                                    }
                                                }
                                            }
                                            appendLine(").apply {")
                                            tab {
                                                for (qp in routable.queryParameters) {
                                                    appendLine("UrlProperties.decodeFromStringMap<${qp.type}>(\"${qp.qpName}\", it.parameters)?.let { this.${qp.name}.value = it }")
                                                }
                                            }
                                            appendLine("}")
                                        }
                                    }
                                    appendLine("},")
                                }
                            }
                        }
                        appendLine("),")
                        appendLine("renderers = mapOf(")
                        tab {
                            for (routable in allRoutables) {
                                val route = routable.routes.first()
                                val rendered = route.joinToString(", ") {
                                    when (it) {
                                        is ParsedRoutable.Segment.Constant -> "\"${it.value}\""
                                        is ParsedRoutable.Segment.Variable -> "UrlProperties.encodeToString(it.${it.name})"
                                    }
                                }
                                appendLine("${routable.source.simpleName!!.asString()}::class to label@{")
                                tab {
                                    appendLine("if (it !is ${routable.source.simpleName!!.asString()}) return@label null")
                                    appendLine("val p = HashMap<String, String>()")
                                    routable.queryParameters.forEach {
                                        appendLine("UrlProperties.encodeToStringMap(it.${it.name}.value, \"${it.qpName}\", p)")
                                    }
                                    appendLine("RouteRendered(UrlLikePath(")
                                    tab {
                                        appendLine("segments = listOf($rendered),")
                                        appendLine("parameters = p")
                                    }
                                    appendLine("), listOf(${routable.queryParameters.joinToString { "it.${it.name}" }}))")
                                }
                                appendLine("},")
                            }
                        }
                        appendLine("),")
                        if (fallbackRoute.classKind == ClassKind.OBJECT)
                            appendLine("fallback = ${fallbackRoute.simpleName!!.asString()}")
                        else
                            appendLine("fallback = ${fallbackRoute.simpleName!!.asString()}()")
                    }
                    appendLine(")")
                }
            }

        logger.info("Complete.")
    }
}

class MyProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return RouterGeneration(
            codeGenerator = environment.codeGenerator,
            logger = environment.logger,
        )
    }
}

class ParsedRoutable(
    val source: KSClassDeclaration
) {
    sealed class Segment {
        data class Constant(val value: String) : Segment()
        data class Variable(val name: String, val type: KSType) : Segment()
    }

    val routes = source.annotations("Routable")!!.map {
        it.arguments[0].value as String
    }.map {
        it.split('/')
            .filter { it.isNotBlank() }
            .map {
                if (it.startsWith('{')) {
                    val n = it.trim('{', '}')
                    Segment.Variable(
                        name = n,
                        type = source.primaryConstructor!!.parameters
                            .find { it.name!!.asString() == n }!!
                            .type
                            .resolve()
                    )
                } else Segment.Constant(it)
            }
    }

    data class QueryParam(val name: String, val type: KSType, val qpName: String)

    val queryParameters = source.getAllProperties()
        .flatMap {
            it.annotations("QueryParameter").map { a ->
                it to ((a.arguments[0].value as? String)?.takeUnless { it.isBlank() } ?: it.simpleName.asString())
            }
        }
        .map {
            QueryParam(
                it.first.simpleName.asString(),
                it.first.type.resolve().arguments.first().type!!.resolve(),
                it.second
            )
        }
        .sortedBy { it.qpName }
}