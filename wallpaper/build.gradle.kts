// :wallpaper — "Hyle Worlds", a Brutalist live wallpaper. It runs the Form-World
// SDF raymarcher (the design system's procedural worlds) as an OpenGL ES 2.0
// WallpaperService, themed from the shared Hyle tokens (:hyle).
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "dev.aarso.hyle.worlds"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.aarso.hyle.worlds"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures { compose = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(project(":hyle"))
    implementation(libs.androidx.core.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
}
