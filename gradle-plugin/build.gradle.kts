import com.lightningkite.deployhelpers.developer
import com.lightningkite.deployhelpers.github
import com.lightningkite.deployhelpers.mit
import com.lightningkite.deployhelpers.standardPublishing

plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    signing
    `maven-publish`
    alias(libs.plugins.dokka)
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
    implementation(libs.fontBox)
}
tasks.validatePlugins {
    enableStricterValidation.set(true)
}

standardPublishing {
    name.set("KiteUI-Gradle-Plugin")
    description.set("Automatically create your routers")
    github("lightningkite", "kiteui")

    licenses {
        mit()
    }

    developers {
        developer(
            id = "LightningKiteJoseph",
            name = "Joseph Ivie",
            email = "joseph@lightningkite.com",
        )
        developer(
            id = "bjsvedin",
            name = "Brady Svedin",
            email = "brady@lightningkite.com",
        )
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