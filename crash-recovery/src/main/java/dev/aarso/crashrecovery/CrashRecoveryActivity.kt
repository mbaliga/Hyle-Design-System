package dev.aarso.crashrecovery

import android.app.Activity
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/**
 * The recovery surface shown on the launch after a captured crash (see [CrashRecovery]).
 * Deliberately built on the bare platform (`android.app.Activity` + `android.widget`
 * views only — no AppCompat, no Compose, no Material) so it can never be brought down by
 * whatever broke in the app that hosted it, and so apps that don't ship Compose (a plain
 * `android.widget`-based app) never have to add it just for this one screen.
 *
 * Structure, in priority order: a one-line headline (what broke), the safe actions
 * (Share / Copy) first, Continue, then the destructive Reset (confirm-gated — a single
 * mistap should never wipe app data), and the full trace collapsed by default behind a
 * "Technical details" toggle so the first thing a tester sees is legible, not a wall of
 * stack frames.
 */
class CrashRecoveryActivity : Activity() {

    @Suppress("DEPRECATION")
    private fun readStyle(): CrashRecoveryStyle =
        (intent.getSerializableExtra(EXTRA_STYLE) as? CrashRecoveryStyle) ?: CrashRecoveryStyle.Default

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appLabel = intent.getStringExtra(EXTRA_APP_LABEL) ?: "App"
        val style = readStyle()
        val decoded = CrashRecovery.pending(this)

        // Nothing to recover from (e.g. launched directly for testing, or cleared between
        // the check in maybeShowRecovery and here) — don't strand the user on a blank screen.
        if (decoded == null) {
            finish()
            return
        }

        setContentView(buildRoot(appLabel, style, decoded))
    }

    private fun buildRoot(appLabel: String, style: CrashRecoveryStyle, decoded: CrashReport.Decoded): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(style.background)
            setPadding(20.dp, 24.dp, 20.dp, 20.dp)
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        root.addView(text("$appLabel hit a snag", size = 22f, color = style.foreground, bold = true))
        root.addView(spacer(6))
        root.addView(
            text(
                "The last launch crashed. This is the recovery screen, not a crash — " +
                    "you're not stuck. Share the report so it can be fixed, or reset the " +
                    "app's local data to continue.",
                size = 13f,
                color = style.muted,
            ),
        )
        root.addView(spacer(10))
        root.addView(localOnlyBadge(style))
        root.addView(spacer(14))
        root.addView(headlineCard(decoded.headline, style))
        root.addView(spacer(16))
        root.addView(actionsRow(style, decoded))
        root.addView(spacer(10))
        root.addView(secondaryRow(appLabel, style))
        root.addView(spacer(16))

        val (toggle, detailsContainer) = technicalDetails(decoded.fullReport, style)
        root.addView(toggle)
        root.addView(detailsContainer)

        return ScrollView(this).apply {
            setBackgroundColor(style.background)
            addView(root)
        }
    }

    private fun localOnlyBadge(style: CrashRecoveryStyle): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            // Icon + label, never colour alone (colour-blind-safe, matching the constellation's
            // Provenance rule) — this is a reassurance, not a status, so it stays neutral.
            addView(text("🔒", size = 13f, color = style.muted))
            addView(spacerHorizontal(6))
            addView(text("Stays on this device until you share it", size = 12f, color = style.muted))
        }

    private fun headlineCard(headline: String, style: CrashRecoveryStyle): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14.dp, 12.dp, 14.dp, 12.dp)
            background = GradientDrawable().apply {
                setColor(style.surface)
                cornerRadius = 10.dp.toFloat()
            }
        }
        card.addView(text("What broke", size = 11f, color = style.muted))
        card.addView(spacer(4))
        card.addView(text(headline, size = 14f, color = style.foreground, bold = true, monospace = true))
        return card
    }

    private fun actionsRow(style: CrashRecoveryStyle, decoded: CrashReport.Decoded): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(
                filledButton("Share report", style) { shareReport(decoded.fullReport) },
            )
            addView(spacerHorizontal(10))
            addView(
                outlinedButton("Copy", style) { copyReport(decoded.fullReport) },
            )
        }

    private fun secondaryRow(appLabel: String, style: CrashRecoveryStyle): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(outlinedButton("Continue", style) { continueToApp() })
            addView(spacerHorizontal(10))
            addView(outlinedButton("Reset app data", style, textColor = style.danger) { confirmReset(appLabel) })
        }

    private fun technicalDetails(fullReport: String, style: CrashRecoveryStyle): Pair<View, View> {
        val detailsBody = ScrollView(this).apply {
            visibility = View.GONE
            addView(
                text(fullReport, size = 11f, color = style.traceText, monospace = true).apply {
                    setPadding(12.dp, 10.dp, 12.dp, 10.dp)
                },
            )
            background = GradientDrawable().apply {
                setColor(style.surface)
                cornerRadius = 8.dp.toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 320.dp)
        }
        val toggle = text("Technical details ▸", size = 13f, color = style.accent, bold = true).apply {
            isClickable = true
            isFocusable = true
            setPadding(0, 8.dp, 0, 8.dp)
            setOnClickListener {
                val expanding = detailsBody.visibility != View.VISIBLE
                detailsBody.visibility = if (expanding) View.VISIBLE else View.GONE
                text = if (expanding) "Technical details ▾" else "Technical details ▸"
            }
        }
        return toggle to detailsBody
    }

    private fun confirmReset(appLabel: String) {
        AlertDialog.Builder(this)
            .setTitle("Reset $appLabel's data?")
            .setMessage("This wipes locally-stored app data and restarts $appLabel. This cannot be undone.")
            .setPositiveButton("Reset") { _, _ -> performReset() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performReset() {
        CrashRecovery.clear(this)
        runCatching {
            (getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).clearApplicationUserData()
        }
    }

    private fun continueToApp() {
        CrashRecovery.clear(this)
        finish()
    }

    private fun shareReport(fullReport: String) {
        runCatching {
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Crash report")
                putExtra(Intent.EXTRA_TEXT, fullReport.take(60_000))
            }
            startActivity(Intent.createChooser(send, "Share crash report"))
        }
    }

    private fun copyReport(fullReport: String) {
        runCatching {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Crash report", fullReport))
            // API 33+ already shows its own copy confirmation; ours would be redundant there.
            if (Build.VERSION.SDK_INT < 33) {
                Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- tiny view-builder helpers (no XML, no external UI dependency) ---

    private fun text(value: String, size: Float, color: Int, bold: Boolean = false, monospace: Boolean = false): TextView =
        TextView(this).apply {
            text = value
            setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
            if (monospace) typeface = Typeface.MONOSPACE
        }

    private fun filledButton(label: String, style: CrashRecoveryStyle, onClick: () -> Unit): Button =
        Button(this).apply {
            text = label
            setTextColor(style.background)
            setBackgroundColor(style.accent)
            setOnClickListener { onClick() }
        }

    private fun outlinedButton(label: String, style: CrashRecoveryStyle, textColor: Int = style.foreground, onClick: () -> Unit): Button =
        Button(this).apply {
            text = label
            setTextColor(textColor)
            background = GradientDrawable().apply {
                setColor(style.background)
                setStroke(1.dp, textColor)
                cornerRadius = 6.dp.toFloat()
            }
            setOnClickListener { onClick() }
        }

    private fun spacer(heightDp: Int): View = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, heightDp.dp)
    }

    private fun spacerHorizontal(widthDp: Int): View = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(widthDp.dp, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    private val Int.dp: Int
        get() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), resources.displayMetrics).toInt()

    companion object {
        private const val EXTRA_APP_LABEL = "dev.aarso.crashrecovery.APP_LABEL"
        private const val EXTRA_STYLE = "dev.aarso.crashrecovery.STYLE"

        fun intent(context: Context, appLabel: String, style: CrashRecoveryStyle = CrashRecoveryStyle.Default): Intent =
            Intent(context, CrashRecoveryActivity::class.java).apply {
                putExtra(EXTRA_APP_LABEL, appLabel)
                putExtra(EXTRA_STYLE, style)
                if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
    }
}
