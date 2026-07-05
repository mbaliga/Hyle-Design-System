// :crash-recovery — a shared reliability utility, deliberately NOT part of `:hyle`. It has
// zero dependency on the `:hyle` design-tokens module (no Compose, no Material, plain
// `android.widget` views only) so apps with their own visual identity that must never
// depend on Hyle (Animalcules, Horizkeeb — Personal-Tracker DECISIONS.md D-L) can still
// take this one dependency. See D-O for why this lives in this repo as a separate artifact
// rather than inside `:hyle` or duplicated per-app.
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    `maven-publish`
}

val crashRecoveryGroup = "dev.aarso"
val crashRecoveryArtifact = "crash-recovery"
val crashRecoveryVersion = "1.0.0"

// Project coordinate — required for Gradle composite-build (`includeBuild`) dependency
// substitution, same mechanism as `:hyle` (see that module's build.gradle.kts).
group = crashRecoveryGroup
version = crashRecoveryVersion

android {
    namespace = "dev.aarso.crashrecovery"
    compileSdk = 36

    defaultConfig {
        // The lowest minSdk among current consumers (Animalcules) — a library's minSdk only
        // needs to be <= the lowest consumer's, never forces anyone's minSdk up.
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

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
                groupId = crashRecoveryGroup
                artifactId = crashRecoveryArtifact
                version = crashRecoveryVersion
            }
        }
    }
}

dependencies {
    testImplementation(libs.junit)
}
