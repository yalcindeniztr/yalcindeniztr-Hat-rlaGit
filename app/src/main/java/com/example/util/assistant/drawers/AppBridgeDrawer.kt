package com.example.util.assistant.drawers

import android.content.Context
import com.example.util.AppLauncherHelper
import com.example.util.DailyNewsHelper
import com.example.util.WeatherHelper
import com.example.util.assistant.AssistantDrawer
import com.example.util.assistant.DrawerResult
import com.example.util.assistant.WardrobeSessionData

object AppBridgeDrawer : AssistantDrawer {
    override val drawerName: String = "Cihaz, Medya, Hava & Haber Köprüsü Çekmecesi"

    override fun canHandle(query: String, lowerQuery: String): Boolean {
        return lowerQuery.contains("youtube") ||
               lowerQuery.contains("çal") ||
               lowerQuery.contains("müzik") ||
               lowerQuery.contains("şarkı") ||
               lowerQuery.contains("gemini") ||
               lowerQuery.contains("hava durumu") ||
               lowerQuery.contains("hava nasıl") ||
               lowerQuery.contains("gazete manşet") ||
               lowerQuery.contains("haberler") ||
               lowerQuery.contains("günün haber") ||
               lowerQuery.startsWith("google'da ara") ||
               lowerQuery.startsWith("internette ara") ||
               lowerQuery.endsWith("aç") ||
               lowerQuery.endsWith("başlat") ||
               lowerQuery.contains("tv'de ne var")
    }

    override suspend fun handle(
        context: Context,
        query: String,
        lowerQuery: String,
        sessionData: WardrobeSessionData
    ): DrawerResult? {
        // 1. Canlı Hava Durumu
        if (lowerQuery.contains("hava durumu") || lowerQuery.contains("hava nasıl")) {
            val weatherText = WeatherHelper.getLiveWeather(
                context = context,
                lat = sessionData.userLat,
                lng = sessionData.userLng,
                cityName = sessionData.userCity.ifBlank { "Bulunduğunuz Şehir" }
            )
            return DrawerResult(
                replyText = weatherText,
                actionSummary = "🌤️ Canlı Hava Durumu: ${sessionData.userCity}"
            )
        }

        // 2. Günlük Gazete Manşetleri & Haberler
        if (lowerQuery.contains("gazete manşet") || lowerQuery.contains("haberler") || lowerQuery.contains("günün haber")) {
            val headlines = DailyNewsHelper.getHeadlinesOnly()
            return DrawerResult(
                replyText = headlines,
                actionSummary = "📰 Günün Gazete Manşetleri"
            )
        }

        // 3. YouTube Müzik Çalma
        if (lowerQuery.contains("çal") || lowerQuery.contains("müzik") || lowerQuery.contains("şarkı") || lowerQuery.contains("youtube")) {
            val songQuery = query.replace(Regex("(?i)youtube'dan|youtube'da|youtube|şarkısını|şarkıyı|müziğini|müzik|çal|aç|oynat|bul"), "").trim().ifBlank { "Müzik" }
            val (_, message) = AppLauncherHelper.playYouTubeSong(context, songQuery)
            return DrawerResult(
                replyText = message,
                actionSummary = "▶️ YouTube: $songQuery"
            )
        }

        // 4. Google Gemini Köprüsü
        if (lowerQuery.contains("gemini")) {
            val q = query.replace(Regex("(?i)^(gemini'yi aç|gemini aç|geminiye sor|gemini)[: ]*"), "").trim()
            val (_, message) = AppLauncherHelper.openGoogleGemini(context, q)
            return DrawerResult(
                replyText = message,
                actionSummary = "✨ Google Gemini Köprüsü"
            )
        }

        // 5. Google / İnternet Arama
        if (lowerQuery.startsWith("google'da ara") || lowerQuery.startsWith("internette ara") || lowerQuery.contains("google'da")) {
            val q = query.replace(Regex("(?i)^(google'da ara|internette ara|google'da)[: ]*"), "").trim()
            val (_, message) = AppLauncherHelper.searchGoogle(context, q)
            return DrawerResult(
                replyText = message,
                actionSummary = "🔍 Google Araması: $q"
            )
        }

        // 6. Uygulama Başlatma
        if (lowerQuery.endsWith("aç") || lowerQuery.endsWith("başlat")) {
            val (success, message) = AppLauncherHelper.openApplicationByVoice(context, query)
            if (success) {
                return DrawerResult(
                    replyText = message,
                    actionSummary = "🚀 $message"
                )
            }
        }

        return null
    }
}
