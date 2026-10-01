package com.example.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import java.util.Locale

object AtillaWakeWordHelper {

    private val WAKE_PATTERNS = listOf(
        "hey atilla", "hey atila", "atilla", "atila", "atilla dinle", "atila dinle", "hey jarvis"
    )

    /**
     * Verilen metnin içinde "Hey ATİLLA" uyandırma ifadesi olup olmadığını denetler.
     */
    fun matchesWakeWord(rawText: String): Boolean {
        val clean = rawText.lowercase(Locale.forLanguageTag("tr-TR")).trim()
        return WAKE_PATTERNS.any { pattern ->
            clean == pattern || clean.startsWith("$pattern ") || clean.contains(" $pattern ") || clean.endsWith(" $pattern")
        }
    }

    /**
     * Uyandırma ifadesi sonrasındaki asıl komut cümlesini ayıklar.
     * Örn: "Hey Atilla feneri aç" ➔ "feneri aç"
     */
    fun extractCommandAfterWakeWord(rawText: String): String {
        var clean = rawText.trim()
        val regex = Regex("""(?i)^(hey\s+)?(atilla|atila|jarvis|usta|asistan|atilla\s+dinle|atila\s+dinle)[,\s!.:]*""")
        clean = clean.replace(regex, "").trim()
        return clean
    }

    /**
     * Dokunsal bildirim: "Hey ATİLLA" algılandığında hafif çift titreşim verir.
     */
    fun triggerHapticFeedback(context: Context) {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 70, 50, 90), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(100)
            }
        } catch (_: Exception) {}
    }
}

/**
 * 7/24 veya Arka Planda "Hey ATİLLA" Dinleyen Ön Plan Servisi (Foreground Service)
 */
class AtillaWakeWordService : Service(), RecognitionListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildForegroundNotification())
        initSpeechRecognizer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startContinuousListening()
        return START_STICKY
    }

    private fun initSpeechRecognizer() {
        try {
            speechRecognizer?.destroy()
            if (SpeechRecognizer.isRecognitionAvailable(this)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
                    setRecognitionListener(this@AtillaWakeWordService)
                }
            }
        } catch (_: Exception) {}
    }

    private fun startContinuousListening() {
        if (isListening || speechRecognizer == null) return
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            speechRecognizer?.startListening(intent)
            isListening = true
        } catch (_: Exception) {
            isListening = false
        }
    }

    override fun onResults(results: Bundle?) {
        isListening = false
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = matches?.firstOrNull() ?: ""
        handleDetectedSpeech(text)
        // Sürekli dinlemeyi yeniden başlat
        startContinuousListening()
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = matches?.firstOrNull() ?: ""
        if (AtillaWakeWordHelper.matchesWakeWord(text)) {
            AtillaWakeWordHelper.triggerHapticFeedback(this)
        }
    }

    private fun handleDetectedSpeech(text: String) {
        if (AtillaWakeWordHelper.matchesWakeWord(text)) {
            val verification = AtillaVoiceprintVerifier.verifySpeaker(this)
            if (!verification.isAuthorized) {
                android.util.Log.w("AtillaWakeWordService", "Wake word speaker rejected: ${verification.reason}")
                return
            }

            AtillaWakeWordHelper.triggerHapticFeedback(this)
            val command = AtillaWakeWordHelper.extractCommandAfterWakeWord(text)
            
            // MainActivity'yi sesli dinleme ve komut çalıştırma modunda ön plana getir
            val launchIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("EXTRA_VOICE_COMMAND", command)
                putExtra("EXTRA_AUTO_LISTEN", command.isBlank())
            }
            startActivity(launchIntent)
        }
    }

    override fun onError(error: Int) {
        isListening = false
        // Kısa bir beklemeden sonra dinleyiciyi yeniden başlat
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            startContinuousListening()
        }, 1000L)
    }

    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() { isListening = false }
    override fun onEvent(eventType: Int, params: Bundle?) {}

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
        speechRecognizer = null
        isListening = false
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ATİLLA Sesli Uyandırma Servisi",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "'Hey ATİLLA' sesli uyandırma motoru arka planda hazır bekliyor."
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ATİLLA Sesli Dinleme Aktif")
            .setContentText("'Hey ATİLLA' dediğinizde eller serbest devreye girer.")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "atilla_wake_word_channel"
        private const val NOTIFICATION_ID = 20261

        fun start(context: Context) {
            val intent = Intent(context, AtillaWakeWordService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AtillaWakeWordService::class.java))
        }
    }
}
