package com.example.util.assistant.drawers

import android.content.Context
import com.example.util.NearbyPlacesHelper
import com.example.util.UstaSessionState
import com.example.util.assistant.AssistantDrawer
import com.example.util.assistant.DrawerResult
import com.example.util.assistant.WardrobeSessionData

object NavigationDrawer : AssistantDrawer {
    override val drawerName: String = "Navigasyon & Konum Çekmecesi"

    override fun canHandle(query: String, lowerQuery: String): Boolean {
        return lowerQuery.contains("nerede") ||
               lowerQuery.contains("en yakın") ||
               lowerQuery.contains("nasıl giderim") ||
               lowerQuery.contains("navigasyon") ||
               lowerQuery.contains("yol tarifi") ||
               lowerQuery.contains("park yeri") ||
               lowerQuery.contains("lokasyona kaydet")
    }

    override suspend fun handle(
        context: Context,
        query: String,
        lowerQuery: String,
        sessionData: WardrobeSessionData
    ): DrawerResult? {
        if (lowerQuery.contains("park yeri")) {
            UstaSessionState.pendingParkCoords = Pair(sessionData.userLat, sessionData.userLng)
            UstaSessionState.isWaitingForParkNote = true
            return DrawerResult(
                replyText = "🚗 Konumunuz (${sessionData.userCity} ${sessionData.userDistrict}) park yeri olarak alındı. Blok veya kat numarası gibi bir not eklemek ister misiniz?"
            )
        }
        if (lowerQuery.contains("lokasyona kaydet")) {
            UstaSessionState.pendingLocationCoords = Pair(sessionData.userLat, sessionData.userLng)
            return DrawerResult(
                replyText = "📍 Konumunuz alındı. 'Kayıtlı Lokasyonlarım' listesine hangi isimle kaydedeyim? (Örn: Evim, İşyeri vb.)"
            )
        }

        // Doğrudan Harita Navigasyonu
        val targetQuery = query.replace(Regex("""(?i)bana|yol tarifi ver|yol tarifini ver|nasıl giderim|haritada göster|nerede|en yakın"""), "").trim().ifBlank { "Hedef" }
        NearbyPlacesHelper.openGoogleMapsNavigation(
            context = context,
            placeName = targetQuery,
            lat = sessionData.userLat,
            lng = sessionData.userLng,
            searchQuery = targetQuery
        )
        return DrawerResult(
            replyText = "Google Haritalar canlı navigasyonunu açıyorum: $targetQuery",
            actionSummary = "🗺️ Navigasyon Başlatıldı: $targetQuery"
        )
    }
}
