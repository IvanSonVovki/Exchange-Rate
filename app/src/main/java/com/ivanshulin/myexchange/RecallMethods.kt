package com.ivanshulin.myexchange

import android.content.Context
import android.net.ConnectivityManager



enum class Key(val key: String) {
    CURRENT_DATE("currentDate"),
    JSON_STRING("jsonString")
}

fun isOnline(context: Context): Boolean {
    if (context != null) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val netInfo = cm.activeNetworkInfo
        return netInfo != null && netInfo.isConnectedOrConnecting
    } else {
        return false
    }
}




