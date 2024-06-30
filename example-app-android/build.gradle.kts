plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
}

android {
    namespace = "com.lightningkite.kiteuiexample.old"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.lightningkite.kiteuiexample.old"
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
    implementation(libs.appCompat)
    implementation(project(":library"))
    api(project(":example-app"))
    testImplementation(libs.kotlinTest)
    implementation(project.dependencies.platform(libs.okhttpBom))
    implementation(libs.okhttp)
}