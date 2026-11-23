import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.devtools.ksp)
}

val waFlutterUiEnabled = providers.gradleProperty("waFlutterUi").orNull == "true"
// A separate install-only QA package. Never change production's app identity
// or accidentally overwrite a user's existing Room database.
val waInstallableTest = providers.gradleProperty("waInstallableTest").orNull == "true"
require(!waInstallableTest || waFlutterUiEnabled) {
    "waInstallableTest requires -PwaFlutterUi=true"
}

android {
    namespace = "com.example"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.waleadsaver"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        buildConfigField("boolean", "WA_FLUTTER_BRIDGE_ENABLED", waFlutterUiEnabled.toString())
        manifestPlaceholders["waFlutterEnabled"] = waFlutterUiEnabled.toString()
        manifestPlaceholders["waQaAppLabel"] = if (waInstallableTest) {
            "WA Lead Saver - QA"
        } else {
            "@string/app_name"
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val envFile = rootProject.file(".env")
        val envProps = Properties()
        if (envFile.exists()) {
            FileInputStream(envFile).use { envProps.load(it) }
        }
        val localPropsFile = rootProject.file("local.properties")
        val localProps = Properties()
        if (localPropsFile.exists()) {
            FileInputStream(localPropsFile).use { localProps.load(it) }
        }
        val envSupabaseUrl = (project.findProperty("SUPABASE_URL") as? String)
            ?: System.getenv("SUPABASE_URL")
            ?: localProps.getProperty("SUPABASE_URL")
            ?: envProps.getProperty("SUPABASE_URL")
            ?: ""
        val envSupabaseAnonKey = (project.findProperty("SUPABASE_ANON_KEY") as? String)
            ?: System.getenv("SUPABASE_ANON_KEY")
            ?: localProps.getProperty("SUPABASE_ANON_KEY")
            ?: envProps.getProperty("SUPABASE_ANON_KEY")
            ?: ""
        val trimmedUrl = envSupabaseUrl.trim().removeSuffix("/")
        val trimmedKey = envSupabaseAnonKey.trim()
        buildConfigField("String", "SUPABASE_URL", "\"$trimmedUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$trimmedKey\"")
    }

    signingConfigs {
        create("release") {
            val keystorePropertiesFile = rootProject.file("key.properties")
            val keystoreProperties = Properties()
            if (keystorePropertiesFile.exists()) {
                FileInputStream(keystorePropertiesFile).use { keystoreProperties.load(it) }
            }
            val localPropsFile = rootProject.file("local.properties")
            val localProps = Properties()
            if (localPropsFile.exists()) {
                FileInputStream(localPropsFile).use { localProps.load(it) }
            }
            val storeFilePath = System.getenv("RELEASE_STORE_FILE")
                ?: (project.findProperty("RELEASE_STORE_FILE") as? String)
                ?: localProps.getProperty("RELEASE_STORE_FILE")
                ?: localProps.getProperty("storeFile")
                ?: keystoreProperties.getProperty("storeFile")
            val storePass = System.getenv("RELEASE_STORE_PASSWORD")
                ?: (project.findProperty("RELEASE_STORE_PASSWORD") as? String)
                ?: localProps.getProperty("RELEASE_STORE_PASSWORD")
                ?: localProps.getProperty("storePassword")
                ?: keystoreProperties.getProperty("storePassword")
            val keyAl = System.getenv("RELEASE_KEY_ALIAS")
                ?: (project.findProperty("RELEASE_KEY_ALIAS") as? String)
                ?: localProps.getProperty("RELEASE_KEY_ALIAS")
                ?: localProps.getProperty("keyAlias")
                ?: keystoreProperties.getProperty("keyAlias")
            val keyPass = System.getenv("RELEASE_KEY_PASSWORD")
                ?: (project.findProperty("RELEASE_KEY_PASSWORD") as? String)
                ?: localProps.getProperty("RELEASE_KEY_PASSWORD")
                ?: localProps.getProperty("keyPassword")
                ?: keystoreProperties.getProperty("keyPassword")

            if (!storeFilePath.isNullOrBlank() && file(storeFilePath).exists()) {
                storeFile = file(storeFilePath)
                storePassword = storePass
                keyAlias = keyAl
                keyPassword = keyPass
            }
        }
    }

    buildTypes {
        debug {
            if (waInstallableTest) {
                applicationIdSuffix = ".hybridqa"
                versionNameSuffix = "-hybrid-qa"
            }
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val releaseSigning = signingConfigs.getByName("release")
            if (releaseSigning.storeFile != null) {
                signingConfig = releaseSigning
            }
        }
    }
    sourceSets {
        getByName("main") {
            if (waFlutterUiEnabled) java.srcDir("src/flutterBridge/java")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    if (waFlutterUiEnabled) implementation(project(":flutter"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.datastore.preferences)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // Image loading & permissions
    implementation(libs.coil.compose)
    implementation(libs.accompanist.permissions)

    // Networking for Supabase Client
    implementation(libs.okhttp)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.core)
    testImplementation(libs.kotlinx.coroutines.test)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
