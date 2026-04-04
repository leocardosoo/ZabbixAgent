package com.example.myapplication

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.net.wifi.WifiManager
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.File
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class ZabbixService : Service() {

    private val CHANNEL_ID = "zabbix_agent_channel"
    private val NOTIFICATION_ID = 1
    private var running = false
    private var thread: Thread? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification("Zabbix Agent rodando..."))

        if (running) {
            Log.d("ZabbixAgent", "Service já está rodando")
            return START_STICKY
        }

        running = true

        val prefs = getSharedPreferences("zabbix_config", Context.MODE_PRIVATE)
        val apiUrl = prefs.getString("api_url", "") ?: ""
        val apiToken = prefs.getString("api_token", "") ?: ""
        val hostName = prefs.getString("host_name", "Android Phone") ?: "Android Phone"
        val interval = prefs.getLong("interval_ms", 5000L)

        thread = Thread {
            Log.d("ZabbixAgent", "Thread iniciada!")
            while (running) {
                try {
                    val metrics = collectMetrics()
                    Log.d("ZabbixAgent", "Enviando ${metrics.size} métricas")
                    sendMetrics(apiUrl, apiToken, hostName, metrics)
                    Thread.sleep(interval)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                } catch (e: Exception) {
                    Log.e("ZabbixAgent", "Erro no loop: ${e.message}", e)
                }
            }
            Log.d("ZabbixAgent", "Thread finalizada")
        }.apply { start() }

        return START_STICKY
    }

    override fun onDestroy() {
        running = false
        thread?.interrupt()
        thread = null
        stopForeground(true)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null

    // ==================== COLETA ====================

    private fun collectMetrics(): List<Pair<String, String>> {
        val metrics = mutableListOf<Pair<String, String>>()
        val batteryIntent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        metrics.add("battery.level" to "${if (level >= 0 && scale > 0) level * 100 / scale else -1}")

        val tempRaw = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        metrics.add("battery.temperature" to "${if (tempRaw >= 0) tempRaw / 10.0 else -1.0}")

        val voltage = (batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) ?: -1) / 1000.0
        metrics.add("battery.voltage" to String.format("%.2f", voltage))

        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
        metrics.add("battery.charging" to if (isCharging) "1" else "0")

        val plugged = batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        metrics.add("battery.charging.type" to when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> "Unplugged"
        })

        val (ramTotal, ramFree, ramUsed) = getMemoryInfo()
        metrics.add("ram.free.kb" to "$ramFree")
        metrics.add("ram.total.kb" to "$ramTotal")
        metrics.add("ram.used.kb" to "$ramUsed")
        metrics.add("ram.used.pct" to String.format("%.1f", ramUsed * 100.0 / ramTotal))

        val statFs = StatFs(filesDir.path)
        val blockSize = statFs.blockSizeLong
        val storageFree = statFs.availableBlocksLong * blockSize
        val storageTotal = statFs.blockCountLong * blockSize
        val storageUsed = storageTotal - storageFree
        metrics.add("storage.free.kb" to "$storageFree")
        metrics.add("storage.total.kb" to "$storageTotal")
        metrics.add("storage.used.kb" to "$storageUsed")
        metrics.add("storage.used.pct" to String.format("%.1f", storageUsed * 100.0 / storageTotal))

        metrics.add("cpu.usage.pct" to String.format("%.1f", getCpuUsage()))
        metrics.add("device.uptime.seconds" to "${SystemClock.elapsedRealtime() / 1000}")

        try {
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo = wifiManager.connectionInfo
            metrics.add("wifi.signal.rssi" to "${wifiInfo.rssi}")
            metrics.add("wifi.signal.quality" to "${WifiManager.calculateSignalLevel(wifiInfo.rssi, 100)}")
            metrics.add("wifi.ssid" to (wifiInfo.ssid?.replace("\"", "") ?: "unknown"))
            metrics.add("wifi.speed.mbps" to "${wifiInfo.linkSpeed}")
        } catch (_: Exception) {}

        try {
            val connManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
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

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        metrics.add("screen.on" to if (powerManager.isInteractive) "1" else "0")

        return metrics
    }

    private fun getMemoryInfo(): Triple<Long, Long, Long> {
        return try {
            val lines = File("/proc/meminfo").readLines()
            val total = lines.first { it.startsWith("MemTotal") }
                .replace(Regex("[^0-9]"), "").toLong() * 1024
            val free = lines.first { it.startsWith("MemAvailable") }
                .replace(Regex("[^0-9]"), "").toLong() * 1024
            Triple(total, free, total - free)
        } catch (e: Exception) {
            Triple(-1L, -1L, -1L)
        }
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

    // ==================== ENVIO ====================

    private fun sendMetrics(apiUrl: String, apiToken: String, hostName: String, metrics: List<Pair<String, String>>) {
        val clock = System.currentTimeMillis() / 1000
        val paramsJson = metrics.joinToString(",") { (k, v) ->
            """{"host":"$hostName","key":"$k","value":"$v","clock":$clock}"""
        }
        val json = """{"jsonrpc":"2.0","method":"history.push","params":[$paramsJson],"id":1}"""

        try {
            val conn = URL(apiUrl).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer $apiToken")
            OutputStreamWriter(conn.outputStream).use { it.write(json) }
            val code = conn.responseCode
            val body = (if (code >= 400) conn.errorStream else conn.inputStream)
                .bufferedReader().use { it.readText() }
            Log.d("ZabbixAgent", "HTTP $code -> $body")
        } catch (e: Exception) {
            Log.e("ZabbixAgent", "Erro HTTP: ${e.message}", e)
        }
    }

    // ==================== NOTIFICAÇÃO ====================

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Zabbix Agent",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Monitoramento em background" }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, ZabbixService::class.java).apply { action = "STOP" },
            PendingIntent.FLAG_IMMUTABLE
        )
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Zabbix Agent")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_upload)
            .setContentIntent(openIntent)
            .addAction(android.R.drawable.ic_delete, "Parar", stopIntent)
            .setOngoing(true)
            .build()
    }
}