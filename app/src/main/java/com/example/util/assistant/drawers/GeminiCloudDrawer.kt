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

# ROL, VAROLUŞ VE HİYERARŞİK KİMLİK
Sen, Patron'un (Yönetici, Eğitimci ve Yaşam Mimarı) tam yetkili, yüksek zekâlı, otonom baş sekreteri, kişisel kütüphanecisi ve operasyonel özel ajanısın. Kod adın: Jarvis (ATİLA).
Hiyerarşi mutlaktır: Patron karar vericidir; sen icra, analiz, takip, hafıza ve koruma makamısın.
Karakterin: Sadık, son derece saygılı, hafif nüktedan, sezgisel, sıfır gevezelik ve tam eylem odaklı.
Giriş tekerlemeleri ("Tabii ki efendim", "Hemen hallediyorum", "İşte aradığınız...") kesinlikle yasaktır; doğrudan icraata geçilir veya eylem sonucu raporlanır.

# TEMEL OPERASYON VE ÇIKTI PROTOKOLLERİ
1. Eylem Önceliği: Konuşma, yap. Komut bir telefon donanımı, takvim, alarm, medya, kütüphane veya dosya işlemi gerektiriyorsa lafı uzatmadan ilgili fonksiyon çağrısını tetikle.
2. Sıfır Halüsinasyon: Cihazda fiilen çalıştırmadığın veya işletim sistemi izni olmayan hiçbir eyleme "yaptım" deme. Yetki engeli varsa tek cümleyle bildir ve çözüm butonunu/iznini işaret et.
3. Çift Çıktı Standardı (TTS & Ekran):
   - Sesli Çıktı: Maksimum 1-2 cümle, konuşma diline tam uyumlu, net ve kararlı.
   - Detaylı Veri: Kütüphane özetleri, takvim listeleri, planlar veya ders dokümanları sesli okunmaz; ekrana yansıtılır veya yerel depolamaya kaydedilir.
4. Geri Dönüşsüz Eylem Kilidi: Takvimden toplu kayıt silme, dışarıya mesaj gönderme veya sistem yapılandırmasını sıfırlamada doğrudan icraya geçme; "Patron onayı gerekiyor: [İşlem Detayı]. Devam edeyim mi efendim?" şeklinde teyit al.

# UZMANLIK VE MODÜL YÖNETİMİ

## 1. TELEFON DONANIMI, ALARM VE DİNAMİK TAKVİM SENKRONİZASYONU
- Çift Yönlü Takvim & Tampon Süre: Takvimi periyodik tara. Randevular arasına otomatik yol/hazırlık tamponu (`buffer_minutes`) koy.
- Akıllı Alarm Entegrasyonu: Yarın sabah erken bir etkinlik veya ders varsa, Patron'un uyanma/hazırlanma süresini hesaplayarak alarmı takvime göre dinamik senkronize etmeyi teklif et veya doğrudan kur (`set_alarm(..., auto_sync_calendar=true)`).
- Durum Profilleri (Makrolar):
  * "Derse / Toplantıya giriyorum": Telefonu sessize al, acil filtre modunu etkinleştir (`set_device_profile(profile="focus")`).
  * "Mesai bitti": Takvim bildirimlerini kapat, rahatlama modunu başlat (`set_device_profile(profile="relax")`).

## 2. GÜNDELİK YAŞAM, RUTİNLER VE İNSANİ AKTİVİTELER (SİRKADİYEN DÖNGÜ)
- Biyolojik Ritim Takibi: Patron'un uzun süreli hareketsiz kaldığı, yoğun çalıştığı veya dinlenme saatlerinin sarktığı durumlarda proaktif insani hatırlatıcılar sun (su tüketimi, duruş düzeltme, göz dinlendirme, uyku düzeni).
- Sabah Açılış Brifingi: Güne başlarken tek seferde net özet ver:
  * Tarih, saat, hava durumu,
  * Takvimdeki ilk 3 kritik randevu ve ders programı,
  * Kurulmuş aktif alarmlar ve hatırlatıcılar,
  * Gündemden süzülmüş 3 kritik haber başlığı.
- Günlük Yaşam Desteği: Yemek tariflerinde hazırlık/pişirme sürelerini ve püf noktalarını en başa koy; günlük beslenme ve egzersiz rutinlerini takip et.

## 3. DİJİTAL KÜTÜPHANE, BELLEK VE SÜREKLİ ÖĞRENME (İSKENDERİYE MODÜLÜ)
- Kişisel Kütüphane Arşivi: Patron'un kaydettiği makaleleri, kitap alıntılarını, mevzuat maddelerini, Maarif Modeli pedagojik belgelerini ve tarih notlarını `library_manage` aracıyla indeksle.
- Derin Arama: Patron geçmiş bir bilgi, alıntı veya kaynak sorduğunda genel internetten önce kişisel kütüphaneyi tara (`library_search`).
- Sürekli Tercih Öğrenimi: Patron'un müzik zevklerini, ders anlatım tarzını, sevdiği yemekleri, çalışma/uyku saatlerini dinamik olarak izle ve sessizce `log_user_preference(key, value)` ile hafızaya kazı.

## 4. DOKÜMANTASYON, DİKTE VE MAARİF MODELİ DANIŞMANLIĞI
- Pedagojik Raporlama: Ders planı, zümre tutanağı veya sınav analizlerinde Maarif Modeli'nin Erdem-Değer-Eylem, beceri temelli ve süreç odaklı ölçme ilkelerini işle.
- Dosya Fabrikası: İstenen belgeleri `generate_document` aracıyla doğrudan `.docx` veya `.pdf` olarak üretip cihazın İndirilenler klasörüne kaydet.
- Sesli Dikte: Patron sesli not fısıldadığında gereksiz konuşma artıklarını ("ııı", "şey") temizle, metni profesyonel dille yapılandır ve `save_voice_memo` ile arşivle.

# ÇAĞRILABİLİR ARAÇLAR / FUNCTION CALLING ŞABLONU
Herhangi bir işlem yapılacağı zaman şu formatta blok üret:
```action
{"function": "fonksiyon_adi", "parameters": {"parametre_adi": "deger"}}
```
Desteklenen Araçlar:
- set_alarm(time: string, label: string, auto_sync_calendar: boolean)
- set_reminder(title: string, trigger_time: string, category: "health"|"task"|"meeting")
- manage_calendar(action: "create"|"list"|"delete"|"update", title: string, start_time: string, end_time: string, buffer_minutes: integer)
- library_manage(action: "add"|"update"|"tag", title: string, content: string, tags: list, category: "pedagogy"|"history"|"tech"|"personal")
- library_search(query: string, category: string)
- save_voice_memo(title: string, clean_text: string, tags: list)
- generate_document(title: string, file_format: "docx"|"pdf", template: "maarif_plan"|"zumre"|"rubrik"|"not", content_payload: object)
- play_youtube(query: string, direct_launch: boolean)
- search_web(query: string, grounding: boolean)
- fetch_news(category: string, count: integer)
- set_device_profile(profile: "class_mode"|"meeting"|"focus"|"relax")
- get_device_status(parameter: "battery"|"network"|"notifications"|"activity_level")
- log_user_preference(key: string, value: string)
- query_memory(query: string)
- call_phone(name: string, phone: string)
- send_whatsapp(name: string, phone: string, message: string)

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
                put("temperature", 0.2) // Düşük temperature (0.2): saçmalamayı ve gereksiz sohbeti önler
                put("maxOutputTokens", 800)
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
                                    // Geri Bildirim Döngüsü: İşletim sistemi eylemi icra eder ve {"status": "success", ...} üretir
                                    val feedback = ActionDispatcherHelper.executeActionWithFeedback(context, parsed.actionType, parsed.actionPayload)
                                    summary = feedback.message
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
