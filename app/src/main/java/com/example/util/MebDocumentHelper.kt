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

data class AnnualPlanParams(
    val schoolName: String = "Cumhuriyet Anadolu Lisesi",
    val principalName: String = "Okul Müdürü",
    val teachers: String = "Zümre Öğretmenleri",
    val courseName: String = "Tarih",
    val gradeLevel: String = "9. Sınıf",
    val academicYear: String = "2026-2027",
    val planType: String = "Yıllık Plan"
)

data class SokMeetingParams(
    val schoolName: String = "Cumhuriyet Anadolu Lisesi",
    val className: String = "10-A",
    val termName: String = "1. Dönem",
    val dateStr: String = "",
    val counselorName: String = "Rehber Öğretmen",
    val classTeacherName: String = "Sınıf Rehber Öğretmeni",
    val teachersList: List<String> = listOf("Türk Dili ve Edebiyatı", "Tarih", "Coğrafya", "Matematik", "Fizik", "Kimya", "Biyoloji", "İngilizce", "Felsefe", "Din Kültürü"),
    val scannedNotes: String = ""
)

object MebDocumentHelper {

    /**
     * MEB Türkiye Yüzyılı Maarif Modeline Uygun Yıllık / Dönemsel Ders Planı PDF Üretimi
     */
    suspend fun createAnnualPlanPdf(
        context: Context,
        params: AnnualPlanParams
    ): Pair<File?, String> = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale("tr", "TR"))
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(Date(now))
            val dateStr = dateFormat.format(Date(now))

            val cleanCourse = params.courseName.trim().ifBlank { "Tarih" }
            val cleanGrade = params.gradeLevel.trim().ifBlank { "9. Sınıf" }
            val safeCourse = cleanCourse.replace(Regex("[^a-zA-Z0-9çÇğĞıİöÖşŞüÜ_ -]"), "").take(20)
            val fileName = "MEB_YillikPlan_${safeCourse}_${cleanGrade.replace(" ", "")}_${timeStamp}.pdf"

            val docsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
            val planFolder = File(docsDir, "HatirlaGit_DersPlanlari")
            if (!planFolder.exists()) planFolder.mkdirs()
            val targetPdfFile = File(planFolder, fileName)

            val pdfDoc = PdfDocument()
            val paint = Paint().apply { isAntiAlias = true }

            val pageInfo1 = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page1 = pdfDoc.startPage(pageInfo1)
            val canvas1: Canvas = page1.canvas

            // Üst MEB Antet Banner
            paint.color = Color.parseColor("#1E3A8A")
            canvas1.drawRect(0f, 0f, 595f, 95f, paint)

            paint.color = Color.WHITE
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 13f
            canvas1.drawText("T.C. MİLLÎ EĞİTİM BAKANLIĞI", 40f, 28f, paint)

            paint.textSize = 15f
            canvas1.drawText(params.schoolName.uppercase(Locale("tr", "TR")), 40f, 50f, paint)

            paint.textSize = 12f
            paint.color = Color.parseColor("#93C5FD")
            canvas1.drawText("TÜRKİYE YÜZYILI MAARİF MODELİ " + params.academicYear + " " + params.planType.uppercase(Locale("tr", "TR")), 40f, 74f, paint)

            // Künye Tablosu
            var y = 120f
            paint.color = Color.parseColor("#F1F5F9")
            canvas1.drawRoundRect(35f, y, 560f, y + 90f, 8f, 8f, paint)

            paint.color = Color.parseColor("#0F172A")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 10.5f
            canvas1.drawText("Ders: $cleanCourse", 50f, y + 22f, paint)
            canvas1.drawText("Sınıf Düzeyi: $cleanGrade", 320f, y + 22f, paint)
            canvas1.drawText("Ders Saati: Haftalık 2 Saat", 50f, y + 44f, paint)
            canvas1.drawText("Eğitim Öğretim Yılı: " + params.academicYear, 320f, y + 44f, paint)
            canvas1.drawText("Ders Öğretmen(leri): " + params.teachers, 50f, y + 66f, paint)
            canvas1.drawText("Okul Müdürü: " + params.principalName, 320f, y + 66f, paint)

            y += 110f

            drawSectionHeader(canvas1, paint, "I. BÖLÜM: MAARİF MODELİ ÖĞRENME ÇIKTILARI VE SÜREÇ BİLEŞENLERİ", y)
            y += 30f

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 9.5f
            paint.color = Color.parseColor("#334155")

            val planHighlights = getCurriculumHighlights(cleanCourse, cleanGrade)
            for (line in planHighlights) {
                if (y > 780f) break
                val chunks = line.chunked(82)
                for (c in chunks) {
                    canvas1.drawText(c, 45f, y, paint)
                    y += 14f
                }
                y += 4f
            }

            y = 790f
            paint.color = Color.parseColor("#64748B")
            paint.textSize = 9f
            canvas1.drawText("Uygundur - " + params.principalName + " (Okul Müdürü)", 350f, y, paint)
            canvas1.drawText("Zümre Öğretmenleri: " + params.teachers, 45f, y, paint)

            pdfDoc.finishPage(page1)

            val fos = FileOutputStream(targetPdfFile)
            pdfDoc.writeTo(fos)
            fos.close()
            pdfDoc.close()

            val reminder = ReminderEntity(
                category = "DERS_PLANI",
                title = "📋 " + params.planType + ": " + cleanCourse + " (" + cleanGrade + ")",
                dueDatetime = dateStr,
                dueDateMillis = now,
                customNote = "Okul: " + params.schoolName + "\nMüdür: " + params.principalName + "\nÖğretmen: " + params.teachers + "\nDosya: " + targetPdfFile.name,
                isFavorite = true,
                encryptedMetadata = "{\"file_path\":\"" + targetPdfFile.absolutePath.replace("\\", "/") + "\",\"school\":\"" + params.schoolName + "\"}",
                actionStep = "NOTE_SAVED"
            )
            AppDatabase.getDatabase(context).reminderDao().insertReminder(reminder)

            val replyMsg = "📄 **" + params.schoolName + "** için **" + cleanCourse + " (" + cleanGrade + ")** MEB Maarif Modeli " + params.planType + " A4 PDF formatında hazırlandı:\n`Documents/HatirlaGit_DersPlanlari/" + targetPdfFile.name + "`"
            return@withContext Pair(targetPdfFile, replyMsg)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext Pair(null, "Plan PDF oluşturulurken hata: " + e.localizedMessage)
        }
    }

    /**
     * ŞÖK (Şube Öğretmenler Kurulu) Toplantı Tutanağı PDF Üretimi
     */
    suspend fun createSokMeetingPdf(
        context: Context,
        params: SokMeetingParams
    ): Pair<File?, String> = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale("tr", "TR"))
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(Date(now))
            val dateStr = if (params.dateStr.isNotBlank()) params.dateStr else dateFormat.format(Date(now))

            val fileName = "SOK_Tutanagi_" + params.className + "_" + timeStamp + ".pdf"
            val docsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
            val sokFolder = File(docsDir, "HatirlaGit_Tutanaklar")
            if (!sokFolder.exists()) sokFolder.mkdirs()
            val targetPdfFile = File(sokFolder, fileName)

            val pdfDoc = PdfDocument()
            val paint = Paint().apply { isAntiAlias = true }

            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDoc.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            paint.color = Color.parseColor("#0F172A")
            canvas.drawRect(0f, 0f, 595f, 90f, paint)

            paint.color = Color.WHITE
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 12f
            canvas.drawText("T.C. MİLLÎ EĞİTİM BAKANLIĞI", 40f, 26f, paint)

            paint.textSize = 14f
            canvas.drawText(params.schoolName.uppercase(Locale("tr", "TR")), 40f, 48f, paint)

            paint.textSize = 11f
            paint.color = Color.parseColor("#E2E8F0")
            canvas.drawText(params.className + " ŞUBESİ " + params.termName.uppercase(Locale("tr", "TR")) + " ŞÖK TOPLANTI TUTANAĞI", 40f, 72f, paint)

            var y = 115f
            paint.color = Color.parseColor("#F8FAFC")
            canvas.drawRoundRect(35f, y, 560f, y + 65f, 6f, 6f, paint)

            paint.color = Color.parseColor("#0F172A")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 10f
            canvas.drawText("Toplantı Tarihi: $dateStr", 50f, y + 20f, paint)
            canvas.drawText("Sınıf: " + params.className, 320f, y + 20f, paint)
            canvas.drawText("Sınıf Rehber Öğretmeni: " + params.classTeacherName, 50f, y + 42f, paint)
            canvas.drawText("Rehberlik Servisi: " + params.counselorName, 320f, y + 42f, paint)

            y += 85f

            drawSectionHeader(canvas, paint, "ŞÖK RESMİ GÜNDEM MADDELERİ VE ALINAN KARARLAR", y)
            y += 28f

            val sokAgendas = listOf(
                "1. Açılış ve yoklama: Kurul üyelerinin tamamının toplantıda hazır bulunduğu görüldü.",
                "2. Bir önceki ŞÖK kararlarının incelenmesi ve alınan tedbirlerin sonuçları değerlendirildi.",
                "3. Başarı Durumu: MEB Maarif Modeli kapsamında Türk Dili ve Edebiyatı 70 barajı ve ders ortalamaları incelendi; akademik destek ihtiyacı olan öğrencilere DYK kursları önerildi.",
                "4. Devam-Devamsızlık: Özürsüz 10 gün, toplam 30 gün sınırına yaklaşan öğrencilerin velilerine bildirim yapılması kararlaştırıldı.",
                "5. Rehberlik ve Özel Eğitim: Kaynaştırma ve BEP planı olan öğrencilerin gelişim süreçleri rehberlik servisiyle eşgüdümlü takip edildi.",
                "6. Maarif Modeli Değerler ve Sosyal Sorumluluk: Erdem-Değer-Eylem çerçevesinde sınıf bazlı sosyal sorumluluk projeleri belirlendi.",
                "7. Başarıyı Artırıcı Somut Tedbirler: Ders içi süreç değerlendirmeleri ve açık uçlu sınav kazanım pekiştirme etütleri planlandı.",
                "8. Dilek, temenniler ve kapanış yapıldı."
            )

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 8.5f
            paint.color = Color.parseColor("#334155")

            for (agenda in sokAgendas) {
                if (y > 700f) break
                val chunks = agenda.chunked(86)
                for (c in chunks) {
                    canvas.drawText(c, 45f, y, paint)
                    y += 12.5f
                }
                y += 2.5f
            }

            if (params.scannedNotes.isNotBlank()) {
                y += 8f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.color = Color.parseColor("#0F172A")
                canvas.drawText("KAMERA / OCR İLE AKTARILAN ÖZEL KARAR VE NOTLAR:", 45f, y, paint)
                y += 14f

                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.color = Color.parseColor("#475569")
                val ocrChunks = params.scannedNotes.lines().take(5)
                for (line in ocrChunks) {
                    if (y > 750f) break
                    val chunks = line.chunked(86)
                    for (c in chunks) {
                        canvas.drawText("• $c", 45f, y, paint)
                        y += 12f
                    }
                }
            }

            y = 780f
            paint.color = Color.parseColor("#64748B")
            paint.textSize = 8.5f
            canvas.drawLine(40f, y - 5f, 555f, y - 5f, paint)
            canvas.drawText(params.classTeacherName + " (Sınıf Rehber Öğretmeni)", 45f, y + 15f, paint)
            canvas.drawText("Kurul Başkanı / Müdür Yardımcısı", 350f, y + 15f, paint)

            pdfDoc.finishPage(page)

            val fos = FileOutputStream(targetPdfFile)
            pdfDoc.writeTo(fos)
            fos.close()
            pdfDoc.close()

            val reminder = ReminderEntity(
                category = "SOK_TUTANAGI",
                title = "📑 ŞÖK Tutanağı: " + params.className + " (" + params.termName + ")",
                dueDatetime = dateStr,
                dueDateMillis = now,
                customNote = "Okul: " + params.schoolName + "\nSınıf: " + params.className + "\nDosya: " + targetPdfFile.name,
                isFavorite = true,
                encryptedMetadata = "{\"file_path\":\"" + targetPdfFile.absolutePath.replace("\\", "/") + "\",\"class\":\"" + params.className + "\"}",
                actionStep = "NOTE_SAVED"
            )
            AppDatabase.getDatabase(context).reminderDao().insertReminder(reminder)

            val replyMsg = "📑 **" + params.className + "** ŞÖK Toplantı Tutanağı MEB yönetmeliği ve 8 resmi gündem maddesiyle A4 PDF olarak oluşturuldu:\n`Documents/HatirlaGit_Tutanaklar/" + targetPdfFile.name + "`"
            return@withContext Pair(targetPdfFile, replyMsg)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext Pair(null, "ŞÖK tutanağı PDF oluşturulurken hata: " + e.localizedMessage)
        }
    }

    /**
     * MEB Açık Uçlu Sınav ve Cevap Anahtarı (Senaryo 1 / 2) PDF Üretimi
     */
    suspend fun createExamPaperPdf(
        context: Context,
        schoolName: String,
        courseName: String,
        gradeLevel: String,
        examName: String,
        examContent: String
    ): Pair<File?, String> = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(Date(now))
            val fileName = "MEB_Sinav_" + courseName.take(15) + "_" + gradeLevel.take(10) + "_" + timeStamp + ".pdf"

            val docsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
            val examFolder = File(docsDir, "HatirlaGit_Sinavlar")
            if (!examFolder.exists()) examFolder.mkdirs()
            val targetPdfFile = File(examFolder, fileName)

            val pdfDoc = PdfDocument()
            val paint = Paint().apply { isAntiAlias = true }

            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDoc.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            paint.color = Color.parseColor("#1E293B")
            canvas.drawRect(0f, 0f, 595f, 85f, paint)

            paint.color = Color.WHITE
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 12f
            canvas.drawText("T.C. MİLLÎ EĞİTİM BAKANLIĞI", 40f, 26f, paint)

            paint.textSize = 14f
            canvas.drawText(schoolName.uppercase(Locale("tr", "TR")), 40f, 48f, paint)

            paint.textSize = 11f
            paint.color = Color.parseColor("#38BDF8")
            canvas.drawText(gradeLevel + " " + courseName + " Dersi " + examName + " (Açık Uçlu Maarif Senaryosu)", 40f, 70f, paint)

            var y = 110f
            paint.color = Color.parseColor("#334155")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 9.5f

            val lines = examContent.lines()
            for (line in lines) {
                if (y > 790f) break
                val chunks = line.chunked(85)
                for (c in chunks) {
                    canvas.drawText(c, 45f, y, paint)
                    y += 14f
                }
                y += 4f
            }

            pdfDoc.finishPage(page)

            val fos = FileOutputStream(targetPdfFile)
            pdfDoc.writeTo(fos)
            fos.close()
            pdfDoc.close()

            val reminder = ReminderEntity(
                category = "SINAV_KAGIDI",
                title = "📝 Sınav: " + courseName + " " + gradeLevel + " (" + examName + ")",
                dueDatetime = SimpleDateFormat("dd.MM.yyyy", Locale("tr", "TR")).format(Date(now)),
                dueDateMillis = now,
                customNote = "Açık uçlu sınav kağıdı ve puanlama rubriği hazırlandı.\nDosya: " + targetPdfFile.name,
                isFavorite = true,
                encryptedMetadata = "{\"file_path\":\"" + targetPdfFile.absolutePath.replace("\\", "/") + "\"}",
                actionStep = "NOTE_SAVED"
            )
            AppDatabase.getDatabase(context).reminderDao().insertReminder(reminder)

            val replyMsg = "📝 **" + courseName + " (" + gradeLevel + ")** Açık Uçlu Sınav Kağıdı ve Cevap Anahtarı A4 PDF formatında oluşturuldu:\n`Documents/HatirlaGit_Sinavlar/" + targetPdfFile.name + "`"
            return@withContext Pair(targetPdfFile, replyMsg)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext Pair(null, "Sınav PDF oluşturulurken hata: " + e.localizedMessage)
        }
    }

    private fun drawSectionHeader(canvas: Canvas, paint: Paint, title: String, y: Float) {
        paint.color = Color.parseColor("#E2E8F0")
        canvas.drawRect(35f, y - 14f, 560f, y + 10f, paint)

        paint.color = Color.parseColor("#1E3A8A")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 10f
        canvas.drawText(title, 45f, y + 2f, paint)
    }

    private fun getCurriculumHighlights(course: String, grade: String): List<String> {
        val lowerCourse = course.lowercase(Locale("tr", "TR"))
        return when {
            lowerCourse.contains("tarih") -> listOf(
                "• 1. Tema: Geçmişin İnşası ve Tarih Yazımı (Kanıt temelli sorgulama ve eleştirel analiz)",
                "• 2. Tema: İlk ve Orta Çağlarda Türk Dünyası (Kültür, medeniyet ve devlet teşkilatı)",
                "• 3. Tema: İslam Medeniyetinin Doğuşu ve Yayılışı (Bilim, sanat ve adalet ilkeleri)",
                "• 4. Tema: Türklerin İslamiyeti Kabulü ve İlk Türk İslam Devletleri (Gazneliler, Karahanlılar, Selçuklular)",
                "• 5. Tema: Türkiye Tarihi ve Anadolu Selçuklu Devleti (Ahilik teşkilatı ve toplumsal dayanışma)",
                "• Maarif Becerileri: Zaman algısı, kronolojik düşünme, tarihsel empati ve neden-sonuç analizi.",
                "• Değerler ve Eğilimler: Vatanseverlik, adalet, kültürel mirasa sahip çıkma, sorumluluk ve bilimsellik."
            )
            lowerCourse.contains("edebiyat") -> listOf(
                "• 1. Ünite: Giriş - Edebiyatın Sanat ve Bilimlerle İlişkisi, Dilin Tarihî Gelişimi",
                "• 2. Ünite: Hikâye (Olay ve Durum Hikâyesi, Anlatım Teknikleri, Çözümleme)",
                "• 3. Ünite: Şiir (Koşuk, Sagu, Destan, Divan ve Halk Şiiri Geleneği, Âhenk Unsurları)",
                "• 4. Ünite: Masal ve Fabl (Kültür aktarımı, evrensel ve millî motifler)",
                "• 5. Ünite: Roman (Millî Edebiyat ve Cumhuriyet Dönemi Romanı, Karakter ve Çatışma Analizi)",
                "• Maarif Becerileri: Eleştirel okuma, metinlerarası anlama, kendini doğru ifade etme, kelime serveti geliştirme.",
                "• Değerler ve Eğilimler: Estetik duyarlılık, sevgi, dürüstlük, millî kimlik ve edebi miras bilinci."
            )
            else -> listOf(
                "• Maarif Modeli Kavramsal Becerileri: Eleştirel düşünme, problem çözme, analitik sorgulama.",
                "• Öğrenme Çıktıları: Öğrencilerin teorik bilgiyi günlük yaşam becerileriyle sentezlemesi.",
                "• Süreç Değerlendirme: Süreç odaklı gözlem formları, açık uçlu rubrikler ve performans görevleri."
            )
        }
    }

    fun openDocument(context: Context, file: File) {
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
