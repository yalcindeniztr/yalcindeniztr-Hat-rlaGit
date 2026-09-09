package com.example.util.assistant.drawers

import android.content.Context
import com.example.util.AppLauncherHelper
import com.example.util.assistant.AssistantDrawer
import com.example.util.assistant.DrawerResult
import com.example.util.assistant.WardrobeSessionData

object AppBridgeDrawer : AssistantDrawer {
    override val drawerName: String = "Cihaz & Medya Köprüsü Çekmecesi"

    override fun canHandle(query: String, lowerQuery: String): Boolean {
        return lowerQuery.contains("youtube") ||
               lowerQuery.contains("gemini") ||
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
        if (lowerQuery.contains("youtube") && (lowerQuery.contains("aç") || lowerQuery.contains("çal") || lowerQuery.contains("oynat"))) {
            val songQuery = query.replace(Regex("(?i)youtube'dan|youtube'da|youtube|şarkısını|şarkıyı|aç|çal|oynat"), "").trim().ifBlank { "Müzik" }
            val (_, message) = AppLauncherHelper.playYouTubeSong(context, songQuery)
            return DrawerResult(
                replyText = message,
                actionSummary = "▶️ YouTube: $songQuery"
            )
        }

        if (lowerQuery.contains("gemini")) {
            val q = query.replace(Regex("(?i)^(gemini'yi aç|gemini aç|geminiye sor)[: ]*"), "").trim()
            val (_, message) = AppLauncherHelper.openGoogleGemini(context, q)
            return DrawerResult(
                replyText = message,
                actionSummary = "✨ Google Gemini Köprüsü"
            )
        }

        if (lowerQuery.startsWith("google'da ara") || lowerQuery.startsWith("internette ara")) {
            val q = query.replace(Regex("(?i)^(google'da ara|internette ara)[: ]*"), "").trim()
            val (_, message) = AppLauncherHelper.searchGoogle(context, q)
            return DrawerResult(
                replyText = message,
                actionSummary = "🔍 Google Araması: $q"
            )
        }

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
