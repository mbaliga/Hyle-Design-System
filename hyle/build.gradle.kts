// Hyle — the material design system (docs/design/material-language.md). The *semantic*
// (local vs from-elsewhere) lives in each consuming app; this module owns the *render
// side* — tokens + the contract the renderer obeys.
//
// This IS Hyle's own repo (mbaliga/Hyle-Design-System) — the single source of truth for
// `dev.aarso:hyle`. Consumers (Android-IDE-core, …) depend on it via git submodule +
// Gradle `includeBuild`, so the project-level `group`/`version` below are what composite
// builds substitute against. The `0.1.0` coordinate is permanently retired (it shipped
// from three divergent copies before this single-sourcing); `0.2.0` was the first
// single-sourced release.
//
// Compose enabled (`cells/`, `theme/`): Hyle ships the render-side *components* (starting
// with the colour picker), not just token data — the whole point of single-sourcing is
// that a consumer never has to re-implement the render layer itself.
//
// `0.2.1`: a faithful-fidelity pass on `HyleColorPicker3D` against the real
// `kit/tactile-kit.html` source — no public API change (still `HyleColorPicker3D(color,
// onColorChange, modifier)`), so this is a patch, not a minor: the slice-plane fills now
// read as gradients instead of one flat averaged swatch, the HCL tab's palette-roll near-
// gray substitution uses LCH-native hue instead of HSV-native hue, and the hue ring's touch
// target now matches the source's 70%/15%-inset region instead of the whole stage.
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    `maven-publish`
}

// Coordinate for the publishable AAR. `./gradlew :hyle:publishToMavenLocal` proves the
// module stands alone as `dev.aarso:hyle`; `includeBuild` consumers match on the
// project-level `group`/`version` set below (not the publication block alone).
val hyleGroup = "dev.aarso"
val hyleArtifact = "hyle"
val hyleVersion = "0.2.1"

// Project coordinate — REQUIRED for Gradle composite-build (`includeBuild`) dependency
// substitution: a consumer's `dev.aarso:hyle:<v>` is replaced by this project only when
// its `group:name` match (`name` = "hyle" from settings `include(":hyle")`).
group = hyleGroup
version = hyleVersion

android {
    namespace = "dev.aarso.hyle"
    compileSdk = 36

    defaultConfig {
        minSdk = 31
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    // Required so maven-publish has a single, named variant to publish.
    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

kotlin {
    jvmToolchain(17)
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = hyleGroup
                artifactId = hyleArtifact
                version = hyleVersion
            }
        }
    }
}

dependencies {
    // `api` rather than `implementation`: these types appear in Hyle's public surface
    // (Modifier, Color, Composable), so consumers must see them transitively.
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.graphics)
    api(libs.androidx.compose.material3)

    testImplementation(libs.junit)
}
