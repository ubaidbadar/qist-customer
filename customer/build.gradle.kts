import java.io.File
import java.util.Base64

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

/** Parses .env files into key-value pairs (workspace root, project root, and customer module). */
fun loadDotEnv(): Map<String, String> {
    val env = mutableMapOf<String, String>()
    val candidateFiles = listOf(
        rootProject.file("../.env"),
        rootProject.file(".env"),
        project.file(".env"),
        rootProject.file(".env.local"),
        project.file(".env.local")
    )
    for (candidate in candidateFiles) {
        if (candidate.isFile) {
            candidate.forEachLine { rawLine ->
                val line = rawLine.trim()
                if (line.isNotEmpty() && !line.startsWith("#") && line.contains("=")) {
                    val parts = line.split("=", limit = 2)
                    val key = parts[0].trim()
                    var value = parts[1].trim()
                    if ((value.startsWith("\"") && value.endsWith("\"")) ||
                        (value.startsWith("'") && value.endsWith("'"))
                    ) {
                        value = value.substring(1, value.length - 1)
                    }
                    env[key] = value
                }
            }
        }
    }
    return env
}

val dotEnv = loadDotEnv()

/** Looks up value across System.getenv, .env, and Gradle properties. */
fun resolveEnv(envKey: String, gradlePropName: String? = null): String? = System.getenv(envKey)
    ?: dotEnv[envKey]
    ?: (gradlePropName?.let { providers.gradleProperty(it).orNull })
    ?: providers.gradleProperty(envKey).orNull

android {
    namespace = "pk.qist.customer"
    compileSdk = 37

    defaultConfig {
        applicationId = "pk.qist.customer"
        minSdk = 24
        targetSdk = 37
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        val defaultVersionCode = listOf(
            rootProject.file("../build-customer.txt"),
            rootProject.file("build-customer.txt"),
            project.file("build-customer.txt")
        ).find { it.isFile }?.readText()?.trim()?.toIntOrNull() ?: 1

        versionCode = providers.gradleProperty("qist.customer.versionCode")
            .map { it.toInt() }
            .orElse(
                providers.provider {
                    resolveEnv("VERSION_CODE")?.toIntOrNull() ?: defaultVersionCode
                }
            )
            .get()
        versionName = "0.2.1"
        val customerApiUrl = resolveEnv("CUSTOMER_API_URL", "qist.customer.apiUrl")
            ?: resolveEnv("API_URL")?.trimEnd('/')?.let { "$it/customer-app/" }
            ?: "https://qist.restora.live/api/v1/customer-app/"
        buildConfigField("String", "CUSTOMER_API_URL", "\"$customerApiUrl\"")
    }

    signingConfigs {
        create("ciRelease") {
            val keystoreBase64 = resolveEnv("ANDROID_KEYSTORE_BASE64")
            val explicitStoreFile = resolveEnv("ANDROID_KEYSTORE_FILE", "qist.signing.storeFile")

            if (!keystoreBase64.isNullOrBlank()) {
                val decodedKeystore = layout.buildDirectory.file("keystore/release.jks").get().asFile
                if (!decodedKeystore.exists() || decodedKeystore.length() == 0L) {
                    decodedKeystore.parentFile.mkdirs()
                    decodedKeystore.writeBytes(Base64.getDecoder().decode(keystoreBase64.trim()))
                }
                storeFile = decodedKeystore
            } else if (!explicitStoreFile.isNullOrBlank()) {
                storeFile = file(explicitStoreFile)
            } else {
                val candidateKeystores = listOf(
                    rootProject.file("../qist-release.jks"),
                    rootProject.file("qist-release.jks"),
                    project.file("qist-release.jks")
                )
                val found = candidateKeystores.find { it.isFile }
                if (found != null) {
                    storeFile = found
                }
            }

            storePassword = resolveEnv("ANDROID_KEYSTORE_PASSWORD", "qist.signing.storePassword")
            keyAlias = resolveEnv("ANDROID_KEY_ALIAS", "qist.signing.keyAlias")
            keyPassword = resolveEnv("ANDROID_KEY_PASSWORD", "qist.signing.keyPassword")
        }
    }

    buildTypes {
        release {
            val ciRelease = signingConfigs.getByName("ciRelease")
            if (ciRelease.storeFile?.isFile == true && !ciRelease.storePassword.isNullOrBlank()) {
                signingConfig = ciRelease
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            it.jvmArgs("--add-opens=java.base/java.io=ALL-UNNAMED", "--add-exports=java.base/jdk.internal.access=ALL-UNNAMED")
        }
    }

    buildFeatures {
        aidl = true
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.activity.compose)
    implementation(libs.compose.material3)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.firebase.messaging)
    implementation(libs.play.app.update)
    implementation(libs.play.auth)
    implementation(libs.coroutines.android)
    implementation(libs.okhttp)
    implementation(libs.serialization.json)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.compose.ui.test.manifest)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
}
