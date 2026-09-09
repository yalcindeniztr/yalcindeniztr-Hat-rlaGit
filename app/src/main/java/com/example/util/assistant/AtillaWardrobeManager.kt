package com.example.util.assistant

import android.content.Context
import com.example.util.assistant.drawers.AlarmDrawer
import com.example.util.assistant.drawers.AppBridgeDrawer
import com.example.util.assistant.drawers.CalendarDrawer
import com.example.util.assistant.drawers.CommunicationDrawer
import com.example.util.assistant.drawers.CultureTravelDrawer
import com.example.util.assistant.drawers.GeminiCloudDrawer
import com.example.util.assistant.drawers.KnowledgeSearchDrawer
import com.example.util.assistant.drawers.NavigationDrawer
import com.example.util.assistant.drawers.TeacherMebDrawer
import com.example.util.assistant.drawers.VisionImageDrawer
import java.util.Locale

object AtillaWardrobeManager {

    private val drawers: List<AssistantDrawer> = listOf(
        VisionImageDrawer,       // 1. Resim / OCR / Belge soruları (kütüphaneyi asla dökmez)
        AlarmDrawer,             // 2. Saat ve alarm komutları
        CalendarDrawer,          // 3. Takvim ve randevu senkronizasyonu
        CommunicationDrawer,     // 4. Telefon arama ve WhatsApp mesajı
        NavigationDrawer,        // 5. Harita, navigasyon ve park yeri
        TeacherMebDrawer,        // 6. MEB, mevzuat, ŞÖK, sınav ve planlama
        AppBridgeDrawer,         // 7. YouTube, Google arama, Gemini köprüsü
        CultureTravelDrawer,     // 8. Kültür, gezi rotaları ve coğrafya
        GeminiCloudDrawer,       // 9. Canlı Bulut Yapay Zeka (Kısa, öz, thinkingBudget=0)
        KnowledgeSearchDrawer    // 10. Çevrimdışı Nokta Atışı Kütüphane Arama (Yalnızca ilgili tek konuyu çeker)
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
            replyText = "Buyrun dostum, sizi dinliyorum. Size nasıl yardımcı olabilirim?"
        )
    }
}
