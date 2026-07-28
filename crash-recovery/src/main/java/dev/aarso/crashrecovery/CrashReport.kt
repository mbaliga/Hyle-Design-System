package dev.aarso.crashrecovery

import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A device-only crash, captured once and reread on the next launch. This module is a
 * reliability utility, not a design-system dependency — it has no dependency on `:hyle`
 * and imposes no visual language, so apps with their own visual identity (that must
 * never depend on Hyle) can still take this one dependency (see Personal-Tracker
 * DECISIONS.md D-O).
 *
 * [headline] is the one line worth reading first (`ExceptionType: message`); [device]
 * is the metadata a maintainer actually needs to reproduce it; [trace] is the full
 * stack, kept separate so a UI can hide it behind a "technical details" toggle instead
 * of opening on a wall of text.
 */
data class CrashReport(
    val appLabel: String,
    val whenMillis: Long,
    val threadName: String,
    val headline: String,
    val device: DeviceInfo,
    val trace: String,
) {
    data class DeviceInfo(
        val appVersionName: String?,
        val appVersionCode: Long?,
        val osSdkInt: Int,
        val deviceManufacturer: String,
        val deviceModel: String,
    )

    /** The full human-readable report — what gets shared or copied. */
    fun render(): String = buildString {
        // A fresh SimpleDateFormat per call — it's not thread-safe, and this can run from a
        // crash handler on whatever thread just crashed, so no shared/static instance.
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        append(appLabel).append(" crash\n")
        append("when: ").append(format.format(Date(whenMillis))).append('\n')
        append("thread: ").append(threadName).append('\n')
        append("app version: ").append(device.appVersionName ?: "?")
        append(" (").append(device.appVersionCode?.toString() ?: "?").append(")\n")
        append("device: ").append(device.deviceManufacturer).append(' ').append(device.deviceModel)
        append(" · Android SDK ").append(device.osSdkInt).append("\n\n")
        append(headline).append("\n\n")
        append(trace)
    }

    /**
     * Persistence encoding: `headline` on its own first line, a blank separator, then
     * [render]'s full text — so reading a report back never needs to re-derive the
     * headline by parsing prose (see [decode]).
     */
    fun encode(): String = "$headline\n\n${render()}"

    companion object {
        /** First line worth reading: `ExceptionType: message` (message omitted if blank). */
        fun headlineOf(throwable: Throwable): String {
            val type = throwable.javaClass.simpleName.ifBlank { throwable.javaClass.name }
            val message = throwable.message?.takeIf { it.isNotBlank() }
            return if (message != null) "$type: $message" else type
        }

        fun stackTraceOf(throwable: Throwable): String =
            StringWriter().also { throwable.printStackTrace(PrintWriter(it)) }.toString()

        fun of(
            appLabel: String,
            whenMillis: Long,
            threadName: String,
            throwable: Throwable,
            device: DeviceInfo,
        ): CrashReport = CrashReport(
            appLabel = appLabel,
            whenMillis = whenMillis,
            threadName = threadName,
            headline = headlineOf(throwable),
            device = device,
            trace = stackTraceOf(throwable),
        )

        /**
         * Decode [encode]'s format into a display-ready pair. Best-effort: any text that
         * doesn't match the expected shape (e.g. a file from an older/foreign writer) still
         * yields a usable pair — the whole text as [fullReport] and its first line as
         * [headline] — so a decode quirk never hides a real crash behind a blank screen.
         */
        fun decode(persisted: String): Decoded {
            val separator = "\n\n"
            val splitAt = persisted.indexOf(separator)
            return if (splitAt >= 0) {
                Decoded(headline = persisted.substring(0, splitAt), fullReport = persisted.substring(splitAt + separator.length))
            } else {
                Decoded(headline = persisted.lines().firstOrNull().orEmpty(), fullReport = persisted)
            }
        }

        /**
         * Sample content for previewing the recovery screen without a real crash (see
         * [dev.aarso.crashrecovery.CrashRecovery.previewIntent]). "PREVIEW" appears in both
         * [Decoded.headline] and the full text so a screenshot, or an accidentally-shared
         * preview report, can never be mistaken for a real crash.
         */
        fun samplePreview(appLabel: String): Decoded {
            val headline = "IllegalStateException: this is a PREVIEW — no real crash occurred"
            val fullReport = buildString {
                append(appLabel).append(" crash — PREVIEW, not a real crash\n")
                append("when: (preview — no timestamp)\n")
                append("thread: main\n")
                append("app version: (preview)\n")
                append("device: (preview)\n\n")
                append(headline).append("\n\n")
                append("java.lang.IllegalStateException: this is a PREVIEW — no real crash occurred\n")
                append("\tat dev.aarso.crashrecovery.CrashRecovery.previewIntent(CrashRecovery.kt)\n")
                append("\tat ").append(appLabel).append(" (preview trigger — not an actual stack trace)\n")
            }
            return Decoded(headline = headline, fullReport = fullReport)
        }
    }

    /** What the recovery UI needs to render — a quick headline plus the full shareable text. */
    data class Decoded(val headline: String, val fullReport: String)
}
