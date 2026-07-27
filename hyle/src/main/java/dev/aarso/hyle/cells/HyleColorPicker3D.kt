package dev.aarso.hyle.cells

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import dev.aarso.hyle.theme.parseHexColor
import dev.aarso.hyle.theme.toHexRgb

/**
 * The real 3D colour picker — a hue ring, an HSV/RGB/Lab/HCL slice, and a live 3D model of the
 * space, exactly as the tactile kit ships it. This is a **verbatim port**, not a reinterpretation:
 * `kit/color-picker.html` (bundled as an asset — see `scripts/build-color-picker.js`, which slices
 * it byte-for-byte from `kit/tactile-kit.html`) is the same document [dev.aarso.hyle.cells.web's
 * `hy-color-picker.ts`][] embeds in an iframe on the web. Here it is the WebView's own document —
 * there is no outer page for it to be an iframe *within*, so there is no second copy of the
 * picker's markup or its THREE.js bundle to keep in sync; loading the asset directly *is* the
 * faithful port.
 *
 * The bridge mirrors `hy-color-picker.ts`'s contract exactly: the document does
 * `parent.postMessage({type:'hy-color', value:'#rrggbb'}, '*')` on every colour change. With no
 * real parent frame, `parent` is the document's own window, so the message never leaves the page
 * on its own — [AndroidColorBridge] is injected specifically to catch it and cross into Kotlin.
 * [HyleColorPicker3D] deliberately does not also read `hy-height`: the kit auto-sizes its iframe
 * for a scrolling web page, but here the WebView gets a fixed [height] instead, matching how every
 * other Hyle cell sizes itself.
 *
 * The initial colour is passed the same way the kit's own picker page reads it back from a
 * reload — a `?value=%23rrggbb` query param — so external colour changes (a preset tap) reload the
 * document at the new colour, exactly as `hy-color-picker.ts`'s reactive `value` property does.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun HyleColorPicker3D(
    color: Color,
    onColorChange: (Color) -> Unit,
    modifier: Modifier = Modifier,
    pickerHeight: Dp = 460.dp,
) {
    val context = LocalContext.current
    val onChange by rememberUpdatedState(onColorChange)
    // The hex the WebView currently shows. Written from two places: here, when [color] changes
    // for a reason other than the bridge (a preset tap); and from the bridge callback itself,
    // which must update this WITHOUT reloading — that's the whole point of tracking it. Compose
    // state, not a plain var, because the bridge callback jumps back to the main thread to touch
    // it (JavascriptInterface methods run on a WebView-internal thread, never the UI thread).
    var shownHex by remember { mutableStateOf("") }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            addJavascriptInterface(
                object {
                    @JavascriptInterface
                    fun onColor(hex: String) {
                        val v = parseHexColor(hex) ?: return
                        mainHandler.post {
                            shownHex = hex.uppercase()
                            onChange(v)
                        }
                    }
                },
                "AndroidColorBridge",
            )
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String?) {
                    // Bridges the page's own `parent.postMessage` (there is no real parent frame,
                    // so it targets the page's own window) into the Kotlin side.
                    view.evaluateJavascript(
                        """
                        (function() {
                          window.addEventListener('message', function(e) {
                            var d = e.data;
                            if (d && d.type === 'hy-color' && typeof d.value === 'string') {
                              AndroidColorBridge.onColor(d.value);
                            }
                          });
                        })();
                        """.trimIndent(),
                        null,
                    )
                }
            }
        }
    }

    val hex = color.toHexRgb().uppercase()
    LaunchedEffect(hex) {
        if (hex != shownHex) {
            shownHex = hex
            webView.loadUrl("file:///android_asset/kit/color-picker.html?value=%23${hex.removePrefix("#")}")
        }
    }

    AndroidView(
        factory = { webView },
        modifier = modifier.fillMaxWidth().height(pickerHeight),
    )
}
