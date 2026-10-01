package com.example.util.assistant.drawers

import android.content.Context
import com.example.util.LiveWebSearchHelper
import com.example.util.UstaSessionState
import com.example.util.assistant.AssistantDrawer
import com.example.util.assistant.DrawerResult
import com.example.util.assistant.WardrobeSessionData

/**
 * ATİLA Canlı İnternet Araştırma ve Gerçek Zamanlı Bilgi Çekmecesi
 * Kayıtlı kalıp metinler yerine internetten canlı veri (Wikipedia, TRT Haber RSS, Canlı Web)
 * toplayarak Patron'a insani, saygılı ve taze bilgiler sunar.
 */
object LiveWebResearchDrawer : AssistantDrawer {
    override val drawerName: String = "Canlı İnternet ve Güncel Araştırma Çekmecesi"

    override fun canHandle(query: String, lowerQuery: String): Boolean {
        // Canlı haberler
        if (lowerQuery.contains("gazete manşet") || lowerQuery.contains("haberler") || lowerQuery.contains("günün haber") || lowerQuery.contains("son dakika") || lowerQuery.contains("gündem")) {
            return true
        }

        // Genel bilgi, soru, merak ve araştırma komutları
        return lowerQuery.contains("nedir") ||
               lowerQuery.contains("kimdir") ||
               lowerQuery.contains("araştır") ||
               lowerQuery.contains("araştırma") ||
               lowerQuery.contains("bilgi ver") ||
               lowerQuery.contains("hakkında") ||
               lowerQuery.contains("nasıl yapılır") ||
               lowerQuery.contains("ne zaman") ||
               lowerQuery.contains("nerede") ||
               lowerQuery.contains("neresi") ||
               lowerQuery.contains("tarihçesi") ||
               lowerQuery.contains("özellikleri") ||
               lowerQuery.contains("anlat") ||
               lowerQuery.contains("açıkla") ||
               lowerQuery.contains("kim bu") ||
               lowerQuery.contains("farkı ne") ||
               lowerQuery.contains("ne demek") ||
               lowerQuery.endsWith("?") ||
               lowerQuery.split(" ").size >= 2
    }

    override suspend fun handle(
        context: Context,
        query: String,
        lowerQuery: String,
        sessionData: WardrobeSessionData
    ): DrawerResult? {
        val isChatMode = UstaSessionState.isChatMode
        val greeting = if (isChatMode) "Dostum, " else if (sessionData.userNick.isNotBlank()) "Sayın Hocam ${sessionData.userNick}, " else "Sayın Hocam, "

        // 1. Canlı Haber Manşetleri (TRT Haber Canlı RSS Akışı)
        if (lowerQuery.contains("gazete manşet") || lowerQuery.contains("haberler") || lowerQuery.contains("günün haber") || lowerQuery.contains("son dakika") || lowerQuery.contains("gündem")) {
            val liveHeadlines = LiveWebSearchHelper.fetchLiveNewsHeadlines()
            val speech = com.example.util.DailyNewsHelper.getVoiceHeadlinesSummary(liveHeadlines, greeting.trim().trimEnd(','))
            return DrawerResult(
                replyText = liveHeadlines,
                actionSummary = "📰 Canlı Güncel Haberler",
                speechText = speech
            )
        }

        // 2. Canlı İnternet Araştırması (Wikipedia TR & Canlı Web Özeti)
        val liveResult = LiveWebSearchHelper.searchLiveWeb(query)
        if (liveResult != null && liveResult.summary.isNotBlank()) {
            val reply = buildString {
                append("🌐 **${greeting}İnternet Üzerinden Yaptığım Anlık Araştırma Sonucu:**\n\n")
                append("📌 **${liveResult.title}**\n\n")
                append("${liveResult.summary}\n\n")
                append("🔗 **Kaynak:** ${liveResult.sourceUrl}\n\n")
                append("💡 _Efendim, bu konuyu telefonunuzdaki Google Gemini uygulamasına da aktarabilirim. 'Gemini'ye sor' demeniz yeterlidir._")
            }

            val firstSentence = liveResult.summary.lines().firstOrNull { it.isNotBlank() } ?: liveResult.summary
            val speechText = "${greeting}${liveResult.title} konusunu internetten araştırdım. ${firstSentence.take(180)}"

            return DrawerResult(
                replyText = reply,
                actionSummary = "🌐 Canlı Araştırma: ${liveResult.title}",
                speechText = speechText
            )
        }

        // İnternet araştırması sonuç vermezse diğer çekmecelere veya LLM'e devreder
        return null
    }
}
