package com.example.util.assistant.drawers

import android.content.Context
import com.example.util.assistant.AssistantDrawer
import com.example.util.assistant.DrawerResult
import com.example.util.assistant.WardrobeSessionData
import java.util.Locale

object KnowledgeSearchDrawer : AssistantDrawer {
    override val drawerName: String = "Nokta Atışı Kütüphane Çekmecesi"

    override fun canHandle(query: String, lowerQuery: String): Boolean = true

    override suspend fun handle(
        context: Context,
        query: String,
        lowerQuery: String,
        sessionData: WardrobeSessionData
    ): DrawerResult? {
        val allKnowledge = sessionData.db.aiKnowledgeDao().getAllKnowledgeList()
        if (allKnowledge.isEmpty()) return null

        val searchWords = lowerQuery.split(Regex("""[\s,?.!;:()'"\-_/]+""")).filter { it.length >= 3 }
        if (searchWords.isEmpty()) return null

        val scored = allKnowledge.map { entity ->
            val titleLower = entity.title.lowercase(Locale("tr", "TR"))
            val contentLower = entity.content.lowercase(Locale("tr", "TR"))
            var score = 0
            for (w in searchWords) {
                if (titleLower.contains(w)) score += 6
                if (contentLower.contains(w)) score += 2
            }
            Pair(entity, score)
        }.filter { it.second >= 6 }.sortedByDescending { it.second }

        val topMatch = scored.firstOrNull()?.first ?: return null

        // Yalnızca soruyla ilgili tek bir konuyu özetle, ASLA tüm kütüphaneyi dökme!
        val cleanSummary = topMatch.content
            .lines()
            .filter { line -> searchWords.any { w -> line.contains(w, ignoreCase = true) } }
            .take(3)
            .joinToString("\n")

        val reply = if (cleanSummary.isNotBlank()) {
            "📌 ${topMatch.title}:\n$cleanSummary"
        } else {
            "📌 ${topMatch.title}:\n${topMatch.content.take(300)}..."
        }

        return DrawerResult(
            replyText = reply,
            actionSummary = "📚 Kütüphane: ${topMatch.title}"
        )
    }
}
