package com.example.util.assistant.drawers

import android.content.Context
import com.example.data.SavedLocationEntity
import com.example.util.LocalStorageManager
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
               lowerQuery.contains("konumumu kaydet") ||
               lowerQuery.contains("konum kaydet") ||
               lowerQuery.contains("burayı kaydet") ||
               lowerQuery.contains("lokasyona kaydet") ||
               lowerQuery.contains("haritaya kaydet")
    }

    override suspend fun handle(
        context: Context,
        query: String,
        lowerQuery: String,
        sessionData: WardrobeSessionData
    ): DrawerResult? {
        // 1. Doğrudan Konum Kaydetme (Garantili Room + LocalStorage)
        if (lowerQuery.contains("konumumu kaydet") || lowerQuery.contains("konum kaydet") ||
            lowerQuery.contains("burayı kaydet") || lowerQuery.contains("haritaya kaydet") ||
            lowerQuery.contains("lokasyona kaydet")) {

            val locName = query.replace(Regex("(?i)konumumu kaydet|burayı kaydet|konum kaydet|haritaya kaydet|lokasyona kaydet|olarak|adıyla|adı|bana"), "").trim()
                .ifBlank { "${sessionData.userCity} ${sessionData.userDistrict} Konumu" }

            val lat = sessionData.userLat
            val lng = sessionData.userLng

            // Room Database Kaydı
            sessionData.db.savedLocationDao().insertLocation(
                SavedLocationEntity(
                    name = locName,
                    lat = lat,
                    lng = lng,
                    timestamp = System.currentTimeMillis()
                )
            )

            // Garantili LocalStorage Dosya Kaydı
            LocalStorageManager.saveLocalLocation(context, locName, lat, lng)

            return DrawerResult(
                replyText = "Efendim, '$locName' konumunuz (${sessionData.userCity} ${sessionData.userDistrict}) 'Kayıtlı Lokasyonlarım' listenize ve yerel belleğe başarıyla kaydedildi.",
                actionSummary = "📍 Konum Kaydedildi: $locName"
            )
        }

        // 2. Park Yeri
        if (lowerQuery.contains("park yeri")) {
            UstaSessionState.pendingParkCoords = Pair(sessionData.userLat, sessionData.userLng)
            UstaSessionState.isWaitingForParkNote = true
            return DrawerResult(
                replyText = "🚗 Konumunuz (${sessionData.userCity} ${sessionData.userDistrict}) park yeri olarak alındı efendim. Blok veya kat numarası gibi bir not eklemek ister misiniz?"
            )
        }

        // 3. Doğrudan Harita Canlı Navigasyonu
        val targetQuery = query.replace(Regex("""(?i)bana|yol tarifi ver|yol tarifini ver|nasıl giderim|haritada göster|nerede|en yakın"""), "").trim().ifBlank { "Hedef" }
        NearbyPlacesHelper.openGoogleMapsNavigation(
            context = context,
            placeName = targetQuery,
            lat = sessionData.userLat,
            lng = sessionData.userLng,
            searchQuery = targetQuery
        )
        return DrawerResult(
            replyText = "Efendim, Google Haritalar navigasyonunu açıyorum: $targetQuery",
            actionSummary = "🗺️ Navigasyon Başlatıldı: $targetQuery"
        )
    }
}
