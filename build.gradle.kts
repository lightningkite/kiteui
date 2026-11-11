import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension

group = "com.lightningkite.kiteui"

allprojects {
    repositories {
        mavenLocal()
        maven("https://lightningkite-maven.s3.us-west-2.amazonaws.com")
        maven("https://jitpack.io")
        google()
        mavenCentral()
    }
}
plugins {

    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.plugin.serialization) apply false
    alias(libs.plugins.kotlin.plugin.compose) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.testing.manual) apply false
    alias(libs.plugins.androidKmpLibrary) apply false
    alias(libs.plugins.vannitechPublishing) apply false
    alias(libs.plugins.dokka) apply false
    `version-catalog`
    `maven-publish`
}
plugins.withType(YarnPlugin::class.java) {
    the<YarnRootExtension>().yarnLockMismatchReport = YarnLockMismatchReport.NONE
    the<YarnRootExtension>().reportNewYarnLock = false
    the<YarnRootExtension>().yarnLockAutoReplace = true
}

// Group and version come from each published module's gradle.properties.  `lk` (in metawork) is the only thing
// that edits the version.
subprojects {
    plugins.withId("com.vanniktech.maven.publish") {
        extensions.configure<MavenPublishBaseExtension> {
            signAllPublications()
            pom {
                name.set(project.name)
                url.set("https://lightningkite-maven.s3.amazonaws.com/${project.group.toString().replace('.', '/')}/${project.name}/${project.version}/docs/index.html")
                scm {
                    connection.set("scm:git:https://github.com/lightningkite/kiteui.git")
                    developerConnection.set("scm:git:https://github.com/lightningkite/kiteui.git")
                    url.set("https://github.com/lightningkite/kiteui")
                }
                licenses {
                    license {
                        name.set("Apache-2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer { id.set("LightningKiteJoseph"); name.set("Joseph Ivie"); email.set("joseph@lightningkite.com") }
                    developer { id.set("bjsvedin"); name.set("Brady Svedin"); email.set("brady@lightningkite.com") }
                    developer { id.set("iHoonter"); name.set("Hunter"); email.set("Hunter@lightningkite.com") }
                }
            }
        }

        extensions.configure<SigningExtension> {
            val signingKey = providers.gradleProperty("signingKey").orNull
            if (signingKey != null) useInMemoryPgpKeys(signingKey, providers.gradleProperty("signingPassword").orNull)
        }
        tasks.withType<Sign>().configureEach {
            val hasKey = providers.gradleProperty("signingKey").isPresent
            onlyIf("a signing key is configured") { hasKey }
        }

        extensions.configure<PublishingExtension> {
            repositories {
                maven { name = "staging"; url = uri(rootProject.layout.buildDirectory.dir("staging-repo")) }
            }
        }
    }

    // configureBasedOnAppliedPlugins picks the javadoc jar from the plugins applied so far, and the modules apply Dokka
    // after vanniktech.  Registered here, before any module script runs, this goes ahead of vanniktech's own
    // afterEvaluate fallback, which would otherwise configure with its defaults.
    afterEvaluate {
        plugins.withId("com.vanniktech.maven.publish") {
            extensions.configure<MavenPublishBaseExtension> {
                // Dokka is slow, so the javadoc jar is built only on request.
                configureBasedOnAppliedPlugins(sourcesJar = true, javadocJar = providers.gradleProperty("withDocs").isPresent)
            }
        }
    }

    plugins.withId("org.jetbrains.dokka") {
        dependencies {
            "dokkaPlugin"("com.lightningkite:dokka-plugin-hide-optin:0.0.4")
        }
        extensions.configure<DokkaExtension> {
            dokkaSourceSets.configureEach {
                sourceRoots.map { it.resolve("module.md") }.firstOrNull { it.exists() }?.let { includes.from(it) }
                sourceLink {
                    localDirectory.set(file("src/$name/kotlin"))
                    remoteUrl.set(uri("https://github.com/lightningkite/kiteui/tree/master/"))
                    remoteLineSuffix.set("#L")
                }
            }
        }
    }

    // `lk` compares a fresh dump with the last release's to choose the next version, so no dumps are committed or checked.
    plugins.withId("org.jetbrains.kotlin.multiplatform") {
        @OptIn(ExperimentalAbiValidation::class)
        extensions.configure<KotlinMultiplatformExtension> { abiValidation() }
    }
    plugins.withId("org.jetbrains.kotlin.jvm") {
        @OptIn(ExperimentalAbiValidation::class)
        extensions.configure<KotlinJvmProjectExtension> { abiValidation() }
    }
    tasks.matching { it.name == "checkKotlinAbi" }.configureEach { enabled = false }
}

// The catalog downstream repos pin: this repo's modules and Gradle plugin at the versions being published.
val published = subprojects.filter { it.file("gradle.properties").let { f -> f.isFile && f.readText().contains(Regex("(?m)^version=")) } }
catalog {
    versionCatalog {
        published.forEach { library(it.name, "${it.group}:${it.name}:${it.version}") }
        plugin("kiteui", "com.lightningkite.kiteui").version(project(":gradle-plugin").version.toString())
    }
}
publishing {
    publications.create<MavenPublication>("catalog") {
        from(components["versionCatalog"])
        groupId = "com.lightningkite.stack"
        artifactId = "kiteui-catalog"
        version = providers.gradleProperty("catalogVersion").getOrElse("unspecified")
    }
    repositories { maven { name = "staging"; url = uri(layout.buildDirectory.dir("staging-repo")) } }
}
