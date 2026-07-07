import com.lightningkite.deployhelpers.*

plugins {
    alias(libs.plugins.kotlin.jvm)
    signing
    alias(libs.plugins.vannitechPublishing)
    alias(libs.plugins.dokka)
}

// Override the allprojects group so the published coordinate is com.lightningkite:kiteui-build
// (not com.lightningkite.kiteui:kiteui-build).  The artifactId comes from project.name ("kiteui-build")
// which is set via settings.gradle.kts — the physical directory stays "build-companion".
group = "com.lightningkite"

kotlin {
    jvmToolchain(17)
    compilerOptions {
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1)
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(libs.junit)
}

lkLibrary("lightningkite", "kiteui") {
    description.set("KiteUI build-time code generation — autoroute generator.")
}
