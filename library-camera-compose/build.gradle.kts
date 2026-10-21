plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.kotlin.plugin.compose)
}

// :library-camera for the Compose backend; see library-compose/build.gradle.kts for why it's a separate module.
val camera = project(":library-camera").projectDir

kotlin {
    jvm()

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        optIn.add("kotlin.time.ExperimentalTime")
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
    }

    sourceSets {
        getByName("commonMain") {
            kotlin.srcDir("$camera/src/commonMain/kotlin")
            dependencies {
                api(project(":library-compose"))
            }
        }
    }
}
