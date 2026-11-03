package com.lightningkite.kiteui

import groovy.lang.GroovyObject
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Copy
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.kotlin.dsl.create
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import java.io.File

interface KiteUiPluginExtension {
    var packageName: String
    /** The Xcode project's source folder; leave unset in modules without iOS targets. */
    var iosProjectRoot: File?
    /** Where the app's common code and resources live; defaults to src/commonMain.  A desktop twin of an app points this at the app's. */
    var commonMainDirectory: File?
}

// Test Note

class KiteUiPlugin : Plugin<Project> {
    override fun apply(project: Project) = with(project) {
        val ext = extensions.create("kiteui", KiteUiPluginExtension::class.java)
        afterEvaluate {
            if (ext.packageName.isEmpty())
                throw IllegalArgumentException("KiteUiPluginExtension property packageName is not set. Please configure KiteUiPluginExtension and provide a value")
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

        // Lazy, since the build script configures the extension after the plugin registers its tasks.
        val commonMain = provider { ext.commonMainDirectory ?: project.file("src/commonMain") }
        val resourceFolder = commonMain.map { it.resolve("resources") }

        tasks.register("kiteuiResourcesCommon", Task::class.java).apply {
            val task = this.get()
            group = "kiteui"
            task.inputs.files(resourceFolder)
            val out = project.file("build/generated/kiteui-common/Resources.kt")
            task.outputs.file(out)
            task.doLast {
                if (resourceFolder.get().listFiles()?.isNotEmpty() == true) {
                    resourcesCommon(resourceFolder.get(), out, ext)
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
            task.from(resourceFolder)
            task.into("src/jsMain/resources/common")
        }
        tasks.register("kiteuiResourcesJs", Copy::class.java).apply {
            val task = this.get()
            task.dependsOn("kiteuiResourcesJsNonVitePart")
            group = "kiteui"
            task.from(resourceFolder)
            // Copy's into() sets a single destinationDir, so calling it twice would just
            // overwrite the first target rather than copy to both. The "common" destination
            // is instead populated by the kiteuiResourcesJsNonVitePart dependency above.
            task.into("src/jsMain/resources/public/common")
            val out = project.file("build/generated/kiteui-js/Resources.js.kt")
            val gitIgnore = project.file("src/jsMain/resources/common/.gitignore")
            val publicGitIgnore = project.file("src/jsMain/resources/public/common/.gitignore")
            task.outputs.file(out)
            task.outputs.file(gitIgnore)
            task.outputs.file(publicGitIgnore)
            task.inputs.files(resourceFolder)
            task.doLast {
                resourcesJs(listOf(gitIgnore, publicGitIgnore), resourceFolder.get(), out, ext)
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
            task.outputs.file(out)
            task.inputs.files(resourceFolder)
            task.doLast {
                resourcesJvm(resourceFolder.get(), out, ext)
            }
            tasks.matching { it.name == "compileKotlinJvm" }.configureEach { dependsOn(task) }
            tasks.matching { it.name == "kspKotlinJvm" }.configureEach { dependsOn(task) }
            // Resources.jvm.kt loads its files from `common/<path>` on the classpath.
            tasks.withType(ProcessResources::class.java).matching { it.name == "jvmProcessResources" }.configureEach {
                from(resourceFolder) { into("common") }
            }
        }
        tasks.register("kiteuiResourcesJvmSsr", Task::class.java).apply {
            val task = this.get()
            task.dependsOn("kiteuiResourcesCommon")
            group = "kiteui"
            val out = project.file("build/generated/kiteui-jvmSsr/Resources.jvm.kt")
            val gitIgnore = project.file("src/jvmSsrMain/resources/common/.gitignore")
            task.outputs.file(out)
            task.outputs.file(gitIgnore)
            task.inputs.files(resourceFolder)
            task.doLast {
                resourcesJs(listOf(gitIgnore), resourceFolder.get(), out, ext)
            }
            tasks.matching { it.name == "compileKotlinJvmSsr" }.configureEach { dependsOn(task) }
            tasks.matching { it.name == "kspKotlinJvmSsr" }.configureEach { dependsOn(task) }
            tasks.matching { it.name == "jvmSsrProcessResources" }.configureEach { dependsOn(task) }
        }

        tasks.register("kiteuiResourcesIos").apply {
            val task = this.get()
            task.dependsOn("kiteuiResourcesCommon")
            group = "kiteui"

            val outKt = project.file("build/generated/kiteui-ios/Resources.ios.kt")
            outKt.parentFile.mkdirs()
            task.outputs.file(outKt)

            task.inputs.dir(resourceFolder)
            afterEvaluate {
                resourceFolder.get().mkdirs()
                val outProject = ext.iosProjectRoot
                if (outProject == null) {
                    if (tasks.names.any { it.startsWith("compile") && it.contains("ios", true) && it.contains("kotlin", true) })
                        throw IllegalArgumentException("KiteUiPluginExtension property iosProjectRoot is not set, but this project has iOS targets.  Set it to the Xcode project's source folder.")
                    return@afterEvaluate
                }
                outProject.mkdirs()
                val outAssets = outProject.resolve("Assets.xcassets")
                val outNonAssets = outProject.resolve("resourcesFromCommon")
                val outPlist = outProject.resolve("Info.plist")
                task.outputs.dir(outAssets)
                task.outputs.dir(outNonAssets)
                task.outputs.file(outPlist)
                task.doLast {
                    resourcesIos(resourceFolder.get(), outPlist, outNonAssets, outAssets, outKt, ext)
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
            task.inputs.files(resourceFolder)
            val androidResFolder = project.file("src/androidMain/res")
            val outKt =
                project.file("build/generated/kiteui-android/Resources.android.kt")
            task.outputs.file(outKt)
            // TODO: Manifest for tracking which files are under our control; git-ignore
            task.doLast {
                resourcesAndroid(resourceFolder.get(), androidResFolder, outKt, ext)
            }
            // compile*KotlinAndroid / generate*Resources come from com.android.application and com.android.library;
            // com.android.kotlin.multiplatform.library names its tasks after the androidMain compilation instead.
            tasks.matching {
                (it.name.startsWith("compile") && it.name.endsWith("KotlinAndroid", true)) ||
                        it.name == "compileAndroidMain"
            }
                .configureEach { dependsOn(task) }
            tasks.matching {
                (it.name.startsWith("generate", true) && it.name.endsWith("Resources", true)) ||
                        it.name in setOf(
                    "parseAndroidMainLocalResources",
                    "packageAndroidMainResources",
                    "compileAndroidMainLibraryResources",
                )
            }
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
            val sources = commonMain.map { it.resolve("kotlin") }
            inputs.dir(sources)
            val out = project.file("build/generated/kiteui-common/autoroutes.kt")
            outputs.file(out)
            doLast {
                generateAutoroutes(sources.get(), out)
            }
            // compileAndroidMain is com.android.kotlin.multiplatform.library's Kotlin compile task.
            tasks.matching {
                (it.name.contains("compile") &&
                        it.name.contains("Kotlin")) ||
                        (it.name.contains("ksp") &&
                                it.name.contains("Kotlin")) ||
                        it.name == "compileAndroidMain"
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
                val iosProjectRoot = ext.iosProjectRoot ?: run {
                    println("No iosProjectRoot set")
                    return@doLast
                }
                val projectFolder = iosProjectRoot.parentFile.listFiles()?.toList()
                    ?.find { it.name.endsWith("xcodeproj", true) }
                    ?: run {
                        println("No xcodeproj found in $iosProjectRoot")
                        return@doLast
                    }
                val projectFile = projectFolder
                    .resolve("project.pbxproj")
                    .also {
                        if (!it.exists()) {
                            println("No xcodeproj found in $iosProjectRoot")
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
