package com.example.myapplication

import android.content.Intent
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.*
import android.text.InputType
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#1F2226"))
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 80)
        }

        // ==================== HEADER ====================
        val accentBar = android.view.View(this).apply {
            setBackgroundColor(Color.parseColor("#D9400B"))
            layoutParams = LinearLayout.LayoutParams(80, 6).apply { bottomMargin = 32 }
        }

        val title = TextView(this).apply {
            text = "ZABBIX"
            textSize = 36f
            setTypeface(Typeface.MONOSPACE, Typeface.BOLD)
            setTextColor(Color.WHITE)
            letterSpacing = 0.2f
        }

        val subtitle = TextView(this).apply {
            text = "Android Agent  //  v1.0"
            textSize = 12f
            setTypeface(Typeface.MONOSPACE, Typeface.NORMAL)
            setTextColor(Color.parseColor("#D9400B"))
            setPadding(0, 8, 0, 64)
            letterSpacing = 0.1f
        }

        // ==================== HELPERS ====================
        fun makeSection(label: String): TextView {
            return TextView(this).apply {
                text = "── $label"
                textSize = 10f
                setTypeface(Typeface.MONOSPACE, Typeface.BOLD)
                setTextColor(Color.parseColor("#D9400B"))
                letterSpacing = 0.2f
                setPadding(0, 0, 0, 20)
            }
        }

        fun makeFieldLabel(text: String): TextView {
            return TextView(this).apply {
                this.text = text
                textSize = 10f
                setTypeface(Typeface.MONOSPACE, Typeface.NORMAL)
                setTextColor(Color.parseColor("#7A7F8E"))
                letterSpacing = 0.15f
                setPadding(0, 0, 0, 6)
            }
        }

        fun makeInput(hint: String, value: String, numeric: Boolean = false): EditText {
            return EditText(this).apply {
                this.hint = hint
                setText(value)
                textSize = 14f
                setTypeface(Typeface.MONOSPACE)
                setTextColor(Color.parseColor("#E8ECF4"))
                setHintTextColor(Color.parseColor("#3A3F4E"))
                if (numeric) inputType = InputType.TYPE_CLASS_NUMBER
                setPadding(32, 28, 32, 28)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 24 }
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#16191D"))
                    setStroke(1, Color.parseColor("#2E3238"))
                    cornerRadius = 4f
                }
            }
        }

        fun makeCard(block: LinearLayout.() -> Unit): LinearLayout {
            return LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(40, 36, 40, 36)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 32 }
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#292D32"))
                    setStroke(1, Color.parseColor("#33383F"))
                    cornerRadius = 8f
                }
                block()
            }
        }

        // ==================== INPUTS ====================
        val inputUrl = makeInput(
            "http://serverIP/zabbix/api_jsonrpc.php",
            ZabbixConfig.getUrl(this)
        )
        val inputToken = makeInput(
            "Paste your API token here",
            ZabbixConfig.getToken(this)
        )
        val inputHost = makeInput(
            "Android Phone",
            ZabbixConfig.getHostName(this)
        )
        val inputInterval = makeInput(
            "5",
            "${ZabbixConfig.getInterval(this) / 1000}",
            numeric = true
        )

        // ==================== CARDS ====================
        val cardServer = makeCard {
            addView(makeSection("SERVER CONFIG"))
            addView(makeFieldLabel("API ENDPOINT"))
            addView(inputUrl)
            addView(makeFieldLabel("AUTH TOKEN"))
            addView(inputToken)
        }

        val cardDevice = makeCard {
            addView(makeSection("DEVICE CONFIG"))
            addView(makeFieldLabel("HOST NAME"))
            addView(inputHost)
            addView(makeFieldLabel("SEND INTERVAL (seconds)"))
            addView(inputInterval)
        }

        // ==================== STATUS ====================
        val statusContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, 40)
        }

        val statusDot = TextView(this).apply {
            text = "●"
            textSize = 10f
            setTextColor(Color.parseColor("#4A4F5A"))
            setPadding(0, 0, 16, 0)
        }

        val statusText = TextView(this).apply {
            text = "AGENT OFFLINE"
            textSize = 11f
            setTypeface(Typeface.MONOSPACE, Typeface.BOLD)
            setTextColor(Color.parseColor("#4A4F5A"))
            letterSpacing = 0.15f
        }

        statusContainer.addView(statusDot)
        statusContainer.addView(statusText)

        // ==================== BUTTONS ====================
        val btnStart = Button(this).apply {
            text = "[ START AGENT ]"
            textSize = 13f
            setTypeface(Typeface.MONOSPACE, Typeface.BOLD)
            setTextColor(Color.WHITE)
            letterSpacing = 0.1f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 148
            ).apply { bottomMargin = 20 }
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#D9400B"))
                cornerRadius = 4f
            }
        }

        val btnStop = Button(this).apply {
            text = "[ STOP AGENT ]"
            textSize = 13f
            setTypeface(Typeface.MONOSPACE, Typeface.BOLD)
            setTextColor(Color.parseColor("#D9400B"))
            letterSpacing = 0.1f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 148
            )
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1F2226"))
                setStroke(1, Color.parseColor("#D9400B"))
                cornerRadius = 4f
            }
        }

        // ==================== ACTIONS ====================
        btnStart.setOnClickListener {
            val url = inputUrl.text.toString().trim()
            val token = inputToken.text.toString().trim()
            val host = inputHost.text.toString().trim().ifEmpty { "Android Phone" }
            val interval = (inputInterval.text.toString().toLongOrNull() ?: 5L) * 1000L

            if (url.isEmpty() || token.isEmpty()) {
                Toast.makeText(this, "Fill in the URL and Token!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            ZabbixConfig.save(this, url, token, host, interval)

            val serviceIntent = Intent(this, ZabbixService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }

            statusDot.setTextColor(Color.parseColor("#D9400B"))
            statusText.setTextColor(Color.parseColor("#D9400B"))
            statusText.text = "AGENT ONLINE"
            Toast.makeText(this, "Agent started!", Toast.LENGTH_SHORT).show()
        }

        btnStop.setOnClickListener {
            stopService(Intent(this, ZabbixService::class.java))
            statusDot.setTextColor(Color.parseColor("#4A4F5A"))
            statusText.setTextColor(Color.parseColor("#4A4F5A"))
            statusText.text = "AGENT OFFLINE"
            Toast.makeText(this, "Agent stopped!", Toast.LENGTH_SHORT).show()
        }

        // ==================== ASSEMBLE ====================
        root.addView(accentBar)
        root.addView(title)
        root.addView(subtitle)
        root.addView(cardServer)
        root.addView(cardDevice)
        root.addView(statusContainer)
        root.addView(btnStart)
        root.addView(btnStop)

        scroll.addView(root)
        setContentView(scroll)
    }
}