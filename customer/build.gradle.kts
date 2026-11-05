plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "pk.qist.customer"
    compileSdk = 37

    defaultConfig {
        applicationId = "pk.qist.customer"
        minSdk = 24
        targetSdk = 37
        versionCode = providers.gradleProperty("qist.customer.versionCode")
            .map { it.toInt() }
            .getOrElse(1)
        versionName = "0.2.1"
        buildConfigField("String", "CUSTOMER_API_URL", "\"https://qist.restora.live/api/v1/customer-app/\"")
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

    buildFeatures { compose = true; buildConfig = true }
    testOptions { unitTests.isIncludeAndroidResources = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.activity.compose)
    implementation(libs.compose.material3)
    implementation(libs.okhttp)
    implementation(libs.coroutines.android)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.okhttp.mockwebserver)
}
