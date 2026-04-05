package com.example.myapplication

import android.util.Log
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object ZabbixSender {

    fun send(apiUrl: String, apiToken: String, hostName: String, metrics: List<Pair<String, String>>) {
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
            Log.d("ZabbixSender", "HTTP $code -> $body")
        } catch (e: Exception) {
            Log.e("ZabbixSender", "HTTP error: ${e.message}", e)
        }
    }
}