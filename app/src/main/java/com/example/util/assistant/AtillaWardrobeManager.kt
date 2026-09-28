package com.example.util.assistant

import android.content.Context
import com.example.util.assistant.drawers.AlarmDrawer
import com.example.util.assistant.drawers.AppBridgeDrawer
import com.example.util.assistant.drawers.CalendarDrawer
import com.example.util.assistant.drawers.CommunicationDrawer
import com.example.util.assistant.drawers.CultureTravelDrawer
import com.example.util.assistant.drawers.GeminiCloudDrawer
import com.example.util.assistant.drawers.KnowledgeSearchDrawer
import com.example.util.assistant.drawers.LiveWebResearchDrawer
import com.example.util.assistant.drawers.NavigationDrawer
import com.example.util.assistant.drawers.OfflineIntelligenceDrawer
import com.example.util.assistant.drawers.TeacherMebDrawer
import com.example.util.assistant.drawers.VisionImageDrawer
import java.util.Locale

object AtillaWardrobeManager {

    private val drawers: List<AssistantDrawer> = listOf(
        VisionImageDrawer,          // 1. Resim / OCR / Belge soruları (kütüphaneyi asla dökmez)
        AlarmDrawer,                // 2. Saat ve alarm komutları
        CalendarDrawer,             // 3. Takvim ve randevu senkronizasyonu
        CommunicationDrawer,        // 4. Telefon arama ve WhatsApp mesajı
        NavigationDrawer,           // 5. Harita, navigasyon ve park yeri
        AppBridgeDrawer,            // 6. YouTube, Google arama, Telefon Gemini köprüsü
        GeminiCloudDrawer,          // 7. Canlı Bulut Yapay Zeka (Geçerli API anahtarı varsa - İnsancıl mod)
        LiveWebResearchDrawer,      // 8. CANLI İNTERNET VE GÜNCEL ARAŞTIRMA (Wikipedia TR, Canlı Haber RSS, Web)
        TeacherMebDrawer,           // 9. MEB resmi plan ve sınav evrak üretimi
        CultureTravelDrawer,        // 10. Kültür ve gezi rotaları
        KnowledgeSearchDrawer,      // 11. Çevrimdışı Nokta Atışı Kütüphane Arama (657, ÖMK, Sendika, MEB)
        OfflineIntelligenceDrawer   // 12. İnsancıl Patron-Çalışan Diyalog, Karar Destek ve Gemini Köprüsü
    )

    suspend fun dispatch(
        context: Context,
        query: String,
        sessionData: WardrobeSessionData
    ): DrawerResult {
        val cleanQuery = query.trim()
        val lowerQuery = cleanQuery.lowercase(Locale.forLanguageTag("tr-TR"))

        for (drawer in drawers) {
            if (drawer.canHandle(cleanQuery, lowerQuery)) {
                try {
                    val result = drawer.handle(context, cleanQuery, lowerQuery, sessionData)
                    if (result != null && result.replyText.isNotBlank()) {
                        return result
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // Genel yedek yanıt
        return DrawerResult(
            replyText = "Emredersiniz efendim, sizi dinliyorum.",
            actionSummary = "⚡ Atila Dinlemede"
        )
    }
}
