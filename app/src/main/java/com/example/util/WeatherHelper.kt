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
    private var lastFetchTimeMillis: Long = 0
    private const val CACHE_DURATION_MS = 15 * 60 * 1000L // 15 dakika önbellek

    suspend fun getLiveWeather(
        context: Context,
        lat: Double,
        lng: Double,
        cityName: String = "Samsun"
    ): String = getWeatherBriefing(context, lat, lng, cityName)

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

💡 **Usta'nın Tavsiyesi:** $advice
                        """.trimIndent()

                        cachedWeatherText = briefing
                        lastFetchTimeMillis = now
                        return@withContext briefing
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Çevrimdışı / Hata Durumu Yedek Yanıtı
        val fallback = "🌤️ $cityName için hava durumu servisine ulaşılamadı; ancak mevsim normallerinde ılıman bir hava hakim görünüyor dostum. Dışarı çıkarken tedbirli olmanda fayda var!"
        cachedWeatherText = fallback
        lastFetchTimeMillis = now
        fallback
    }

    private fun parseWmoWeather(code: Int, temp: Double, windSpeed: Double): Triple<String, String, String> {
        return when (code) {
            0 -> Triple(
                "Açık ve Güneşli", "☀️",
                if (temp > 28) "Hava oldukça sıcak, bol su içmeyi ve güneşten korunmayı unutma can dostum!"
                else "Hava pırıl pırıl, tam bir yürüyüş ve işlerini halletme havası!"
            )
            1, 2, 3 -> Triple(
                "Parçalı Bulutlu", "⛅",
                "Hava gayet ferah ve tatlı bir serinlik var. Günün tadını çıkarabilirsin dostum."
            )
            45, 48 -> Triple(
                "Sisli ve Puslu", "🌫️",
                "Görüş mesafesi düşük olabilir, özellikle araç kullanacaksan dikkatli sür usta!"
            )
            51, 53, 55 -> Triple(
                "Hafif Çisenti Yağışlı", "🌦️",
                "İnce ince yağmur atıştırıyor. Yanına hafif bir şemsiye veya yağmurluk alırsan rahat edersin."
            )
            61, 63, 65 -> Triple(
                "Yağmurlu", "🌧️",
                "Bereket yağıyor! Şemsiyesiz dışarı çıkma, ayaklarını ıslatmamaya dikkat et can dostum."
            )
            71, 73, 75, 77 -> Triple(
                "Kar Yağışlı", "❄️",
                "Lapa lapa kar var! Sıkı giyin, atkını bereni tak ve yollarda kaymamaya dikkat et."
            )
            80, 81, 82 -> Triple(
                "Sağanak Yağışlı", "⛈️",
                "Kuvvetli sağanak yağış bekleniyor. Acele etme, kapalı alanlarda kalmaya gayret et usta."
            )
            95, 96, 99 -> Triple(
                "Gök Gürültülü Fırtına", "⚡",
                "Hava fırtınalı ve şimşekli. Tedbiri elden bırakma, güvenli bir yerde kal can dostum."
            )
            else -> Triple(
                "Ilıman ve Değişken", "🌤️",
                "Hava değişken görünüyor, kat kat giyinmek her zaman en iyisidir usta!"
            )
        }
    }
}
