package com.lightningkite.kiteui

import groovy.lang.GroovyObject
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Copy
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import java.io.File

interface KiteUiPluginExtension {
    var packageName: String
    var iosProjectRoot: File
}

// Test Note

class KiteUiPlugin : Plugin<Project> {
    override fun apply(project: Project) = with(project) {
        val ext = extensions.create("kiteui", KiteUiPluginExtension::class.java)
        afterEvaluate {
            if (ext.packageName == null)
                throw IllegalArgumentException("KiteUiPluginExtension property packageName is null. Please configure KiteUiPluginExtension and provide a value")
            if (ext.iosProjectRoot == null)
                throw IllegalArgumentException("KiteUiPluginExtension property iosProjectRoot is null. Please configure KiteUiPluginExtension and provide a value")
        }

        val kotlinExtension = project.extensions.findByName("kotlin")
        if (kotlinExtension != null) {
            // Calls 'KotlinSourceSetContainer.getSourceSets()'
            afterEvaluate {
                val kotlinSourceSets = kotlinExtension.javaClass.getMethod("getSourceSets").invoke(kotlinExtension)
                if (kotlinSourceSets is NamedDomainObjectContainer<*>) {
                    for (kotlinSourceSet in kotlinSourceSets) {
                        (kotlinSourceSet as KotlinSourceSet).let {
                            if (it.name.endsWith("Main")) {
                                it.kotlin.srcDir("build/generated/kiteui-${it.name.removeSuffix("Main")}")
                            }
                        }
                    }
                }
            }
        }

        tasks.create("kiteuiResourcesCommon", Task::class.java).apply {
            val task = this
            group = "kiteui"
            val resourceFolder = project.file("src/commonMain/resources")
            inputs.files(resourceFolder)
            val out = project.file("build/generated/kiteui-common/Resources.kt")
            outputs.file(out)
            doLast {
                if (resourceFolder.listFiles()?.isNotEmpty() == true) {
                    resourcesCommon(resourceFolder, out, ext)
                }
            }
            tasks.matching { it.name == "compileCommonMainKotlinMetadata" }.configureEach { dependsOn(task) }
            tasks.matching { it.name.startsWith("ksp") && it.name.contains("metadata") }.configureEach { println("CONFIGURE kspKotlinJs"); dependsOn(task) }
        }

        tasks.create("kiteuiResourcesJsNonVitePart", Copy::class.java).apply {
            val task = this
            dependsOn("kiteuiResourcesCommon")
            group = "kiteui"
            from("src/commonMain/resources")
            into("src/jsMain/resources/common")
        }
        tasks.create("kiteuiResourcesJs", Copy::class.java).apply {
            val task = this
            dependsOn("kiteuiResourcesJsNonVitePart")
            group = "kiteui"
            from("src/commonMain/resources")
            into("src/jsMain/resources/common")
            into("src/jsMain/resources/public/common")
            val out = project.file("build/generated/kiteui-js/Resources.js.kt")
            val gitIgnore = project.file("src/jsMain/resources/common/.gitignore")
            val publicGitIgnore = project.file("src/jsMain/resources/public/common/.gitignore")
            outputs.file(out)
            outputs.file(gitIgnore)
            outputs.file(publicGitIgnore)
            val resourceFolder = project.file("src/commonMain/resources")
            inputs.files(resourceFolder)
            doLast {
                resourcesJs(listOf(gitIgnore, publicGitIgnore), resourceFolder, out, ext)
            }
            tasks.matching { it.name == "compileKotlinJs" }.configureEach { dependsOn(task) }
            tasks.matching { it.name == "kspKotlinJs" }.configureEach { println("CONFIGURE kspKotlinJs"); dependsOn(task) }
            tasks.matching { it.name == "jsProcessResources" }.configureEach { dependsOn(task) }
        }

        tasks.create("kiteuiResourcesJvm", Task::class.java).apply {
            val task = this
            dependsOn("kiteuiResourcesCommon")
            group = "kiteui"
            val out = project.file("build/generated/kiteui-jvm/Resources.jvm.kt")
            val gitIgnore = project.file("src/jvmMain/resources/common/.gitignore")
            outputs.file(out)
            outputs.file(gitIgnore)
            val resourceFolder = project.file("src/commonMain/resources")
            inputs.files(resourceFolder)
            doLast {
                resourcesJs(listOf(gitIgnore), resourceFolder, out, ext)
            }
            tasks.matching { it.name == "compileKotlinJvm" }.configureEach { dependsOn(task) }
            tasks.matching { it.name == "kspKotlinJvm" }.configureEach { dependsOn(task) }
            tasks.matching { it.name == "jvmProcessResources" }.configureEach { dependsOn(task) }
        }

        tasks.create("kiteuiResourcesIos").apply {
            val task = this
            dependsOn("kiteuiResourcesCommon")
            group = "kiteui"
            val resourceFolder = project.file("src/commonMain/resources")
            resourceFolder.mkdirs()

            val outKt = project.file("build/generated/kiteui-ios/Resources.ios.kt")
            outKt.parentFile.mkdirs()
            outputs.file(outKt)

            inputs.dir(resourceFolder)
            afterEvaluate {
                val outProject = ext.iosProjectRoot
                outProject.mkdirs()
                val outAssets = outProject.resolve("Assets.xcassets")
                val outNonAssets = outProject.resolve("resourcesFromCommon")
                val outPlist = outProject.resolve("Info.plist")
                outputs.dir(outAssets)
                outputs.dir(outNonAssets)
                outputs.file(outPlist)
                doLast {
                    resourcesIos(resourceFolder, outPlist, outNonAssets, outAssets, outKt, ext)
                }
            }
            tasks.matching { it.name.startsWith("compileKotlin") && it.name.contains("ios", true) }
                .configureEach { dependsOn(task) }
            tasks.matching { it.name.startsWith("kspKotlin") && it.name.contains("ios", true) }
                .configureEach { dependsOn(task) }
            tasks.matching { it.name.contains("ios", true) && it.name.endsWith("ProcessResources") }
                .configureEach { dependsOn(task) }
        }

        tasks.create("kiteuiResourcesAndroid").apply {
            val task = this
            dependsOn("kiteuiResourcesCommon")
            group = "kiteui"
            val resourceFolder = project.file("src/commonMain/resources")
            inputs.files(resourceFolder)
            val androidResFolder = project.file("src/androidMain/res")
            val outKt =
                project.file("build/generated/kiteui-android/Resources.android.kt")
            outputs.file(outKt)
            // TODO: Manifest for tracking which files are under our control; git-ignore
            doLast {
                resourcesAndroid(resourceFolder, androidResFolder, outKt, ext)
            }
            tasks.matching { it.name.startsWith("compile") && it.name.endsWith("KotlinAndroid", true) }
                .configureEach { dependsOn(task) }
            tasks.matching { it.name.startsWith("generate", true) && it.name.endsWith("Resources", true) }
                .configureEach { dependsOn(task) }
        }

        tasks.create("kiteuiResourcesAll").apply {
            val task = this
            group = "kiteui"
            dependsOn("kiteuiResourcesCommon")
            dependsOn("kiteuiResourcesJs")
            dependsOn("kiteuiResourcesIos")
            dependsOn("kiteuiResourcesAndroid")
            dependsOn("kiteuiResourcesJvm")
        }

        tasks.create("generateAutoRoutes") {
            val task = this
            group = "kiteui"
            val sources = project.file("src/commonMain/kotlin")
            inputs.dir(sources)
            val out = project.file("build/generated/kiteui-common/autoroutes.kt")
            outputs.file(out)
            doLast {
                generateAutoroutes(sources, out)
            }
            tasks.matching {
                (it.name.contains("compile") &&
                        it.name.contains("Kotlin")) ||
                        (it.name.contains("ksp") &&
                                it.name.contains("Kotlin"))
            }.configureEach { dependsOn(task) }
        }

        tasks.create("syncVersionsIos") {
            val task = this
            group = "kiteui"
            doLast {
                val versionName =
                    project.extensions.findByName("android")?.groovyObject?.getPropertyAsObject("defaultConfig")
                        ?.getProperty("versionName") as? String ?: project.version.toString()
                val versionCode =
                    project.extensions.findByName("android")?.groovyObject?.getPropertyAsObject("defaultConfig")
                        ?.getProperty("versionCode") as? Int ?: 0
                val projectFolder = ext.iosProjectRoot.parentFile.listFiles()?.toList()
                    ?.find { it.name.endsWith("xcodeproj", true) }
                    ?: run {
                        println("No xcodeproj found in ${ext.iosProjectRoot}")
                        return@doLast
                    }
                val projectFile = projectFolder
                    .resolve("project.pbxproj")
                    .also {
                        if (!it.exists()) {
                            println("No xcodeproj found in ${ext.iosProjectRoot}")
                            return@doLast
                        }
                    }
                projectFile.readText()
                    .replace(Regex("CURRENT_PROJECT_VERSION = [0-9]+;"), "CURRENT_PROJECT_VERSION = $versionCode;")
                    .replace(Regex("MARKETING_VERSION = [0-9.]+;"), "MARKETING_VERSION = $versionName;")
                    .let { projectFile.writeText(it) }
            }
            // NOTE: WE DON'T DO THIS ON PURPOSE
            // Problem is that if the project file changes that the build is cancelled.
            // TODO: this is dumb, how do we make this work?
//            tasks.matching {
//                it.name == "syncFramework"
//            }.configureEach { dependsOn(task) }

        }
        tasks.create("syncVersionsJs") {
            val task = this
            group = "kiteui"
            val out1 = project.file("src/jsMain/resources/version.js")
            val out2 = project.file("src/jsMain/resources/public/version.js")
            outputs.file(out1)
            outputs.file(out2)
            doLast {
                val versionName =
                    project.extensions.findByName("android")?.groovyObject?.getPropertyAsObject("defaultConfig")
                        ?.getProperty("versionName") as? String ?: project.version.toString()
                val versionCode =
                    project.extensions.findByName("android")?.groovyObject?.getPropertyAsObject("defaultConfig")
                        ?.getProperty("versionCode") as? Int ?: 0
                val js = """
                    window.version = "$versionName";
                    window.debug = false;
                """.trimIndent()
                out1.writeText(js)
                out2.writeText(js)
            }
            tasks.matching {
                it.name == "jsProcessResources"
            }.configureEach { dependsOn(task) }

        }
        tasks.create("syncVersions") {
            dependsOn("syncVersionsIos")
            dependsOn("syncVersionsJs")
        }
        Unit
    }

}

val Any?.groovyObject: GroovyObject? get() = this as? GroovyObject
fun GroovyObject.getPropertyAsObject(key: String): GroovyObject? = getProperty(key) as? GroovyObject
