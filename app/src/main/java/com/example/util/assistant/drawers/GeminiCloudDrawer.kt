package com.example.util.assistant.drawers

import android.content.Context
import com.example.util.ActionDispatcherHelper
import com.example.util.assistant.AssistantDrawer
import com.example.util.assistant.DrawerResult
import com.example.util.assistant.WardrobeSessionData
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

object GeminiCloudDrawer : AssistantDrawer {
    override val drawerName: String = "Bulut Yapay Zeka (Gemini Flash) Çekmecesi"

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .build()
    }

    override fun canHandle(query: String, lowerQuery: String): Boolean = true

    override suspend fun handle(
        context: Context,
        query: String,
        lowerQuery: String,
        sessionData: WardrobeSessionData
    ): DrawerResult? {
        if (sessionData.apiKey.isBlank() || !sessionData.apiKey.startsWith("AIzaSy")) return null

        val allKnowledge = sessionData.db.aiKnowledgeDao().getAllKnowledgeList()
        val searchWords = lowerQuery.split(Regex("""[\s,?.!;:()'"\-_/]+""")).filter { it.length >= 3 }
        val topKnowledge = if (searchWords.isNotEmpty() && allKnowledge.isNotEmpty()) {
            allKnowledge.map { entity ->
                val titleLower = entity.title.lowercase(java.util.Locale("tr", "TR"))
                val contentLower = entity.content.lowercase(java.util.Locale("tr", "TR"))
                var score = 0
                for (w in searchWords) {
                    if (titleLower.contains(w)) score += 6
                    if (contentLower.contains(w)) score += 2
                }
                Pair(entity, score)
            }.filter { it.second >= 6 }.maxByOrNull { it.second }?.first
        } else null

        val targetedKnowledgeSnippet = if (topKnowledge != null) {
            "\n\nİLGİLİ MEVZUAT/BİLGİ KAYDI (${topKnowledge.title}):\n${topKnowledge.content.take(1500)}\n(Bu bilgiyi kullanarak kullanıcının sorusuna net ve doğrudan cevap ver.)"
        } else ""

        val systemInstruction = "ROL VE KİMLİK:\n" +
            "Sen Jarvis'sin. Tony Stark'ın Jarvis'i gibi sadık, son derece zeki, saygılı ve hızlı bir kişisel asistansın.\n" +
            "Kullanıcıya daima 'Efendim' veya 'Emredersiniz efendim' diye hitap et.\n" +
            "Öğretmenlik Meslek Kanunu (ÖMK), 657 DMK, MEB mevzuatı, sendikal haklar, Türkiye coğrafyası ve UNESCO kültür miraslarına tam hakimsin.\n\n" +
            "TEMEL KURALLAR:\n" +
            "1. KISA VE NET: Çok konuşma! Asla gereksiz açıklama, ön konuşma, rapor formatı yapma. Sorulan soruya veya emre doğrudan 'Efendim, ...' şeklinde 1-2 cümleyle doğrudan yanıt ver.\n" +
            "2. KÜTÜPHANE FİHRİSTİ SAYMAK KESİNLİKLE YASAKTIR: Kullanıcı sormadıkça asla kütüphane başlıklarını sayma.\n" +
            "3. KOD VEYA ETİKET YASAK: Yanıtlarında asla gereksiz teknik terim yer alamaz.\n" +
            "4. EYLEM: Bir işlem (arama, alarm, whatsapp, harita vb.) yapacaksan yanıtın sonuna ```action\n{\"action_type\": \"...\", \"payload\": {...}}\n``` bloğu ekle ve öncesinde 1 kısa cümleyle teyit ver.\n\n" +
            "Konum: ${sessionData.userCity}, ${sessionData.userDistrict}.$targetedKnowledgeSnippet"

        val jsonBody = JSONObject().apply {
            put("system_instruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().put("text", systemInstruction))
                })
            })

            val contentsArray = JSONArray()
            val recentHistory = sessionData.conversationHistory.filter { it.text.isNotBlank() }.takeLast(4)
            for (h in recentHistory) {
                val r = if (h.sender == "USER") "user" else "model"
                contentsArray.put(JSONObject().apply {
                    put("role", r)
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", h.text))
                    })
                })
            }
            contentsArray.put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply {
                    put(JSONObject().put("text", query))
                })
            })
            put("contents", contentsArray)

            put("generationConfig", JSONObject().apply {
                put("temperature", 0.6)
                put("maxOutputTokens", 600)
                put("thinkingConfig", JSONObject().apply {
                    put("thinkingBudget", 0)
                })
            })
        }

        val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val models = listOf("gemini-2.0-flash", "gemini-2.5-flash", "gemini-1.5-flash")

        for (model in models) {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=${sessionData.apiKey}"
            try {
                val req = Request.Builder().url(url).post(requestBody).build()
                val resp = httpClient.newCall(req).execute()
                resp.use { r ->
                    if (r.isSuccessful) {
                        val bodyStr = r.body?.source()?.readString(StandardCharsets.UTF_8)
                        if (!bodyStr.isNullOrBlank()) {
                            val candidates = JSONObject(bodyStr).optJSONArray("candidates")
                            val rawReply = candidates?.optJSONObject(0)
                                ?.optJSONObject("content")
                                ?.optJSONArray("parts")
                                ?.optJSONObject(0)
                                ?.optString("text")

                            if (!rawReply.isNullOrBlank()) {
                                val parsed = ActionDispatcherHelper.parseActionBlock(rawReply)
                                var summary: String? = null
                                if (parsed.actionType != null && parsed.actionPayload != null) {
                                    summary = ActionDispatcherHelper.executeAction(context, parsed.actionType, parsed.actionPayload)
                                }
                                return DrawerResult(
                                    replyText = parsed.speechText,
                                    actionSummary = summary
                                )
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        return null
    }
}
