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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

val okHttpVersion: String = "4.11.0"

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation(project(":library"))
    api(project(":example-app"))
    testImplementation("junit:junit:4.13.2")
    implementation(platform("com.squareup.okhttp3:okhttp-bom:$okHttpVersion"))
    implementation("com.squareup.okhttp3:okhttp")
}