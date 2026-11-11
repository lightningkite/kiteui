plugins {
    `java-gradle-plugin`
    kotlin("jvm")
    alias(libs.plugins.kotlin.plugin.samWithReceiver)
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

// Claude: kotlin-dsl would do this for us; Gradle's Action lambdas use `this` as the receiver.
samWithReceiver {
    annotation("org.gradle.api.HasImplicitReceiver")
}
repositories {
    mavenCentral()
}
dependencies {
    implementation(libs.kotlin.gradle.plugin.api)
    compileOnly(gradleKotlinDsl())
    implementation(libs.fontbox)
    testImplementation(libs.junit)
}
tasks.validatePlugins {
    enableStricterValidation.set(true)
}
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2)
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2)
    }
}

tasks.register("publishLocally", Copy::class.java) {
    from(file("src/main/kotlin/KiteUiPlugin.kt"))
    into(rootProject.file("buildSrc/src/main/kotlin"))
}

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    pom { description.set("Automatically create your routers") }
}