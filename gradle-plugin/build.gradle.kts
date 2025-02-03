import com.lightningkite.deployhelpers.*
import com.vanniktech.maven.publish.SonatypeHost

plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    signing
    id("com.vanniktech.maven.publish") version "0.30.0"
    id("org.jetbrains.dokka")
}

gradlePlugin {
    plugins {
        create("lightningkite-kiteui") {
            id = "com.lightningkite.kiteui"
            implementationClass = "com.lightningkite.kiteui.KiteUiPlugin"
        }
    }
}

repositories {
    mavenCentral()
}
dependencies {
    implementation("org.apache.pdfbox:fontbox:2.0.27")
    testImplementation("junit:junit:4.13.2")
}
tasks.validatePlugins {
    enableStricterValidation.set(true)
}

val lk = project.lk {
    version = gitBasedVersion().also { println("Determined version to be $it") }
}
mavenPublishing {
    // publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()
    coordinates(group.toString(), name, version.toString())
    pom {
        name.set("KiteUI-Gradle-Plugin")
        description.set("Automatically create your routers")
        github("lightningkite", "kiteui")

        licenses {
            mit()
        }

        developers {
            joseph()
            brady()
        }
    }
}

tasks.create("publishLocally", Copy::class.java) {
    from(file("src/main/kotlin/KiteUiPlugin.kt"))
    into(rootProject.file("buildSrc/src/main/kotlin"))
}

afterEvaluate {
    tasks.findByName("signPluginMavenPublication")?.let { signingTask ->
        tasks.filter { it.name.startsWith("publish") && it.name.contains("PluginMarkerMavenPublication") }.forEach {
            it.dependsOn(signingTask)
        }
    }
    tasks.findByName("signLightningkite-kiteuiPluginMarkerMavenPublication")?.let { signingTask ->
        tasks.findByName("publishPluginMavenPublicationToMavenLocal")?.dependsOn(signingTask)
        tasks.findByName("publishPluginMavenPublicationToSonatypeRepository")?.dependsOn(signingTask)
    }
}