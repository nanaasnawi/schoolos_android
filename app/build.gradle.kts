import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val rawDevUrl: String = localProps.getProperty("dev.serverUrl", "https://schoolosbackend-production.up.railway.app/api/v1/") ?: "https://schoolosbackend-production.up.railway.app/api/v1/"
val devServerUrl: String = if (!rawDevUrl.trimEnd('/').endsWith("/api/v1")) "${rawDevUrl.trimEnd('/')}/api/v1/" else "${rawDevUrl.trimEnd('/')}/"
val rawProdUrl: String = localProps.getProperty("prod.serverUrl", "https://schoolosbackend-production.up.railway.app/api/v1/") ?: "https://schoolosbackend-production.up.railway.app/api/v1/"
val prodServerUrl: String = if (!rawProdUrl.trimEnd('/').endsWith("/api/v1")) "${rawProdUrl.trimEnd('/')}/api/v1/" else "${rawProdUrl.trimEnd('/')}/"

android {
    namespace = "com.schoolos.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.schoolos.android"
        minSdk = 26
        targetSdk = 36
        versionCode = 29
        versionName = "1.2.8"
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"$devServerUrl\"")
        }
        release {
            buildConfigField("String", "API_BASE_URL", "\"$prodServerUrl\"")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            ndk {
                debugSymbolLevel = "FULL"
            }

            val keystoreProp = (localProps.getProperty("release.keystoreFile") ?: System.getenv("RELEASE_KEYSTORE_FILE"))?.replace("\\:", ":")
            val resolvedKeystore = when {
                !keystoreProp.isNullOrEmpty() && file(keystoreProp).exists() -> file(keystoreProp)
                !keystoreProp.isNullOrEmpty() && rootProject.file(keystoreProp).exists() -> rootProject.file(keystoreProp)
                rootProject.file("keystore/schoolos-upload.jks").exists() -> rootProject.file("keystore/schoolos-upload.jks")
                else -> null
            }
            val resolvedStorePassword = localProps.getProperty("release.storePassword") ?: System.getenv("RELEASE_STORE_PASSWORD")
            val resolvedKeyAlias = localProps.getProperty("release.keyAlias") ?: System.getenv("RELEASE_KEY_ALIAS")
            val resolvedKeyPassword = localProps.getProperty("release.keyPassword") ?: System.getenv("RELEASE_KEY_PASSWORD")

            if (resolvedKeystore != null && resolvedKeystore.exists() &&
                !resolvedStorePassword.isNullOrBlank() &&
                !resolvedKeyAlias.isNullOrBlank() &&
                !resolvedKeyPassword.isNullOrBlank()
            ) {
                val releaseSigning = signingConfigs.create("release") {
                    storeFile = resolvedKeystore
                    storePassword = resolvedStorePassword
                    keyAlias = resolvedKeyAlias
                    keyPassword = resolvedKeyPassword
                }
                signingConfig = releaseSigning
                logger.lifecycle(">>> SIGNING RELEASE WITH KEYSTORE: ${resolvedKeystore.absolutePath}")
            } else {
                logger.warn(">>> WARNING: RELEASE KEYSTORE OR CREDENTIALS NOT CONFIGURED! FALLING BACK TO DEBUG SIGNING")
                signingConfig = signingConfigs.getByName("debug")
            }
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
}

dependencies {
    implementation(project(":core"))
    implementation(project(":domain"))
    implementation(project(":data"))

    implementation(project(":feature:auth"))
    implementation(project(":feature:home"))
    implementation(project(":feature:learning"))
    implementation(project(":feature:assignments"))
    implementation(project(":feature:quizzes"))
    implementation(project(":feature:sessions"))
    implementation(project(":feature:grades"))
    implementation(project(":feature:progress"))
    implementation(project(":feature:achievements"))
    implementation(project(":feature:notifications"))
    implementation(project(":feature:profile"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.navigation.compose)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)
    implementation(libs.core.ktx)
    implementation(libs.timber)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.play.app.update)
    implementation(libs.play.app.update.ktx)

    debugImplementation(libs.compose.ui.tooling)
}

val packageReleaseNativeDebugSymbols = tasks.register<Zip>("packageReleaseNativeDebugSymbols") {
    description = "Packages native debug symbols into a zip file for Google Play Console"
    dependsOn("mergeReleaseNativeLibs")
    from(layout.buildDirectory.dir("intermediates/merged_native_libs/release/mergeReleaseNativeLibs/out/lib"))
    archiveFileName.set("native-debug-symbols.zip")
    destinationDirectory.set(layout.buildDirectory.dir("outputs/native-debug-symbols/release"))
}

tasks.matching { it.name == "bundleRelease" }.configureEach {
    finalizedBy(packageReleaseNativeDebugSymbols)
}
