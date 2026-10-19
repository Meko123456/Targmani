package io.github.meko123456.targmani.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Whether the phone's network is Wi-Fi right now, kept current.
 *
 * With "Download over Wi-Fi only" on, ML Kit holds a model download until there is Wi-Fi and says
 * nothing, so on mobile data the screens showed "Downloading…" for as long as that took, with
 * nothing downloading. This is how they tell waiting from downloading.
 */
fun Context.wifiConnection(): Flow<Boolean> = callbackFlow {
    val connectivity = getSystemService(ConnectivityManager::class.java)
    fun onWifi(): Boolean = connectivity.getNetworkCapabilities(connectivity.activeNetwork)
        ?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { trySend(onWifi()) }
        override fun onLost(network: Network) { trySend(onWifi()) }
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) { trySend(onWifi()) }
    }
    trySend(onWifi())
    connectivity.registerDefaultNetworkCallback(callback)
    awaitClose { connectivity.unregisterNetworkCallback(callback) }
}.distinctUntilChanged()
