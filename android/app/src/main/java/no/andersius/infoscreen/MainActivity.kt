package no.andersius.infoscreen

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.window.OnBackInvokedDispatcher

class MainActivity : Activity() {

    private lateinit var webView: WebView
    private var loadedUrl: String? = null
    private var longPressHandled = false

    private val handler = Handler(Looper.getMainLooper())
    private val retryLoad = Runnable { loadedUrl?.let { webView.loadUrl(it) } }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        WebView.setWebContentsDebuggingEnabled(
            (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        )

        webView = WebView(this).apply {
            setBackgroundColor(Color.BLACK)
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            isFocusable = true
            isFocusableInTouchMode = true
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                mediaPlaybackRequiresUserGesture = false
                // Allows http images/video on an https page; still blocks mixed scripts
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            }
            webViewClient = InfoscreenWebViewClient()
            webChromeClient = object : WebChromeClient() {
                // Hide the grey play-button poster WebView shows before a video starts
                override fun getDefaultVideoPoster(): Bitmap =
                    Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
            }
        }
        setContentView(webView)
        webView.requestFocus()

        if (Build.VERSION.SDK_INT >= 33) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT
            ) { handleBack() }
        }

        applyUrlExtra(intent)
        if (!FeedPrefs.isSet(this)) {
            // First run: store the default so the screen works even if settings is dismissed
            FeedPrefs.setUrl(this, FeedPrefs.DEFAULT_URL)
            openSettings()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        applyUrlExtra(intent)
    }

    override fun onResume() {
        super.onResume()
        hideSystemUi()
        webView.onResume()
        val url = FeedPrefs.url(this)
        if (url != loadedUrl) load(url)
    }

    override fun onPause() {
        webView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        handler.removeCallbacks(retryLoad)
        (webView.parent as? ViewGroup)?.removeView(webView)
        webView.destroy()
        super.onDestroy()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        when (event.keyCode) {
            KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_SETTINGS -> {
                if (event.action == KeyEvent.ACTION_UP) openSettings()
                return true
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    if (event.repeatCount == 0) {
                        longPressHandled = false
                    } else {
                        // Holding OK opens settings. Auto-repeats are swallowed so the
                        // player doesn't skip through slides while the key is held.
                        if (!longPressHandled && event.eventTime - event.downTime >= LONG_PRESS_MS) {
                            longPressHandled = true
                            openSettings()
                        }
                        return true
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    @Deprecated("Used below API 33; newer versions use OnBackInvokedCallback")
    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        handleBack()
    }

    private fun handleBack() {
        if (webView.canGoBack()) webView.goBack() else finish()
    }

    /** `adb shell am start -n ... --es url <url>` sets the feed URL without the remote. */
    private fun applyUrlExtra(intent: Intent?) {
        val raw = intent?.getStringExtra(EXTRA_URL) ?: return
        FeedPrefs.normalize(raw)?.let { FeedPrefs.setUrl(this, it) }
    }

    private fun openSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    private fun load(url: String) {
        handler.removeCallbacks(retryLoad)
        loadedUrl = url
        webView.clearHistory()
        webView.loadUrl(url)
    }

    @Suppress("DEPRECATION")
    private fun hideSystemUi() {
        if (Build.VERSION.SDK_INT >= 30) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.systemBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
        }
    }

    // Setting any WebViewClient keeps navigation inside the WebView instead of opening a browser
    private inner class InfoscreenWebViewClient : WebViewClient() {
        // Main-frame load failed (e.g. no network right after boot): retry until it works.
        // On API 23+ the default onReceivedError(request, error) forwards main-frame errors here.
        @Deprecated("Deprecated in Java")
        @Suppress("OVERRIDE_DEPRECATION")
        override fun onReceivedError(view: WebView, errorCode: Int, description: String?, failingUrl: String?) {
            handler.removeCallbacks(retryLoad)
            handler.postDelayed(retryLoad, RETRY_MS)
        }

        // Without this, a crashed or killed renderer takes the whole app down
        override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
            recreate()
            return true
        }
    }

    companion object {
        const val EXTRA_URL = "url"
        private const val LONG_PRESS_MS = 1500L
        private const val RETRY_MS = 15_000L
    }
}
