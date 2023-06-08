package com.example.weatherapp

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

object Constants {
    // https://api.openweathermap.org/data/2.5/forecast/daily?q=lisbon&appid=ed60fcfbd110ee65c7150605ea8aceea&units=imperial

    const val API_ID = "951dd9324c4199bbc283d2f5f5334c7f"
//    const val API_ID: String = "ed60fcfbd110ee65c7150605ea8aceea"
    const val BASE_URL: String = "https://api.openweathermap.org/data/"
    const val METRIC_UNIT: String = "metric"

    fun isNetworkAvailable(context: Context) : Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

            return when {
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> return true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> return true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> return true
                else -> return false
            }
        } else {
            val networkInfo = connectivityManager.activeNetworkInfo
            return networkInfo != null && networkInfo.isConnectedOrConnecting
        }
    }
}
