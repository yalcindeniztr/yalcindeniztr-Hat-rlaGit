package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.ReminderEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportExportHelper {

    fun exportToPdf(context: Context, reminders: List<ReminderEntity>, titlePrefix: String = "HatırlaGit_Rapor"): File? {
        return try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val paint = Paint()
            val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr"))
            val dateOnlyFormat = SimpleDateFormat("dd MMMM yyyy", Locale("tr"))

            // Header Background
            paint.color = Color.parseColor("#0F172A")
            canvas.drawRect(0f, 0f, 595f, 90f, paint)

            // Header Title
            paint.color = Color.WHITE
            paint.textSize = 22f
            paint.isFakeBoldText = true
            canvas.drawText("HatırlaGit - Yaşam & Randevu Raporu", 30f, 45f, paint)

            paint.textSize = 12f
            paint.isFakeBoldText = false
            paint.color = Color.parseColor("#94A3B8")
            canvas.drawText("Oluşturulma Tarihi: ${dateOnlyFormat.format(Date())} • Toplam Kayıt: ${reminders.size}", 30f, 70f, paint)

            // Table Header
            var yPos = 130f
            paint.color = Color.parseColor("#E2E8F0")
            canvas.drawRect(30f, yPos - 20f, 565f, yPos + 10f, paint)

            paint.color = Color.parseColor("#1E293B")
            paint.textSize = 12f
            paint.isFakeBoldText = true
            canvas.drawText("Kategori", 40f, yPos, paint)
            canvas.drawText("Başlık & Not", 140f, yPos, paint)
            canvas.drawText("Tarih / Saat", 420f, yPos, paint)

            yPos += 30f
            paint.isFakeBoldText = false
            paint.textSize = 10f

            for ((index, reminder) in reminders.take(25).withIndex()) {
                if (index % 2 == 0) {
                    paint.color = Color.parseColor("#F8FAFC")
                    canvas.drawRect(30f, yPos - 15f, 565f, yPos + 15f, paint)
                }

                paint.color = Color.parseColor("#0F172A")
                paint.isFakeBoldText = true
                canvas.drawText(reminder.category.take(15), 40f, yPos, paint)

                paint.isFakeBoldText = false
                val displayTitle = if (reminder.customNote.isNotBlank()) "${reminder.title} (${reminder.customNote.take(20)})" else reminder.title
                canvas.drawText(displayTitle.take(42), 140f, yPos, paint)

                val dateStr = if (reminder.dueDateMillis > 0) dateFormat.format(Date(reminder.dueDateMillis)) else reminder.dueDatetime
                canvas.drawText(dateStr, 420f, yPos, paint)

                yPos += 24f
                if (yPos > 790f) break
            }

            // Footer
            paint.color = Color.parseColor("#64748B")
            paint.textSize = 9f
            canvas.drawText("HatırlaGit • %100 Çevrimdışı Güvenli Kişisel Yaşam Asistanı", 30f, 820f, paint)

            pdfDocument.finishPage(page)

            val outputDir = File(context.cacheDir, "reports").apply { mkdirs() }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(outputDir, "${titlePrefix}_$timeStamp.pdf")
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            outputStream.close()
            pdfDocument.close()

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun exportToCsv(context: Context, reminders: List<ReminderEntity>, titlePrefix: String = "HatirlaGit_Veriler"): File? {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val builder = StringBuilder()
            builder.append("ID,Kategori,Baslik,Not,Tarih,Favori\n")

            for (r in reminders) {
                val dateStr = if (r.dueDateMillis > 0) dateFormat.format(Date(r.dueDateMillis)) else r.dueDatetime
                val safeTitle = r.title.replace("\"", "\"\"")
                val safeNote = r.customNote.replace("\"", "\"\"")
                builder.append("${r.id},\"${r.category}\",\"$safeTitle\",\"$safeNote\",\"$dateStr\",${r.isFavorite}\n")
            }

            val outputDir = File(context.cacheDir, "reports").apply { mkdirs() }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(outputDir, "${titlePrefix}_$timeStamp.csv")
            file.writeText(builder.toString(), Charsets.UTF_8)
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun shareFile(context: Context, file: File, mimeType: String, title: String = "Raporu Paylaş") {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
