package com.ivanshulin.myexchange

import android.content.Context
import android.net.ConnectivityManager


fun isOnline(context: Context): Boolean {
    if (context != null) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val netInfo = cm.activeNetworkInfo
        return netInfo != null && netInfo.isConnectedOrConnecting
    } else {
        return false
    }
}


