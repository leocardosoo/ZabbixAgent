package com.example.myapplication

import android.content.Context

object ZabbixConfig {
    private const val PREFS_NAME = "zabbix_config"

    fun save(context: Context, url: String, token: String, hostName: String, intervalMs: Long) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString("api_url", url)
            .putString("api_token", token)
            .putString("host_name", hostName)
            .putLong("interval_ms", intervalMs)
            .apply()
    }

    fun getUrl(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString("api_url", "") ?: ""

    fun getToken(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString("api_token", "") ?: ""

    fun getHostName(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString("host_name", "Android Phone") ?: "Android Phone"

    fun getInterval(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong("interval_ms", 5000L)
}