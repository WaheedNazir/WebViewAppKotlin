package com.wavetech.webappkotlin.utilities

import android.content.Context
import android.net.ConnectivityManager

/**
 * Network Util Class
 */

class NetworkUtils {

    fun haveNetworkConnection(context: Context): Boolean {
        try {
            val connMgr = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo = connMgr.activeNetworkInfo
            return (networkInfo != null && networkInfo.isConnected)
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}