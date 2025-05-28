package com.soft.bookteria.helpers

import android.content.Context
import android.net.ConnectivityManager

class NetworkObserver(context: Context) {
    enum class Status{
        Avaiable, Unavailable, Loosing, Lost
    }
    
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
}