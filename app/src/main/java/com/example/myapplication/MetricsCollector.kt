package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.net.wifi.WifiManager
import android.os.*
import android.util.Log
import java.io.File

class MetricsCollector(private val context: Context) {

    fun collect(): List<Pair<String, String>> {
        val metrics = mutableListOf<Pair<String, String>>()

        collectBattery(metrics)
        collectMemory(metrics)
        collectStorage(metrics)
        collectCpu(metrics)
        collectNetwork(metrics)
        collectScreen(metrics)

        return metrics
    }

    private fun collectBattery(metrics: MutableList<Pair<String, String>>) {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        metrics.add("battery.level" to "${if (level >= 0 && scale > 0) level * 100 / scale else -1}")

        val tempRaw = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        metrics.add("battery.temperature" to "${if (tempRaw >= 0) tempRaw / 10.0 else -1.0}")

        val voltage = (intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) ?: -1) / 1000.0
        metrics.add("battery.voltage" to String.format("%.2f", voltage))

        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
        metrics.add("battery.charging" to if (isCharging) "1" else "0")

        val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        metrics.add("battery.charging.type" to when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> "Unplugged"
        })
    }

    private fun collectMemory(metrics: MutableList<Pair<String, String>>) {
        try {
            val lines = File("/proc/meminfo").readLines()
            val total = lines.first { it.startsWith("MemTotal") }
                .replace(Regex("[^0-9]"), "").toLong() * 1024
            val free = lines.first { it.startsWith("MemAvailable") }
                .replace(Regex("[^0-9]"), "").toLong() * 1024
            val used = total - free
            metrics.add("ram.total.kb" to "$total")
            metrics.add("ram.free.kb" to "$free")
            metrics.add("ram.used.kb" to "$used")
            metrics.add("ram.used.pct" to String.format("%.1f", used * 100.0 / total))
        } catch (e: Exception) {
            Log.w("MetricsCollector", "Error reading RAM: ${e.message}")
        }
    }

    private fun collectStorage(metrics: MutableList<Pair<String, String>>) {
        val statFs = StatFs(context.filesDir.path)
        val blockSize = statFs.blockSizeLong
        val free = statFs.availableBlocksLong * blockSize
        val total = statFs.blockCountLong * blockSize
        val used = total - free
        metrics.add("storage.free.kb" to "$free")
        metrics.add("storage.total.kb" to "$total")
        metrics.add("storage.used.kb" to "$used")
        metrics.add("storage.used.pct" to String.format("%.1f", used * 100.0 / total))
    }

    private fun collectCpu(metrics: MutableList<Pair<String, String>>) {
        metrics.add("cpu.usage.pct" to String.format("%.1f", getCpuUsage()))
        metrics.add("device.uptime.seconds" to "${SystemClock.elapsedRealtime() / 1000}")
    }

    private fun collectNetwork(metrics: MutableList<Pair<String, String>>) {
        try {
            val wifiManager = context.applicationContext
                .getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo = wifiManager.connectionInfo
            metrics.add("wifi.signal.rssi" to "${wifiInfo.rssi}")
            metrics.add("wifi.signal.quality" to
                    "${WifiManager.calculateSignalLevel(wifiInfo.rssi, 100)}")
            metrics.add("wifi.ssid" to (wifiInfo.ssid?.replace("\"", "") ?: "unknown"))
            metrics.add("wifi.speed.mbps" to "${wifiInfo.linkSpeed}")
        } catch (_: Exception) {}

        try {
            val connManager = context
                .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val capabilities = connManager.getNetworkCapabilities(connManager.activeNetwork)
            metrics.add("network.type" to when {
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "WiFi"
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Mobile"
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "Ethernet"
                else -> "None"
            })
        } catch (_: Exception) {}

        metrics.add("network.rx.kb" to "${TrafficStats.getTotalRxBytes()}")
        metrics.add("network.tx.kb" to "${TrafficStats.getTotalTxBytes()}")
    }

    private fun collectScreen(metrics: MutableList<Pair<String, String>>) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        metrics.add("screen.on" to if (powerManager.isInteractive) "1" else "0")
    }

    private fun getCpuUsage(): Double {
        return try {
            val pid = Process.myPid()
            val f1 = File("/proc/$pid/stat").readText().split(" ")
            val t1 = f1[13].toLong() + f1[14].toLong()
            Thread.sleep(200)
            val f2 = File("/proc/$pid/stat").readText().split(" ")
            val t2 = f2[13].toLong() + f2[14].toLong()
            (t2 - t1).toDouble()
        } catch (e: Exception) {
            -1.0
        }
    }
}