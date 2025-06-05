package com.example.parent_teacher_engagement.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.example.parent_teacher_engagement.firebase.FirebaseService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NetworkConnectivityMonitor private constructor(private val context: Context) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private var wasOffline = false
    
    companion object {
        private var instance: NetworkConnectivityMonitor? = null
        
        fun getInstance(context: Context): NetworkConnectivityMonitor {
            return instance ?: synchronized(this) {
                instance ?: NetworkConnectivityMonitor(context.applicationContext).also { instance = it }
            }
        }
    }
    
    fun startMonitoring() {
        // Check initial state
        wasOffline = !isNetworkAvailable()
        
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d("NetworkMonitor", "Network is available")
                if (wasOffline) {
                    // Network was previously unavailable, now it's available
                    Log.d("NetworkMonitor", "Network reconnected - syncing messages")
                    wasOffline = false
                    
                    // Sync messages and check for new ones
                    CoroutineScope(Dispatchers.IO).launch {
                        FirebaseService.getInstance().syncMessagesAfterReconnect(context)
                    }
                }
            }
            
            override fun onLost(network: Network) {
                Log.d("NetworkMonitor", "Network lost")
                wasOffline = true
            }
        }
        
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }
    
    private fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
} 