plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services) apply false
}

android {
    namespace = "com.kidsee.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kidsee.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 15
        versionName = "1.0.14"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file("${rootDir}/kidsee-release.jks")
            storePassword = "kidseepass"
            keyAlias = "kidsee"
            keyPassword = "kidseepass"
            enableV1Signing = true
            enableV2Signing = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            signingConfig = signingConfigs.getByName("release")
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:network"))
    implementation(project(":core:ui"))

    // AndroidX & Core
    implementation(libs.core.ktx)
    implementation(libs.core.splashscreen)
    implementation(libs.appcompat)
    implementation(libs.activity.compose)
    implementation(libs.bundles.lifecycle)
    implementation(libs.lifecycle.service)
    implementation(libs.navigation.compose)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)

    // TV Compose
    implementation(libs.tv.foundation)
    implementation(libs.tv.material)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.messaging)

    // WebRTC
    implementation(libs.webrtc)

    // Coroutines & Serialization
    implementation(libs.bundles.coroutines)
    implementation(libs.coroutines.play.services)
    implementation(libs.serialization.json)

    // WorkManager & DataStore
    implementation(libs.workmanager)
    implementation(libs.datastore.preferences)

    // QR Code & CameraX (for pairing)
    implementation(libs.zxing.core)
    implementation(libs.bundles.camerax)

    // Coil
    implementation(libs.coil.compose)

    // Timber
    implementation(libs.timber)

    // Testing
    testImplementation(libs.bundles.testing)
    androidTestImplementation(libs.junit.ext)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.bundles.compose.debug)
}

tasks.matching { it.name in listOf("assembleDebug", "assembleRelease") }.configureEach {
    doLast {
        val releaseApk = layout.buildDirectory.file("outputs/apk/release/app-release.apk").get().asFile
        val debugApk = layout.buildDirectory.file("outputs/apk/debug/app-debug.apk").get().asFile
        val src = if (releaseApk.exists()) releaseApk else debugApk

        if (src.exists()) {
            val destKidSee = File(rootDir, "KidSee.apk")
            val destApp = File(rootDir, "app-debug.apk")
            src.copyTo(destKidSee, overwrite = true)
            src.copyTo(destApp, overwrite = true)
            logger.lifecycle("Successfully copied finished APK to: " + destKidSee.absolutePath)
        }
    }
}
