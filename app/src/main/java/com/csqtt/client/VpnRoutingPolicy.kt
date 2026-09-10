// SPDX-FileCopyrightText: 2026 amurcanov
// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package com.csqtt.client

import android.net.VpnService
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.InetAddress
import java.net.URL

data class RouteCidr(val address: String, val prefixLength: Int)

object VpnRoutingPolicy {
    const val MODE_ALL = "all"
    const val MODE_SELECTIVE = "selective"

    private const val TAG = "VpnRoutingPolicy"

    // Presets for popular blocked resources to route through tunnel
    val POPULAR_BLOCKED_CIDRS = listOf(
        // Cloudflare / Fastly CDN
        RouteCidr("104.16.0.0", 13),
        RouteCidr("104.24.0.0", 14),
        RouteCidr("172.64.0.0", 13),
        RouteCidr("151.101.0.0", 16),
        // YouTube / Google Video
        RouteCidr("142.250.0.0", 15),
        RouteCidr("172.217.0.0", 16),
        RouteCidr("216.58.192.0", 19),
        // Instagram / Facebook / Meta
        RouteCidr("157.240.0.0", 16),
        RouteCidr("31.13.64.0", 18),
        // Twitter / X
        RouteCidr("104.244.42.0", 24),
        // Discord
        RouteCidr("162.158.0.0", 15),
    )

    fun parseCidrs(raw: String): List<RouteCidr> {
        return raw.split(Regex("[,;\\s\\n]+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { entry ->
                parseCidr(entry)
            }
    }

    fun parseCidr(entry: String): RouteCidr? {
        val parts = entry.split("/")
        if (parts.size != 2) return null
        val ip = parts[0].trim()
        val prefix = parts[1].trim().toIntOrNull() ?: return null
        if (prefix !in 0..32) return null
        val octets = ip.split(".")
        if (octets.size != 4 || octets.any { it.toIntOrNull() !in 0..255 }) return null
        return RouteCidr(ip, prefix)
    }

    fun parseDomains(raw: String): List<String> {
        return raw.split(Regex("[,;\\s\\n]+"))
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() && it.contains(".") && !it.startsWith("#") }
            .distinct()
    }

    suspend fun resolveDomainIps(domains: List<String>): Set<String> = withContext(Dispatchers.IO) {
        val ips = mutableSetOf<String>()
        for (domain in domains) {
            runCatching {
                val addresses = InetAddress.getAllByName(domain)
                for (addr in addresses) {
                    if (addr is Inet4Address) {
                        ips.add(addr.hostAddress)
                    }
                }
            }.onFailure {
                Log.w(TAG, "Failed to resolve bypass domain: $domain", it)
            }
        }
        ips
    }

    suspend fun downloadRoutesFromUrl(urlStr: String): Result<List<RouteCidr>> = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL(urlStr.trim())
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10000
            conn.readTimeout = 15000
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "CSQTT-Android")
            val content = conn.inputStream.bufferedReader().use { it.readText() }
            val cidrs = parseCidrs(content)
            if (cidrs.isEmpty()) throw IllegalStateException("В файле не найдено корректных CIDR маршрутов")
            cidrs
        }
    }

    fun applyRoutes(
        builder: VpnService.Builder,
        mode: String,
        customCidrsRaw: String,
        bypassDomainsRaw: String = "",
    ): Int {
        if (mode == MODE_SELECTIVE) {
            val userCidrs = parseCidrs(customCidrsRaw)
            val targets = if (userCidrs.isNotEmpty()) userCidrs else POPULAR_BLOCKED_CIDRS
            var added = 0
            for (route in targets) {
                runCatching {
                    builder.addRoute(route.address, route.prefixLength)
                    added++
                }.onFailure {
                    Log.w(TAG, "Failed to add route ${route.address}/${route.prefixLength}", it)
                }
            }
            if (added > 0) {
                Log.d(TAG, "Applied $added selective routes")
                return added
            }
            Log.w(TAG, "No selective routes were successfully added, falling back to 0.0.0.0/0")
        }
        // Default: Full tunnel (0.0.0.0/0)
        builder.addRoute("0.0.0.0", 0)
        return 1
    }
}
