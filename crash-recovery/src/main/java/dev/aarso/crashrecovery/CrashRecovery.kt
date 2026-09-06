@file:Suppress("unused", "UNUSED_PARAMETER")

package dev.aarso.crashrecovery

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent

/*
 * # Tombstone — `dev.aarso:crash-recovery` has moved
 *
 * The real module now lives in **mbaliga/Shared-Libraries-asoc**. This stub stays behind
 * deliberately, and the way it fails is deliberate too.
 *
 * ## Why a stub rather than deleting the module
 *
 * Consumers resolve this artifact through Gradle composite-build substitution, which matches on
 * `group:name`. Delete the module and that substitution simply stops matching, so Gradle falls
 * through to the remote repositories and reports:
 *
 *     Could not find dev.aarso:crash-recovery:1.1.0
 *
 * which says nothing about where it went. Keeping the coordinate alive means the build still
 * resolves and the failure can carry an actionable message instead.
 *
 * ## Why it fails at COMPILE time and not at configuration time
 *
 * Gradle configures *every* project inside an `includeBuild`, including ones the consumer never
 * depends on. Throwing from this module's `build.gradle.kts` would therefore break apps that use
 * `dev.aarso:hyle` and have never touched crash-recovery. A `DeprecationLevel.ERROR` deprecation
 * fires only where the API is actually referenced, so the blast radius is exactly the set of
 * real users — which is the point.
 *
 * ## Migrating
 *
 * See `crash-recovery/MOVED.md` here, or `MIGRATION.md` in Shared-Libraries-asoc. In short:
 *
 *     // settings.gradle.kts
 *     includeBuild("shared-libraries")
 *
 *     // build.gradle.kts
 *     implementation("dev.aarso:crash-recovery:1.2.0")
 *
 * The package (`dev.aarso.crashrecovery`) and the API are unchanged, so no import or call-site
 * edits are needed. If this repo's submodule existed *only* for crash-recovery — as it did for
 * BOS_launcher, Clackpad and hnm_playground — remove `hyle-design-system` entirely.
 *
 * Note that 1.2.0 is not simply the old 1.1.0: it merges forward the `previewIntent` /
 * `samplePreview` work from the never-merged `33b0faa` branch, which Android-IDE-core pinned and
 * calls. Those two histories had diverged and neither was a superset.
 */

private const val MOVED: String =
    "dev.aarso:crash-recovery has MOVED to mbaliga/Shared-Libraries-asoc. " +
        "Add includeBuild(\"shared-libraries\") to settings.gradle.kts and depend on " +
        "dev.aarso:crash-recovery:1.2.0 from there. The package and API are unchanged, so no " +
        "code edits are needed. If this repo's hyle-design-system submodule existed only for " +
        "crash-recovery, remove it. See crash-recovery/MOVED.md."

private const val TOMBSTONE: String = "Tombstone — the implementation lives in Shared-Libraries-asoc."

/**
 * Moved — see the file header. Every member is [DeprecationLevel.ERROR], so a real consumer
 * fails to compile with a message naming the new home instead of an unresolved-dependency error.
 *
 * Signatures mirror the module as it stood at 1.1.0 so a consumer's *existing* call sites still
 * bind here and produce the migration error, rather than an "unresolved reference" that would be
 * no more useful than the missing artifact.
 */
@Deprecated(MOVED, level = DeprecationLevel.ERROR)
object CrashRecovery {

    @Deprecated(MOVED, level = DeprecationLevel.ERROR)
    fun install(app: Application, appLabel: String): Unit = error(TOMBSTONE)

    @Deprecated(MOVED, level = DeprecationLevel.ERROR)
    fun captureInitError(context: Context, appLabel: String, throwable: Throwable): Unit = error(TOMBSTONE)

    @Deprecated(MOVED, level = DeprecationLevel.ERROR)
    fun consecutiveCount(context: Context): Int = error(TOMBSTONE)

    @Deprecated(MOVED, level = DeprecationLevel.ERROR)
    fun clearStreak(context: Context): Unit = error(TOMBSTONE)

    @Deprecated(MOVED, level = DeprecationLevel.ERROR)
    fun pending(context: Context): Nothing = error(TOMBSTONE)

    @Deprecated(MOVED, level = DeprecationLevel.ERROR)
    fun clear(context: Context): Unit = error(TOMBSTONE)

    @Deprecated(MOVED, level = DeprecationLevel.ERROR)
    fun maybeShowRecovery(
        activity: Activity,
        appLabel: String,
        contactEmail: String? = null,
    ): Boolean = error(TOMBSTONE)

    /**
     * Kept even though it never existed on this repo's `main`: Android-IDE-core pinned the
     * unmerged `33b0faa` and calls it, so without this stub that repo would get an "unresolved
     * reference" rather than the migration message it actually needs.
     */
    @Deprecated(MOVED, level = DeprecationLevel.ERROR)
    fun previewIntent(context: Context, appLabel: String): Intent = error(TOMBSTONE)
}
