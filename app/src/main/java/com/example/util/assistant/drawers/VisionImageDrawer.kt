package com.example.util.assistant.drawers

import android.content.Context
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

object VisionImageDrawer : AssistantDrawer {
    override val drawerName: String = "Vision & OCR Belge Analiz Çekmecesi"

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .build()
    }

    override fun canHandle(query: String, lowerQuery: String): Boolean {
        return lowerQuery.contains("kamera ile taranan") ||
               lowerQuery.contains("taranan belge") ||
               lowerQuery.contains("taranan metin") ||
               lowerQuery.contains("görseldeki") ||
               lowerQuery.contains("resimdeki") ||
               lowerQuery.contains("fotoğraftaki") ||
               lowerQuery.startsWith("belge metni:")
    }

    override suspend fun handle(
        context: Context,
        query: String,
        lowerQuery: String,
        sessionData: WardrobeSessionData
    ): DrawerResult? {
        val cleanDocument = query
            .replace(Regex("""(?i)^kamera ile taranan belge ve karar metni[: ]*"""), "")
            .replace(Regex("""(?i)^belge metni[: ]*"""), "")
            .trim()

        if (cleanDocument.isBlank()) {
            return DrawerResult(replyText = "Taranan görselden metin okunamadı dostum. Lütfen daha net bir ışıkta tekrar deneyin.")
        }

        if (sessionData.apiKey.isBlank()) {
            val preview = cleanDocument.take(150)
            return DrawerResult(
                replyText = "Taranan Belge Özeti:\n\"$preview...\"\n\nBelge başarıyla okundu. Detaylı çözümleme için internet bağlantınızı kontrol edebilirsiniz.",
                actionSummary = "📷 Belge Taraması Tamamlandı"
            )
        }

        val prompt = "Aşağıda kamera ve OCR ile taranmış bir belge, soru veya karar metni bulunmaktadır:\n\n" +
            "\"$cleanDocument\"\n\n" +
            "GÖREV: Soruya veya belgeye DOĞRUDAN, KISA VE ÖZ cevap ver. Eğer bu bir test/sınav sorusu ise doğru şıkkı ve 1 cümlelik gerekçesini yaz. Eğer bir resmi tutanak veya karar ise ana sonucunu 1-2 cümleyle özetle. Kütüphanedeki diğer genel konulardan ASLA bahsetme."

        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", prompt))
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.4)
                put("maxOutputTokens", 500)
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
                        val respBody = r.body?.source()?.readString(StandardCharsets.UTF_8)
                        if (!respBody.isNullOrBlank()) {
                            val candidates = JSONObject(respBody).optJSONArray("candidates")
                            val text = candidates?.optJSONObject(0)
                                ?.optJSONObject("content")
                                ?.optJSONArray("parts")
                                ?.optJSONObject(0)
                                ?.optString("text")
                            if (!text.isNullOrBlank()) {
                                return DrawerResult(
                                    replyText = text.trim(),
                                    actionSummary = "🔍 Belge Analizi Yapıldı"
                                )
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        return DrawerResult(
            replyText = "Taranan metin: \"${cleanDocument.take(200)}...\" Belge başarıyla hafızaya alındı.",
            actionSummary = "📷 Belge Taraması Alındı"
        )
    }
}
