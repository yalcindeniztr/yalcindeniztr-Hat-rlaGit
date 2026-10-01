package com.example.util

import android.app.ActivityManager
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import java.util.Locale

data class DeviceTelemetry(
    val batteryPercent: Int,
    val isCharging: Boolean,
    val storageFreeGb: Double,
    val storageTotalGb: Double,
    val ramAvailableMb: Long,
    val ramTotalMb: Long,
    val isNetworkConnected: Boolean,
    val networkType: String,
    val audioProfile: String,
    val uptimeString: String,
    val voiceReport: String,
    val screenReport: String
)

object AtillaJarvisCoreHelper {

    const val ASSISTANT_NAME = "ATİLLA"
    const val ASSISTANT_TITLE = "Kişisel Taktiksel, Operasyonel ve Pedagojik Yapay Zeka Asistanı"

    /**
     * 1. DONANIM TEŞHİS VE SİSTEM TELEMETRİSİ (JARVIS HUD TEKMİLİ)
     */
    fun getDeviceTelemetry(context: Context, patronPrefix: String = "Sayın Patronum"): DeviceTelemetry {
        // Batarya
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val batteryLevel = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        val isCharging = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            bm?.isCharging ?: false
        } else false

        // Depolama
        val (storageFreeGb, storageTotalGb) = try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val free = stat.availableBytes / (1024.0 * 1024.0 * 1024.0)
            val total = stat.totalBytes / (1024.0 * 1024.0 * 1024.0)
            Pair(free, total)
        } catch (_: Exception) {
            Pair(0.0, 0.0)
        }

        // RAM Bellek
        val (ramAvailMb, ramTotalMb) = try {
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            actManager?.getMemoryInfo(memInfo)
            Pair(memInfo.availMem / (1024 * 1024), memInfo.totalMem / (1024 * 1024))
        } catch (_: Exception) {
            Pair(0L, 0L)
        }

        // Ağ Bağlantısı
        val (isConnected, netType) = try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val activeNet = cm?.activeNetwork
            val caps = cm?.getNetworkCapabilities(activeNet)
            val connected = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            val type = when {
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "Wi-Fi (Yerel Ağ)"
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Mobil Veri (LTE/5G)"
                else -> if (connected) "Aktif Ağ" else "Çevrimdışı / İnternetsiz"
            }
            Pair(connected, type)
        } catch (_: Exception) {
            Pair(false, "Bilinmiyor")
        }

        // Ses Profili
        val audioProfile = try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            when (am?.ringerMode) {
                AudioManager.RINGER_MODE_SILENT -> "Sessiz Mod"
                AudioManager.RINGER_MODE_VIBRATE -> "Titreşim Modu"
                AudioManager.RINGER_MODE_NORMAL -> "Normal Sesli Mod"
                else -> "Standart"
            }
        } catch (_: Exception) {
            "Standart"
        }

        // Sistem Uptime
        val uptimeHours = SystemClock.elapsedRealtime() / (1000 * 60 * 60)
        val uptimeMinutes = (SystemClock.elapsedRealtime() / (1000 * 60)) % 60
        val uptimeStr = "${uptimeHours}s ${uptimeMinutes}dk"

        val voiceReport = "Tüm çekirdek sistemler nominal $patronPrefix. Enerji hücreleri %$batteryLevel, bellek ve depolama optimal düzeyde. Göreve ve emrinize amadeyim."

        val screenReport = buildString {
            append("⚡ **ATİLLA SİSTEM TEŞHİS RAPORU (TELEMETRİ)**\n")
            append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
            append("🔋 **Enerji Durumu:** %$batteryLevel ${if (isCharging) "(⚡ Şarj Ediliyor)" else "(Deşarj)"}\n")
            append("🧠 **Bellek (RAM):** ${ramAvailMb} MB Boş / ${ramTotalMb} MB Toplam\n")
            append("💾 **Dahili Depolama:** ${String.format(Locale.ROOT, "%.1f", storageFreeGb)} GB Boş / ${String.format(Locale.ROOT, "%.1f", storageTotalGb)} GB Toplam\n")
            append("🌐 **Ağ Durumu:** $netType ${if (isConnected) "(🟢 Çevrimiçi)" else "(⚪ Çevrimdışı Bellek Aktif)"}\n")
            append("🔊 **Ses Profili:** $audioProfile\n")
            append("⏱️ **Sistem Uptime:** $uptimeStr\n")
            append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
            append("🛡️ **Durum:** Jarvis operasyonel çekirdeği tam kapasite devrede, $patronPrefix.")
        }

        return DeviceTelemetry(
            batteryPercent = batteryLevel,
            isCharging = isCharging,
            storageFreeGb = storageFreeGb,
            storageTotalGb = storageTotalGb,
            ramAvailableMb = ramAvailMb,
            ramTotalMb = ramTotalMb,
            isNetworkConnected = isConnected,
            networkType = netType,
            audioProfile = audioProfile,
            uptimeString = uptimeStr,
            voiceReport = voiceReport,
            screenReport = screenReport
        )
    }

    /**
     * 2. DONANIM AYDINLATMA / FENER KONTROLÜ (TORCH CONTROL)
     */
    fun setFlashlight(context: Context, enabled: Boolean): Pair<Boolean, String> {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            val cameraId = cameraManager?.cameraIdList?.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: cameraManager?.cameraIdList?.firstOrNull()

            if (cameraManager != null && cameraId != null) {
                cameraManager.setTorchMode(cameraId, enabled)
                val state = if (enabled) "açıldı" else "kapatıldı"
                Pair(true, "💡 Fener $state efendim.")
            } else {
                Pair(false, "Cihazda fener donanımı tespit edilemedi efendim.")
            }
        } catch (e: Exception) {
            Pair(false, "Fener donanımı kontrol edilemedi: ${e.localizedMessage}")
        }
    }

    /**
     * 3. SES VE SESSİZLİK PROFİLİ KONTROLÜ (AUDIO PROFILE)
     */
    fun setAudioProfile(context: Context, profile: String): Pair<Boolean, String> {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            val p = profile.lowercase(Locale.ROOT)
            when {
                p.contains("silent") || p.contains("sessiz") || p.contains("toplantı") || p.contains("dnd") -> {
                    audioManager?.ringerMode = AudioManager.RINGER_MODE_SILENT
                    Pair(true, "🔕 Cihaz sessiz moda alındı efendim. Bildirim sesleri susturuldu.")
                }
                p.contains("vibrate") || p.contains("titreşim") || p.contains("titresim") -> {
                    audioManager?.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                    Pair(true, "📳 Cihaz titreşim moduna alındı efendim.")
                }
                p.contains("normal") || p.contains("sesli") || p.contains("aç") || p.contains("ac") -> {
                    audioManager?.ringerMode = AudioManager.RINGER_MODE_NORMAL
                    Pair(true, "🔔 Cihaz normal sesli moda alındı efendim.")
                }
                else -> Pair(false, "Bilinmeyen ses profili.")
            }
        } catch (e: Exception) {
            Pair(false, "Ses profili değiştirilemedi: ${e.localizedMessage}")
        }
    }

    /**
     * 4. KİMLİK VE VAROLUŞ BEYANI (ATİLLA / JARVIS)
     */
    fun getIdentitySpeech(patronPrefix: String = "Sayın Patronum"): String {
        return "Ben ATİLLA $patronPrefix. Jarvis mimarisini temel alan, yüksek operasyonel zeka, canlı internet araştırması, çevrimdışı arşiv ve cihaz donanım köprüsüyle donatılmış kişisel yapay zeka asistanınızım. Sıfır gevezelik ve kesin icra ile emrinizdeyim."
    }

    fun getIdentityBriefing(patronPrefix: String = "Sayın Patronum"): String {
        return buildString {
            append("🤖 **KİMLİK: ATİLLA (Jarvis Operasyonel Çekirdeği)**\n\n")
            append("Sayın Patronum, ben sizin akıllı telefonunuza entegre edilmiş tam yetkili baş danışmanınız, taktik sekreteriniz ve pedagojik yardımcınızım.\n\n")
            append("🎖️ **Temel Yeteneklerim:**\n")
            append("• ⚡ **Sistem Telemetrisi:** Batarya, RAM, depolama ve ağ durumunu anlık teşhis ederim.\n")
            append("• 💡 **Donanım Köprüsü:** Feneri açıp kapatır, ses profillerini (sessiz/titreşim) yönetirim.\n")
            append("• 🌐 **Canlı Web İstihbaratı:** Wikipedia TR ve TRT Haber canlı manşetlerinden anlık veri çekerim.\n")
            append("• 📚 **Tarih & Maarif Protokolü:** Sınıf düzeyine özel resmi A4 MEB Tarih ders planı ve tez üretirim.\n")
            append("• 🍽️ **Gurme Şef Zekası:** Buzdolabındaki malzemeleri analiz edip ezbere olmayan gurme tarif sentezlerim.\n")
            append("• 🏥 **Taktik Sağlık:** Nöbetçi eczaneleri tespit edip Google Haritalar canlı navigasyonunu başlatırım.\n")
            append("• 🎵 **Medya & Eğlence:** YouTube üzerinden doğrudan oyun havası veya parça oynatırım.\n")
            append("• 🛡️ **Çevrimdışı Bellek:** İnternetsiz ortamda bile tarih, bilim, felsefe ve mevzuat sorularınızı cevaplarım.\n\n")
            append("💡 _Her zaman yanınızdayım efendim; emrinizi bekliyorum._")
        }
    }
}
