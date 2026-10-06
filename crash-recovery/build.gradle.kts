// :crash-recovery — TOMBSTONE.
//
// The real module moved to mbaliga/Shared-Libraries-asoc (see MOVED.md). What remains here is a
// stub that keeps the `dev.aarso:crash-recovery` coordinate resolvable, so consumers get an
// actionable compile error naming the new home instead of Gradle's uninformative
// "Could not find dev.aarso:crash-recovery".
//
// This module deliberately still builds and still declares its coordinate. It must NOT fail at
// configuration time: Gradle configures every project in an `includeBuild`, so a configuration
// failure here would break consumers that only use `dev.aarso:hyle` and never touched
// crash-recovery. The failure is a DeprecationLevel.ERROR in the Kotlin source, which fires only
// at real call sites.
//
// Delete this module once every consumer has migrated. Known holdouts at time of writing: the
// three unmerged adoption PRs (BOS_launcher#3, Clackpad#5, hnm_playground#3), which pin this
// repo at 4bd8746 and would otherwise wire up a submodule whose crash-recovery is this stub.
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    `maven-publish`
}

val crashRecoveryGroup = "dev.aarso"
val crashRecoveryArtifact = "crash-recovery"
val crashRecoveryVersion = "1.1.0"

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

// No dependencies: the tombstone has no implementation and no tests.
