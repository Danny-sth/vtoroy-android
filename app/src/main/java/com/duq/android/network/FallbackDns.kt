package com.duq.android.network

import android.util.Log
import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/**
 * Custom DNS resolver with fallback to DNS-over-HTTPS.
 *
 * When system DNS fails (common on some WiFi networks),
 * falls back to Cloudflare DoH (1.1.1.1) or Google DoH (8.8.8.8).
 */
class FallbackDns private constructor(
    private val systemDns: Dns,
    private val dohDns: Dns
) : Dns {

    companion object {
        private const val TAG = "FallbackDns"

        /**
         * Create FallbackDns with DoH fallback.
         * Uses a bootstrap client without custom DNS to avoid circular dependency.
         */
        fun create(): FallbackDns {
            // Bootstrap client for DoH requests (uses system DNS)
            val bootstrapClient = OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build()

            // Cloudflare DNS-over-HTTPS
            val cloudflareDoH = DnsOverHttps.Builder()
                .client(bootstrapClient)
                .url("https://1.1.1.1/dns-query".toHttpUrl())
                .bootstrapDnsHosts(
                    InetAddress.getByName("1.1.1.1"),
                    InetAddress.getByName("1.0.0.1")
                )
                .build()

            return FallbackDns(Dns.SYSTEM, cloudflareDoH)
        }
    }

    override fun lookup(hostname: String): List<InetAddress> {
        // First, try system DNS
        return try {
            val addresses = systemDns.lookup(hostname)
            if (addresses.isNotEmpty()) {
                addresses
            } else {
                fallbackLookup(hostname)
            }
        } catch (e: UnknownHostException) {
            Log.w(TAG, "System DNS failed for $hostname, trying DoH fallback")
            fallbackLookup(hostname)
        }
    }

    private fun fallbackLookup(hostname: String): List<InetAddress> {
        return try {
            val addresses = dohDns.lookup(hostname)
            if (addresses.isNotEmpty()) {
                Log.d(TAG, "Resolved $hostname via DoH: ${addresses.first().hostAddress}")
                addresses
            } else {
                throw UnknownHostException("DoH returned empty result for $hostname")
            }
        } catch (e: Exception) {
            Log.e(TAG, "DoH fallback also failed for $hostname: ${e.message}")
            throw UnknownHostException("Unable to resolve $hostname: ${e.message}")
        }
    }
}
