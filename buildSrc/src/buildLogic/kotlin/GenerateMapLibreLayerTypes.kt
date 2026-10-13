import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Generates typed MapLibre layer classes from the MapLibre style spec (`v8.json`).
 *
 * For each layer type in the spec (e.g. `circle`), this generates a data class (`CircleLayer`)
 * implementing the hand-written `Layer` interface, with the source and one mutable
 * `Expression<T>?` property per paint and layout property.
 *
 * Enum-typed properties get enum classes. Enums used by a single layer type are nested in its
 * class without the layer's name (`CircleLayer.PitchAlignment`); enums shared between layer types
 * are top-level (`TranslateAnchor`).
 *
 * Encountering a spec type it doesn't know how to map fails the build, so adding a layer type
 * surfaces any missing value types immediately.
 */
@CacheableTask
abstract class GenerateMapLibreLayerTypes : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val specFile: RegularFileProperty

    @get:Input
    abstract val packageName: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        @Suppress("UNCHECKED_CAST")
        val spec = JsonSlurper().parse(specFile.get().asFile) as Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val layerTypeSpecs =
            (spec["layer"] as Map<String, Map<String, Any?>>)["type"]!!["values"] as Map<String, Map<String, Any?>>
        val layerTypes = layerTypeSpecs.keys
        val pkg = packageName.get()

        val properties = layerTypes.flatMap { layerType ->
            val groupSpecs = listOf("paint", "layout").associateWith { group ->
                @Suppress("UNCHECKED_CAST")
                spec["${group}_$layerType"] as? Map<String, Map<String, Any?>>
                    ?: error("The style spec has no '${group}_$layerType'")
            }
            val layerPropertyNames = groupSpecs.values.flatMap { it.keys }.toSet()
            groupSpecs.flatMap { (group, groupSpec) ->
                groupSpec.map { (name, propertySpec) ->
                    StyleProperty(layerType, group, name, propertySpec, layerPropertyNames)
                }
            }
        }.filter { it.supportedAnywhere }

        val enums = assignEnums(properties)

        val dir = outputDir.get().asFile
        dir.deleteRecursively()
        val packageDir = dir.resolve(pkg.replace('.', '/')).apply { mkdirs() }

        packageDir.resolve("StyleEnums.kt").writeText(buildString {
            appendLine(HEADER)
            appendLine("package $pkg")
            for (enum in enums.values.distinct().filter { it.ownerLayerType == null }.sortedBy { it.name }) {
                appendLine()
                appendEnum(enum, "")
            }
        })

        for (layerType in layerTypes) {
            val className = layerClassName(layerType)
            val typeDoc = layerTypeSpecs.getValue(layerType)["doc"] as? String
            val layerProperties = properties.filter { it.layerType == layerType }
            val nestedEnums = enums.values.distinct().filter { it.ownerLayerType == layerType }.sortedBy { it.name }
            val needsAnchorOffset = layerProperties.any { it.type == "variableAnchorOffsetCollection" }

            packageDir.resolve("$className.kt").writeText(buildString {
                appendLine(HEADER)
                appendLine("package $pkg")
                appendLine()
                appendLine("import com.lightningkite.kiteui.models.Color")
                appendLine("import kotlinx.serialization.json.JsonObject")
                appendLine("import kotlin.uuid.Uuid")
                appendLine()

                appendKdoc(
                    "${typeDoc ?: "A $layerType layer."}\n\nSee https://maplibre.org/maplibre-style-spec/layers/#$layerType",
                    ""
                )
                appendLine("data class $className(")
                appendLine("    /** The id of the source this layer draws, as a key of `MapView.sources`. */")
                appendLine("    override val source: Uuid,")
                appendLine("    override var filter: Expression<Boolean>? = null,")
                appendLine("    override var minZoom: Number? = null,")
                appendLine("    override var maxZoom: Number? = null,")
                for (property in layerProperties) {
                    appendKdoc(property.kdoc(), "    ")
                    appendLine("    var ${property.kotlinName}: Expression<${property.kotlinType(enums)}>? = null,")
                }
                appendLine(") : Layer {")
                appendLine("    override fun toJson(): JsonObject = layerJson(")
                appendLine("        paint = mapOf(")
                for (property in layerProperties.filter { it.group == "paint" }) {
                    appendLine("            \"${property.name}\" to ${property.kotlinName},")
                }
                appendLine("        ),")
                appendLine("        layout = mapOf(")
                for (property in layerProperties.filter { it.group == "layout" }) {
                    appendLine("            \"${property.name}\" to ${property.kotlinName},")
                }
                appendLine("        ),")
                appendLine("    )")
                for (enum in nestedEnums) {
                    appendLine()
                    appendEnum(enum, "    ")
                }
                if (needsAnchorOffset) {
                    val anchor = enums.getValue(ANCHOR_OFFSET_ANCHOR_PROPERTY).reference
                    appendLine()
                    appendLine("    /** An anchor paired with the text offset to use when the label is placed at it. */")
                    appendLine("    data class AnchorOffset(val anchor: $anchor, val offset: Point)")
                }
                appendLine("}")
            })
        }
    }

    private class StyleEnum(val name: String, val ownerLayerType: String?, val values: Map<String, String?>) {
        /** How generated code refers to this enum. */
        val reference = if (ownerLayerType == null) name else "${layerClassName(ownerLayerType)}.$name"
    }

    /**
     * Groups enum properties by name, minus their layer and `icon-` or `text-` prefixes, then by
     * values. Each group of properties with the same values shares one enum:
     * - Used by multiple layer types: top-level, named after the group (`TranslateAnchor`).
     * - Used by one layer type: nested in that layer's class, named after the group if multiple
     *   properties share it (`SymbolLayer.Anchor`), or else the property without its layer
     *   prefix (`CircleLayer.PitchAlignment`, `SymbolLayer.IconTextFit`).
     */
    private fun assignEnums(properties: List<StyleProperty>): Map<String, StyleEnum> {
        val result = HashMap<String, StyleEnum>()
        val enumProperties = properties.filter { it.enumValues != null }
        for ((enumKey, byKey) in enumProperties.groupBy { SHARED_ENUMS[it.name] ?: it.enumKey }) {
            for (group in byKey.groupBy { it.enumValues!!.keys }.values) {
                val owner = group.map { it.layerType }.distinct().singleOrNull()
                val name = pascalCase(if (group.size > 1) enumKey else group.single().shortName)
                val enum = StyleEnum(name, owner, group.first().enumValues!!)
                for (property in group) result[property.name] = enum
            }
        }
        result.values.distinct().groupBy { it.reference }.forEach { (reference, clashing) ->
            check(clashing.size == 1) { "Generated enums clash on the name $reference" }
        }
        return result
    }

    private class StyleProperty(
        val layerType: String,
        val group: String,
        val name: String,
        val spec: Map<String, Any?>,
        /** The names of all properties of [layerType], to avoid shortening one into another. */
        layerPropertyNames: Set<String>,
    ) {
        val type = spec["type"] as String

        /**
         * The name without the layer prefix, unless that is another property's name (`raster-resampling`
         * vs. `resampling`). `symbol` properties keep their `icon-` and `text-` prefixes.
         */
        val shortName = name.removePrefix("$layerType-").takeUnless { it != name && it in layerPropertyNames } ?: name

        val kotlinName = camelCase(shortName)

        val enumKey = when {
            shortName.startsWith("icon-") -> shortName.removePrefix("icon-")
            shortName.startsWith("text-") -> shortName.removePrefix("text-")
            else -> shortName
        }

        @Suppress("UNCHECKED_CAST")
        val enumValues: Map<String, String?>? = when {
            type == "enum" -> spec["values"]
            type == "array" && spec["value"] == "enum" -> spec["values"]
            else -> null
        }?.let { values -> (values as Map<String, Map<String, Any?>?>).mapValues { it.value?.get("doc") as? String } }

        @Suppress("UNCHECKED_CAST")
        private val basicSupport = (spec["sdk-support"] as? Map<String, Map<String, Any?>>)?.get("basic functionality")

        /** Platforms whose basic support is an issue link rather than a version. */
        val unsupportedOn = listOf("js" to "Web", "android" to "Android", "ios" to "iOS")
            .filter { (key, _) -> basicSupport?.get(key).toString().startsWith("http") }
            .map { it.second }

        val supportedAnywhere get() = unsupportedOn.size < 3

        fun kotlinType(enums: Map<String, StyleEnum>): String = when (type) {
            "number" -> "Number"
            "boolean" -> "Boolean"
            "string" -> "String"
            "color" -> "Color"
            "enum" -> enums.getValue(name).reference
            "formatted" -> "Formatted"
            "resolvedImage" -> "ResolvedImage"
            "padding" -> "Padding"
            "numberArray" -> "List<Number>"
            "colorArray" -> "List<Color>"
            "variableAnchorOffsetCollection" -> "List<${layerClassName(layerType)}.AnchorOffset>"
            "array" -> when (val itemType = spec["value"]) {
                "number" -> when (spec["length"]?.toString()?.toInt()) {
                    2 -> "Point"
                    4 -> "Padding"
                    null -> "List<Number>"
                    else -> error("Unsupported number array length for $name")
                }

                "string" -> "List<String>"
                "enum" -> "List<${enums.getValue(name).reference}>"
                else -> error("Unsupported array item type '$itemType' for $name")
            }

            else -> error("Unsupported style spec type '$type' for $name")
        }

        fun kdoc(): String = buildList {
            add("Style spec: [`$name`](https://maplibre.org/maplibre-style-spec/layers/#$name)")
            spec["doc"]?.let { add(it) }
            spec["requires"]?.let { add("Requires: $it") }
            spec["default"]?.let { add("Default: `${formatJson(it)}`") }
            spec["units"]?.let { add("Units: $it") }
            spec["minimum"]?.let { add("Minimum: $it") }
            spec["maximum"]?.let { add("Maximum: $it") }
            when (spec["property-type"]) {
                "data-constant" -> add("Can't depend on feature data")
                "color-ramp" -> add("Evaluated per point along the line with `lineProgress()`")
            }
            if (unsupportedOn.isNotEmpty()) add("**Not supported on ${unsupportedOn.joinToString()}**")
        }.joinToString("\n\n")
    }

    private companion object {
        /** Enum properties that should reuse another property's enum key, despite different names. */
        val SHARED_ENUMS = mapOf("text-variable-anchor" to "anchor")

        /** The property whose enum `AnchorOffset.anchor` uses. */
        const val ANCHOR_OFFSET_ANCHOR_PROPERTY = "text-anchor"

        const val HEADER = "// Generated by GenerateMapLibreStyle from the MapLibre style spec. Do not edit."

        fun layerClassName(layerType: String) = pascalCase(layerType) + "Layer"
        fun pascalCase(name: String) =
            name.split('-', '_').joinToString("") { it.replaceFirstChar(Char::uppercaseChar) }

        fun camelCase(name: String) = pascalCase(name).replaceFirstChar(Char::lowercaseChar)

        fun formatJson(value: Any?): String = when (value) {
            is String -> "\"$value\""
            is List<*> -> value.joinToString(", ", "[", "]") { formatJson(it) }
            else -> value.toString()
        }

        fun StringBuilder.appendKdoc(text: String, indent: String) {
            val lines = text.replace("*/", "*&#47;").trim().lines().map { it.trimEnd() }
            appendLine("$indent/**")
            for (line in lines) appendLine(if (line.isEmpty()) "$indent *" else "$indent * $line")
            appendLine("$indent */")
        }

        fun StringBuilder.appendEnum(enum: StyleEnum, indent: String) {
            appendLine("${indent}enum class ${enum.name}(override val value: String) : StyleEnum {")
            for ((value, doc) in enum.values) {
                doc?.let { appendKdoc(it, "$indent    ") }
                appendLine("$indent    ${pascalCase(value)}(\"$value\"),")
            }
            appendLine("$indent}")
        }
    }
}
