package com.example.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
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
 * Türkiye Yüzyılı Maarif Modeli - Tarih Dersi Günlük Plan ve Performans Tezi PDF Motoru
 * Tarih Öğretmeni ve Lise öğrencileri için MEB yönergelerine tam uyumlu A4 resmi evrak üretir.
 */
object HistoryLessonPlanPdfHelper {

    suspend fun createMaarifHistoryPlanPdf(
        context: Context,
        gradeLevel: String = "10. Sınıf",
        topicTitle: String = "Beylikten Devlete Osmanlı Siyaseti",
        teacherName: String = "Tarih Öğretmeni"
    ): Pair<File?, String> = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale("tr", "TR"))
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(Date(now))
            val dateStr = dateFormat.format(Date(now))

            val safeTopic = topicTitle.replace(Regex("[^a-zA-Z0-9çÇğĞıİöÖşŞüÜ_ -]"), "").take(25)
            val fileName = "Maarif_TarihPlani_${gradeLevel.replace(" ", "")}_${safeTopic}_${timeStamp}.pdf"

            val docsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
            val folder = File(docsDir, "HatirlaGit_TarihPlanlari")
            if (!folder.exists()) folder.mkdirs()
            val targetPdf = File(folder, fileName)

            val pdfDoc = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDoc.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val paint = Paint().apply { isAntiAlias = true }

            // Üst Antet Banner (MEB Kırmızı-Bordo ve Lacivert)
            paint.color = Color.parseColor("#881337") // Rose 900
            canvas.drawRect(0f, 0f, 595f, 95f, paint)

            paint.color = Color.WHITE
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 13f
            canvas.drawText("T.C. MİLLÎ EĞİTİM BAKANLIĞI", 40f, 30f, paint)

            paint.textSize = 16f
            canvas.drawText("TÜRKİYE YÜZYILI MAARİF MODELİ GÜNLÜK DERS PLANI", 40f, 54f, paint)

            paint.color = Color.parseColor("#FECDD3")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 9.5f
            canvas.drawText("Ders: TARİH | Düzey: $gradeLevel | Öğretmen: $teacherName | Tarih: $dateStr | Süre: 40+40 Dk", 40f, 76f, paint)

            var yPos = 125f

            fun drawSectionHeader(title: String) {
                paint.color = Color.parseColor("#F1F5F9")
                canvas.drawRect(35f, yPos - 13f, 560f, yPos + 18f, paint)
                paint.color = Color.parseColor("#881337")
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 10.5f
                canvas.drawText(title, 45f, yPos + 4f, paint)
                yPos += 30f
            }

            fun drawTextLine(text: String, isBold: Boolean = false) {
                paint.typeface = Typeface.create(Typeface.DEFAULT, if (isBold) Typeface.BOLD else Typeface.NORMAL)
                paint.textSize = 9f
                paint.color = if (isBold) Color.parseColor("#0F172A") else Color.parseColor("#334155")
                val chunks = text.chunked(90)
                for (chunk in chunks) {
                    if (yPos > 790f) break
                    canvas.drawText(chunk, 45f, yPos, paint)
                    yPos += 13.5f
                }
            }

            // BÖLÜM 1: DERS KÜNYESİ VE ALAN BECERİLERİ
            drawSectionHeader("I. BÖLÜM: MAARİF ALAN BECERİLERİ VE ERDEM-DEĞER ODAĞI")
            drawTextLine("• Öğrenme Alanı / Ünite: $topicTitle", isBold = true)
            drawTextLine("• Tarihsel Düşünme Becerileri: Kronolojik Düşünme, Tarihsel Kavrama ve Kanıta Dayalı Yorumlama.")
            drawTextLine("• Erdem-Değer-Eylem (EDE): Vatanseverlik, Adalet, Millî Birlik ve Tarih Bilinci.")
            drawTextLine("• Maarif Yetkinlikleri: Eleştirel düşünme, birincil kaynak analizi ve tarihsel empati kurma.")
            yPos += 10f

            // BÖLÜM 2: ÖĞRENME ÇIKTILARI VE SÜREÇ
            drawSectionHeader("II. BÖLÜM: 5E MODELİ İLE ÖĞRENME-ÖĞRETME SÜRECİ")
            drawTextLine("1. Giriş (Güdüleme): Döneme ait harita ve görsel sunularak öğrencinin ön bilgileri yoklanır.", isBold = true)
            drawTextLine("2. Keşfetme (Kaynak İncelemesi): Dönemin kronikleri, fermanları ve seyahatname alıntıları incelenir.")
            drawTextLine("3. Açıklama (Kavramsal Derinlik): Siyasi teşkilatlanma, fetih siyaseti ve gaza ruhu irdelenir.")
            drawTextLine("4. Derinleştirme: Günümüz coğrafi ve stratejik dengeleriyle tarihsel analojiler kurulur.")
            drawTextLine("5. Değerlendirme: Süreç odaklı formatif değerlendirme ve açık uçlu senaryo soruları yöneltilir.")
            yPos += 10f

            // BÖLÜM 3: ÖLÇME & DEĞERLENDİRME VE SENARYO SORULARI
            drawSectionHeader("III. BÖLÜM: ÖLÇME - DEĞERLENDİRME & PERFORMANS GÖREVİ")
            drawTextLine("• Açık Uçlu Senaryo Sorusu: 'Dönemin jeopolitik şartları göz önüne alındığında fetihlerin yönünü ve kalıcılığını sağlayan temel etkenleri 3 madde halinde analiz ediniz.'", isBold = true)
            drawTextLine("• Ölçme Aracı: MEB Açık Uçlu Sınav Rubriği (Gerekçelendirme: 40P, Tarihsel Doğruluk: 40P, Dil-Muhakeme: 20P).")
            drawTextLine("• Bireysel Farklılaştırma: Destekleme ihtiyacı olan öğrencilere kavram haritası sunulur.")

            // Alt Bilgi Notu
            paint.color = Color.parseColor("#CBD5E1")
            canvas.drawLine(40f, 805f, 555f, 805f, paint)
            paint.color = Color.parseColor("#64748B")
            paint.textSize = 8.5f
            canvas.drawText("HatırlaGit Asistan ATİLA • MEB Türkiye Yüzyılı Maarif Modeli Tarih Dersi Resmi Planı", 40f, 822f, paint)

            pdfDoc.finishPage(page)

            val fos = FileOutputStream(targetPdf)
            pdfDoc.writeTo(fos)
            fos.close()
            pdfDoc.close()

            // Room Database'e Not Olarak Kaydet
            val reminder = ReminderEntity(
                category = "MAARİF_PLAN",
                title = "📚 Maarif Tarih Planı: $gradeLevel - $topicTitle",
                dueDatetime = dateStr,
                dueDateMillis = now,
                customNote = "MEB Maarif Modeline uygun Tarih Günlük Planı PDF olarak oluşturuldu.\nDosya: ${targetPdf.name}",
                isFavorite = true,
                encryptedMetadata = "{\"file_path\":\"${targetPdf.absolutePath}\"}",
                actionStep = "NOTE_SAVED"
            )
            AppDatabase.getDatabase(context).reminderDao().insertReminder(reminder)

            // PDF Dosyasını Aç
            ResearchPdfHelper.openPdfFile(context, targetPdf)

            Pair(targetPdf, "📄 **T.C. MEB Maarif Modeli Tarih Günlük Ders Planı** hazırlandı ve ekranda açıldı:\n`Documents/HatirlaGit_TarihPlanlari/${targetPdf.name}`")
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(null, "Tarih ders planı PDF'i oluşturulamadı: ${e.localizedMessage}")
        }
    }

    suspend fun createStudentHistoryThesisPdf(
        context: Context,
        thesisTopic: String,
        studentName: String = "Lise Öğrencisi",
        gradeLevel: String = "10. Sınıf"
    ): Pair<File?, String> = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale("tr", "TR"))
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(Date(now))
            val dateStr = dateFormat.format(Date(now))

            val cleanTopic = thesisTopic.trim().ifBlank { "Tarih Performans Tezi" }
            val safeTopic = cleanTopic.replace(Regex("[^a-zA-Z0-9çÇğĞıİöÖşŞüÜ_ -]"), "").take(25)
            val fileName = "TarihTezi_${safeTopic}_${timeStamp}.pdf"

            val docsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
            val folder = File(docsDir, "HatirlaGit_OgrenciTezleri")
            if (!folder.exists()) folder.mkdirs()
            val targetPdf = File(folder, fileName)

            val pdfDoc = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDoc.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val paint = Paint().apply { isAntiAlias = true }

            // Kapak / Üst Başlık (Koyu Lacivert)
            paint.color = Color.parseColor("#1E293B")
            canvas.drawRect(0f, 0f, 595f, 95f, paint)

            paint.color = Color.parseColor("#38BDF8")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 12f
            canvas.drawText("LİSE TARİH DERSİ PERFORMANS & TEZ RAPORU", 40f, 32f, paint)

            paint.color = Color.WHITE
            paint.textSize = 15f
            canvas.drawText(cleanTopic.take(50), 40f, 56f, paint)

            paint.color = Color.parseColor("#94A3B8")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 9.5f
            canvas.drawText("Öğrenci: $studentName | Düzey: $gradeLevel | Tarih: $dateStr", 40f, 76f, paint)

            var yPos = 125f

            fun drawSectionHeader(title: String) {
                paint.color = Color.parseColor("#F8FAFC")
                canvas.drawRect(35f, yPos - 13f, 560f, yPos + 18f, paint)
                paint.color = Color.parseColor("#0284C7")
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 10.5f
                canvas.drawText(title, 45f, yPos + 4f, paint)
                yPos += 28f
            }

            fun drawBodyText(text: String, isBold: Boolean = false) {
                paint.typeface = Typeface.create(Typeface.DEFAULT, if (isBold) Typeface.BOLD else Typeface.NORMAL)
                paint.textSize = 9f
                paint.color = if (isBold) Color.parseColor("#0F172A") else Color.parseColor("#334155")
                val chunks = text.chunked(90)
                for (chunk in chunks) {
                    if (yPos > 790f) break
                    canvas.drawText(chunk, 45f, yPos, paint)
                    yPos += 13.5f
                }
            }

            drawSectionHeader("1. GİRİŞ VE ARAŞTIRMANIN AMACI")
            drawBodyText("Bu çalışma, $cleanTopic konusunun dönemsel dinamiklerini, tarihsel sebep-sonuç bağlamlarını ve medeniyet gelişimine etkilerini birincil ve ikincil kaynaklar ışığında incelemeyi amaçlamaktadır.")
            drawBodyText("Temel Hipotez: Tarihsel olaylar tecrit edilmiş vakalar olmayıp sosyal, iktisadi ve jeopolitik koşulların kaçınılmaz birer sentezidir.")
            yPos += 8f

            drawSectionHeader("2. TARİHSEL SÜREÇ VE KAYNAK ANALİZİ")
            drawBodyText("• Dönemin Siyasi ve Askeri Şartları: Bölgesel dengeler ve güç odaklarının ilişkileri irdelenmiştir.")
            drawBodyText("• Toplumsal ve İktisadi Dönüşüm: Olayın halk tabanındaki yansımaları ve iktisadi sonuçları tahlil edilmiştir.")
            drawBodyText("• Belge ve Kronik İncelemesi: Dönemin vakanüvis kayıtları ve resmî vesikaları karşılaştırmalı olarak taranmıştır.")
            yPos += 8f

            drawSectionHeader("3. SONUÇ VE PEDAGOJİK DEĞERLENDİRME")
            drawBodyText("Araştırma bulguları, incelenen konunun günümüz toplum ve devlet yapısına etkilerinin süreklilik arz ettiğini ortaya koymaktadır.")
            drawBodyText("Tarihsel bilincin gelişmesinde eleştirel kaynak okumasının ve çok boyutlu yaklaşımın vazgeçilmez olduğu sonucuna ulaşılmıştır.")
            yPos += 8f

            drawSectionHeader("4. KAYNAKÇA")
            drawBodyText("1. Halil İnalcık, 'Devlet-i 'Aliyye: Osmanlı İmparatorluğu Üzerine Araştırmalar', İş Bankası Kültür Yayınları.")
            drawBodyText("2. İlber Ortaylı, 'Gelenekten Geleceğe', Timaş Yayınları.")
            drawBodyText("3. Millî Eğitim Bakanlığı, 'Ortaöğretim Tarih Ders Kitabı', Devlet Kitapları.")

            // Alt Çizgi
            paint.color = Color.parseColor("#CBD5E1")
            canvas.drawLine(40f, 805f, 555f, 805f, paint)
            paint.color = Color.parseColor("#64748B")
            paint.textSize = 8.5f
            canvas.drawText("HatırlaGit ATİLA • Lise Akademik Performans Tezi Formatı", 40f, 822f, paint)

            pdfDoc.finishPage(page)

            val fos = FileOutputStream(targetPdf)
            pdfDoc.writeTo(fos)
            fos.close()
            pdfDoc.close()

            // Room Database Kaydı
            val reminder = ReminderEntity(
                category = "ÖĞRENCİ_TEZİ",
                title = "🎓 Tarih Tezi: $cleanTopic",
                dueDatetime = dateStr,
                dueDateMillis = now,
                customNote = "Lise Performans Tezi PDF formatında hazırlandı.\nDosya: ${targetPdf.name}",
                isFavorite = false,
                encryptedMetadata = "{\"file_path\":\"${targetPdf.absolutePath}\"}",
                actionStep = "NOTE_SAVED"
            )
            AppDatabase.getDatabase(context).reminderDao().insertReminder(reminder)

            ResearchPdfHelper.openPdfFile(context, targetPdf)

            Pair(targetPdf, "🎓 **Lise Tarih Performans Tezi** hazırlandı ve ekranda açıldı:\n`Documents/HatirlaGit_OgrenciTezleri/${targetPdf.name}`")
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(null, "Tez PDF'i oluşturulurken hata meydana geldi: ${e.localizedMessage}")
        }
    }
}
