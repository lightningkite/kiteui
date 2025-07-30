import com.lightningkite.deployhelpers.*

plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    signing
    alias(libs.plugins.vannitechPublishing)
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
useGitBasedVersion()
useLocalDependencies()
publishing()
setupDokka("lightningkite", "kiteui")

repositories {
    mavenCentral()
}
dependencies {
    implementation(libs.kotlinGradlePluginApi)
    implementation(libs.fontbox)
    testImplementation(libs.junit)
}
tasks.validatePlugins {
    enableStricterValidation.set(true)
}

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()
    coordinates(group.toString(), name, version.toString())
    pom {
        name.set("KiteUI-Gradle-Plugin")
        description.set("Automatically create your routers")
        github("lightningkite", "kiteui")
        url.set(dokkaPublicHostingIndex)

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