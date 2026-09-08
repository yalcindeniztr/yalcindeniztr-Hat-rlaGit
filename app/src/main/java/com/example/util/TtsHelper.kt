package com.example.util

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.speech.tts.UtteranceProgressListener
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

    fun speak(context: Context, text: String, onDone: (() -> Unit)? = null) {
        val appContext = context.applicationContext
        val cleanText = sanitizeForSpeech(text)
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
                                    executeSpeak(engine, t, onDone)
                                    pendingText = null
                                }
                            }
                        } else {
                            Log.w(TAG, "TextToSpeech initialization failed with code $status")
                        }
                    }
                } else if (isInitialized) {
                    tts?.let { engine ->
                        executeSpeak(engine, cleanText, onDone)
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

    fun sanitizeForSpeech(rawText: String): String {
        var text = rawText
        
        // 1. Kod blokları, JSON eylem blokları ve action taglerini tamamen kaldır
        text = text.replace(Regex("""(?s)```[a-zA-Z0-9_-]*\s*[\s\S]*?```"""), " ")
        text = text.replace(Regex("""(?s)<(?:action|json|code)>[\s\S]*?</(?:action|json|code)>"""), " ")

        // Süslü parantezli ({ ... }) tüm JSON veya kod bloklarını (iç içe olsa dahi) temizle
        var braceStart = text.indexOf('{')
        var loopGuard = 0
        while (braceStart != -1 && loopGuard < 20) {
            loopGuard++
            var depth = 0
            var braceEnd = -1
            for (i in braceStart until text.length) {
                if (text[i] == '{') depth++
                else if (text[i] == '}') {
                    depth--
                    if (depth == 0) {
                        braceEnd = i
                        break
                    }
                }
            }
            if (braceEnd != -1) {
                text = text.substring(0, braceStart) + " " + text.substring(braceEnd + 1)
            } else {
                text = text.replace("{", "")
                break
            }
            braceStart = text.indexOf('{')
        }

        text = text.replace(Regex("""(?i)\b(?:action_type|action_step|payload|target_package|coords|timestamp|status|action|code|json)\s*:\s*[^,\n\}]+"""), " ")

        // 2. Metin başındaki olası teknik prefix, kod veya etiket kalıntılarını temizle
        text = text.replace(Regex("""^(?:```[a-zA-Z0-9_-]*|```|\[[a-zA-Z0-9_-]+\]|CODE:|ACTION:)\s*""", RegexOption.IGNORE_CASE), "")

        // 3. Markdown ve biçimlendirme işaretlerini kaldır (*, **, #, _, ~, `, >, =)
        text = text.replace(Regex("""[*#_~`>=]+"""), " ")
        text = text.replace(Regex("""\[(.*?)\]\(.*?\)"""), "$1")

        // 4. Emojileri ve Unicode sembollerini temizle
        text = text.replace(Regex("""[\uD83C-\uDBFF\uDC00-\uDFFF]+"""), " ")
        text = text.replace(Regex("""[\u2600-\u27BF]+"""), " ")

        // 5. ASCII gülen yüzleri ve emoticonları temizle (yıldız/gülen yüz denmesini önler)
        val smileys = listOf(
            ":)", ":-)", ";)", ";-)", ":D", ":-D", ":P", ":-P", 
            ":(", ":-(", ":/", ":\\", "<3", "^^", "XD", "xD", "O_o", "o_O",
            ":o", ":O", ";D", "B)", "8)"
        )
        for (smiley in smileys) {
            text = text.replace(smiley, "")
        }

        // 6. Madde imlerini akıcı hale getir
        text = text.replace(Regex("""(?m)^\s*[•\-\*]\s*"""), "")
        text = text.replace(Regex("""(?m)^\s*\d+\.\s*"""), "")

        // 7. Sıcaklık ve birim okunuşlarını düzelt
        text = text.replace("°C", " derece")
        text = text.replace("km/s", " kilometre bölü saat")
        text = text.replace("%", "yüzde ")

        // 8. Parantez içi dosya yolları veya teknik kodları temizle
        text = text.replace(Regex("""\([a-zA-Z0-9_/\\.-]{5,}\)"""), "")

        // 9. Başta kalan noktalama işaretlerini ve boşlukları temizle (Türkçe harfleri korur: \p{L})
        text = text.replace(Regex("""^[^\p{L}\p{N}]+"""), "")

        // 10. Çoklu boşlukları temizle
        text = text.replace(Regex("""\s+"""), " ").trim()

        return text
    }

    private fun executeSpeak(engine: TextToSpeech, text: String, onDone: (() -> Unit)? = null) {
        engine.setPitch(0.80f)
        engine.setSpeechRate(0.98f)
        
        val utteranceId = "HatirlaGitMaleVoiceUtterance_" + System.currentTimeMillis()
        if (onDone != null) {
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(id: String?) {
                    if (id == utteranceId) {
                        Handler(Looper.getMainLooper()).post {
                            onDone.invoke()
                        }
                    }
                }
                override fun onError(utteranceId: String?) {
                    if (utteranceId == utteranceId) {
                        Handler(Looper.getMainLooper()).post {
                            onDone.invoke()
                        }
                    }
                }
            })
        }
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
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
