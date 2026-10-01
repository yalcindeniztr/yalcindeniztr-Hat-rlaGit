package com.example.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import kotlin.math.abs

/**
 * ATİLLA Floating 3D Hologram Bubble Service.
 * Renders an always-on-top interactive floating assistant bubble.
 * Supports fluid dragging, touch gestures (Single tap = Listen, Long tap = Screen Vision).
 */
class AtillaFloatingBubbleService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var bubbleImageView: ImageView? = null

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var lastClickTime = 0L

    companion object {
        private const val TAG = "AtillaFloatingBubble"
        private const val NOTIF_CHANNEL_ID = "ATILLA_FLOATING_BUBBLE_CHANNEL"
        private const val NOTIF_ID = 9988

        const val ACTION_START = "ACTION_START_BUBBLE"
        const val ACTION_STOP = "ACTION_STOP_BUBBLE"

        var isRunning = false
            private set

        fun startBubble(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                Log.w(TAG, "SYSTEM_ALERT_WINDOW permission not granted")
                return
            }
            val intent = Intent(context, AtillaFloatingBubbleService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopBubble(context: Context) {
            val intent = Intent(context, AtillaFloatingBubbleService::class.java).apply {
                action = ACTION_STOP
            }
            context.stopService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        startForegroundNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (floatingView == null) {
            initFloatingView()
        }

        return START_STICKY
    }

    private fun startForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIF_CHANNEL_ID,
                "ATİLLA Yüzen Asistan Küresi",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "ATİLLA Canlı Hologram Küresi Arka Plan Servisi"
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(channel)
        }

        val openIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, NOTIF_CHANNEL_ID)
            .setContentTitle("ATİLLA Hologram Küresi Aktif")
            .setContentText("Küreye dokunarak anında sesli komut veya ekran taraması başlatabilirsiniz.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(NOTIF_ID, notification)
    }

    private fun initFloatingView() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val sizePx = (64 * resources.displayMetrics.density).toInt()

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 50
            y = 250
        }

        val container = FrameLayout(this).apply {
            val backgroundGlow = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#E6091B33")) // Yarı saydam gece mavisi
                setStroke((2.5f * resources.displayMetrics.density).toInt(), Color.parseColor("#4DE8F4")) // Cyan Jarvis neon çerçeve
            }
            background = backgroundGlow
            elevation = 16f
        }

        bubbleImageView = ImageView(this).apply {
            setImageResource(R.drawable.ic_jarvis_logo)
            scaleType = ImageView.ScaleType.FIT_CENTER
            val padding = (8 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
        }

        container.addView(
            bubbleImageView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        container.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager?.updateViewLayout(container, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val diffX = abs(event.rawX - initialTouchX)
                    val diffY = abs(event.rawY - initialTouchY)
                    if (diffX < 12 && diffY < 12) {
                        onBubbleClicked()
                    }
                    true
                }
                else -> false
            }
        }

        container.setOnLongClickListener {
            onBubbleLongClicked()
            true
        }

        floatingView = container
        try {
            windowManager?.addView(floatingView, params)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding floating bubble window", e)
        }
    }

    /**
     * Tek Dokunuş: Asistanı dinleme modunda açar
     */
    private fun onBubbleClicked() {
        val now = System.currentTimeMillis()
        if (now - lastClickTime < 400) {
            // Çift tık: Ekran Gözü Analizi
            triggerScreenVision()
        } else {
            // Tek tık: Sesli Komut Modu
            val launchIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("action", "OPEN_AI_ASSISTANT")
                putExtra("auto_listen", true)
                putExtra("EXTRA_AUTO_LISTEN", true)
            }
            startActivity(launchIntent)
        }
        lastClickTime = now
    }

    /**
     * Uzun Dokunuş: Ekran Gözü Canlı Tarama ve Soru Çözümü
     */
    private fun onBubbleLongClicked() {
        triggerScreenVision()
    }

    private fun triggerScreenVision() {
        TtsHelper.speak(this, "Ekran taranıyor patron, hemen inceliyorum...")
        val insight = AtillaScreenVisionHelper.analyzeCurrentScreen(this)
        TtsHelper.speak(this, insight.spokenReply)
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        floatingView?.let {
            try {
                windowManager?.removeView(it)
            } catch (_: Exception) {}
        }
        floatingView = null
    }
}
