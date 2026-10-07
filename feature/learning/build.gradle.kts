import java.util.Properties

plugins { alias(libs.plugins.android.library); alias(libs.plugins.kotlin.android); alias(libs.plugins.kotlin.compose); alias(libs.plugins.hilt); alias(libs.plugins.ksp); alias(libs.plugins.kotlin.serialization) }

val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        load(file.inputStream())
    }
}
val youtubeApiKey = localProps.getProperty("youtube.apiKey", "")

android {
    namespace = "com.schoolos.android.feature.learning"
    compileSdk = 36
    buildFeatures {
        buildConfig = true
    }
    defaultConfig {
        minSdk = 26
        buildConfigField("String", "YOUTUBE_API_KEY", "\"$youtubeApiKey\"")
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation(project(":core")); implementation(project(":domain"))
    implementation(platform(libs.compose.bom)); implementation(libs.compose.material3); implementation(libs.compose.material.icons.extended); implementation(libs.compose.ui.tooling.preview)
    implementation(libs.activity.compose); implementation(libs.lifecycle.runtime.compose); implementation(libs.lifecycle.viewmodel.compose); implementation(libs.navigation.compose)
    implementation(libs.hilt.android); ksp(libs.hilt.compiler); implementation(libs.hilt.navigation.compose); implementation(libs.coil.compose); implementation(libs.timber)
    implementation(libs.kotlinx.serialization.json)
}
