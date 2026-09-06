package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.MediaStore
import android.provider.Settings
import java.util.Locale

object AppLauncherHelper {

    fun playYouTubeSong(context: Context, songQuery: String): Pair<Boolean, String> {
        return try {
            val encodedQuery = Uri.encode(songQuery)
            val appIntent = Intent(Intent.ACTION_SEARCH).apply {
                setPackage("com.google.android.youtube")
                putExtra("query", songQuery)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (appIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(appIntent)
                Pair(true, "▶️ YouTube'da '$songQuery' aranıyor ve oynatılıyor...")
            } else {
                val webUri = Uri.parse("https://www.youtube.com/results?search_query=$encodedQuery")
                val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(webIntent)
                Pair(true, "▶️ Tarayıcı üzerinden YouTube'da '$songQuery' açılıyor...")
            }
        } catch (e: Exception) {
            Pair(false, "YouTube açılamadı: ${e.localizedMessage}")
        }
    }

    fun openGoogleAssistant(context: Context): Pair<Boolean, String> {
        return try {
            val voiceIntent = Intent(Intent.ACTION_VOICE_COMMAND).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (voiceIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(voiceIntent)
                Pair(true, "🎙️ Google Asistan köprüsü kuruldu, sesli asistan dinliyor...")
            } else {
                val assistIntent = Intent(Intent.ACTION_ASSIST).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(assistIntent)
                Pair(true, "🎙️ Google Asistan başlatıldı...")
            }
        } catch (e: Exception) {
            Pair(false, "Google Asistan başlatılamadı.")
        }
    }

    fun openApplicationByVoice(context: Context, appKeyword: String): Pair<Boolean, String> {
        val lower = appKeyword.lowercase(Locale("tr", "TR")).trim()
        return try {
            when {
                lower.contains("whatsapp") || lower.contains("watsap") || lower.contains("vatsap") -> {
                    launchPackage(context, "com.whatsapp", "WhatsApp")
                }
                lower.contains("youtube") || lower.contains("yutup") -> {
                    launchPackage(context, "com.google.android.youtube", "YouTube")
                }
                lower.contains("kamera") || lower.contains("fotoğraf çek") -> {
                    val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(cameraIntent)
                    Pair(true, "📷 Kamera açıldı.")
                }
                lower.contains("galeri") || lower.contains("fotoğraflar") || lower.contains("resimler") -> {
                    val galleryIntent = Intent(Intent.ACTION_VIEW).apply {
                        type = "image/*"
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(galleryIntent)
                    Pair(true, "🖼️ Galeri açıldı.")
                }
                lower.contains("harita") || lower.contains("navigasyon") || lower.contains("maps") -> {
                    val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=")).apply {
                        setPackage("com.google.android.apps.maps")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    if (mapIntent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(mapIntent)
                    } else {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com")).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
                    }
                    Pair(true, "🗺️ Google Haritalar açıldı.")
                }
                lower.contains("alarm") || lower.contains("saat") || lower.contains("alarmlar") -> {
                    val alarmIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(alarmIntent)
                    Pair(true, "⏰ Saat ve Alarmlar açıldı.")
                }
                lower.contains("takvim") || lower.contains("ajanda") -> {
                    val calIntent = Intent(Intent.ACTION_VIEW).apply {
                        data = CalendarContract.CONTENT_URI
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(calIntent)
                    Pair(true, "📅 Takvim açıldı.")
                }
                lower.contains("roborock") || lower.contains("süpürge") -> {
                    val r1 = launchPackage(context, "com.roborock.smart", "Roborock")
                    if (r1.first) r1 else launchPackage(context, "com.xiaomi.smarthome", "Mi Home")
                }
                lower.contains("mi home") || lower.contains("mihome") || lower.contains("xiaomi") -> {
                    launchPackage(context, "com.xiaomi.smarthome", "Mi Home")
                }
                lower.contains("mail") || lower.contains("eposta") || lower.contains("gmail") -> {
                    launchPackage(context, "com.google.android.gm", "Gmail")
                }
                lower.contains("ayar") || lower.contains("ayarlar") -> {
                    context.startActivity(Intent(Settings.ACTION_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
                    Pair(true, "⚙️ Telefon Ayarları açıldı.")
                }
                lower.contains("google asistan") || lower.contains("asistan") -> {
                    openGoogleAssistant(context)
                }
                else -> {
                    Pair(false, "Uygulama bulunamadı veya henüz izinli listeye eklenmemiş usta.")
                }
            }
        } catch (e: Exception) {
            Pair(false, "Uygulama açılırken hata oluştu: ${e.localizedMessage}")
        }
    }

    private fun launchPackage(context: Context, packageName: String, appLabel: String): Pair<Boolean, String> {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        return if (launchIntent != null) {
            launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(launchIntent)
            Pair(true, "🚀 $appLabel uygulaması açıldı usta!")
        } else {
            Pair(false, "📱 $appLabel uygulaması cihazınızda yüklü görünmüyor.")
        }
    }
}
