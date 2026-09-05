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

object LessonPlanPdfHelper {

    suspend fun createLessonPlanPdf(
        context: Context,
        courseName: String,
        gradeLevel: String,
        planBody: String
    ): Pair<File?, String> = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale("tr", "TR"))
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(Date(now))
            val dateStr = dateFormat.format(Date(now))

            val cleanCourse = courseName.trim().ifBlank { "Genel Ders" }
            val cleanGrade = gradeLevel.trim().ifBlank { "Ortaöğretim" }
            val safeCourse = cleanCourse.replace(Regex("[^a-zA-Z0-9çÇğĞıİöÖşŞüÜ_ -]"), "").take(25)
            val fileName = "DersPlani_${safeCourse}_${timeStamp}.pdf"

            // 1. Özel Klasör: Documents/HatirlaGit_DersPlanlari
            val docsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
            val planFolder = File(docsDir, "HatirlaGit_DersPlanlari")
            if (!planFolder.exists()) {
                planFolder.mkdirs()
            }
            val targetPdfFile = File(planFolder, fileName)

            // 2. Android Native PdfDocument İle A4 Formatında Plan Çizimi
            val pdfDoc = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Standart Ölçü
            val page = pdfDoc.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val paint = Paint().apply { isAntiAlias = true }

            // Üst Antet Banner
            paint.color = Color.parseColor("#1E3A8A") // MEB Laciverti
            canvas.drawRect(0f, 0f, 595f, 95f, paint)

            // Başlık Metinleri
            paint.color = Color.WHITE
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 14f
            canvas.drawText("T.C. MİLLÎ EĞİTİM BAKANLIĞI", 40f, 32f, paint)

            paint.textSize = 17f
            canvas.drawText("TÜRKİYE YÜZYILI MAARİF MODELİ GÜNLÜK DERS PLANI", 40f, 58f, paint)

            paint.color = Color.parseColor("#93C5FD")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 10f
            canvas.drawText("Ders: $cleanCourse | Sınıf: $cleanGrade | Tarih: $dateStr | Süre: 40 Dakika", 40f, 80f, paint)

            // Bilgi Şeridi
            var yPos = 125f
            paint.color = Color.parseColor("#F1F5F9")
            canvas.drawRect(35f, yPos - 15f, 560f, yPos + 22f, paint)

            paint.color = Color.parseColor("#0F172A")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 11f
            canvas.drawText("I. BÖLÜM: DERS KÜNYESİ VE MAARİF BECERİLERİ", 45f, yPos + 5f, paint)

            yPos += 45f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 9.5f
            paint.color = Color.parseColor("#334155")

            // Satırları formatlayarak yaz
            val lines = planBody.split("\n")
            for (rawLine in lines) {
                if (yPos > 800f) break // Sayfa sonu emniyeti
                val line = rawLine.trim()

                if (line.startsWith("##") || line.startsWith("**") && line.endsWith("**")) {
                    yPos += 8f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    paint.color = Color.parseColor("#1E3A8A")
                    canvas.drawText(line.replace("#", "").replace("*", "").take(85), 45f, yPos, paint)
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    paint.color = Color.parseColor("#334155")
                    yPos += 16f
                } else if (line.isNotBlank()) {
                    val cleanText = line.replace("*", "").replace("- ", "• ")
                    // Metin uzunsa satırlara böl
                    val chunks = cleanText.chunked(85)
                    for (chunk in chunks) {
                        if (yPos > 805f) break
                        canvas.drawText(chunk, 45f, yPos, paint)
                        yPos += 14f
                    }
                } else {
                    yPos += 6f
                }
            }

            // Alt Bilgi Notu
            paint.color = Color.parseColor("#94A3B8")
            paint.textSize = 8.5f
            canvas.drawLine(40f, 815f, 555f, 815f, paint)
            canvas.drawText("HatırlaGit Asistan Usta tarafından MEB Maarif Modeli yönergelerine uygun olarak üretilmiştir.", 40f, 830f, paint)

            pdfDoc.finishPage(page)

            // Dosyaya Yaz
            val fos = FileOutputStream(targetPdfFile)
            pdfDoc.writeTo(fos)
            fos.close()
            pdfDoc.close()

            // 3. HatırlaGit Room Notlarına Kaydet
            val reminder = ReminderEntity(
                category = "DERS_PLANI",
                title = "📋 Ders Planı: $cleanCourse ($cleanGrade)",
                dueDatetime = dateStr,
                dueDateMillis = now,
                customNote = "MEB Maarif Modeline uygun günlük ders planı PDF formatında kaydedildi.\nDosya: ${targetPdfFile.name}",
                isFavorite = true,
                encryptedMetadata = "{\"file_path\":\"${targetPdfFile.absolutePath}\"}",
                actionStep = "NOTE_SAVED"
            )
            AppDatabase.getDatabase(context).reminderDao().insertReminder(reminder)

            return@withContext Pair(targetPdfFile, "📄 **$cleanCourse ($cleanGrade)** MEB Maarif Modeli günlük ders planı hazırlandı ve telefonunuzun belgelerine PDF olarak kaydedildi:\n`Documents/HatirlaGit_DersPlanlari/${targetPdfFile.name}`")
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext Pair(null, "Ders planı PDF'i oluşturulurken hata oluştu: ${e.localizedMessage}")
        }
    }

    fun openPdf(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
