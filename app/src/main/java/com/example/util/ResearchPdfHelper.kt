package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import androidx.core.content.FileProvider
import com.example.data.AppDatabase
import com.example.data.ReminderEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ATİLA Genel Araştırma, Rapor, Ders Planı ve İçerik PDF Üreticisi
 * Kullanıcının istediği herhangi bir araştırma metnini, lise performans tezini,
 * maarif planını veya yemek tarifini şık, kurumsal A4 PDF formatında cihaz belgelerine kaydeder.
 */
object ResearchPdfHelper {

    suspend fun createAndOpenPdf(
        context: Context,
        title: String,
        content: String,
        categoryTag: String = "ARAŞTIRMA RAPORU"
    ): Pair<File?, String> = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr", "TR"))
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(Date(now))
            val dateStr = dateFormat.format(Date(now))

            val cleanTitle = title.trim().ifBlank { "Genel Araştırma Raporu" }
            val safeFileName = cleanTitle.replace(Regex("[^a-zA-Z0-9çÇğĞıİöÖşŞüÜ_ -]"), "").take(30)
            val fileName = "ATILA_${safeFileName}_${timeStamp}.pdf"

            val docsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
            val folder = File(docsDir, "HatirlaGit_Raporlar")
            if (!folder.exists()) {
                folder.mkdirs()
            }
            val targetPdf = File(folder, fileName)

            val pdfDoc = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
            val page = pdfDoc.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val paint = Paint().apply { isAntiAlias = true }

            // Üst Başlık Banner (Koyu Mavi - Gri Kurumsal Ton)
            paint.color = Color.parseColor("#0F172A") // Slate 900
            canvas.drawRect(0f, 0f, 595f, 90f, paint)

            paint.color = Color.parseColor("#38BDF8") // Sky 400
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 11f
            canvas.drawText("HATIRLAGİT ATİLA • $categoryTag", 40f, 32f, paint)

            paint.color = Color.WHITE
            paint.textSize = 15f
            canvas.drawText(cleanTitle.take(50), 40f, 56f, paint)

            paint.color = Color.parseColor("#94A3B8") // Slate 400
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 9.5f
            canvas.drawText("Tarih: $dateStr | Kaynak: Onaylı Web & Cihaz Belleği", 40f, 76f, paint)

            var yPos = 120f
            val lines = content.split("\n")

            for (rawLine in lines) {
                if (yPos > 790f) break
                val line = rawLine.trim()

                when {
                    line.startsWith("##") || line.startsWith("###") || (line.startsWith("**") && line.endsWith("**")) -> {
                        yPos += 8f
                        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        paint.textSize = 11f
                        paint.color = Color.parseColor("#0369A1") // Sky 700
                        val heading = line.replace("#", "").replace("*", "").take(85)
                        canvas.drawText(heading, 40f, yPos, paint)
                        yPos += 16f
                    }
                    line.startsWith("•") || line.startsWith("-") || line.startsWith("*") -> {
                        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                        paint.textSize = 9.5f
                        paint.color = Color.parseColor("#1E293B")
                        val bulletText = "• " + line.replace(Regex("^[•\\-*]\\s*"), "").replace("*", "")
                        val chunks = bulletText.chunked(85)
                        for (c in chunks) {
                            if (yPos > 790f) break
                            canvas.drawText(c, 48f, yPos, paint)
                            yPos += 14f
                        }
                    }
                    line.isNotBlank() -> {
                        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                        paint.textSize = 9.5f
                        paint.color = Color.parseColor("#334155")
                        val cleanText = line.replace("*", "")
                        val chunks = cleanText.chunked(88)
                        for (chunk in chunks) {
                            if (yPos > 790f) break
                            canvas.drawText(chunk, 40f, yPos, paint)
                            yPos += 14f
                        }
                    }
                    else -> {
                        yPos += 6f
                    }
                }
            }

            // Alt Bilgi Notu
            paint.color = Color.parseColor("#CBD5E1")
            canvas.drawLine(40f, 805f, 555f, 805f, paint)
            paint.color = Color.parseColor("#64748B")
            paint.textSize = 8.5f
            canvas.drawText("HatırlaGit ATİLA Asistan tarafından üretilmiştir. Resmi ve akademik referans amaçlıdır.", 40f, 822f, paint)

            pdfDoc.finishPage(page)

            val fos = FileOutputStream(targetPdf)
            pdfDoc.writeTo(fos)
            fos.close()
            pdfDoc.close()

            // Room Database'e Not Olarak da Ekle
            val reminder = ReminderEntity(
                category = "RAPOR_PDF",
                title = "📄 $cleanTitle",
                dueDatetime = dateStr,
                dueDateMillis = now,
                customNote = "PDF Belgesi oluşturuldu:\n${targetPdf.name}",
                isFavorite = false,
                encryptedMetadata = "{\"file_path\":\"${targetPdf.absolutePath}\"}",
                actionStep = "NOTE_SAVED"
            )
            AppDatabase.getDatabase(context).reminderDao().insertReminder(reminder)

            // PDF'i Doğrudan Ekranda Aç
            openPdfFile(context, targetPdf)

            Pair(targetPdf, "📄 '$cleanTitle' başlıklı PDF belgeniz hazırlandı ve ekranda açıldı:\n`Documents/HatirlaGit_Raporlar/${targetPdf.name}`")
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(null, "PDF oluşturulurken hata meydana geldi: ${e.localizedMessage}")
        }
    }

    fun openPdfFile(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "PDF Belgesini Aç").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            // Doğrudan fallback
            try {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    fun getLatestGeneratedPdf(context: Context): File? {
        return try {
            val docsDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
            val subFolders = listOf(
                File(docsDir, "HatirlaGit_TarihPlanlari"),
                File(docsDir, "HatirlaGit_Raporlar"),
                File(docsDir, "HatirlaGit_OgrenciTezleri"),
                File(docsDir, "HatirlaGit_MebEvraklari")
            )
            var latestFile: File? = null
            var latestTime: Long = 0L

            for (folder in subFolders) {
                if (folder.exists() && folder.isDirectory) {
                    val files = folder.listFiles { f -> f.extension.equals("pdf", ignoreCase = true) }
                    files?.forEach { f ->
                        if (f.lastModified() > latestTime) {
                            latestTime = f.lastModified()
                            latestFile = f
                        }
                    }
                }
            }
            latestFile
        } catch (e: Exception) {
            null
        }
    }
}
