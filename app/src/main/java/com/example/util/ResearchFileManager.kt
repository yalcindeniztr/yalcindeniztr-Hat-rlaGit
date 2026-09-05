package com.example.util

import android.content.Context
import android.os.Environment
import com.example.data.AppDatabase
import com.example.data.ReminderEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ResearchFileManager {

    suspend fun saveResearch(
        context: Context,
        topic: String,
        findings: String
    ): String = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr", "TR"))
            val fileDate = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(Date(now))
            val dateStr = sdf.format(Date(now))

            val cleanTopic = topic.trim().ifBlank { "Genel Araştırma" }
            val sanitizedFileName = cleanTopic.replace(Regex("[^a-zA-Z0-9çÇğĞıİöÖşŞüÜ_ -]"), "").take(30)
            val fileName = "Usta_Arastirma_${sanitizedFileName}_${fileDate}.txt"

            // 1. Cihaz Dahili/Harici Hafızasına Dosya Olarak Kaydet (Documents dizini)
            val docsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
            val researchFolder = File(docsDir, "HatirlaGit_Arastirmalar")
            if (!researchFolder.exists()) {
                researchFolder.mkdirs()
            }

            val targetFile = File(researchFolder, fileName)
            val fileContent = """
                ==================================================
                HATIRLAGİT - ASİSTAN USTA ARAŞTIRMA RAPORU
                Konu: $cleanTopic
                Tarih: $dateStr
                Kaynak: Cihaz İçi Güvenli Bellek & Asistan Bilgi Tabanı
                ==================================================

                $findings

                --------------------------------------------------
                Bu rapor telefonunuzun güvenli hafızasında saklanmaktadır.
            """.trimIndent()
            targetFile.writeText(fileContent, Charsets.UTF_8)

            // 2. Room Database Notlarına Kaydet (HatırlaGit Kütüphanesinde görünsün)
            val noteSnippet = if (findings.length > 500) findings.take(500) + "...\n(Tam metin dosya hafızasında)" else findings
            val reminder = ReminderEntity(
                category = "ARAŞTIRMA",
                title = "📚 $cleanTopic",
                dueDatetime = dateStr,
                dueDateMillis = now,
                customNote = noteSnippet,
                isFavorite = false,
                encryptedMetadata = "{\"file_path\": \"${targetFile.name}\"}",
                actionStep = "NOTE_SAVED"
            )
            AppDatabase.getDatabase(context).reminderDao().insertReminder(reminder)

            return@withContext "📁 Araştırma başarıyla cihaz hafızasına ve HatırlaGit notlarınıza kaydedildi.\nDosya: ${targetFile.name}"
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext "Araştırma kaydedilirken hata oluştu: ${e.localizedMessage}"
        }
    }
}
