package com.example.util.assistant.drawers

import android.content.Context
import com.example.util.ActionDispatcherHelper
import com.example.util.UstaSessionState
import com.example.util.assistant.AssistantDrawer
import com.example.util.assistant.DrawerResult
import com.example.util.assistant.WardrobeSessionData
import org.json.JSONObject
import java.util.Locale

object AlarmDrawer : AssistantDrawer {
    override val drawerName: String = "Saat & Alarm Çekmecesi"

    override fun canHandle(query: String, lowerQuery: String): Boolean {
        return lowerQuery.contains("alarm") || lowerQuery.contains("uyandır")
    }

    override suspend fun handle(
        context: Context,
        query: String,
        lowerQuery: String,
        sessionData: WardrobeSessionData
    ): DrawerResult? {
        val timeMatch = Regex("""(?i)(?:saat\s*)?(\d{1,2})[:.](\d{2})""").find(query)
        val hourOnlyMatch = Regex("""(?i)(?:saat\s*)?(\d{1,2})\s*(?:'ye|'ya|'e|'a|'de|'da)""").find(query)
        val anyHourMatch = Regex("""(?i)\b(\d{1,2})\b""").find(query)

        val parsedHour = timeMatch?.groupValues?.get(1)?.toIntOrNull()
            ?: hourOnlyMatch?.groupValues?.get(1)?.toIntOrNull()
            ?: anyHourMatch?.groupValues?.get(1)?.toIntOrNull()
        val parsedMin = timeMatch?.groupValues?.get(2)?.toIntOrNull() ?: 0

        if (parsedHour != null && parsedHour in 0..23 && parsedMin in 0..59) {
            val label = query.replace(Regex("""(?i)(?:saat\s*)?\d{1,2}(?:[:.]\d{2})?|bana|alarm|kur|ayarla|için|uyandır|'ye|'ya|'e|'a|'de|'da"""), "").trim().ifBlank { "Atilla Alarm" }
            val payload = JSONObject().apply {
                put("hour", parsedHour)
                put("minute", parsedMin)
                put("title", label)
            }
            ActionDispatcherHelper.executeAction(context, "SET_ALARM", payload)
            val timeFormatted = String.format(Locale.ROOT, "%02d:%02d", parsedHour, parsedMin)
            return DrawerResult(
                replyText = "Emredersiniz, alarm $timeFormatted için telefonunuza ve saatinize kuruldu.",
                actionSummary = "⏰ Alarm: $timeFormatted ($label)"
            )
        } else if (!lowerQuery.contains("nasıl") && !lowerQuery.contains("nedir")) {
            UstaSessionState.isWaitingForAlarmTime = true
            return DrawerResult(
                replyText = "Alarmı hangi saate kurmamı istersiniz? (Örneğin: 07:30 veya 8:00)"
            )
        }
        return null
    }
}
