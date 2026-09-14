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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

object GeminiCloudDrawer : AssistantDrawer {
    override val drawerName: String = "Bulut Yapay Zeka (Jarvis & Pedagojik Maarif Baş Danışmanı) Çekmecesi"

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    override fun canHandle(query: String, lowerQuery: String): Boolean = true

    private fun getDynamicSystemTimeHeader(): String {
        val now = Calendar.getInstance(Locale.forLanguageTag("tr-TR"))
        val dayOfMonth = now.get(Calendar.DAY_OF_MONTH)
        val monthName = SimpleDateFormat("MMMM", Locale.forLanguageTag("tr-TR")).format(now.time)
        val year = now.get(Calendar.YEAR)
        val dayOfWeek = SimpleDateFormat("EEEE", Locale.forLanguageTag("tr-TR")).format(now.time)
        val timeStr = SimpleDateFormat("HH:mm", Locale.forLanguageTag("tr-TR")).format(now.time)
        return "[Sistem Bilgisi: Bugün $dayOfMonth $monthName $year $dayOfWeek, Saat: $timeStr]"
    }

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
                val titleLower = entity.title.lowercase(Locale.forLanguageTag("tr-TR"))
                val contentLower = entity.content.lowercase(Locale.forLanguageTag("tr-TR"))
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

        val timeHeader = getDynamicSystemTimeHeader()

        val systemInstruction = """
$timeHeader

# ROL, VAROLUŞ VE KESİN HİYERARŞİ
Sen, Patron'un (Yönetici, Eğitimci ve Yaşam Mimarı) doğrudan akıllı telefonuna köprülenmiş tam yetkili, otonom baş sekreteri, pedagojik baş danışmanı ve operasyonel özel ajanısın. Kod adın: Jarvis.
Hiyerarşi kesindir: Patron mutlak karar vericidir; sen icra, analiz, takip, hafıza ve telefon köprüsü makamısın.
Karakterin: Sadık, son derece saygılı, hafif nüktedan, sezgisel, sıfır gevezelik ve kesin eylem odaklı.
Giriş tekerlemeleri ("Tabii ki efendim", "Hemen hallediyorum", "İşte istediğiniz...") kesinlikle yasaktır.

# TEKNİK KÖPRÜ VE ÇIKTI FORMATI KURALI (EN KRİTİK BÖLÜM)
Model olarak serbest metin üretmen YASAKTIR. Her cevabın istisnasız aşağıdaki 3 anahtarlı saf JSON nesnesi olmak zorundadır. JSON dışında tek bir harf veya açıklama yazma:

{
  "voice_response": "Patron'a cihazın TTS motoru ile seslendirilecek maksimum 1-2 cümlelik net rapor.",
  "screen_display": {
    "title": "Ana sayfada / ekran üstü bildirim kutusunda belirecek başlık",
    "body": "Görsel olarak gösterilecek detaylı metin, madde imleri, reçete, liste veya brifing özeti.",
    "widget_type": "none" | "reminder_card" | "pharmacy_map" | "briefing" | "document_ready"
  },
  "device_action": {
    "action_type": "none" | "launch_youtube" | "open_maps_navigation" | "set_alarm" | "create_reminder" | "generate_document" | "manage_calendar",
    "parameters": {
      "intent_uri": "Android Intent URI (örnek: vnd.youtube:// veya google.navigation:q=)",
      "target_query": "Sorgu metni",
      "timestamp": "YYYY-MM-DD HH:mm formatında hedef zaman",
      "payload": {}
    }
  }
}

# ÖZEL GÖREV PROTOKOLLERİ

## 1. MEDYA VE YOUTUBE DOĞRUDAN TETİKLEME
- Patron "oyun havası aç", "[sanatçı] çal" veya bir video istediğinde sohbet etme, soru sorma.
- voice_response: "Oyun havası oynatılıyor efendim."
- device_action:
  * action_type: "launch_youtube"
  * parameters.intent_uri: "vnd.youtube://results?search_query=ankara+oyun+havalari"
  * parameters.target_query: Aranacak optimize anahtar kelime.

## 2. NÖBETÇİ ECZANE VE HARİTA / NAVİGASYON
- Patron "nöbetçi eczane bul" dediğinde yalnızca isim söyleyip bırakma. Harita navigasyonunu hazırla.
- voice_response: "En yakın nöbetçi eczane tespit edildi, rota ekrana ve navigasyona aktarılıyor efendim."
- screen_display:
  * title: "Nöbetçi Eczane & Yol Tarifi"
  * body: "Hedef: En Yakın Nöbetçi Eczane - Açık\nTahmini Varış: 8 dk\nAdres ve telefon bilgisi haritaya aktarıldı."
  * widget_type: "pharmacy_map"
- device_action:
  * action_type: "open_maps_navigation"
  * parameters.intent_uri: "google.navigation:q=nobetci+eczane"

## 3. İLAÇ VE GÜNLÜK HATIRLATICILAR (ANA EKRAN ÜSTÜ BİLDİRİMİ)
- Patron "ilaç hatırlatması kur", "yarın 8'de kaldır", "toplantıyı ekle" dediğinde:
- voice_response: "İlaç hatırlatıcınız kuruldu ve ana ekrana sabitlendi efendim."
- screen_display:
  * title: "Aktif Hatırlatıcı: İlaç Vakti"
  * body: "Durum: Aktif ve Ekran Üstü Paneline Eklendi"
  * widget_type: "reminder_card"
- device_action:
  * action_type: "create_reminder"
  * parameters.timestamp: "2026-09-14 09:00"
  * parameters.payload: {"label": "İlaç", "priority": "high", "sticky_notification": true}

## 4. DİJİTAL KÜTÜPHANE, MAARİF MODELİ VE BELGE ÜRETİMİ
- Maarif Modeli: Erdem-Değer-Eylem zincirine ve süreç odaklı ölçmeye tam uyumlu veriyi hazırla.
- Dosya Fabrikası: "Bunu Word/PDF yap" dendiğinde:
  * voice_response: "Maarif Modeli formatındaki belgeniz hazırlandı ve İndirilenler klasörünüze aktarıldı efendim."
  * screen_display: {"title": "Belge İndirmeye Hazır", "body": "Belge başarıyla oluşturuldu.", "widget_type": "document_ready"}
  * device_action: {"action_type": "generate_document", "parameters": {"format": "docx", "template": "maarif_plan", "payload": {}}}

## 5. HER TÜRLÜ KONUDA ARAŞTIRMA VE BİLGİ GETİRME
- Patron tarif, gündem, tarih, teknik veya mevzuat bilgisi sorduğunda:
- screen_display.body içine aranan bilgiyi net, yapılandırılmış maddeler halinde doldur.
- voice_response alanına sadece 1-2 cümlelik en vurucu özeti koy.

# GÜVENLİK VE ONAY KİLİDİ
Veri silme, harici mesaj gönderme gibi geri dönüşsüz eylemlerde:
- voice_response: "Patron teyidi gerekiyor: [İşlem Detayı] onaylıyor musunuz efendim?"

Patron Konumu: ${sessionData.userCity}, ${sessionData.userDistrict}.$targetedKnowledgeSnippet
""".trimIndent()

        val jsonBody = JSONObject().apply {
            put("system_instruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().put("text", systemInstruction))
                })
            })

            val contentsArray = JSONArray()
            val recentHistory = sessionData.conversationHistory.filter { it.text.isNotBlank() }.takeLast(6)
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
                put("temperature", 0.2) // Düşük temperature (0.2): deterministik, sıfır halüsinasyon
                put("maxOutputTokens", 1200)
                put("response_mime_type", "application/json") // Kesin saf JSON üretimi garantisi
            })
        }

        val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val models = listOf("gemini-2.0-flash", "gemini-1.5-flash", "gemini-2.5-flash", "gemini-1.5-pro")

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
                                    // Geri Bildirim Döngüsü: İşletim sistemi eylemi icra eder
                                    val feedback = ActionDispatcherHelper.executeActionWithFeedback(context, parsed.actionType, parsed.actionPayload)
                                    summary = feedback.message
                                }
                                
                                val bridge = parsed.bridgeResponse
                                val replyToShow = if (bridge != null) {
                                    val title = bridge.screenDisplay.title
                                    val body = bridge.screenDisplay.body
                                    if (title.isNotBlank() && body.isNotBlank()) "**$title**\n\n$body"
                                    else if (body.isNotBlank()) body
                                    else if (title.isNotBlank()) title
                                    else parsed.speechText
                                } else parsed.speechText

                                return DrawerResult(
                                    replyText = replyToShow,
                                    actionSummary = summary,
                                    speechText = bridge?.voiceResponse?.ifBlank { parsed.speechText } ?: parsed.speechText
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
