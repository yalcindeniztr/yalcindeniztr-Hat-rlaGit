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
 * Patron'a özel A4 formatında Günlük Çalışma ve Yaşam Rutini PDF Üreticisi
 */
object DailyRoutinePdfHelper {

    suspend fun createDailyPlanPdf(
        context: Context,
        userName: String = "Sayın Patronum",
        customNotes: String = ""
    ): Pair<File?, String> = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            val dateFormat = SimpleDateFormat("dd MMMM yyyy, EEEE", Locale("tr", "TR"))
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(Date(now))
            val dateStr = dateFormat.format(Date(now))

            val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "HatirlaGit_Planlar")
            if (!dir.exists()) dir.mkdirs()

            val targetPdfFile = File(dir, "Gunluk_Program_$timeStamp.pdf")

            val pdfDoc = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standart A4
            val page = pdfDoc.startPage(pageInfo)
            val canvas: Canvas = page.canvas
            val paint = Paint()

            // 1. Üst Başlık ve Kurumsal Çerçeve
            paint.color = Color.parseColor("#0F172A") // Koyu Lacivert / Titanyum
            canvas.drawRect(30f, 30f, 565f, 95f, paint)

            paint.color = Color.parseColor("#00E5FF") // Neon Cyan
            paint.strokeWidth = 2f
            paint.style = Paint.Style.STROKE
            canvas.drawRect(30f, 30f, 565f, 95f, paint)
            paint.style = Paint.Style.FILL

            paint.color = Color.WHITE
            paint.textSize = 15f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("JARVIS // GÜNLÜK ÇALIŞMA VE YAŞAM PROGRAMI", 595f / 2f, 58f, paint)

            paint.color = Color.parseColor("#94A3B8")
            paint.textSize = 9.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("Sirkadiyen Biyolojik Ritim & Yüksek Odaklı Verimlilik Protokolü", 595f / 2f, 78f, paint)

            paint.textAlign = Paint.Align.LEFT

            // 2. Bilgi Paneli
            var y = 120f
            paint.color = Color.parseColor("#1E293B")
            paint.textSize = 10f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Plan Sahibi:", 45f, y, paint)
            canvas.drawText("Tarih & Gün:", 320f, y, paint)

            paint.color = Color.parseColor("#334155")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(if (userName.isNotBlank()) userName else "Sayın Patronum", 125f, y, paint)
            canvas.drawText(dateStr, 395f, y, paint)

            y += 18f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Sistem Statüsü:", 45f, y, paint)
            canvas.drawText("Otonom Yönetici:", 320f, y, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.color = Color.parseColor("#059669")
            canvas.drawText("Aktif & Senkronize", 135f, y, paint)
            paint.color = Color.parseColor("#334155")
            canvas.drawText("ATİLA / Jarvis Özel Asistan", 420f, y, paint)

            // Çizgi
            y += 15f
            paint.color = Color.parseColor("#CBD5E1")
            paint.strokeWidth = 1f
            canvas.drawLine(40f, y, 555f, y, paint)

            // 3. Tablo Başlığı
            y += 25f
            paint.color = Color.parseColor("#1E293B")
            canvas.drawRect(40f, y - 14f, 555f, y + 8f, paint)

            paint.color = Color.WHITE
            paint.textSize = 10f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("ZAMAN", 55f, y, paint)
            canvas.drawText("BLOK / AKTİVİTE BAŞLIĞI", 130f, y, paint)
            canvas.drawText("HEDEF VE PEDAGOJİK / İDARİ DETAYLAR", 310f, y, paint)

            // 4. Tablo Satırları
            val routines = DailyRoutinePlanner.DEFAULT_ROUTINES
            y += 24f

            for ((index, block) in routines.withIndex()) {
                val rowBg = if (index % 2 == 0) Color.parseColor("#F8FAFC") else Color.WHITE
                paint.color = rowBg
                canvas.drawRect(40f, y - 14f, 555f, y + 16f, paint)

                // Zaman
                paint.color = Color.parseColor("#0F172A")
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 10f
                val timeStr = String.format(Locale.ROOT, "%02d:%02d", block.hour, block.minute)
                canvas.drawText(timeStr, 55f, y, paint)

                // Başlık
                paint.color = Color.parseColor("#0369A1")
                paint.textSize = 9.5f
                canvas.drawText(block.title, 130f, y, paint)

                // Not
                paint.color = Color.parseColor("#475569")
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.textSize = 8.5f
                val noteShort = block.note.take(50)
                canvas.drawText(noteShort, 310f, y, paint)

                // Alt çizgi
                paint.color = Color.parseColor("#E2E8F0")
                canvas.drawLine(40f, y + 16f, 555f, y + 16f, paint)

                y += 30f
            }

            // 5. Sirkadiyen Sağlık & Maarif İlkeleri Not Paneli
            y += 15f
            paint.color = Color.parseColor("#F1F5F9")
            canvas.drawRoundRect(40f, y, 555f, y + 110f, 8f, 8f, paint)

            paint.color = Color.parseColor("#0284C7")
            paint.strokeWidth = 1.5f
            paint.style = Paint.Style.STROKE
            canvas.drawRoundRect(40f, y, 555f, y + 110f, 8f, 8f, paint)
            paint.style = Paint.Style.FILL

            paint.color = Color.parseColor("#0F172A")
            paint.textSize = 10.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("💡 JARVIS SİRKADİYEN & VERİMLİLİK İLKELERİ", 55f, y + 20f, paint)

            paint.color = Color.parseColor("#334155")
            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("• Hidrasyon: Gün boyunca asgari 2.5 litre su tüketimi planlanmıştır.", 55f, y + 40f, paint)
            canvas.drawText("• 20-20-20 Kuralı: Ekran karşısında her 20 dakikada bir 20 saniye göz dinlendirme yapılmalıdır.", 55f, y + 56f, paint)
            canvas.drawText("• Erdem-Değer-Eylem: Ders ve toplantılarda süreç odaklı pedagojik tutum esastır.", 55f, y + 72f, paint)
            canvas.drawText("• Dijital Detoks: Saat 22:00'den sonra mavi ışık maruziyeti sıfırlanmalıdır.", 55f, y + 88f, paint)

            // 6. İmza ve Onay Alanı
            y += 140f
            paint.color = Color.parseColor("#64748B")
            paint.textSize = 9f
            canvas.drawText("Onaylayan: " + (if (userName.isNotBlank()) userName else "Sayın Patronum"), 55f, y, paint)
            canvas.drawText("Sistem İcrası: Jarvis / ATİLA Otonom Asistan", 330f, y, paint)

            pdfDoc.finishPage(page)

            val fos = FileOutputStream(targetPdfFile)
            pdfDoc.writeTo(fos)
            fos.close()
            pdfDoc.close()

            // Room Database'e Kalıcı Kayıt
            val db = AppDatabase.getDatabase(context)
            db.reminderDao().insertReminder(
                ReminderEntity(
                    category = "GÜNLÜK_PLAN",
                    title = "📄 Günlük Çalışma & Yaşam Programı (PDF)",
                    dueDatetime = dateStr,
                    dueDateMillis = now,
                    customNote = "Dosya: " + targetPdfFile.name + "\nKonum: " + targetPdfFile.absolutePath,
                    isFavorite = true,
                    encryptedMetadata = "{}",
                    actionStep = "PDF_GENERATED"
                )
            )

            // Otomatik Açma / Görüntüleme Intent'i
            try {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    targetPdfFile
                )
                val openIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(openIntent)
            } catch (_: Exception) {}

            val summaryMsg = "📄 **Günlük Çalışma ve Yaşam Programınız (PDF) Hazırlandı!**\n\n" +
                    "• **Dosya:** `${targetPdfFile.name}`\n" +
                    "• **Tarih:** $dateStr\n" +
                    "• **Kayıt Yeri:** Cihaz Belgeler Klasörü\n\n" +
                    "💡 Belge otomatik olarak ekranda açıldı. Dilediğiniz zaman bildirim panelinden veya Dosyalarım uygulamasından erişebilirsiniz efendim."

            Pair(targetPdfFile, summaryMsg)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(null, "PDF üretimi sırasında bir hata oluştu: ${e.localizedMessage}")
        }
    }
}
