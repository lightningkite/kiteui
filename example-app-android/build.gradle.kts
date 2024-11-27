import java.util.*

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.lightningkite.kiteuiexample"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.lightningkite.kiteuiexample"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        // Flag to enable support for the new language APIs
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    dependencies {
        coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.3")
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    val props = project.rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { stream ->
        Properties().apply { load(stream) }
    }
    if (props != null && props.getProperty("appSigningKeystore") != null) {
        signingConfigs {
            this.create("release") {
                storeFile = project.rootProject.file(props.getProperty("appSigningKeystore"))
                storePassword = props.getProperty("appSigningPassword")
                keyAlias = props.getProperty("appSigningAlias")
                keyPassword = props.getProperty("appSigningAliasPassword")
            }
            this.getByName("debug") {
                storeFile = project.rootProject.file(props.getProperty("appSigningKeystore"))
                storePassword = props.getProperty("appSigningPassword")
                keyAlias = props.getProperty("appSigningAlias")
                keyPassword = props.getProperty("appSigningAliasPassword")
            }
        }
        buildTypes {
            this.getByName("release") {
                this.isMinifyEnabled = false
                this.proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
                this.signingConfig = signingConfigs.getByName("release")
            }
        }
    }
}

val okHttpVersion: String = "4.11.0"

dependencies {
    implementation(project(":library"))
    api(project(":example-app"))
    testImplementation("junit:junit:4.13.2")
}