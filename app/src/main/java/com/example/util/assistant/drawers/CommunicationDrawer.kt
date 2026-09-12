package com.example.util.assistant.drawers

import android.content.Context
import com.example.util.ActionDispatcherHelper
import com.example.util.assistant.AssistantDrawer
import com.example.util.assistant.DrawerResult
import com.example.util.assistant.WardrobeSessionData
import org.json.JSONObject

object CommunicationDrawer : AssistantDrawer {
    override val drawerName: String = "İletişim (Arama, WhatsApp & SMS) Çekmecesi"

    private val BLACKLIST = setOf("harita", "alarm", "takvim", "yemek", "usta", "atilla", "atila", "google", "hava")

    override fun canHandle(query: String, lowerQuery: String): Boolean {
        return lowerQuery.endsWith("ara") ||
               lowerQuery.contains("telefon et") ||
               lowerQuery.contains("çağrı yap") ||
               lowerQuery.contains("telefon aç") ||
               lowerQuery.contains("whatsapp") ||
               lowerQuery.contains("watsap") ||
               lowerQuery.contains("sms") ||
               (lowerQuery.contains("mesaj") && (lowerQuery.contains("at") || lowerQuery.contains("yaz") || lowerQuery.contains("gönder")))
    }

    override suspend fun handle(
        context: Context,
        query: String,
        lowerQuery: String,
        sessionData: WardrobeSessionData
    ): DrawerResult? {
        val greeting = if (sessionData.userNick.isNotBlank()) "Efendim Sayın ${sessionData.userNick}, " else "Efendim, "

        // 1. TELEFON ARAMASI
        if (lowerQuery.endsWith("ara") || lowerQuery.contains("telefon et") || lowerQuery.contains("çağrı yap") || lowerQuery.contains("telefon aç")) {
            val contactName = query
                .replace(Regex("""(?i)^(?:lütfen|bana|hemen)?\s*"""), "")
                .replace(Regex("""(?i)\s*(?:'yi|'yı|'i|'ı|'yu|'yü|'ü|'u|yi|yı|e|a)?\s*(?:ara|telefon et|çağrı yap|telefon aç)$"""), "")
                .trim()

            if (contactName.isNotBlank() && !BLACKLIST.contains(contactName.lowercase())) {
                val payload = JSONObject().apply { put("name", contactName) }
                val summary = ActionDispatcherHelper.executeAction(context, "CALL_PHONE", payload)
                val speech = "${greeting}$contactName kişisini arıyorum."
                return DrawerResult(
                    replyText = "📞 $summary",
                    actionSummary = summary,
                    speechText = speech
                )
            }
        }

        // 2. WHATSAPP MESAJI
        if (lowerQuery.contains("whatsapp") || lowerQuery.contains("watsap")) {
            var targetName = ""
            var messageBody = "Merhaba"

            val p1 = Regex("""(?i)(?:whatsapp'tan|whatsapp|whatsappta|watsaptan)\s+([a-zA-ZçğıöşüÇĞİÖŞÜ]+)(?:['’][a-z]+|[ea]ye|[ea])?\s*(?:mesaj(?:ı)?\s*(?:at|yaz|gönder|ilet)?)?[: ]*(.*)""")
            val p2 = Regex("""(?i)([a-zA-ZçğıöşüÇĞİÖŞÜ]+)(?:['’][a-z]+|[ea]ye|[ea])?\s*(?:whatsapp'tan|whatsapp|whatsappta|watsaptan)\s*(?:mesaj(?:ı)?\s*(?:at|yaz|gönder|ilet)?)?[: ]*(.*)""")

            val match1 = p1.find(query)
            val match2 = p2.find(query)

            if (match1 != null) {
                targetName = match1.groupValues[1].trim()
                val candidateMsg = match1.groupValues[2].trim()
                if (candidateMsg.isNotBlank()) messageBody = candidateMsg
            } else if (match2 != null) {
                targetName = match2.groupValues[1].trim()
                val candidateMsg = match2.groupValues[2].trim()
                if (candidateMsg.isNotBlank()) messageBody = candidateMsg
            }

            if (targetName.isNotBlank() && !BLACKLIST.contains(targetName.lowercase())) {
                val payload = JSONObject().apply {
                    put("name", targetName)
                    put("message", messageBody)
                }
                val summary = ActionDispatcherHelper.executeAction(context, "SEND_WHATSAPP", payload)
                val speech = "${greeting}$targetName kişisine WhatsApp mesajınızı hazırladım."
                return DrawerResult(
                    replyText = "💬 $summary",
                    actionSummary = summary,
                    speechText = speech
                )
            }
        }

        // 3. NORMAL SMS MESAJI
        if (lowerQuery.contains("sms") || (lowerQuery.contains("mesaj") && !lowerQuery.contains("sesli not"))) {
            val contactMatch = Regex("""(?i)([a-zA-ZçğıöşüÇĞİÖŞÜ]+)(?:['’][a-z]+|[ea]ye|[ea])?\s*(?:sms|normal mesaj|mesaj)\s*(?:at|yaz|gönder)[: ]*(.*)""").find(query)
            val targetName = contactMatch?.groupValues?.get(1)?.trim() ?: ""
            val messageBody = contactMatch?.groupValues?.get(2)?.trim()?.ifBlank { "Merhaba" } ?: "Merhaba"

            if (targetName.isNotBlank() && !BLACKLIST.contains(targetName.lowercase())) {
                val payload = JSONObject().apply {
                    put("name", targetName)
                    put("message", messageBody)
                }
                val summary = ActionDispatcherHelper.executeAction(context, "SEND_SMS", payload)
                val speech = "${greeting}$targetName kişisine SMS mesajınızı hazırladım."
                return DrawerResult(
                    replyText = "✉️ $summary",
                    actionSummary = summary,
                    speechText = speech
                )
            }
        }

        return null
    }
}
