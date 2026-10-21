plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.kidsee.core.data"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:domain"))
    implementation(project(":core:network"))

    implementation(libs.core.ktx)
    implementation(libs.bundles.coroutines)
    implementation(libs.serialization.json)
    implementation(libs.datastore.preferences)
    implementation(libs.timber)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.database)

    testImplementation(libs.bundles.testing)
}
