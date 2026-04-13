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
    implementation(libs.kotlin.gradle.plugin.api)
    implementation(libs.fontbox)
    testImplementation(libs.junit)
}
tasks.validatePlugins {
    enableStricterValidation.set(true)
}
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1)
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1)
    }
}

tasks.create("publishLocally", Copy::class.java) {
    from(file("src/main/kotlin/KiteUiPlugin.kt"))
    into(rootProject.file("buildSrc/src/main/kotlin"))
}

lkLibrary("lightningkite", "kiteui") {
    description.set("Automatically create your routers")
}