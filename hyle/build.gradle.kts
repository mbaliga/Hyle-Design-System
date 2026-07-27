// Hyle — the material design system (docs/design/material-language.md). The *semantic*
// (local vs from-elsewhere) lives in each consuming app; this module owns the *render
// side* — tokens + the contract the renderer obeys. The Compose Modifiers and AGSL
// shaders land next and are owner-verified on device, so this first cut is deliberately
// pure data (JVM-tested).
//
// This IS Hyle's own repo (mbaliga/Hyle-Design-System) — the single source of truth for
// `dev.aarso:hyle`. Consumers (Android-IDE-core, …) depend on it via git submodule +
// Gradle `includeBuild`, so the project-level `group`/`version` below are what composite
// builds substitute against. The `0.1.0` coordinate is permanently retired (it shipped
// from three divergent copies before this single-sourcing); this is the first
// single-sourced release, `0.2.0`.
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
val hyleVersion = "0.2.0"

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

    // Hyle now ships the *components*, not just the tokens. Before this, consumers
    // got a token sheet and re-implemented the render layer themselves, which is how
    // 0.1.0 ended up shipping from three divergent copies.
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
