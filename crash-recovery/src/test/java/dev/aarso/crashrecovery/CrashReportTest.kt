package dev.aarso.crashrecovery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashReportTest {

    private val device = CrashReport.DeviceInfo(
        appVersionName = "1.2.3",
        appVersionCode = 42L,
        osSdkInt = 34,
        deviceManufacturer = "Nubia",
        deviceModel = "RedMagic 11 Pro",
    )

    @Test
    fun `headline includes exception type and message`() {
        val headline = CrashReport.headlineOf(IllegalStateException("container not ready"))
        assertEquals("IllegalStateException: container not ready", headline)
    }

    @Test
    fun `headline omits message when blank`() {
        val headline = CrashReport.headlineOf(RuntimeException())
        assertEquals("RuntimeException", headline)
    }

    @Test
    fun `render includes app label, device metadata, headline, and trace`() {
        val report = CrashReport.of(
            appLabel = "Runout",
            whenMillis = 0L,
            threadName = "main",
            throwable = IllegalStateException("boom"),
            device = device,
        )
        val rendered = report.render()

        assertTrue(rendered.startsWith("Runout crash\n"))
        assertTrue(rendered.contains("thread: main"))
        assertTrue(rendered.contains("app version: 1.2.3 (42)"))
        assertTrue(rendered.contains("Nubia RedMagic 11 Pro"))
        assertTrue(rendered.contains("Android SDK 34"))
        assertTrue(rendered.contains("IllegalStateException: boom"))
        assertTrue(rendered.contains("at dev.aarso.crashrecovery.CrashReportTest"))
    }

    @Test
    fun `encode then decode round-trips the headline and full report`() {
        val report = CrashReport.of(
            appLabel = "Clackpad",
            whenMillis = 1_700_000_000_000L,
            threadName = "main",
            throwable = NullPointerException("keymap missing"),
            device = device,
        )

        val decoded = CrashReport.decode(report.encode())

        assertEquals("NullPointerException: keymap missing", decoded.headline)
        assertEquals(report.render(), decoded.fullReport)
    }

    @Test
    fun `decode is best-effort on a foreign or malformed string`() {
        val decoded = CrashReport.decode("just some text with no blank-line separator")

        assertEquals("just some text with no blank-line separator", decoded.headline)
        assertEquals("just some text with no blank-line separator", decoded.fullReport)
    }

    @Test
    fun `decode handles an empty string without throwing`() {
        val decoded = CrashReport.decode("")

        assertEquals("", decoded.headline)
        assertEquals("", decoded.fullReport)
    }

    @Test
    fun `samplePreview is clearly labelled and never claims a real crash`() {
        val decoded = CrashReport.samplePreview("Runout")

        assertTrue(decoded.headline.contains("PREVIEW"))
        assertTrue(decoded.fullReport.contains("PREVIEW"))
        assertTrue(decoded.fullReport.contains("Runout"))
    }
}
