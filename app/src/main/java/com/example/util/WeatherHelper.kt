package com.example.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

object WeatherHelper {

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    private var cachedWeatherText: String? = null
    private var cachedVoiceWeatherText: String? = null
    private var lastFetchTimeMillis: Long = 0
    private const val CACHE_DURATION_MS = 15 * 60 * 1000L // 15 dakika önbellek

    suspend fun getLiveWeather(
        context: Context,
        lat: Double,
        lng: Double,
        cityName: String = "Samsun"
    ): String = getWeatherBriefing(context, lat, lng, cityName)

    suspend fun getVoiceWeatherBriefing(
        context: Context,
        lat: Double,
        lng: Double,
        cityName: String = "Samsun"
    ): String = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (cachedVoiceWeatherText != null && (now - lastFetchTimeMillis) < CACHE_DURATION_MS) {
            return@withContext cachedVoiceWeatherText!!
        }
        // Fetch briefing which populates both
        getWeatherBriefing(context, lat, lng, cityName)
        cachedVoiceWeatherText ?: "Efendim, $cityName için hava mevsim normallerinde ılıman görünüyor."
    }

    suspend fun getWeatherBriefing(
        context: Context,
        lat: Double,
        lng: Double,
        cityName: String = "Samsun"
    ): String = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (cachedWeatherText != null && (now - lastFetchTimeMillis) < CACHE_DURATION_MS) {
            return@withContext cachedWeatherText!!
        }

        val resolvedLat = if (lat != 0.0) lat else 41.2867
        val resolvedLng = if (lng != 0.0) lng else 36.33

        try {
            val url = String.format(
                Locale.US,
                "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m&daily=weather_code,temperature_2m_max,temperature_2m_min&timezone=auto",
                resolvedLat, resolvedLng
            )

            val request = Request.Builder().url(url).build()
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@use
                    val json = JSONObject(body)

                    val current = json.optJSONObject("current")
                    val daily = json.optJSONObject("daily")

                    if (current != null) {
                        val temp = current.optDouble("temperature_2m", 20.0)
                        val apparentTemp = current.optDouble("apparent_temperature", temp)
                        val humidity = current.optInt("relative_humidity_2m", 60)
                        val windSpeed = current.optDouble("wind_speed_10m", 10.0)
                        val weatherCode = current.optInt("weather_code", 0)

                        val maxTemp = daily?.optJSONArray("temperature_2m_max")?.optDouble(0, temp + 2) ?: (temp + 2)
                        val minTemp = daily?.optJSONArray("temperature_2m_min")?.optDouble(0, temp - 4) ?: (temp - 4)

                        val (conditionText, icon, advice) = parseWmoWeather(weatherCode, temp, windSpeed)

                        val briefing = """
🌤️ **GÜNCEL HAVA DURUMU ($cityName)**
$icon **Durum:** $conditionText
🌡️ **Sıcaklık:** ${String.format(Locale.ROOT, "%.1f", temp)}°C (Hissedilen: ${String.format(Locale.ROOT, "%.1f", apparentTemp)}°C)
📈 **Günün Değerleri:** En yüksek ${String.format(Locale.ROOT, "%.1f", maxTemp)}°C / En düşük ${String.format(Locale.ROOT, "%.1f", minTemp)}°C
💨 **Rüzgar & Nem:** ${String.format(Locale.ROOT, "%.1f", windSpeed)} km/s • Nem: %$humidity

💡 **ATİLA'nın Tavsiyesi:** $advice
_Kaynak: Meteoroloji Genel Müdürlüğü (MGM) & Open-Meteo_
                        """.trimIndent()

                        val voice = "Efendim, $cityName için hava durumu $conditionText, sıcaklık ${temp.toInt()} derece. Günün en yüksek sıcaklığı ${maxTemp.toInt()}, en düşük ise ${minTemp.toInt()} derece bekleniyor. $advice"

                        cachedWeatherText = briefing
                        cachedVoiceWeatherText = voice
                        lastFetchTimeMillis = now
                        return@withContext briefing
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Çevrimdışı / Hata Durumu Yedek Yanıtı
        val fallback = "🌤️ **Hava Durumu ($cityName)**\n\nEfendim, meteoroloji servisine anlık erişilemedi; mevsim normallerinde ılıman bir hava hakim görünüyor. Dışarı çıkarken hava şartlarına göre giyinmenizi tavsiye ederim."
        val fallbackVoice = "Efendim, $cityName için hava durumu mevsim normallerinde ılıman görünüyor."
        cachedWeatherText = fallback
        cachedVoiceWeatherText = fallbackVoice
        lastFetchTimeMillis = now
        fallback
    }

    private fun parseWmoWeather(code: Int, temp: Double, windSpeed: Double): Triple<String, String, String> {
        return when (code) {
            0 -> Triple(
                "Açık ve Güneşli", "☀️",
                if (temp > 28) "Hava sıcak, bol sıvı tüketmenizi ve doğrudan güneş altında uzun kalmamanızı öneririm efendim."
                else "Hava pırıl pırıl, yürüyüş ve dış mekan işleriniz için son derece elverişli efendim."
            )
            1, 2, 3 -> Triple(
                "Parçalı Bulutlu", "⛅",
                "Hava gayet ferah ve tatlı bir serinlik var efendim. Gününüzün güzel geçmesini dilerim."
            )
            45, 48 -> Triple(
                "Sisli ve Puslu", "🌫️",
                "Görüş mesafesi düşük olabilir efendim, trafikteyseniz lütfen takip mesafesini koruyunuz."
            )
            51, 53, 55 -> Triple(
                "Hafif Çisenti Yağışlı", "🌦️",
                "İnce ince yağmur atıştırıyor efendim. Yanınıza hafif bir şemsiye almanızı tavsiye ederim."
            )
            61, 63, 65 -> Triple(
                "Yağmurlu", "🌧️",
                "Yağmur etkili oluyor efendim. Şemsiyenizi yanınızda bulundurmanızı rica ederim."
            )
            71, 73, 75, 77 -> Triple(
                "Kar Yağışlı", "❄️",
                "Kar yağışı mevcut efendim. Lütfen sıcak giyinin ve kaygan zeminlere dikkat ediniz."
            )
            80, 81, 82 -> Triple(
                "Sağanak Yağışlı", "⛈️",
                "Kuvvetli sağanak yağış bekleniyor efendim. Zorunlu olmadıkça açık alanda kalmamanızı öneririm."
            )
            95, 96, 99 -> Triple(
                "Gök Gürültülü Fırtına", "⚡",
                "Hava fırtınalı ve şimşekli efendim. Güvenli kapalı alanlarda kalınız."
            )
            else -> Triple(
                "Ilıman ve Değişken", "🌤️",
                "Hava değişken görünüyor efendim, mevsime uygun giyinmeniz faydalı olacaktır."
            )
        }
    }
}
