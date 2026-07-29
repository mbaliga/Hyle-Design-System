package dev.aarso.crashrecovery

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import java.io.File

/**
 * The shared crash-recovery utility: **install** an uncaught-exception handler that
 * captures a device-only launch/runtime crash to a file (CI never sees these — CI runs
 * unit tests, never launches the app), then **recover** on the next launch by showing
 * [CrashRecoveryActivity] instead of the app's real content.
 *
 * Every operation is `runCatching`-guarded so the handler can never itself crash, and
 * nothing here is ever sent anywhere — the report lives in the app's private files dir
 * until the user explicitly shares or copies it from the recovery screen.
 *
 * Usage — call once from [Application.onCreate], before constructing anything that could
 * itself throw:
 * ```
 * CrashRecovery.install(this, appLabel = "Runout")
 * ```
 * then, first thing in the launcher `Activity.onCreate`:
 * ```
 * if (CrashRecovery.maybeShowRecovery(this, appLabel = "Runout")) return
 * ```
 */
object CrashRecovery {
    private const val FILE_NAME = "crash_recovery_report.txt"

    /** Installs the handler. Chains to any previously-installed handler so this composes. */
    fun install(app: Application, appLabel: String) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { capture(app, appLabel, throwable, thread.name) }
            previous?.uncaughtException(thread, throwable)
        }
    }

    /**
     * For a failure that happens synchronously during your own init (e.g. a DI container
     * that throws in its constructor) — call this from a `catch` block instead of letting
     * it propagate, so the recovery screen has a trace even though nothing crashed the
     * process outright.
     */
    fun captureInitError(context: Context, appLabel: String, throwable: Throwable) {
        runCatching { capture(context, appLabel, throwable, Thread.currentThread().name) }
    }

    private fun capture(context: Context, appLabel: String, throwable: Throwable, threadName: String) {
        val report = CrashReport.of(
            appLabel = appLabel,
            whenMillis = System.currentTimeMillis(),
            threadName = threadName,
            throwable = throwable,
            device = deviceInfo(context),
        )
        file(context).writeText(report.encode())
        android.util.Log.e("CrashRecovery", "captured crash for $appLabel", throwable)
    }

    @Suppress("DEPRECATION")
    private fun legacyVersionCode(info: android.content.pm.PackageInfo): Long = info.versionCode.toLong()

    private fun deviceInfo(context: Context): CrashReport.DeviceInfo = runCatching {
        val app = context.applicationContext
        val pm = app.packageManager
        val pkg = app.packageName
        val info = pm.getPackageInfo(pkg, 0)
        val versionCode = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else legacyVersionCode(info)
        val (freeMb, totalMb) = memoryMb(app)
        CrashReport.DeviceInfo(
            appVersionName = info.versionName,
            appVersionCode = versionCode,
            osSdkInt = Build.VERSION.SDK_INT,
            deviceManufacturer = Build.MANUFACTURER ?: "?",
            deviceModel = Build.MODEL ?: "?",
            packageName = pkg,
            installSource = installSource(app, pkg),
            freeMemMb = freeMb,
            totalMemMb = totalMb,
        )
    }.getOrDefault(CrashReport.DeviceInfo(null, null, Build.VERSION.SDK_INT, "?", "?"))

    /** Free / total device RAM in MB at capture time — the metadata an OOM report lives or dies by. */
    private fun memoryMb(context: Context): Pair<Long?, Long?> = runCatching {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val mi = android.app.ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        (mi.availMem / (1024 * 1024)) to (mi.totalMem / (1024 * 1024))
    }.getOrDefault(null to null)

    /** A human-readable install origin ("Play Store", "Sideloaded", …), never an identifier. */
    @Suppress("DEPRECATION")
    private fun installSource(context: Context, pkg: String): String? = runCatching {
        val installer = if (Build.VERSION.SDK_INT >= 30) {
            context.packageManager.getInstallSourceInfo(pkg).installingPackageName
        } else {
            context.packageManager.getInstallerPackageName(pkg)
        }
        when (installer) {
            null -> "Sideloaded"
            "com.android.vending" -> "Play Store"
            "com.amazon.venezia" -> "Amazon Appstore"
            "com.sec.android.app.samsungapps" -> "Galaxy Store"
            "org.fdroid.fdroid" -> "F-Droid"
            "com.google.android.packageinstaller",
            "com.android.packageinstaller" -> "Sideloaded"
            else -> installer
        }
    }.getOrNull()

    /** Non-null if a crash was captured and not yet cleared. */
    fun pending(context: Context): CrashReport.Decoded? = runCatching {
        file(context).takeIf { it.exists() }?.readText()?.let(CrashReport::decode)
    }.getOrNull()

    fun clear(context: Context) {
        runCatching { file(context).delete() }
    }

    /**
     * Call first thing in your launcher Activity's `onCreate`. If a crash is pending, this
     * starts [CrashRecoveryActivity] and **finishes the calling activity** — it was the one
     * that (or whose process) crashed last time, so it's left in a half-initialized state
     * (no `setContent`/`setContentView` called); finishing it means "Continue" on the
     * recovery screen relaunches a clean instance instead of returning to a blank one.
     * Returns `true` when recovery was shown (the caller should `return` immediately without
     * building its real UI), `false` when there's nothing to recover from.
     */
    fun maybeShowRecovery(
        activity: Activity,
        appLabel: String,
        style: CrashRecoveryStyle = CrashRecoveryStyle.Default,
        contactEmail: String? = null,
    ): Boolean {
        if (pending(activity) == null) return false
        activity.startActivity(CrashRecoveryActivity.intent(activity, appLabel, style, contactEmail))
        activity.finish()
        return true
    }

    private fun file(context: Context): File = File(context.applicationContext.filesDir, FILE_NAME)
}
