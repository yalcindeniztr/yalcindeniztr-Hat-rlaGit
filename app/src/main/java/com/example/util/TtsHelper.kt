package com.example.util

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import java.util.Locale

/**
 * Enterprise-grade Helper to speak reminder titles, alarms, and AI responses using Turkish Male Voice.
 */
object TtsHelper {
    private const val TAG = "TtsHelper"
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var pendingText: String? = null

    private val FEMALE_KEYWORDS = listOf("female", "woman", "kadin", "bayan", "dff", "dfz", "dfa", "f0", "f1", "f2")
    private val MALE_KEYWORDS = listOf("male", "man", "erkek", "tfe", "tfa", "ter", "m0", "m1", "m2", "tr-tr-x-tfe", "tr-tr-x-tfa")

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
                                configureMaleVoice(engine)
                                isInitialized = true
                                pendingText?.let { t ->
                                    executeSpeak(engine, t)
                                    pendingText = null
                                }
                            }
                        } else {
                            Log.w(TAG, "TextToSpeech initialization failed with code $status")
                        }
                    }
                } else if (isInitialized) {
                    tts?.let { engine ->
                        executeSpeak(engine, cleanText)
                    }
                } else {
                    pendingText = cleanText
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error initiating speech synthesis", e)
            }
        }
    }

    private fun configureMaleVoice(engine: TextToSpeech) {
        try {
            val turkishLocale = Locale("tr", "TR")
            engine.setLanguage(turkishLocale)

            val allVoices = engine.voices
            if (!allVoices.isNullOrEmpty()) {
                val trVoices = allVoices.filter { it.locale.language == "tr" }

                // 1. Öncelik: Açıkça erkek anahtar kelimesi içeren ve kadın etiketi barındırmayan Türkçe ses
                var selectedVoice: Voice? = trVoices.firstOrNull { voice ->
                    val nameLower = voice.name.lowercase(Locale.ROOT)
                    val isMale = MALE_KEYWORDS.any { nameLower.contains(it) } || voice.features.any { it.contains("male", ignoreCase = true) }
                    val isFemale = FEMALE_KEYWORDS.any { nameLower.contains(it) } || voice.features.any { it.contains("female", ignoreCase = true) }
                    isMale && !isFemale
                }

                // 2. Öncelik: Kadın etiketi taşımayan herhangi bir Türkçe ses
                if (selectedVoice == null) {
                    selectedVoice = trVoices.firstOrNull { voice ->
                        val nameLower = voice.name.lowercase(Locale.ROOT)
                        val isFemale = FEMALE_KEYWORDS.any { nameLower.contains(it) } || voice.features.any { it.contains("female", ignoreCase = true) }
                        !isFemale
                    }
                }

                if (selectedVoice != null) {
                    engine.voice = selectedVoice
                    Log.d(TAG, "Selected Male Voice: ${selectedVoice.name}")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Voice configuration exception", e)
        }

        // Derin, tok ve karizmatik erkek tonu
        engine.setPitch(0.80f)
        engine.setSpeechRate(0.98f)
    }

    private fun executeSpeak(engine: TextToSpeech, text: String) {
        engine.setPitch(0.80f)
        engine.setSpeechRate(0.98f)
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "HatirlaGitMaleVoiceUtterance")
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
