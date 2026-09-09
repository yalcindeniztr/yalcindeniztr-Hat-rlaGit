package com.example.util.assistant.drawers

import android.content.Context
import com.example.util.ActionDispatcherHelper
import com.example.util.assistant.AssistantDrawer
import com.example.util.assistant.DrawerResult
import com.example.util.assistant.WardrobeSessionData
import org.json.JSONObject

object CommunicationDrawer : AssistantDrawer {
    override val drawerName: String = "İletişim (Arama & WhatsApp) Çekmecesi"

    private val BLACKLIST = setOf("harita", "alarm", "takvim", "yemek", "usta", "atilla")

    override fun canHandle(query: String, lowerQuery: String): Boolean {
        return lowerQuery.endsWith("ara") ||
               lowerQuery.contains("telefon et") ||
               lowerQuery.contains("çağrı yap") ||
               lowerQuery.contains("whatsapp")
    }

    override suspend fun handle(
        context: Context,
        query: String,
        lowerQuery: String,
        sessionData: WardrobeSessionData
    ): DrawerResult? {
        if (lowerQuery.endsWith("ara") || lowerQuery.contains("telefon et") || lowerQuery.contains("çağrı yap")) {
            val contactName = query.replace(Regex("""(?i)lütfen|bana|'yi ara|'yı ara|'i ara|'ı ara|'yu ara|'yü ara|'ü ara|'u ara|ara|telefon et|çağrı yap"""), "").trim()
            if (contactName.isNotBlank() && !BLACKLIST.contains(contactName.lowercase())) {
                val payload = JSONObject().apply { put("name", contactName) }
                val summary = ActionDispatcherHelper.executeAction(context, "CALL_PHONE", payload)
                return DrawerResult(
                    replyText = "Hemen $contactName kişisini arıyorum.",
                    actionSummary = summary
                )
            }
        }
        if (lowerQuery.contains("whatsapp") && (lowerQuery.contains("mesaj") || lowerQuery.contains("yaz") || lowerQuery.contains("gönder"))) {
            val contactMatch = Regex("""(?i)(?:whatsapp'tan|whatsapp|whatsappta)\s+([a-zA-ZçğıöşüÇĞİÖŞÜ]+)""").find(query)
            val targetName = contactMatch?.groupValues?.get(1)?.trim() ?: ""
            if (targetName.isNotBlank()) {
                val messageContent = query.replace(Regex("""(?i)^.*?mesaj(?:ı)?\s*(?:at|yaz|gönder)[: ]*"""), "").trim().ifBlank { "Merhaba" }
                val payload = JSONObject().apply {
                    put("name", targetName)
                    put("message", messageContent)
                }
                val summary = ActionDispatcherHelper.executeAction(context, "SEND_WHATSAPP", payload)
                return DrawerResult(
                    replyText = "$targetName kişisine WhatsApp mesajını hazırladım.",
                    actionSummary = summary
                )
            }
        }
        return null
    }
}
