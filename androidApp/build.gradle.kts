import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.ktlint)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.googleGmsGoogleServices)
    alias(libs.plugins.googleFirebaseCrashlytics)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    constraints {
        implementation(libs.androidx.fragment)
    }

    implementation(project(":shared"))
    implementation(libs.firebase.crashlytics)
    implementation(libs.ktor.okhttp)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

fun keystoreProperties(): Properties? {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) return Properties().apply { file.inputStream().use { load(it) } }
    val store = System.getenv("ROUTY_KEYSTORE_FILE") ?: return null
    return Properties().apply {
        setProperty("storeFile", store)
        setProperty("storePassword", System.getenv("ROUTY_KEYSTORE_PASSWORD").orEmpty())
        setProperty("keyAlias", System.getenv("ROUTY_KEY_ALIAS").orEmpty())
        setProperty("keyPassword", System.getenv("ROUTY_KEY_PASSWORD").orEmpty())
    }
}

val keystore = keystoreProperties()

android {
    namespace = "ge.routy.transport"
    compileSdk =
        libs.versions.android.compileSdk
            .get()
            .toInt()

    defaultConfig {
        applicationId = "ge.routy.transport"
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
        targetSdk =
            libs.versions.android.targetSdk
                .get()
                .toInt()
        versionCode = 3
        versionName = "1.0.0"
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64") }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    signingConfigs {
        keystore?.let { properties ->
            create("release") {
                storeFile = rootProject.file(properties.getProperty("storeFile"))
                storePassword = properties.getProperty("storePassword")
                keyAlias = properties.getProperty("keyAlias")
                keyPassword = properties.getProperty("keyPassword")
            }
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

ktlint {
    filter { exclude { it.file.path.contains("/build/") } }
}
