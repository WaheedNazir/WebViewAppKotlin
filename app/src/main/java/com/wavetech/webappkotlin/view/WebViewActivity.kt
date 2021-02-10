package com.wavetech.webappkotlin.view

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.view.View
import android.webkit.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDialog
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.wavetech.webappkotlin.BuildConfig
import com.wavetech.webappkotlin.R
import com.wavetech.webappkotlin.utilities.NetworkChangeReceiver
import com.wavetech.webappkotlin.utilities.NetworkUtils
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.general_custom_dialog_network_error.*

open class WebViewActivity : AppCompatActivity() {

    private val networkUtils = NetworkUtils()
    private val networkChangeReceiver = NetworkChangeReceiver()

    override fun onStart() {
        super.onStart()

        LocalBroadcastManager.getInstance(this).registerReceiver(
            mNotificationReceiverInternet,
            IntentFilter(getString(R.string.keySendInternetStatus))
        )

        if (Build.VERSION.SDK_INT >= 23) {
            // Above marshmallow Manifest Connectivity Changes not working.
            val intentFilter = IntentFilter("android.net.conn.CONNECTIVITY_CHANGE")
            this.registerReceiver(networkChangeReceiver, intentFilter)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (networkUtils.haveNetworkConnection(this@WebViewActivity)) {
            loadWeb(BuildConfig.URL)
        } else {
            imgv_network_error.visibility = View.GONE
            webView.visibility = View.VISIBLE
            overlayView.visibility = View.VISIBLE
            connectionLostAlert("Quit", BuildConfig.URL)
        }
    }

    /**
     */
    @SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface", "ClickableViewAccessibility")
    private fun loadWeb(url: String) {
        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.builtInZoomControls = false
        webView.webViewClient = myWebClient()
        webView.webChromeClient = MyWebChromeClient()
        webView.addJavascriptInterface(JavaScriptHandler(), "Your_Handler_NAME")
        try {
            webView.loadData("", "text/html", null)
            webView.loadUrl(url)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        webView.setOnTouchListener { _, _ ->
            if (!networkUtils.haveNetworkConnection(this)) {
                webView.url?.let { connectionLostAlert("Quit", it) }
            }
            false
        }
    }

    /**
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    /**
     *
     */
    inner class myWebClient : WebViewClient() {
        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
            if (networkUtils.haveNetworkConnection(this@WebViewActivity)) {
                imgv_network_error.visibility = View.GONE
                webView.visibility = View.VISIBLE
                overlayView.visibility = View.VISIBLE
                super.onPageStarted(view, url, favicon)
            } else {
                webView.visibility = View.GONE
                imgv_network_error.setVisibility(View.VISIBLE)
                overlayView.visibility = View.VISIBLE
                connectionLostAlert("Quit", url)
            }
        }

        override fun onPageFinished(view: WebView, url: String) {
            if (networkUtils.haveNetworkConnection(this@WebViewActivity)) {
                webView.visibility = View.VISIBLE
                overlayView.visibility = View.GONE
                super.onPageFinished(view, url)
            }
        }

        override fun onReceivedError(
            view: WebView,
            request: WebResourceRequest,
            error: WebResourceError
        ) {
            try {
                webView.visibility = View.GONE
                imgv_network_error.visibility = View.VISIBLE
                overlayView.visibility = View.VISIBLE
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }

    /**
     *
     */
    internal inner class MyWebChromeClient : WebChromeClient() {

        override fun onJsConfirm(
            view: WebView,
            url: String,
            message: String,
            result: JsResult
        ): Boolean {
            return super.onJsConfirm(view, url, message, result)
        }

        override fun onJsPrompt(
            view: WebView,
            url: String,
            message: String,
            defaultValue: String,
            result: JsPromptResult
        ): Boolean {
            return super.onJsPrompt(view, url, message, defaultValue, result)
        }

        override fun onJsAlert(
            view: WebView,
            url: String,
            message: String,
            result: JsResult
        ): Boolean {
            result.confirm()
            if (message.equals("exit", ignoreCase = true)) {
                finish()
            } else {
                showToast(message)
            }
            return true
        }
    }


    class JavaScriptHandler internal constructor() {

        @JavascriptInterface
        fun setResult(value: String?, msg: String, status: String) {
            // You can control your flow by checking status
        }
    }


    /**
     * Back press callback onBackPressed
     */
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            generalDailog(getString(R.string.app_name), "Are you sure you want to quit?")
        }
    }


    /**
     * Back Press Alert Dialog
     */
    private fun generalDailog(title: String, message: String) {
        try {
            val builder = AlertDialog.Builder(this@WebViewActivity)

            builder.setTitle(title)
            builder.setMessage(message)
            builder.setCancelable(false)
            builder.setPositiveButton("YES") { _, _ ->
                try {
                    webView.clearCache(true)
                    finish()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            builder.setNegativeButton("No") { dialog, _ ->
                dialog.cancel()
            }
            val dialog: AlertDialog = builder.create()
            dialog.show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    /**
     */
    private val mNotificationReceiverInternet = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {

            if (intent != null && intent.extras != null && !intent.extras!!.isEmpty) {
                if (!intent.getBooleanExtra("isConnected", false)) {
                    val url = if (webView.url == null) {
                        BuildConfig.URL
                    } else {
                        webView.url
                    }
                    url?.let { url1 ->
                        connectionLostAlert("Quit", url1)
                    }
                }
            }
        }
    }


    /***
     * @param noButtonText Button text
     * @param url Url
     */
    protected fun connectionLostAlert(noButtonText: String, url: String) {
        try {
            // custom dialog
            webView.visibility = View.GONE
            val customDialog = AppCompatDialog(this)
            customDialog.setContentView(R.layout.general_custom_dialog_network_error)
            customDialog.setCanceledOnTouchOutside(false)
            customDialog.setCancelable(false)
            customDialog.tvDialogTitle.text = getString(R.string.noInternetConnection)

            customDialog.tvDialogRetry.setOnClickListener { _ ->
                customDialog.cancel()
                if (networkUtils.haveNetworkConnection(this)) {
                    if (!isTextEmpty(url))
                        loadWeb(url)
                    customDialog.cancel()
                } else {
                    connectionLostAlert(noButtonText, url)
                }
            }
            customDialog.tvDialogCancel.text = noButtonText
            customDialog.tvDialogCancel.setOnClickListener { _ ->
                customDialog.cancel()
                finish()
            }

            if (!customDialog.isShowing()) {
                customDialog.show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    /**
     */
    private fun showToast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show()
    }

    /**
     */
    private fun isTextEmpty(text: String?): Boolean {
        var result = ""
        return try {
            if (text != null) {
                result = text.trim { it <= ' ' }
                result.isEmpty() || result.equals("null", ignoreCase = true)
            } else {
                true
            }
        } catch (e: Exception) {
            false
        }

    }

    /**
     *
     */
    override fun onDestroy() {
        try {
            LocalBroadcastManager.getInstance(this)
                .unregisterReceiver(mNotificationReceiverInternet)
            LocalBroadcastManager.getInstance(this).unregisterReceiver(networkChangeReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        super.onDestroy()
    }

}
