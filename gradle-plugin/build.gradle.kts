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

tasks.create("publishLocally", Copy::class.java) {
    from(file("src/main/kotlin/KiteUiPlugin.kt"))
    into(rootProject.file("buildSrc/src/main/kotlin"))
}

lkLibrary("lightningkite", "kiteui") {
    description.set("Automatically create your routers")
}