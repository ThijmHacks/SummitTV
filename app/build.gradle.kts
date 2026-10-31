plugins {
    id("com.android.application")
}

android {
    namespace = "com.summit.tv"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.summit.tv"
        minSdk = 23
        targetSdk = 32

        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
        }
    }
}