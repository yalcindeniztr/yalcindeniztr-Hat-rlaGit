package com.example.util.assistant.drawers

import android.content.Context
import com.example.util.ActionDispatcherHelper
import com.example.util.assistant.AssistantDrawer
import com.example.util.assistant.DrawerResult
import com.example.util.assistant.WardrobeSessionData
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object CalendarDrawer : AssistantDrawer {
    override val drawerName: String = "Takvim & Randevu Çekmecesi"

    override fun canHandle(query: String, lowerQuery: String): Boolean {
        return lowerQuery.contains("takvim") ||
               lowerQuery.contains("etkinlik ekle") ||
               lowerQuery.contains("randevu ekle") ||
               lowerQuery.contains("toplantı ekle")
    }

    override suspend fun handle(
        context: Context,
        query: String,
        lowerQuery: String,
        sessionData: WardrobeSessionData
    ): DrawerResult? {
        val cal = Calendar.getInstance()
        if (lowerQuery.contains("yarın")) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        val timeMatch = Regex("""(?i)(?:saat\s*)?(\d{1,2})[:.](\d{2})""").find(query)
        val hourOnlyMatch = Regex("""(?i)(?:saat\s*)?(\d{1,2})\s*(?:'ye|'ya|'e|'a|'de|'da)""").find(query)
        val parsedHour = timeMatch?.groupValues?.get(1)?.toIntOrNull() ?: hourOnlyMatch?.groupValues?.get(1)?.toIntOrNull() ?: 10
        val parsedMin = timeMatch?.groupValues?.get(2)?.toIntOrNull() ?: 0

        cal.set(Calendar.HOUR_OF_DAY, parsedHour)
        cal.set(Calendar.MINUTE, parsedMin)
        cal.set(Calendar.SECOND, 0)
        if (cal.timeInMillis <= System.currentTimeMillis() && !lowerQuery.contains("yarın")) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        val eventTitle = query.replace(Regex("""(?i)takvime ekle|takvimime ekle|etkinlik ekle|randevu ekle|toplantı ekle|yarın|bugün|saat\s*\d{1,2}(?:[:.]\d{2})?|'ye|'ya|'e|'a|'de|'da"""), "").trim().ifBlank { "Toplantı / Randevu" }
        val payload = JSONObject().apply {
            put("title", eventTitle)
            put("description", "Atilla tarafından oluşturuldu.")
            put("startTimeMillis", cal.timeInMillis)
            put("endTimeMillis", cal.timeInMillis + 3600000L)
        }
        ActionDispatcherHelper.executeAction(context, "CREATE_EVENT", payload)
        val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(cal.time)
        return DrawerResult(
            replyText = "Emredersiniz, '$eventTitle' etkinliği $dateStr için telefon takviminize ve akıllı saatinize işlendi.",
            actionSummary = "📅 Takvim: $eventTitle ($dateStr)"
        )
    }
}
