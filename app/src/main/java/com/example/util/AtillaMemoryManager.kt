package com.example.util

import android.content.Context
import com.example.data.AiKnowledgeEntity
import com.example.data.AppDatabase
import com.example.data.DataStoreManager
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class UserProfileInsight(
    val title: String,
    val details: String,
    val profession: String = "Tarih Öğretmeni & Eğitim Yöneticisi",
    val favoriteTopics: List<String> = listOf("Tarih", "Maarif Modeli", "Bilim", "Strateji", "Gurme Yemek"),
    val recentConversations: List<String> = emptyList(),
    val customPreferences: Map<String, String> = emptyMap()
)

object AtillaMemoryManager {

    private const val MEMORY_CATEGORY = "ATILLA_USER_PROFILE"
    private const val EPISODIC_CATEGORY = "ATILLA_CONVERSATION_HISTORY"

    // Oturum içi hafıza (Episodik Konuşma Tamponu - Son 10 konuşma)
    private val conversationBuffer = mutableListOf<Pair<String, String>>() // Pair(UserMessage, AtillaResponse)

    /**
     * Konuşmayı hafızaya kaydeder (Oturum ve Kalıcı Veritabanı)
     */
    suspend fun recordConversation(
        context: Context,
        userMessage: String,
        assistantReply: String
    ) {
        if (userMessage.isBlank() || assistantReply.isBlank()) return

        synchronized(conversationBuffer) {
            conversationBuffer.add(Pair(userMessage, assistantReply))
            if (conversationBuffer.size > 15) {
                conversationBuffer.removeAt(0)
            }
        }

        try {
            val db = AppDatabase.getDatabase(context)
            val now = System.currentTimeMillis()
            val timeStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.forLanguageTag("tr-TR")).format(Date(now))
            
            // Eğer mesajda kişisel bir tercih veya bilgi varsa ("adım", "severim", "dersim", "öğretmenim", "unutmama")
            val lower = userMessage.lowercase(Locale.forLanguageTag("tr-TR"))
            if (lower.contains("severim") || lower.contains("adım") || lower.contains("tercih") || 
                lower.contains("benim") || lower.contains("unutma") || lower.contains("aklında tut") ||
                lower.contains("öğretmen") || lower.contains("okul")) {
                
                db.aiKnowledgeDao().insertKnowledge(
                    AiKnowledgeEntity(
                        title = "Patron Tercihi: ${userMessage.take(40)}",
                        content = "Kayıt Zamanı: $timeStr | Kullanıcı İfadesi: $userMessage",
                        category = MEMORY_CATEGORY,
                        isOfficialVerified = true,
                        source = "ATİLLA Öğrenen Zeka Motoru",
                        createdAt = now
                    )
                )
            }
        } catch (_: Exception) {}
    }

    /**
     * Kullanıcıyı tanıma ve profil sentezi raporu üretir
     */
    suspend fun getProfileBriefing(context: Context, patronPrefix: String = "Sayın Patronum"): Pair<String, String> {
        val dataStoreManager = DataStoreManager(context)
        val currentNick = dataStoreManager.userNick.first()?.trim() ?: ""
        val db = AppDatabase.getDatabase(context)

        val savedMemories = try {
            db.aiKnowledgeDao().getAllKnowledgeList()
                .filter { it.category == MEMORY_CATEGORY }
                .takeLast(5)
                .map { it.content.substringAfter("Kullanıcı İfadesi: ") }
        } catch (_: Exception) {
            emptyList()
        }

        val recentTalks = synchronized(conversationBuffer) {
            conversationBuffer.takeLast(4).map { "• Siz: \"${it.first.take(35)}\" ➔ ATİLLA: \"${it.second.take(45)}...\"" }
        }

        val speech = buildString {
            append("Sizi çok iyi tanıyorum $patronPrefix. ")
            if (currentNick.isNotBlank()) append("Adınız $currentNick. ")
            append("Tarih Öğretmeni ve eğitim yöneticisisiniz. Maarif modeli, stratejik planlama ve yüksek verimlilik odaklısınız. Hafızamdaki tüm kayıtlar emrinizdedir.")
        }

        val reply = buildString {
            append("🧠 **ATİLLA ÖĞRENEN ZEKA: KULLANICI PROFİLİ VE HAFIZA RAPORU**\n")
            append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n")
            append("👤 **Kimlik & Hitap:** $patronPrefix ${if (currentNick.isNotBlank()) "($currentNick)" else ""}\n")
            append("🎓 **Meslek & Uzmanlık:** Tarih Öğretmeni / Pedagojik Lider (MEB Maarif Modeli Uzmanı)\n")
            append("⭐ **Çalışma İlkeleri:** Yüksek disiplin, süreç odaklı ölçme, sıfır gevezelik, kesin icraat\n")
            append("🍽️ **Mutfak & Yaşam Tarzı:** Gurme lezzetler, dengeli beslenme ve sirkadiyen sağlık ritmi\n\n")
            
            if (savedMemories.isNotEmpty()) {
                append("📌 **Hafızama Kazınan Özel Notlarınız:**\n")
                savedMemories.forEach { append("   • $it\n") }
                append("\n")
            }

            if (recentTalks.isNotEmpty()) {
                append("💬 **Son Konuşulan Konular (Epizodik Bellek):**\n")
                recentTalks.forEach { append("   $it\n") }
                append("\n")
            }

            append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
            append("💡 _Bana söylediğiniz her yeni bilgi, alışkanlık ve tercih öğrenen hafızama güvenle işlenmektedir._")
        }

        return Pair(reply, speech)
    }

    /**
     * "Daha önce ne konuşmuştuk?" sorusuna yanıt üretir
     */
    fun getRecentConversationsSummary(patronPrefix: String = "Sayın Patronum"): Pair<String, String> {
        val recentTalks = synchronized(conversationBuffer) {
            conversationBuffer.takeLast(5)
        }

        if (recentTalks.isEmpty()) {
            val s = "Bu oturumda henüz kayıtlı bir konuşma geçmişimiz bulunmuyor $patronPrefix. Ama tüm sistemlerim hazır, sizi dinliyorum."
            return Pair("💬 **Konuşma Hafızası:**\n\n$s", s)
        }

        val reply = buildString {
            append("💬 **Son Konuşmalarımızın Özeti ($patronPrefix):**\n\n")
            recentTalks.forEachIndexed { idx, pair ->
                append("${idx + 1}. **Talep:** \"${pair.first}\"\n")
                append("   ↳ **İcra:** ${pair.second.take(120)}...\n\n")
            }
            append("💡 _Hafızam güncel ve kesintisizdir efendim._")
        }

        val speech = "$patronPrefix, son konuştuğumuz ${recentTalks.size} konuyu hatırlıyorum: " +
                recentTalks.take(2).joinToString(", ") { it.first.take(30) } + ". Ayrıntıları ekranınıza getirdim."

        return Pair(reply, speech)
    }
}
