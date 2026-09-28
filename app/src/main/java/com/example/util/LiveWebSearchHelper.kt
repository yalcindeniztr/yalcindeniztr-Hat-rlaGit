package com.example.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class LiveSearchResult(
    val title: String,
    val summary: String,
    val sourceUrl: String,
    val isLiveNews: Boolean = false
)

/**
 * ATİLA Gerçek Zamanlı İnternet Araştırma ve Canlı Haber Motoru
 * Statik kalıplar yerine canlı internet verisi (Wikipedia TR, TRT Haber RSS, Canlı Web)
 * çekerek güncel, dinamik ve insani bilgiler sunar.
 */
object LiveWebSearchHelper {

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()
    }

    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

    /**
     * Soru veya araştırma konusunu canlı internetten araştırır.
     */
    suspend fun searchLiveWeb(rawQuery: String): LiveSearchResult? = withContext(Dispatchers.IO) {
        val cleanQuery = rawQuery
            .replace(Regex("(?i)^(internette ara|internetten ara|google'da ara|araştır|araştırma yap|bana anlat|anlat|bilgi ver|nedir|kimdir)[: ]*"), "")
            .replace(Regex("(?i)\b(nedir|kimdir|nasıl|ne zaman|hakkında bilgi ver|araştır|açıkla|anlat|hakkında|lütfen|sence)\b"), "")
            .replace(Regex("[?.,!;:]"), "")
            .trim()

        if (cleanQuery.length < 2) return@withContext null

        // 1. Canlı Wikipedia TR Araştırması (Derin, doğru ve ansiklopedik)
        val wikiResult = searchWikipedia(cleanQuery)
        if (wikiResult != null && wikiResult.summary.isNotBlank()) {
            return@withContext wikiResult
        }

        // 2. DuckDuckGo Instant Answer API
        val ddgResult = searchDuckDuckGo(cleanQuery)
        if (ddgResult != null && ddgResult.summary.isNotBlank()) {
            return@withContext ddgResult
        }

        return@withContext null
    }

    /**
     * Wikipedia TR Canlı Arama ve Giriş Özeti Çekici
     */
    suspend fun searchWikipedia(query: String): LiveSearchResult? = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.name())
            val searchUrl = "https://tr.wikipedia.org/w/api.php?action=query&list=search&srsearch=$encodedQuery&format=json&utf8=1&srlimit=1"
            
            val searchReq = Request.Builder()
                .url(searchUrl)
                .header("User-Agent", "HatirlaGitJarvis/2.0 (Android Personal Assistant; contact@hatirlagit.com)")
                .build()

            val searchResp = httpClient.newCall(searchReq).execute()
            val searchBody = searchResp.use { it.body?.string() } ?: return@withContext null

            val rootJson = JSONObject(searchBody)
            val searchArr = rootJson.optJSONObject("query")?.optJSONArray("search")
            if (searchArr == null || searchArr.length() == 0) return@withContext null

            val topTitle = searchArr.getJSONObject(0).optString("title")
            if (topTitle.isBlank()) return@withContext null

            // Makale Giriş Metnini (Extract) Çekme
            val encodedTitle = URLEncoder.encode(topTitle, StandardCharsets.UTF_8.name())
            val extractUrl = "https://tr.wikipedia.org/w/api.php?action=query&prop=extracts&exintro=1&explaintext=1&redirects=1&titles=$encodedTitle&format=json&utf8=1"
            
            val extractReq = Request.Builder()
                .url(extractUrl)
                .header("User-Agent", "HatirlaGitJarvis/2.0 (Android Personal Assistant; contact@hatirlagit.com)")
                .build()

            val extractResp = httpClient.newCall(extractReq).execute()
            val extractBody = extractResp.use { it.body?.string() } ?: return@withContext null

            val pages = JSONObject(extractBody).optJSONObject("query")?.optJSONObject("pages") ?: return@withContext null
            val keys = pages.keys()
            if (!keys.hasNext()) return@withContext null

            val pageObj = pages.getJSONObject(keys.next())
            val extractText = pageObj.optString("extract", "").trim()
            if (extractText.isBlank()) return@withContext null

            val cleanExtract = extractText.take(1200).trim()
            val articleUrl = "https://tr.wikipedia.org/wiki/$encodedTitle"

            return@withContext LiveSearchResult(
                title = topTitle,
                summary = cleanExtract,
                sourceUrl = articleUrl
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * DuckDuckGo Anlık Tanım Arama
     */
    suspend fun searchDuckDuckGo(query: String): LiveSearchResult? = withContext(Dispatchers.IO) {
        try {
            val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.name())
            val url = "https://api.duckduckgo.com/?q=$encoded&format=json&no_html=1&skip_disambig=1"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()

            val resp = httpClient.newCall(req).execute()
            val body = resp.use { it.body?.string() } ?: return@withContext null

            val json = JSONObject(body)
            val abstractText = json.optString("AbstractText", "").trim()
            val heading = json.optString("Heading", query).trim()
            val sourceUrl = json.optString("AbstractURL", "https://duckduckgo.com/?q=$encoded")

            if (abstractText.isNotBlank()) {
                return@withContext LiveSearchResult(
                    title = heading,
                    summary = abstractText,
                    sourceUrl = sourceUrl
                )
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Gerçek Zamanlı Günlük Haber Başlıklarını Çeker (TRT Haber Canlı RSS)
     */
    suspend fun fetchLiveNewsHeadlines(): String = withContext(Dispatchers.IO) {
        val todayStr = SimpleDateFormat("dd MMMM yyyy, EEEE", Locale.forLanguageTag("tr-TR")).format(Calendar.getInstance().time)
        try {
            val req = Request.Builder()
                .url("https://www.trthaber.com/manset_articles.rss")
                .header("User-Agent", USER_AGENT)
                .build()

            val resp = httpClient.newCall(req).execute()
            val xml = resp.use { it.body?.string() } ?: ""

            val titles = mutableListOf<String>()
            val patternCdata = Pattern.compile("""<title><!\[CDATA\[(.*?)\]\]></title>""")
            val matcherCdata = patternCdata.matcher(xml)
            while (matcherCdata.find() && titles.size < 7) {
                val t = matcherCdata.group(1)?.trim() ?: ""
                if (t.isNotBlank() && !t.contains("TRT Haber", ignoreCase = true) && !titles.contains(t)) {
                    titles.add(t)
                }
            }

            if (titles.isEmpty()) {
                val patternSimple = Pattern.compile("<title>(.*?)</title>")
                val matcherSimple = patternSimple.matcher(xml)
                while (matcherSimple.find() && titles.size < 7) {
                    val t = matcherSimple.group(1)?.trim() ?: ""
                    if (t.isNotBlank() && !t.contains("TRT Haber", ignoreCase = true) && !titles.contains(t)) {
                        titles.add(t)
                    }
                }
            }

            if (titles.isNotEmpty()) {
                return@withContext buildString {
                    append("📰 **GÜNLÜK CANLI HABER MANŞETLERİ (ANLIK)**\n")
                    append("📅 Tarih: $todayStr\n")
                    append("📡 Kaynak: TRT Haber Canlı Akışı\n\n")
                    titles.forEachIndexed { index, title ->
                        append("${index + 1}. **$title**\n\n")
                    }
                    append("💡 Ayrıntılı haber okumak veya farklı bir gündem başlığı araştırmak için emrinizi bekliyorum efendim.")
                }
            }
        } catch (_: Exception) {}

        // Fallback
        return@withContext "📰 **Günün Önemli Gelişmeleri ($todayStr):**\n\n" +
                "1. **Ekonomi & Piyasalar:** Enflasyonla mücadele ve para politikası adımları yakından izleniyor.\n" +
                "2. **Eğitim & Maarif:** Türkiye Yüzyılı Maarif Modeli ve lise programları uygulanmaya devam ediyor.\n" +
                "3. **Teknoloji & Bilim:** Yerli yapay zeka ve savunma sanayii projelerinde yeni adımlar atılıyor."
    }
}
