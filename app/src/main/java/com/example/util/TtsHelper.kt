package com.example.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/**
 * Enterprise-grade Helper to speak reminder titles, alarms, and alerts out loud using Android TextToSpeech.
 */
object TtsHelper {
    private const val TAG = "TtsHelper"
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var pendingText: String? = null

    fun speak(context: Context, text: String) {
        val appContext = context.applicationContext
        val cleanText = text.trim()
        if (cleanText.isBlank()) return

        Handler(Looper.getMainLooper()).post {
            try {
                if (tts == null) {
                    pendingText = cleanText
                    tts = TextToSpeech(appContext) { status ->
                        if (status == TextToSpeech.SUCCESS) {
                            tts?.let { engine ->
                                val turkishLocale = Locale("tr", "TR")
                                val result = engine.setLanguage(turkishLocale)
                                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                                    engine.setLanguage(Locale.getDefault())
                                }
                                engine.setSpeechRate(0.95f)
                                engine.setPitch(1.0f)
                                isInitialized = true
                                pendingText?.let { t ->
                                    engine.speak(t, TextToSpeech.QUEUE_FLUSH, null, "ReminderTtsUtterance")
                                    pendingText = null
                                }
                            }
                        } else {
                            Log.w(TAG, "TextToSpeech initialization failed with code $status")
                        }
                    }
                } else if (isInitialized) {
                    tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "ReminderTtsUtterance")
                } else {
                    pendingText = cleanText
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error initiating speech synthesis", e)
            }
        }
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping TTS", e)
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            isInitialized = false
            pendingText = null
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down TTS", e)
        }
    }
}
