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
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    override fun canHandle(query: String, lowerQuery: String): Boolean = true

    override suspend fun handle(
        context: Context,
        query: String,
        lowerQuery: String,
        sessionData: WardrobeSessionData
    ): DrawerResult? {
        if (sessionData.apiKey.isBlank() || sessionData.apiKey.length < 15) return null

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
            "\n\nRESMİ DEVLET MEVZUATI / BİLGİ KAYDI (${topKnowledge.title}):\n${topKnowledge.content.take(1500)}\n(Bu resmi bilgiyi referans alarak kullanıcıya net ve doyurucu cevap ver.)"
        } else ""

        val systemInstruction = "ROL VE KİMLİK:\n" +
            "Sen ATİLA'sın. Tony Stark'ın Jarvis'i gibi son derece zeki, saygılı, esprili, pratik ve her konuya hakim üstün bir kişisel asistansın.\n" +
            "Kullanıcıya daima 'Efendim' veya 'Emredersiniz efendim' diye hitap et.\n" +
            "Öğretmenlik Meslek Kanunu (ÖMK), 657 DMK, MEB mevzuatı, sendikal haklar, Türkiye coğrafyası, devletin resmi kaynakları (mevzuat.gov.tr, meb.gov.tr, resmigazete.gov.tr, titck.gov.tr, mgm.gov.tr) konusunda tam bir uzmansın.\n\n" +
            "YETENEKLER VE DAVRANIŞ:\n" +
            "1. FİKİR VE TAVSİYE: Kullanıcı bir konuda fikrini sorduğunda (örn: bir karar, telefon, araba, mesleki adım) doğrudan akılcı, artı ve eksileri özetleyen net bir değerlendirme sun.\n" +
            "2. GÜNDELİK VE ANSİKLOPEDİK SORULAR: Bilim, sanat, teknoloji, tarih veya gündelik hayat sorularında doğrudan, akıcı ve doyurucu cevap ver.\n" +
            "3. DOĞAL VE AKICI DİL: Asla robotik veya sıkıcı olma. Gereksiz rapor formatı ve fihrist sayma yapma.\n" +
            "4. SİSTEM AKSİYONLARI: Bir işlem (arama, whatsapp, sms, alarm, harita, uygulama açma vb.) yapacaksan yanıtın sonuna ```action\n{\"action_type\": \"...\", \"payload\": {...}}\n``` bloğu ekle.\n\n" +
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
                put("temperature", 0.7)
                put("maxOutputTokens", 800)
            })
        }

        val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val models = listOf("gemini-1.5-flash", "gemini-2.0-flash", "gemini-1.5-pro")

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
                                    actionSummary = summary,
                                    speechText = parsed.speechText
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
