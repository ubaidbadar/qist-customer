plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "pk.qist.device.customer"
    compileSdk = 37

    defaultConfig {
        applicationId = "pk.qist.device.customer"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }

    val signingPath = providers.gradleProperty("qist.signing.storeFile").orNull
    signingConfigs {
        create("ciRelease") {
            if (signingPath != null) {
                storeFile = file(signingPath)
                storePassword = providers.gradleProperty("qist.signing.storePassword").orNull
                keyAlias = providers.gradleProperty("qist.signing.keyAlias").orNull
                keyPassword = providers.gradleProperty("qist.signing.keyPassword").orNull
            }
        }
    }

    buildTypes {
        release {
            if (signingPath != null) {
                signingConfig = signingConfigs.getByName("ciRelease")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    buildFeatures { compose = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.activity.compose)
    implementation(libs.compose.material3)
}
