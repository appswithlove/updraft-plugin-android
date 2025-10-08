import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.updraft)
}

android {
    namespace = "com.appswithlove.sample"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.appswithlove.updraft.sample"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    flavorDimensions += "main"
    productFlavors {
        create("stage") {
            dimension = "main"
        }
        create("prod") {
            dimension = "main"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    tasks.withType<KotlinJvmCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
            freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

kotlin {
    jvmToolchain(11)
}

updraft {
    urls = mapOf(
        "StageDebug" to listOf("https://app.getupdraft.com/api_upload/.../.../"),
        "ProdRelease" to listOf("https://app.getupdraft.com/api_upload/.../.../"),
    )
}

dependencies {
    implementation(libs.junit)
}
