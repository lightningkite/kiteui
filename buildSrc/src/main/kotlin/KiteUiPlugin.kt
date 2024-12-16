package com.lightningkite.kiteui

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project
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

        tasks.create("kiteuiResourcesCommon", Task::class.java).apply {
            val task = this
            group = "kiteui"
            val resourceFolder = project.file("src/commonMain/resources")
            inputs.files(resourceFolder)
            afterEvaluate {
//                tasks.findByName("compileCommonMainKotlinMetadata")?.dependsOn(task)

                val out = project.file("src/commonMain/kotlin/${ext.packageName.replace(".", "/")}/ResourcesExpect.kt")
                outputs.file(out)
                doLast {
                    if(resourceFolder.listFiles()?.isNotEmpty() == true) {
                        resourcesCommon(resourceFolder, out, ext)
                    }
                }
            }
        }

        tasks.create("kiteuiResourcesJs", Copy::class.java).apply {
            val task = this
            dependsOn("kiteuiResourcesCommon")
            group = "kiteui"
            from("src/commonMain/resources")
            into("src/jsMain/resources/common")
            into("src/jsMain/resources/public/common")
            afterEvaluate {
//                tasks.findByName("compileKotlinJs")?.dependsOn(task)
                val out = project.file("src/jsMain/kotlin/${ext.packageName.replace(".", "/")}/ResourcesActual.kt")
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
            }
        }

        tasks.create("kiteuiResourcesJvm", Task::class.java).apply {
            val task = this
            dependsOn("kiteuiResourcesCommon")
            group = "kiteui"
            afterEvaluate {
//                tasks.findByName("compileKotlinJvm")?.dependsOn(task)
                val out = project.file("src/jvmMain/kotlin/${ext.packageName.replace(".", "/")}/ResourcesActual.kt")
                val gitIgnore = project.file("src/jvmMain/resources/common/.gitignore")
                outputs.file(out)
                outputs.file(gitIgnore)
                val resourceFolder = project.file("src/commonMain/resources")
                inputs.files(resourceFolder)
                doLast {
                    resourcesJs(listOf(gitIgnore), resourceFolder, out, ext)
                }
            }
        }

        tasks.create("kiteuiResourcesIos").apply {
            val task = this
            dependsOn("kiteuiResourcesCommon")
            group = "kiteui"

            afterEvaluate {
//                tasks.findByName("compileKotlinIosSimulatorArm64")?.dependsOn(task)
//                tasks.findByName("compileKotlinIosArm64")?.dependsOn(task)
//                tasks.findByName("compileKotlinIosX64")?.dependsOn(task)
                val outKt = project.file("src/iosMain/kotlin/${ext.packageName.replace(".", "/")}/ResourcesActual.kt")
                outputs.file(outKt)

                val outProject = ext.iosProjectRoot
                val outAssets = outProject.resolve("Assets.xcassets")
                val outNonAssets = outProject.resolve("resourcesFromCommon")
                val outPlist = outProject.resolve("Info.plist")
                outputs.dir(outAssets)
                outputs.dir(outNonAssets)
                outputs.file(outPlist)
                val resourceFolder = project.file("src/commonMain/resources")
                inputs.dir(resourceFolder)
                doLast {
                    resourcesIos(resourceFolder, outPlist, outNonAssets, outAssets, outKt, ext)
                }
            }
        }

        tasks.create("kiteuiResourcesAndroid").apply {
            val task = this
            dependsOn("kiteuiResourcesCommon")
            group = "kiteui"
            val resourceFolder = project.file("src/commonMain/resources")
            inputs.files(resourceFolder)
            val androidResFolder = project.file("src/androidMain/res")

            afterEvaluate {
//                tasks.findByName("compileReleaseKotlinAndroid")?.dependsOn(task)
//                tasks.findByName("compileDebugKotlinAndroid")?.dependsOn(task)
                val outKt =
                    project.file("src/androidMain/kotlin/${ext.packageName.replace(".", "/")}/ResourcesActual.kt")
                outputs.file(outKt)
                // TODO: Manifest for tracking which files are under our control; git-ignore
                doLast {
                    resourcesAndroid(resourceFolder, androidResFolder, outKt, ext)
                }
            }
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

        tasks.create("kiteuiLocalize").apply {
            val task = this
            group = "kiteui"
            afterEvaluate {
                val commonMain = project.file("src/commonMain/kotlin/${ext.packageName.replace(".", "/")}")
                if (!commonMain.exists()) {
                    println("File $commonMain does not exist.  No localization possible.")
                    return@afterEvaluate
                }
                val toRead = commonMain.listFiles().filter { it.name != "Strings.kt" }
                toRead.forEach {
                    if (it.isDirectory) inputs.dir(it)
                    else inputs.file(it)
                }
                val outKt = commonMain.resolve("Strings.kt")
                outputs.file(outKt)
                doLast {
                    generateLocalizations(toRead, outKt, ext)
                }
            }
        }

        tasks.create("generateAutoRoutes") {
            val task = this
            group = "kiteui"
            val sources = project.file("src/commonMain/kotlin")
            inputs.dir(sources)
            val out = project.file("build/generated/kiteui/autoroutes.kt")
            outputs.file(out)
            doLast {
                generateAutoroutes(sources, out)
            }
            afterEvaluate {
                afterEvaluate {
                    tasks.filter { it.name.contains("compileKotlin") }.forEach { it.dependsOn(task) }
                    tasks.filter {
                        it.name.contains("kspKotlin")
                    }.forEach {
                        it.dependsOn(task)
                    }
                }
            }
        }

        afterEvaluate {


//            sourceSets {
//                val commonMain by getting {
//                    dependencies {
//                        api(project(":library"))
//                    }
//                    kotlin {
//                        srcDir(file("build/generated/ksp/common/commonMain/kotlin"))
//                    }
//                }
        }

        Unit
    }

}
