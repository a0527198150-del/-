package com.example

import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class YouTubeBrowserActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var fullscreenContainer: FrameLayout
    
    // משתנים לשמירת הוידאו שמוצג במסך מלא
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_youtube_browser)

        webView = findViewById(R.id.webView)
        fullscreenContainer = findViewById(R.id.fullscreenContainer)

        setupWebView()
    }

    private fun setupWebView() {
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false

        webView.webViewClient = WebViewClient()
        
        // כאן קורה הקסם של המסך המלא
        webView.webChromeClient = object : WebChromeClient() {
            
            // נקרא אוטומטית כשהמשתמש לוחץ על כפתור מסך מלא ביוטיוב
            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                super.onShowCustomView(view, callback)
                
                if (customView != null) {
                    onHideCustomView()
                    return
                }

                customView = view
                customViewCallback = callback

                // מסתירים את ה-WebView הראשי ומציגים את אזור המסך המלא
                webView.visibility = View.GONE
                fullscreenContainer.visibility = View.VISIBLE
                fullscreenContainer.addView(customView)

                // מעלימים את סרגלי המערכת של הטלפון
                hideSystemUI()
            }

            // נקרא אוטומטית כשהמשתמש יוצא ממצב מסך מלא
            override fun onHideCustomView() {
                super.onHideCustomView()

                if (customView == null) return

                // מסירים את הוידאו מהמסך המלא ומחזירים את ה-WebView הראשי
                fullscreenContainer.removeView(customView)
                fullscreenContainer.visibility = View.GONE
                webView.visibility = View.VISIBLE

                customView = null
                customViewCallback?.onCustomViewHidden()
                customViewCallback = null

                // מחזירים את סרגלי המערכת לתצוגה רגילה
                showSystemUI()
            }
        }

        // טעינת אתר יוטיוב
        webView.loadUrl("https://www.youtube.com")
    }

    // העלמת סרגלי מערכת (סטטוס וניווט) בצורה מודרנית
    private fun hideSystemUI() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    // החזרת סרגלי מערכת לתצוגה רגילה
    private fun showSystemUI() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
    }

    // טיפול בכפתור החזור של הטלפון
    override fun onBackPressed() {
        if (customView != null) {
            // אם המשתמש במסך מלא, לחיצה על "חזור" תצא ממסך מלא במקום לסגור את האפליקציה
            webView.webChromeClient?.onHideCustomView()
        } else if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
