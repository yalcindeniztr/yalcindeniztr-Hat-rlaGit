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

    fun openGoogleGemini(context: Context, query: String = ""): Pair<Boolean, String> {
        return try {
            val geminiPackage = "com.google.android.apps.bard"
            val launchIntent = context.packageManager.getLaunchIntentForPackage(geminiPackage)
            if (launchIntent != null) {
                launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                if (query.isNotBlank()) {
                    launchIntent.putExtra(Intent.EXTRA_TEXT, query)
                }
                context.startActivity(launchIntent)
                Pair(true, "✨ Google Gemini köprüsü kuruldu, uygulama açılıyor...")
            } else {
                val targetUrl = if (query.isNotBlank()) {
                    "https://gemini.google.com/app?q=${Uri.encode(query)}"
                } else {
                    "https://gemini.google.com"
                }
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(webIntent)
                Pair(true, "✨ Google Gemini web köprüsü açılıyor...")
            }
        } catch (e: Exception) {
            Pair(false, "Gemini köprüsü kurulamadı: ${e.localizedMessage}")
        }
    }

    fun searchGoogle(context: Context, searchQuery: String): Pair<Boolean, String> {
        return try {
            val encoded = Uri.encode(searchQuery)
            val searchIntent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(android.app.SearchManager.QUERY, searchQuery)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (searchIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(searchIntent)
                Pair(true, "🔍 Google'da '$searchQuery' aranıyor...")
            } else {
                val webUri = Uri.parse("https://www.google.com/search?q=$encoded")
                val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(webIntent)
                Pair(true, "🔍 Web üzerinden '$searchQuery' aranıyor...")
            }
        } catch (e: Exception) {
            Pair(false, "Google araması açılamadı: ${e.localizedMessage}")
        }
    }

    fun openApplicationByVoice(context: Context, appKeyword: String): Pair<Boolean, String> {
        val lower = appKeyword.lowercase(Locale("tr", "TR")).trim()
        return try {
            when {
                lower.contains("gemini") || lower.contains("bard") || lower.contains("google ai") -> {
                    val q = lower.replace(Regex("(?i)gemini'yi aç|gemini aç|geminiye sor|gemini|bard"), "").trim()
                    openGoogleGemini(context, q)
                }
                lower.startsWith("google'da ara") || lower.startsWith("internette ara") || lower.startsWith("webde ara") -> {
                    val q = lower.replace(Regex("(?i)^(google'da ara|internette ara|webde ara)[: ]*"), "").trim()
                    searchGoogle(context, q)
                }
                lower.contains("whatsapp") || lower.contains("watsap") || lower.contains("vatsap") -> {
                    launchPackage(context, "com.whatsapp", "WhatsApp")
                }
                lower.contains("youtube") || lower.contains("yutup") -> {
                    launchPackage(context, "com.google.android.youtube", "YouTube")
                }
                lower.contains("mebbis") -> {
                    val r = launchPackage(context, "tr.gov.eba.mebbis", "MEBBİS")
                    if (r.first) r else {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://mebbis.meb.gov.tr")).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
                        Pair(true, "📚 MEBBİS portalı açıldı dostum.")
                    }
                }
                lower.contains("e-okul") || lower.contains("eokul") -> {
                    val r = launchPackage(context, "com.meb.eokulogrenci", "E-Okul")
                    if (r.first) r else {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://e-okul.meb.gov.tr")).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
                        Pair(true, "🏫 E-Okul portalı açıldı dostum.")
                    }
                }
                lower.contains("eba") -> {
                    val r = launchPackage(context, "tr.gov.eba.hesap", "EBA")
                    if (r.first) r else {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://eba.gov.tr")).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
                        Pair(true, "📖 EBA portalı açıldı dostum.")
                    }
                }
                lower.contains("edevlet") || lower.contains("e-devlet") -> {
                    val r = launchPackage(context, "tr.gov.turkiye.edevlet.kapisi", "E-Devlet")
                    if (r.first) r else {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://turkiye.gov.tr")).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
                        Pair(true, "🇹🇷 E-Devlet Kapısı açıldı dostum.")
                    }
                }
                lower.contains("hesap makinesi") || lower.contains("hesapla") -> {
                    val r1 = launchPackage(context, "com.google.android.calculator", "Hesap Makinesi")
                    if (r1.first) r1 else {
                        val calcIntent = Intent(Intent.ACTION_MAIN).apply {
                            addCategory(Intent.CATEGORY_APP_CALCULATOR)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        if (calcIntent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(calcIntent)
                            Pair(true, "🔢 Hesap Makinesi açıldı.")
                        } else {
                            Pair(false, "Hesap makinesi bulunamadı.")
                        }
                    }
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
                    // Dinamik Yüklü Uygulama Eşleştiricisi
                    val pm = context.packageManager
                    val cleanTarget = lower.replace("uygulamasını aç", "")
                        .replace("uygulamayı aç", "")
                        .replace("uygulaması", "")
                        .replace("aç", "")
                        .trim()
                    
                    val installedApps = pm.getInstalledApplications(0)
                    val matchedApp = installedApps.firstOrNull { appInfo ->
                        val appLabel = pm.getApplicationLabel(appInfo).toString().lowercase(Locale("tr", "TR"))
                        appLabel.contains(cleanTarget) || cleanTarget.contains(appLabel)
                    }

                    if (matchedApp != null) {
                        val launchIntent = pm.getLaunchIntentForPackage(matchedApp.packageName)
                        if (launchIntent != null) {
                            launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            context.startActivity(launchIntent)
                            val resolvedName = pm.getApplicationLabel(matchedApp).toString()
                            Pair(true, "🚀 $resolvedName uygulaması açıldı dostum!")
                        } else {
                            Pair(false, "Uygulama açılamadı dostum.")
                        }
                    } else {
                        Pair(false, "Cihazınızda '$cleanTarget' isimli bir uygulama bulunamadı dostum.")
                    }
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
            Pair(true, "🚀 $appLabel uygulaması açıldı dostum!")
        } else {
            Pair(false, "📱 $appLabel uygulaması cihazınızda yüklü görünmüyor.")
        }
    }
}
