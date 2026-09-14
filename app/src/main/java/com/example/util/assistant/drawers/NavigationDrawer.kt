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

        // 3. En Yakın 3 Mekan Tespiti ve Listeleme (Eczane, Market, Fırın, Benzinlik vb.)
        val patronPrefix = if (sessionData.userNick.isNotBlank()) "Sayın Patronum ${sessionData.userNick}" else "Sayın Patronum"
        val targetQuery = query.replace(Regex("""(?i)bana|yol tarifi ver|yol tarifini ver|nasıl giderim|haritada göster|nerede|en yakın"""), "").trim().ifBlank { "Mekanlar" }
        
        val top3Places = NearbyPlacesHelper.getTop3NearbyPlaces(
            context = context,
            userLat = sessionData.userLat,
            userLng = sessionData.userLng,
            rawQuery = query
        )

        val reply = buildString {
            append("📍 **$patronPrefix, Konumunuza En Yakın 3 Yer Tespit Edildi:**\n\n")
            top3Places.forEachIndexed { idx, p ->
                append("${idx + 1}. **${p.name}**\n")
                append("   • ${p.typeLabel} (${p.distanceMeters} metre)\n")
                append("   • ${p.address}\n\n")
            }
            append("💡 Aşağıdaki kartlardan **'Yol Tarifi'** veya **'Telefon'** butonuna dokunarak doğrudan canlı navigasyonu başlatabilirsiniz.")
        }

        val top1 = top3Places.firstOrNull()
        var navLaunched = false
        if (top1 != null && (lowerQuery.contains("yol") || lowerQuery.contains("tarif") || lowerQuery.contains("navigasyon") || lowerQuery.contains("nasıl giderim") || lowerQuery.contains("git") || lowerQuery.contains("rota"))) {
            NearbyPlacesHelper.openGoogleMapsNavigation(context, top1.name, top1.lat, top1.lng, top1.address)
            navLaunched = true
        }

        val speech = if (navLaunched && top1 != null) {
            "$patronPrefix, ${top1.name} için Google Haritalar canlı yol tarifini başlattım efendim."
        } else {
            "$patronPrefix, konumunuza en yakın 3 yer listelendi. İlk sırada ${top1?.distanceMeters ?: 200} metre mesafedeki ${top1?.name ?: targetQuery} yer alıyor."
        }

        return DrawerResult(
            replyText = reply,
            recommendedPlaces = top3Places,
            actionSummary = if (navLaunched && top1 != null) "🗺️ Yol Tarifi: ${top1.name}" else "📍 En Yakın 3 Yer: $targetQuery",
            speechText = speech
        )
    }
}
