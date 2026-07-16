pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

// Hyle — the render-side design system (tokens + the contract the renderer obeys).
// Publishable as dev.aarso:hyle:0.2.0; :hyle-probe is the on-device render harness.
// Hyle Worlds (the live-wallpaper app built on this design system) lives in the
// mbaliga/portfolio repo, not here — it consumes :hyle via a composite build.
rootProject.name = "Hyle"
include(":hyle")
include(":hyle-probe")
include(":crash-recovery")
