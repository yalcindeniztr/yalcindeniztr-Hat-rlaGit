package com.example.util

import java.util.Locale

/**
 * ATİLLA Emotional & Acoustic Sentiment Engine.
 * Analyzes conversational mood, stress, fatigue, joy, and instructional context
 * to adapt personality, empathetic preambles, and neural speech synthesis parameters.
 */
object AtillaEmotionEngine {

    enum class EmotionState {
        TIRED,          // Yorgun, tükenmiş
        STRESSED,       // Stresli, bunalmış, yetişemeyen
        JOYFUL,         // Neşeli, keyifli, enerjik
        URGENT,         // Acil, telaşlı, seri
        TEACHER_MAARIF, // Maarif modeli, ders, tarih, eğitimci modu
        WITTY,          // Esprili, eğlenceli sohbet
        NEUTRAL         // Asil, profesyonel, dengeli Jarvis tonu
    }

    data class EmotionAnalysis(
        val state: EmotionState,
        val confidence: Float,
        val empathyPreamble: String,
        val voicePitch: Float,
        val speechRate: Float,
        val explanation: String
    )

    private val TIRED_KEYWORDS = listOf(
        "yorgunum", "çok yoruldum", "bittim", "pestilim çıktı", "canım çıktı",
        "uyuyacağım", "dinlenmek istiyorum", "başım ağrıyor", "takatim kalmadı",
        "tükendim", "halsizim", "uykum var", "dinlenmem gerek", "bittik"
    )

    private val STRESSED_KEYWORDS = listOf(
        "stresliyim", "bunaldım", "yetişmiyor", "sıkıldım", "ne yapacağımı bilmiyorum",
        "kafam çok dolu", "boğuldum", "patlamak üzereyim", "panik", "çıldıracağım",
        "daraldım", "bıktım", "nasıl yetişecek", "yetiştiremedim"
    )

    private val JOYFUL_KEYWORDS = listOf(
        "harika", "süper", "çok iyi", "yaşasın", "mükemmel", "kazandık", "başardık",
        "tebrikler", "mutluyum", "çok sevindim", "harikasın", "bravo", "şahane",
        "keyfim yerinde", "çok güzel"
    )

    private val URGENT_KEYWORDS = listOf(
        "acil", "çabuk", "hemen", "yetişmem lazım", "acele et", "hızlı ol", "koş",
        "derhal", "zamanım yok", "vakit dar", "geciktim", "geç kaldım"
    )

    private val TEACHER_MAARIF_KEYWORDS = listOf(
        "ders", "müfredat", "maarif", "tarih", "öğrenci", "9. sınıf", "10. sınıf",
        "11. sınıf", "12. sınıf", "kazanım", "osmanlı", "selçuklu", "inkılap",
        "sınav sorusu", "yazılı", "performans ödevi", "ders planı", "tyt", "ayt"
    )

    private val WITTY_KEYWORDS = listOf(
        "espri yap", "fıkra anlat", "güldür beni", "komik bir şey söyle", "şaka yap",
        "takılma bana", "moralim bozuk güldür", "esprili ol"
    )

    /**
     * Kullanıcı girdisini analiz ederek duygu durumunu ve ses profili parametrelerini belirler.
     */
    fun analyze(userInput: String, patronPrefix: String = "Sayın Patronum"): EmotionAnalysis {
        val lower = userInput.lowercase(Locale.forLanguageTag("tr-TR")).trim()

        if (lower.isBlank()) {
            return EmotionAnalysis(
                state = EmotionState.NEUTRAL,
                confidence = 0.5f,
                empathyPreamble = "",
                voicePitch = 0.82f,
                speechRate = 1.02f,
                explanation = "Standart nötr akış."
            )
        }

        // 1. Aciliyet Kontrolü
        if (URGENT_KEYWORDS.any { lower.contains(it) } || lower.endsWith("!!!")) {
            return EmotionAnalysis(
                state = EmotionState.URGENT,
                confidence = 0.95f,
                empathyPreamble = "Hemen $patronPrefix, hiç vakit kaybetmiyoruz. ",
                voicePitch = 0.84f,
                speechRate = 1.14f,
                explanation = "Aciliyet tespit edildi, konuşma hızı artırıldı."
            )
        }

        // 2. Yorgunluk Kontrolü (Empati & Şefkat)
        if (TIRED_KEYWORDS.any { lower.contains(it) }) {
            return EmotionAnalysis(
                state = EmotionState.TIRED,
                confidence = 0.92f,
                empathyPreamble = "Patron, sesin ve sözlerin günün yorgunluğunu hissettiriyor. İzninle ayrıntıları ben toparlayayım, sen biraz soluklan. ",
                voicePitch = 0.79f,
                speechRate = 0.94f,
                explanation = "Yorgunluk tespit edildi; ses tonu sakinleştirici ve dinlendirici moda alındı."
            )
        }

        // 3. Stres ve Bunalım Kontrolü (Teselli & Güven)
        if (STRESSED_KEYWORDS.any { lower.contains(it) }) {
            return EmotionAnalysis(
                state = EmotionState.STRESSED,
                confidence = 0.90f,
                empathyPreamble = "Derin bir nefes al $patronPrefix. Ben buradayım; tarihte aşılamayan hiçbir kriz olmadı, bunu da adım adım çözeceğiz. ",
                voicePitch = 0.80f,
                speechRate = 0.96f,
                explanation = "Stres tespit edildi; güven verici, teselli edici ve kararlı frekans devrede."
            )
        }

        // 4. Esprili & Eğlenceli İstekler
        if (WITTY_KEYWORDS.any { lower.contains(it) }) {
            return EmotionAnalysis(
                state = EmotionState.WITTY,
                confidence = 0.88f,
                empathyPreamble = "Tarihin tozlu sayfalarını aralayıp yüzünü güldürecek bir nükte patlatıyorum patron! ",
                voicePitch = 0.86f,
                speechRate = 1.04f,
                explanation = "Esprili mod aktif; canlı ve nüktedan tonlama."
            )
        }

        // 5. Neşeli & Enerjik Durum
        if (JOYFUL_KEYWORDS.any { lower.contains(it) }) {
            return EmotionAnalysis(
                state = EmotionState.JOYFUL,
                confidence = 0.85f,
                empathyPreamble = "Bu harika enerjini duymak beni de şarj etti $patronPrefix! ",
                voicePitch = 0.85f,
                speechRate = 1.05f,
                explanation = "Yüksek motivasyon tespit edildi; dinamik ses temposu devrede."
            )
        }

        // 6. Maarif & Tarih Eğitimi Modu
        if (TEACHER_MAARIF_KEYWORDS.any { lower.contains(it) }) {
            return EmotionAnalysis(
                state = EmotionState.TEACHER_MAARIF,
                confidence = 0.85f,
                empathyPreamble = "Sayın Hocam, Maarif modelimiz ve ders kazanımlarımız rehberliğinde konuyu ele alıyorum. ",
                voicePitch = 0.82f,
                speechRate = 0.98f,
                explanation = "Eğitimci modu; bilge, dengeli ve akademik tonlama."
            )
        }

        // Varsayılan Asil Jarvis Dengesi
        return EmotionAnalysis(
            state = EmotionState.NEUTRAL,
            confidence = 0.70f,
            empathyPreamble = "",
            voicePitch = 0.82f,
            speechRate = 1.02f,
            explanation = "Dengeli, karizmatik erkek ses tonu."
        )
    }
}
