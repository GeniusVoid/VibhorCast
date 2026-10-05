package com.vibhor.cast.cast

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.URL

object TVDiscovery {
    private const val TAG = "TVDiscovery"
    private const val EXOAIRPLAYER_PORT = 8192

    /**
     * Get the phone's hotspot IP address (usually on ap0 or wlan0 interface)
     */
    fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (!networkInterface.isUp || networkInterface.isLoopback) continue
                
                // Prefer ap0 (hotspot) interface
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        val ip = addr.hostAddress
                        if (ip != null && !ip.startsWith("127.")) {
                            Log.d(TAG, "Found IP: $ip on ${networkInterface.name}")
                            return ip
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get local IP", e)
        }
        return null
    }

    /**
     * Scan the local subnet for ExoAirPlayer instances.
     * Returns list of IP:PORT strings.
     */
    suspend fun discoverTVs(): List<String> = withContext(Dispatchers.IO) {
        val localIp = getLocalIpAddress() ?: return@withContext emptyList()
        val subnet = localIp.substringBeforeLast('.')
        
        Log.d(TAG, "Scanning subnet $subnet.0/24 for ExoAirPlayer...")

        coroutineScope {
            (1..254).map { i ->
                async(Dispatchers.IO) {
                    val ip = "$subnet.$i"
                    if (ip == localIp) return@async null
                    try {
                        val addr = InetAddress.getByName(ip)
                        if (addr.isReachable(500)) {
                            // Check if ExoAirPlayer is running on this host
                            try {
                                val url = URL("http://$ip:$EXOAIRPLAYER_PORT/server-info")
                                val conn = url.openConnection() as HttpURLConnection
                                conn.connectTimeout = 1500
                                conn.readTimeout = 1500
                                val code = conn.responseCode
                                conn.disconnect()
                                if (code in 200..299) {
                                    Log.i(TAG, "Found ExoAirPlayer at $ip:$EXOAIRPLAYER_PORT")
                                    return@async "$ip:$EXOAIRPLAYER_PORT"
                                }
                            } catch (_: Exception) { }
                        }
                    } catch (_: Exception) { }
                    null
                }
            }.awaitAll().filterNotNull()
        }
    }
}
