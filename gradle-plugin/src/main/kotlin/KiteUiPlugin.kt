package com.lightningkite.kiteui

import groovy.lang.GroovyObject
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.create
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
            if (ext.packageName.isEmpty())
                throw IllegalArgumentException("KiteUiPluginExtension property packageName is not set. Please configure KiteUiPluginExtension and provide a value")
            if (ext.iosProjectRoot.path.isEmpty())
                throw IllegalArgumentException("KiteUiPluginExtension property iosProjectRoot is not set. Please configure KiteUiPluginExtension and provide a value")
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

        tasks.register("kiteuiResourcesCommon", Task::class.java).apply {
            val task = this.get()
            group = "kiteui"
            val resourceFolder = project.file("src/commonMain/resources")
            task.inputs.files(resourceFolder)
            val out = project.file("build/generated/kiteui-common/Resources.kt")
            task.outputs.file(out)
            task.doLast {
                if (resourceFolder.listFiles()?.isNotEmpty() == true) {
                    resourcesCommon(resourceFolder, out, ext)
                }
            }
            tasks.matching { it.name == "compileCommonMainKotlinMetadata" }.configureEach { dependsOn(task) }
            tasks.matching { it.name.startsWith("ksp") && it.name.contains("metadata") }
                .configureEach { println("CONFIGURE kspKotlinJs"); dependsOn(task) }
        }

        tasks.register("kiteuiResourcesJsNonVitePart", Copy::class.java).apply {
            val task = this.get()
            task.dependsOn("kiteuiResourcesCommon")
            group = "kiteui"
            task.from("src/commonMain/resources")
            task.into("src/jsMain/resources/common")
        }
        tasks.register("kiteuiResourcesJs", Copy::class.java).apply {
            val task = this.get()
            task.dependsOn("kiteuiResourcesJsNonVitePart")
            group = "kiteui"
            task.from("src/commonMain/resources")
            task.into("src/jsMain/resources/common")
            task.into("src/jsMain/resources/public/common")
            val out = project.file("build/generated/kiteui-js/Resources.js.kt")
            val gitIgnore = project.file("src/jsMain/resources/common/.gitignore")
            val publicGitIgnore = project.file("src/jsMain/resources/public/common/.gitignore")
            task.outputs.file(out)
            task.outputs.file(gitIgnore)
            task.outputs.file(publicGitIgnore)
            val resourceFolder = project.file("src/commonMain/resources")
            task.inputs.files(resourceFolder)
            task.doLast {
                resourcesJs(listOf(gitIgnore, publicGitIgnore), resourceFolder, out, ext)
            }
            tasks.matching { it.name == "compileKotlinJs" }.configureEach { dependsOn(task) }
            tasks.matching { it.name == "kspKotlinJs" }
                .configureEach { println("CONFIGURE kspKotlinJs"); dependsOn(task) }
            tasks.matching { it.name == "jsProcessResources" }.configureEach { dependsOn(task) }
        }

        tasks.register("kiteuiResourcesJvm", Task::class.java).apply {
            val task = this.get()
            task.dependsOn("kiteuiResourcesCommon")
            group = "kiteui"
            val out = project.file("build/generated/kiteui-jvm/Resources.jvm.kt")
            val gitIgnore = project.file("src/jvmMain/resources/common/.gitignore")
            task.outputs.file(out)
            task.outputs.file(gitIgnore)
            val resourceFolder = project.file("src/commonMain/resources")
            task.inputs.files(resourceFolder)
            task.doLast {
                resourcesJs(listOf(gitIgnore), resourceFolder, out, ext)
            }
            tasks.matching { it.name == "compileKotlinJvm" }.configureEach { dependsOn(task) }
            tasks.matching { it.name == "kspKotlinJvm" }.configureEach { dependsOn(task) }
            tasks.matching { it.name == "jvmProcessResources" }.configureEach { dependsOn(task) }
        }
        tasks.register("kiteuiResourcesJvmSsr", Task::class.java).apply {
            val task = this.get()
            task.dependsOn("kiteuiResourcesCommon")
            group = "kiteui"
            val out = project.file("build/generated/kiteui-jvmSsr/Resources.jvm.kt")
            val gitIgnore = project.file("src/jvmSsrMain/resources/common/.gitignore")
            task.outputs.file(out)
            task.outputs.file(gitIgnore)
            val resourceFolder = project.file("src/commonMain/resources")
            task.inputs.files(resourceFolder)
            task.doLast {
                resourcesJs(listOf(gitIgnore), resourceFolder, out, ext)
            }
            tasks.matching { it.name == "compileKotlinJvmSsr" }.configureEach { dependsOn(task) }
            tasks.matching { it.name == "kspKotlinJvmSsr" }.configureEach { dependsOn(task) }
            tasks.matching { it.name == "jvmSsrProcessResources" }.configureEach { dependsOn(task) }
        }

        tasks.register("kiteuiResourcesIos").apply {
            val task = this.get()
            task.dependsOn("kiteuiResourcesCommon")
            group = "kiteui"
            val resourceFolder = project.file("src/commonMain/resources")
            resourceFolder.mkdirs()

            val outKt = project.file("build/generated/kiteui-ios/Resources.ios.kt")
            outKt.parentFile.mkdirs()
            task.outputs.file(outKt)

            task.inputs.dir(resourceFolder)
            afterEvaluate {
                val outProject = ext.iosProjectRoot
                outProject.mkdirs()
                val outAssets = outProject.resolve("Assets.xcassets")
                val outNonAssets = outProject.resolve("resourcesFromCommon")
                val outPlist = outProject.resolve("Info.plist")
                task.outputs.dir(outAssets)
                task.outputs.dir(outNonAssets)
                task.outputs.file(outPlist)
                task.doLast {
                    resourcesIos(resourceFolder, outPlist, outNonAssets, outAssets, outKt, ext)
                }
            }
            tasks.matching {
                it.name.startsWith("compile") && it.name.contains(
                    "ios",
                    true
                ) && it.name.contains("kotlin", true)
            }
                .configureEach { dependsOn(task) }
            tasks.matching {
                it.name.startsWith("ksp") && it.name.contains("ios", true) && it.name.contains(
                    "kotlin",
                    true
                )
            }
                .configureEach { dependsOn(task) }
            tasks.matching { it.name.contains("ios", true) && it.name.endsWith("ProcessResources") }
                .configureEach { dependsOn(task) }
        }

        tasks.register("kiteuiResourcesAndroid").apply {
            val task = this.get()
            task.dependsOn("kiteuiResourcesCommon")
            group = "kiteui"
            val resourceFolder = project.file("src/commonMain/resources")
            task.inputs.files(resourceFolder)
            val androidResFolder = project.file("src/androidMain/res")
            val outKt =
                project.file("build/generated/kiteui-android/Resources.android.kt")
            task.outputs.file(outKt)
            // TODO: Manifest for tracking which files are under our control; git-ignore
            task.doLast {
                resourcesAndroid(resourceFolder, androidResFolder, outKt, ext)
            }
            tasks.matching { it.name.startsWith("compile") && it.name.endsWith("KotlinAndroid", true) }
                .configureEach { dependsOn(task) }
            tasks.matching { it.name.startsWith("generate", true) && it.name.endsWith("Resources", true) }
                .configureEach { dependsOn(task) }
        }

        tasks.register("kiteuiResourcesAll").apply {
            val task = this.get()
            group = "kiteui"
            task.dependsOn("kiteuiResourcesCommon")
            task.dependsOn("kiteuiResourcesJs")
            task.dependsOn("kiteuiResourcesIos")
            task.dependsOn("kiteuiResourcesAndroid")
            task.dependsOn("kiteuiResourcesJvm")
        }

        tasks.create("generateAutoRoutes", configuration = autoRoutes@{
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
            }.configureEach { dependsOn(this@autoRoutes) }
        })

        tasks.create("syncVersionsIos", configuration = {
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

        })
        tasks.create(name = "syncVersionsJs", configuration = jsVersion@{
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
            }.configureEach { dependsOn(this@jsVersion) }

        })
        tasks.create("syncVersions", configuration = {
            dependsOn("syncVersionsIos")
            dependsOn("syncVersionsJs")
        })
        registerAiDriverTasks(project)
        Unit
    }

}

internal val Any?.groovyObject: GroovyObject? get() = this as? GroovyObject
internal fun GroovyObject.getPropertyAsObject(key: String): GroovyObject? = getProperty(key) as? GroovyObject
