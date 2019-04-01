package com.finja.payrollplus.view

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
import com.finja.payrollplus.BuildConfig
import com.finja.payrollplus.R
import com.finja.payrollplus.utilities.NetworkChangeReceiver
import com.finja.payrollplus.utilities.NetworkUtils
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.general_custom_dialog_network_error.*

class PayRollActivity : AppCompatActivity() {

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

        if (networkUtils.haveNetworkConnection(this@PayRollActivity)) {
            loadWeb(BuildConfig.URL)
        } else {
            imgv_network_error.setVisibility(View.GONE)
            webView.setVisibility(View.VISIBLE)
            overlayView.visibility = View.VISIBLE
            connectionLostAlert("Quit", BuildConfig.URL)
        }
    }

    /**
     */
    @SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface", "ClickableViewAccessibility")
    private fun loadWeb(url: String?) {
        val webSettings = webView.getSettings()
        webSettings.setJavaScriptEnabled(true)
        webSettings.setBuiltInZoomControls(false)
        webView.setWebViewClient(myWebClient())
        webView.setWebChromeClient(MyWebChromeClient())
        /*webView.addJavascriptInterface(JavaScriptHandler(), getString(R.string.myhandler))*/
        try {
            webView.loadData("", "text/html", null)
            webView.loadUrl(url)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        webView.setOnTouchListener { _, event ->
            if (!networkUtils.haveNetworkConnection(this)) {
                connectionLostAlert("Quit", webView.getUrl())
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
            if (networkUtils.haveNetworkConnection(this@PayRollActivity)) {
                imgv_network_error.setVisibility(View.GONE)
                webView.setVisibility(View.VISIBLE)
                overlayView.visibility = View.VISIBLE
                super.onPageStarted(view, url, favicon)
            } else {
                webView.setVisibility(View.GONE)
                imgv_network_error.setVisibility(View.VISIBLE)
                overlayView.visibility = View.VISIBLE
                connectionLostAlert("Quit", url)
            }
        }

        override fun onPageFinished(view: WebView, url: String) {
            if (networkUtils.haveNetworkConnection(this@PayRollActivity)) {
                webView.setVisibility(View.VISIBLE)
                overlayView.visibility = View.GONE
                super.onPageFinished(view, url)
            }
        }

        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
            try {
                webView.setVisibility(View.GONE)
                imgv_network_error.setVisibility(View.VISIBLE)
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

        override fun onJsConfirm(view: WebView, url: String, message: String, result: JsResult): Boolean {
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

        override fun onJsAlert(view: WebView, url: String, message: String, result: JsResult): Boolean {
            result.confirm()
            if (message.equals("exit", ignoreCase = true)) {
                finish()
            } else {
                showToast(message)
            }
            return true
        }
    }

    /**
     *
     */
    /*inner class JavaScriptHandler internal constructor() {

        @JavascriptInterface
        fun setResult(value: String?, msg: String, status: String) {

            if (status.equals("success", ignoreCase = true)) {
                if (value != null && !value.equals("", ignoreCase = true) && !value.isEmpty()) {
                    // API Call Confirm Payment

                } else {
                    // showErrorMsg("Your ticket can not be purchased please try again")
                }
            }
            if (status.equals("exit", ignoreCase = true)) {
                finish()
            } else {
                showToast(status)
            }
        }
    }*/


    /**
     * Back press callback onBackPressed
     */
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            generalDailog("PayrollPlus", "Are you sure you want to quit?")
        }
    }


    /**
     * Back Press Alert Dialog
     */
    fun generalDailog(title: String, message: String) {
        try {
            val builder = AlertDialog.Builder(this@PayRollActivity)

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
                    var url = ""
                    if (webView.getUrl() == null) {
                        url = BuildConfig.URL
                    } else {
                        url = webView.getUrl();
                    }
                    connectionLostAlert("Quit", url)
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
     * showToast
     */
    private fun showToast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show()
    }

    /**
     */
    protected fun isTextEmpty(text: String?): Boolean {
        var result = ""
        try {
            if (text != null) {
                result = text.trim { it <= ' ' }
                return result.isEmpty() || result.equals("null", ignoreCase = true)
            } else {
                return true
            }
        } catch (e: Exception) {
            return false
        }

    }

    /**
     *
     */
    override fun onDestroy() {
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mNotificationReceiverInternet)
            LocalBroadcastManager.getInstance(this).unregisterReceiver(networkChangeReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        super.onDestroy()
    }

}
