package com.example.util.assistant.drawers

import android.content.Context
import com.example.util.assistant.AssistantDrawer
import com.example.util.assistant.DrawerResult
import com.example.util.assistant.WardrobeSessionData

object CultureTravelDrawer : AssistantDrawer {
    override val drawerName: String = "Kültür, Tarih & Seyahat Çekmecesi"

    override fun canHandle(query: String, lowerQuery: String): Boolean {
        return lowerQuery.contains("unesco") ||
               lowerQuery.contains("göbeklitepe") ||
               lowerQuery.contains("kapadokya") ||
               lowerQuery.contains("efes") ||
               lowerQuery.contains("nemrut") ||
               lowerQuery.contains("divriği") ||
               lowerQuery.contains("gezilecek yerler") ||
               lowerQuery.contains("tarihi yerler") ||
               lowerQuery.contains("coğrafya") ||
               lowerQuery.contains("dağları") ||
               lowerQuery.contains("akarsuları") ||
               lowerQuery.contains("gölleri")
    }
    override suspend fun handle(
        context: Context,
        query: String,
        lowerQuery: String,
        sessionData: WardrobeSessionData
    ): DrawerResult? {
        if (lowerQuery.contains("gezilecek yerler") || lowerQuery.contains("tarihi yerler") || lowerQuery.contains("nereleri gezebilirim")) {
            val (text, places) = com.example.util.NearbyPlacesHelper.getTouristAttractions(
                context = context,
                targetCity = sessionData.userCity,
                userLat = sessionData.userLat,
                userLng = sessionData.userLng
            )
            return DrawerResult(
                replyText = text,
                recommendedPlaces = places,
                actionSummary = "🏛️ Gezi Rotaları: ${sessionData.userCity}"
            )
        }
        return null // Diğer UNESCO ve kültür detayları KnowledgeSearchDrawer veya Gemini tarafından cevaplanır
    }
}
