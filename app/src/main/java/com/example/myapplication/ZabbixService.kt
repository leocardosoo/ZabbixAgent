package com.example.myapplication

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

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

        startForeground(NOTIFICATION_ID, buildNotification("Zabbix Agent running..."))

        if (running) return START_STICKY

        running = true

        val apiUrl = ZabbixConfig.getUrl(this)
        val apiToken = ZabbixConfig.getToken(this)
        val hostName = ZabbixConfig.getHostName(this)
        val interval = ZabbixConfig.getInterval(this)
        val collector = MetricsCollector(this)

        thread = Thread {
            Log.d("ZabbixAgent", "Thread started!")
            while (running) {
                try {
                    val metrics = collector.collect()
                    Log.d("ZabbixAgent", "Sending ${metrics.size} metrics")
                    ZabbixSender.send(apiUrl, apiToken, hostName, metrics)
                    Thread.sleep(interval)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                } catch (e: Exception) {
                    Log.e("ZabbixAgent", "Loop error: ${e.message}", e)
                }
            }
            Log.d("ZabbixAgent", "Thread finished")
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

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Zabbix Agent",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Background monitoring" }
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
            .addAction(android.R.drawable.ic_delete, "Stop", stopIntent)
            .setOngoing(true)
            .build()
    }
}