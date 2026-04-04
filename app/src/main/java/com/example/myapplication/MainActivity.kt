package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("zabbix_config", Context.MODE_PRIVATE)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }

        val title = TextView(this).apply {
            text = "Zabbix Agent"
            textSize = 24f
            setPadding(0, 0, 0, 32)
        }

        val inputUrl = EditText(this).apply {
            hint = "http://yourserver/zabbix/api_jsonrpc.php"
            setText(prefs.getString("api_url", ""))
        }

        val inputToken = EditText(this).apply {
            hint = "token"
            setText(prefs.getString("api_token", ""))
        }

        val inputHost = EditText(this).apply {
            hint = "Device name"
            setText(prefs.getString("host_name", "Android Phone"))
        }

        val inputInterval = EditText(this).apply {
            hint = "5"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText("${prefs.getLong("interval_ms", 5000L) / 1000}")
        }

        val btnStart = Button(this).apply { text = "▶ start" }
        val btnStop = Button(this).apply { text = "■ stop" }

        val status = TextView(this).apply {
            text = "Status: stopped"
        }

        btnStart.setOnClickListener {
            val url = inputUrl.text.toString().trim()
            val token = inputToken.text.toString().trim()
            val host = inputHost.text.toString().ifEmpty { "Android Phone" }
            val interval = (inputInterval.text.toString().toLongOrNull() ?: 5L) * 1000L

            if (url.isEmpty() || token.isEmpty()) {
                Toast.makeText(this, "Preencha a URL e o Token!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            prefs.edit()
                .putString("api_url", url)
                .putString("api_token", token)
                .putString("host_name", host)
                .putLong("interval_ms", interval)
                .apply()

            // Inicia direto (sem delay)
            ContextCompat.startForegroundService(
                this,
                Intent(this, ZabbixService::class.java)
            )

            status.text = "Status: rodando ✅"
        }

        // ✅ STOP CORRETO
        btnStart.setOnClickListener {
            val url = inputUrl.text.toString().trim()
            val token = inputToken.text.toString().trim()
            val host = inputHost.text.toString().trim().ifEmpty { "Android Phone" }
            val interval = (inputInterval.text.toString().toLongOrNull() ?: 5L) * 1000L

            if (url.isEmpty() || token.isEmpty()) {
                Toast.makeText(this, "Preencha a URL e o Token!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            prefs.edit()
                .putString("api_url", url)
                .putString("api_token", token)
                .putString("host_name", host)
                .putLong("interval_ms", interval)
                .apply()

            val serviceIntent = Intent(this, ZabbixService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }

            status.text = "Status: rodando ✅"
            Toast.makeText(this, "Agente iniciado!", Toast.LENGTH_SHORT).show()
        }

        layout.addView(title)
        layout.addView(inputUrl)
        layout.addView(inputToken)
        layout.addView(inputHost)
        layout.addView(inputInterval)
        layout.addView(btnStart)
        layout.addView(btnStop)
        layout.addView(status)

        setContentView(ScrollView(this).apply { addView(layout) })
    }
}