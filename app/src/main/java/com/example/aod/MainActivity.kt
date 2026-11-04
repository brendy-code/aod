package com.example.aod

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Typeface
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextClock
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var box: LinearLayout
    private lateinit var time: TextClock
    private lateinit var date: TextClock
    private lateinit var bat: TextView

    private val handler = Handler(Looper.getMainLooper())
    private var style = 0
    private var level = 1
    private val brightness = floatArrayOf(0.01f, 0.08f, 0.3f)
    private val textAlpha = floatArrayOf(0.45f, 0.65f, 0.9f)

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return
            val lvl = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
            if (lvl >= 0) {
                bat.text = "${lvl * 100 / scale}%" + if (charging) " charging" else ""
            }
        }
    }

    // Moves the whole clock a little every minute so no pixel stays lit forever (burn-in).
    private val shifter = object : Runnable {
        override fun run() {
            val d = resources.displayMetrics.density
            box.animate()
                .translationX(Random.nextInt(-20, 21) * d)
                .translationY(Random.nextInt(-30, 31) * d)
                .setDuration(3000)
                .start()
            handler.postDelayed(this, 60_000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= 27) setShowWhenLocked(true)
        if (Build.VERSION.SDK_INT >= 28) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        val thin = Typeface.create("sans-serif-thin", Typeface.NORMAL)
        val light = Typeface.create("sans-serif-light", Typeface.NORMAL)
        val ink = Color.parseColor("#D9D3C5")

        time = TextClock(this).apply {
            typeface = thin
            setTextColor(ink)
            gravity = Gravity.CENTER
            includeFontPadding = false
        }
        date = TextClock(this).apply {
            typeface = light
            setTextColor(ink)
            format12Hour = "EEEE, MMMM d"
            format24Hour = "EEEE, MMMM d"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, 0)
        }
        bat = TextView(this).apply {
            typeface = light
            setTextColor(ink)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, 0)
        }

        box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            addView(time)
            addView(date)
            addView(bat)
        }

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            addView(
                box,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER
                )
            )
            // Tap = next style, long press = next brightness level
            setOnClickListener { style = (style + 1) % 3; applyStyle() }
            setOnLongClickListener { level = (level + 1) % 3; applyLevel(); true }
        }
        setContentView(root)

        applyStyle()
        applyLevel()
    }

    override fun onStart() {
        super.onStart()
        ContextCompat.registerReceiver(
            this, batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        handler.post(shifter)
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(batteryReceiver)
        handler.removeCallbacks(shifter)
    }

    private fun applyStyle() {
        when (style) {
            0 -> {
                time.format12Hour = "h:mm"; time.format24Hour = "h:mm"
                time.setTextSize(TypedValue.COMPLEX_UNIT_SP, 96f)
                time.setLineSpacing(0f, 1f)
                date.visibility = View.VISIBLE; bat.visibility = View.VISIBLE
            }
            1 -> {
                time.format12Hour = "h'\n'mm"; time.format24Hour = "h'\n'mm"
                time.setTextSize(TypedValue.COMPLEX_UNIT_SP, 130f)
                time.setLineSpacing(0f, 0.85f)
                date.visibility = View.VISIBLE; bat.visibility = View.VISIBLE
            }
            else -> {
                time.format12Hour = "h:mm"; time.format24Hour = "h:mm"
                time.setTextSize(TypedValue.COMPLEX_UNIT_SP, 48f)
                time.setLineSpacing(0f, 1f)
                date.visibility = View.GONE; bat.visibility = View.GONE
            }
        }
    }

    private fun applyLevel() {
        val lp = window.attributes
        lp.screenBrightness = brightness[level]
        window.attributes = lp
        box.alpha = textAlpha[level]
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
