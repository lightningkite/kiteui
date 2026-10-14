plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.kotlin.plugin.compose)
}

// :library-lottie for the Compose backend; see library-compose/build.gradle.kts for why it's a separate module.
val lottie = project(":library-lottie").projectDir

kotlin {
    jvm()

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        optIn.add("kotlin.time.ExperimentalTime")
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
    }

    sourceSets {
        getByName("commonMain") {
            kotlin.srcDir("$lottie/src/commonMain/kotlin")
            dependencies {
                api(project(":library-compose"))
            }
        }
    }
}
