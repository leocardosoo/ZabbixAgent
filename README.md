# Zabbix Android Agent

A lightweight Android app written in Kotlin that turns your phone into a Zabbix monitoring agent. It collects device metrics and sends them directly to a Zabbix server via the JSON-RPC API every few seconds — even when the app is closed.

---

## Features

- Runs in the background as a Foreground Service
- Persistent notification with a quick Stop button
- Simple configuration screen (no hardcoded values)
- Sends data via Zabbix API using Bearer token authentication
- Survives app close — keeps sending metrics in background

---

## Metrics Collected

| Key | Description | Unit |
|-----|-------------|------|
| `battery.level` | Battery percentage | % |
| `battery.temperature` | Battery temperature | °C |
| `battery.voltage` | Battery voltage | V |
| `battery.charging` | Charging status (1/0) | - |
| `battery.charging.type` | Charging type (USB/AC/Wireless) | - |
| `ram.free.kb` | Available RAM | Bytes |
| `ram.total.kb` | Total RAM | Bytes |
| `ram.used.kb` | Used RAM | Bytes |
| `ram.used.pct` | RAM usage percentage | % |
| `storage.free.kb` | Free internal storage | Bytes |
| `storage.total.kb` | Total internal storage | Bytes |
| `storage.used.kb` | Used internal storage | Bytes |
| `storage.used.pct` | Storage usage percentage | % |
| `cpu.usage.pct` | CPU usage (app process) | - |
| `device.uptime.seconds` | Device uptime since last boot | Seconds |
| `wifi.signal.rssi` | WiFi signal strength | dBm |
| `wifi.signal.quality` | WiFi signal quality | % |
| `wifi.ssid` | Connected WiFi network name | - |
| `wifi.speed.mbps` | WiFi link speed | Mbps |
| `network.type` | Connection type (WiFi/Mobile/Ethernet) | - |
| `network.rx.kb` | Total bytes received | Bytes |
| `network.tx.kb` | Total bytes sent | Bytes |
| `screen.on` | Screen status (1=on, 0=off) | - |

---

## Requirements

- Android 8.0 (API 26) or higher
- Zabbix 7.0+ (uses `history.push` method)
- Phone and Zabbix server on the same network

---

## Zabbix Setup

### 1. Create a Host
- Go to **Configuration → Hosts → Create host**
- Host name: `Android Phone` (or whatever you prefer)
- Add it to a group

### 2. Create an API Token
- Go to **User settings → API tokens → Create API token**
- Copy the generated token

### 3. Create Items
For each metric, create an item with:
- **Type:** Zabbix trapper
- **Key:** exactly as listed in the metrics table above
- **Type of information:** Numeric (unsigned), Numeric (float), or Text depending on the metric

---

## App Setup

1. Install the app on your Android device
2. Open the app
3. Fill in the configuration screen:
   - **Zabbix API URL:** `http://YOUR_SERVER_IP/zabbix/api_jsonrpc.php`
   - **API Token:** paste the token generated in Zabbix
   - **Host name:** must match exactly the host name created in Zabbix
   - **Interval:** how often to send metrics (in seconds)
4. Tap **Start**
5. The agent will keep running in the background

---

## Permissions Required

| Permission | Reason |
|------------|--------|
| `INTERNET` | Send data to Zabbix server |
| `ACCESS_NETWORK_STATE` | Read network connection type |
| `ACCESS_WIFI_STATE` | Read WiFi signal and SSID |
| `ACCESS_FINE_LOCATION` | Required to read WiFi SSID on Android 10+ |
| `FOREGROUND_SERVICE` | Run in background |
| `FOREGROUND_SERVICE_DATA_SYNC` | Required on Android 14+ |
| `POST_NOTIFICATIONS` | Show persistent notification |

---

## Project Structure


---

## Known Limitations

- **Storage:** reads only the app data partition, not the full device storage
- **CPU:** measures only the app process usage, not total system CPU
- **WiFi SSID:** shows `<unknown ssid>` if location permission is denied
- **Battery voltage:** reported in Volts (converted from mV)
- Phone and server must be on the same network (or reachable via IP)

---

## Personal Project

This is a personal learning project built to explore Android development and Zabbix integration. It is not intended for production use.

Feel free to fork, modify, and improve it!
