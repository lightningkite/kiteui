package com.lightningkite.kiteui

import org.gradle.api.Task
import java.io.File


internal fun resourcesCommon(resourceFolder: File, out: File, ext: KiteUiPluginExtension) {
    var usesBlob = false
    val lines = resourceFolder.resources()
        .entries
        .sortedBy { it.key }
        .joinToString("\n    ") {
            when (val r = it.value) {
                is Resource.Font -> "val ${r.name}: Font"
                is Resource.Image -> "val ${r.name}: ImageResource"
                is Resource.Video -> "val ${r.name}: VideoResource"
                is Resource.Audio -> "val ${r.name}: AudioResource"
                is Resource.Binary -> {
                    usesBlob = true; "suspend fun ${r.name}(): Blob"
                }

                else -> ""
            }
        }

    val imports = mutableListOf("import com.lightningkite.kiteui.models.*")
        .also { if (usesBlob) it.add("import com.lightningkite.kiteui.Blob") }
        .joinToString("\n")

    out.writeText(
        """
    package ${ext.packageName}
    
    $imports
    
    expect object Resources {
        $lines
    }
            """.trimIndent()
    )
}

internal fun resourcesJs(gitIgnore: File, resourceFolder: File, out: File, ext: KiteUiPluginExtension) {
    var usesBlob = false
    gitIgnore.writeText("*\n")
    val lines = resourceFolder.resources()
        .entries
        .sortedBy { it.key }
        .joinToString("\n    ") {
            when (val r = it.value) {
                is Resource.Font -> {
                    val normal = r.normal.entries.joinToString { "${it.key} to \"common/${it.value.relativeFile.toString().replace(File.separatorChar, '/')}\"" }
                    val italics = r.italics.entries.joinToString { "${it.key} to \"common/${it.value.relativeFile.toString().replace(File.separatorChar, '/')}\"" }
                    "actual val ${r.name}: Font = Font(cssFontFamilyName = \"${r.name}\", direct = FontDirect(normal = mapOf($normal), italics = mapOf($italics)))"
                }

                is Resource.Image -> "actual val ${r.name}: ImageResource = ImageResource(\"common/${
                    r.relativeFile.toString().replace(File.separatorChar, '/')
                }\")"

                is Resource.Video -> "actual val ${r.name}: VideoResource = VideoResource(\"common/${
                    r.relativeFile.toString().replace(File.separatorChar, '/')
                }\")"

                is Resource.Audio -> "actual val ${r.name}: AudioResource = AudioResource(\"common/${
                    r.relativeFile.toString().replace(File.separatorChar, '/')
                }\")"

                is Resource.Binary -> {
                    usesBlob = true; "actual suspend fun ${r.name}(): Blob = fetch(\"common/${
                        r.relativeFile.toString().replace(File.separatorChar, '/')
                    }\").blob()"
                }

                else -> ""
            }
        }

    val imports = mutableListOf("import com.lightningkite.kiteui.models.*")
        .also {
            if (usesBlob) {
                it.add("import com.lightningkite.kiteui.Blob")
                it.add("import com.lightningkite.kiteui.fetch")
            }
        }
        .joinToString("\n")

    out.writeText(
        """
    package ${ext.packageName}
    
    $imports
    
    actual object Resources {
        $lines
    }
            """.trimIndent()
    )
}

internal fun resourcesIos(
    resourceFolder: File,
    outPlist: File,
    outNonAssets: File,
    outAssets: File,
    outKt: File,
    ext: KiteUiPluginExtension
) {
    val resources = resourceFolder.resources()
        .entries
        .sortedBy { it.key }

    run {
        val original = outPlist.readText()
        val uiAppFontsContent = resources.map { it.value }.filterIsInstance<Resource.Font>().flatMap {
            it.files.map { f ->
                val copyName = it.name + "-" + f.source.name
                f.source.copyTo(outNonAssets.resolve(copyName), overwrite = true)
                copyName
            }
        }.joinToString("") {
            "<string>$it</string>"
        }
        if (original.contains("<key>UIAppFonts</key>")) {
            outPlist.writeText(
                original
                    .substringBefore("<key>UIAppFonts</key>") +
                        "<key>UIAppFonts</key><array>" +
                        uiAppFontsContent +
                        "</array>" +
                        original.substringAfter("<key>UIAppFonts</key>")
                            .substringAfter("</array>")
            )
        } else {
            outPlist.writeText(
                original
                    .substringBefore("</dict>\n</plist>") +
                        "<key>UIAppFonts</key><array>" +
                        uiAppFontsContent +
                        "</array>\n" +
                        "</dict>\n</plist>"
            )
        }
    }

    resources.forEach {
        when (val r = it.value) {
            is Resource.Font -> {}
            is Resource.Image -> {
                val f = outAssets.resolve(it.key + ".imageset")
                f.mkdirs()
                val i = f.resolve(r.source.name)
                r.source.copyTo(i, overwrite = true)
                f.resolve("Contents.json").writeText(
                    """
                                    {
                                        "info": { "version": 1, "author": "xcode" },
                                        "images": [
                                            { 
                                                "filename": "${i.name}",
                                                "scale": "1x",
                                                "idiom": "universal"
                                            }
                                        ]
                                    }
                                """.trimIndent()
                )
            }

            is Resource.Audio -> {
                val i = outNonAssets.resolve(r.name + "." + r.relativeFile.extension)
                r.source.copyTo(i, overwrite = true)
            }

            is Resource.Video -> {
                val i = outNonAssets.resolve(r.name + "." + r.relativeFile.extension)
                r.source.copyTo(i, overwrite = true)
            }

            is Resource.Binary -> {
                val f = outAssets.resolve(it.key + ".dataset")
                f.mkdirs()
                val i = f.resolve(r.source.name)
                r.source.copyTo(i, overwrite = true)
                f.resolve("Contents.json").writeText(
                    """
                                        {
                                          "data" : [
                                            {
                                              "filename" : "${i.name}",
                                              "idiom" : "universal"
                                            }
                                          ],
                                          "info" : {
                                            "author" : "xcode",
                                            "version" : 1
                                          }
                                        }
    
                                """.trimIndent()
                )
            }

            else -> {}
        }
    }

    var usesBlob = false
    val lines = resources
        .joinToString("\n    ") {
            when (val r = it.value) {
                is Resource.Font -> {
                    val normal = r.normal.entries.joinToString { "${it.key} to ${it.value.postScriptName.str()}" }
                    val italics = r.italics.entries.joinToString { "${it.key} to ${it.value.postScriptName.str()}" }
                    "actual val ${r.name}: Font = fontFromFamilyInfo(normal = mapOf($normal), italics = mapOf($italics))  // ${r}"
                }

                is Resource.Image -> "actual val ${r.name}: ImageResource = ImageResource(\"${it.key}\")"
                is Resource.Video -> "actual val ${r.name}: VideoResource = VideoResource(\"${it.key}\", \"${r.source.extension}\")"
                is Resource.Audio -> "actual val ${r.name}: AudioResource = AudioResource(\"${it.key}\", \"${r.source.extension}\")"
                is Resource.Binary -> {
                    usesBlob = true; "actual suspend fun ${r.name}(): Blob = TODO()"
                }

                else -> ""
            }
        }

    val imports = mutableListOf("import com.lightningkite.kiteui.models.*")
        .also { if (usesBlob) it.add("import com.lightningkite.kiteui.Blob") }
        .joinToString("\n")

    outKt.writeText(
        """
    package ${ext.packageName}
    
    $imports
    
    actual object Resources {
        $lines
    }
            """.trimIndent()
    )
}

internal fun resourcesAndroid(resourceFolder: File, androidResFolder: File, outKt: File, ext: KiteUiPluginExtension) {
    val resources = resourceFolder.resources()
        .entries
        .sortedBy { it.key }
    val androidDrawableFolder = androidResFolder.resolve("drawable-xhdpi").also { it.mkdirs() }
    val androidRawFolder = androidResFolder.resolve("raw").also { it.mkdirs() }
    resources.forEach { (key, value) ->
        if (value !is Resource.Image) return@forEach
        val destFile = androidDrawableFolder.resolve(key.snakeCase() + "." + value.source.extension)
        value.source.copyTo(destFile, overwrite = true)
    }
    resources.forEach { (key, value) ->
        if (value !is Resource.Video) return@forEach
        val destFile = androidRawFolder.resolve(key.snakeCase() + "." + value.source.extension)
        value.source.copyTo(destFile, overwrite = true)
    }
    resources.forEach { (key, value) ->
        if (value !is Resource.Audio) return@forEach
        val destFile = androidRawFolder.resolve(key.snakeCase() + "." + value.source.extension)
        value.source.copyTo(destFile, overwrite = true)
    }
    resources.forEach { (key, value) ->
        if (value !is Resource.Binary) return@forEach
        val destFile = androidRawFolder.resolve(key.snakeCase() + "." + value.source.extension)
        value.source.copyTo(destFile, overwrite = true)
    }
    val androidFontFolder = androidResFolder.resolve("font").also { it.mkdirs() }
    resources.forEach { (key, value) ->
        if (value !is Resource.Font) return@forEach
        val xmlFile = androidFontFolder.resolve(key.snakeCase() + ".xml")
        val variants = value.normal.map {
            val destFile =
                androidFontFolder.resolve(key.snakeCase() + "_${it.key}_normal." + it.value.source.extension)
            it.value.source.copyTo(destFile, overwrite = true)
            """
                                <font
                                    android:fontStyle="normal"
                                    android:fontWeight="${it.key}"
                                    android:font="@font/${destFile.nameWithoutExtension}" />
                                """.trimIndent()
        } + value.italics.map {
            val destFile =
                androidFontFolder.resolve(key.snakeCase() + "_${it.key}_italic." + it.value.source.extension)
            it.value.source.copyTo(destFile, overwrite = true)
            """
                                <font
                                    android:fontStyle="italic"
                                    android:fontWeight="${it.key}"
                                    android:font="@font/${destFile.nameWithoutExtension}" />
                                """.trimIndent()
        }
        xmlFile.writeText(
            """
    <?xml version="1.0" encoding="utf-8"?>
    <font-family xmlns:android="http://schemas.android.com/apk/res/android">
    ${variants.joinToString("\n")}
    </font-family>
                        """.trim()
        )
    }
    var usesBlob = false
    val lines = resources
        .joinToString("\n    ") {
            when (val r = it.value) {
                is Resource.Font -> "actual val ${r.name}: Font = AndroidAppContext.applicationCtx.resources.getFont(R.font.${it.key.snakeCase()})"
                is Resource.Image -> "actual val ${r.name}: ImageResource = ImageResource(R.drawable.${it.key.snakeCase()})"
                is Resource.Video -> "actual val ${r.name}: VideoResource = VideoResource(R.raw.${it.key.snakeCase()})"
                is Resource.Audio -> "actual val ${r.name}: AudioResource = AudioResource(R.raw.${it.key.snakeCase()})"
                is Resource.Binary -> {
                    usesBlob = true; "actual suspend fun ${r.name}(): Blob = TODO()"
                }

                else -> ""
            }
        }

    val imports = mutableListOf(
        "import com.lightningkite.kiteui.models.*",
        "import com.lightningkite.kiteui.views.AndroidAppContext"
    ).also { if (usesBlob) it.add("import com.lightningkite.kiteui.Blob") }
        .joinToString("\n")

    outKt.writeText(
        """
    package ${ext.packageName}
    
    $imports
    
    actual object Resources {
        $lines
    }
            """.trimIndent()
    )
}