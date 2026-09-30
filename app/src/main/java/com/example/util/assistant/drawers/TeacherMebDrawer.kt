package com.example.util.assistant.drawers

import android.content.Context
import com.example.util.AnnualPlanParams
import com.example.util.MebDocumentHelper
import com.example.util.SokMeetingParams
import com.example.util.assistant.AssistantDrawer
import com.example.util.assistant.DrawerResult
import com.example.util.assistant.WardrobeSessionData

object TeacherMebDrawer : AssistantDrawer {
    override val drawerName: String = "Öğretmen & MEB Mevzuatı Çekmecesi"

    override fun canHandle(query: String, lowerQuery: String): Boolean {
        return lowerQuery.contains("yıllık plan") ||
               lowerQuery.contains("öğretmen plan") ||
               lowerQuery.contains("ders programı") ||
               lowerQuery.contains("haftalık plan") ||
               lowerQuery.contains("şök tutanağı") ||
               lowerQuery.contains("şök hazırla") ||
               lowerQuery.contains("sınav kağıdı") ||
               lowerQuery.contains("açık uçlu sınav") ||
               lowerQuery.contains("ders anlat") ||
               lowerQuery.contains("tarih anlat") ||
               lowerQuery.contains("test hazırla") ||
               lowerQuery.contains("soru sor") ||
               lowerQuery.contains("maarif")
    }

    override suspend fun handle(
        context: Context,
        query: String,
        lowerQuery: String,
        sessionData: WardrobeSessionData
    ): DrawerResult? {
        val patronPrefix = if (sessionData.userNick.isNotBlank()) "Sayın Öğretmenim ${sessionData.userNick}" else "Sayın Öğretmenim"

        // 1. Öğretmen Haftalık Ders & Çalışma Programı
        if (lowerQuery.contains("ders programı") || lowerQuery.contains("öğretmen plan") || lowerQuery.contains("haftalık plan")) {
            val plan = buildString {
                append("📚 **$patronPrefix, Haftalık MEB Öğretmen ve Ders Çalışma Programınız:**\n\n")
                append("🗓️ **Pazartesi:**\n")
                append(" • 08:30 - 12:00: Ders Blokları (Konu Girişi & EBA İçerikleri)\n")
                append(" • 13:00 - 15:00: Ölçme & Değerlendirme / Soru Çözümü\n\n")
                append("🗓️ **Salı (Nöbet Günü):**\n")
                append(" • 08:00 - 15:30: Okul Kat Nöbet Görevi & Dersler\n")
                append(" • 15:40 - 16:30: Günlük Nöbet Defteri ve Yoklama İşlemleri\n\n")
                append("🗓️ **Çarşamba:**\n")
                append(" • 09:00 - 12:30: Zümre Öğretmenler Kurulu İstişaresi & Ortak Sınav Hazırlığı\n")
                append(" • 13:30 - 15:00: e-Okul Not Girişi ve Kazanım Takibi\n\n")
                append("🗓️ **Perşembe:**\n")
                append(" • 08:30 - 12:00: Branş Dersleri & Öğrenci Proje Değerlendirmeleri\n")
                append(" • 13:00 - 14:30: Sosyal Kulüp / Rehberlik Çalışmaları\n\n")
                append("🗓️ **Cuma:**\n")
                append(" • 08:30 - 12:00: Haftalık Kazanım Pekiştirme & Quiz Uygulaması\n")
                append(" • 13:30 - 15:00: Haftalık Ders Defteri İmzaları ve MEBBİS Kontrolleri\n\n")
                append("💡 Bu programı Google Takviminize işlemek için _'Öğretmen programını takvime işle'_ demeniz yeterlidir.")
            }

            val speech = "$patronPrefix, haftalık ders, nöbet ve zümre planlamanızı hazırladım. Dilerseniz takviminize işleyebilirim."

            return DrawerResult(
                replyText = plan,
                actionSummary = "📚 Öğretmen Haftalık Planı Hazır",
                speechText = speech
            )
        }

        if (lowerQuery.contains("yıllık plan")) {
            val isEdebiyat = lowerQuery.contains("edebiyat")
            val courseName = if (isEdebiyat) "Türk Dili ve Edebiyatı" else "Tarih"
            val grade = if (lowerQuery.contains("10")) "10. Sınıf" else if (lowerQuery.contains("11")) "11. Sınıf" else if (lowerQuery.contains("12")) "12. Sınıf" else "9. Sınıf"
            val (file, report) = MebDocumentHelper.createAnnualPlanPdf(
                context = context,
                params = AnnualPlanParams(
                    schoolName = "${sessionData.userCity} Anadolu Lisesi",
                    principalName = "Okul Müdürü",
                    teachers = if (sessionData.userNick.isNotBlank()) "${sessionData.userNick} Öğretmen" else "Zümre Öğretmenleri",
                    courseName = courseName,
                    gradeLevel = grade,
                    planType = "Yıllık Plan"
                )
            )
            return DrawerResult(
                replyText = report,
                actionSummary = "📋 Yıllık Plan Hazırlandı: $courseName ($grade)",
                generatedPdfFile = file
            )
        }

        if (lowerQuery.contains("şök")) {
            val className = "10-A"
            val (file, report) = MebDocumentHelper.createSokMeetingPdf(
                context = context,
                params = SokMeetingParams(
                    schoolName = "${sessionData.userCity} Anadolu Lisesi",
                    className = className,
                    termName = "1. Dönem",
                    classTeacherName = if (sessionData.userNick.isNotBlank()) "${sessionData.userNick} Öğretmen" else "Sınıf Rehber Öğretmeni"
                )
            )
            return DrawerResult(
                replyText = report,
                actionSummary = "📑 ŞÖK Tutanağı Hazırlandı: $className",
                generatedPdfFile = file
            )
        }

        if (lowerQuery.contains("sınav kağıdı") || lowerQuery.contains("açık uçlu sınav")) {
            val isEdebiyat = lowerQuery.contains("edebiyat")
            val courseName = if (isEdebiyat) "Türk Dili ve Edebiyatı" else "Tarih"
            val (file, report) = MebDocumentHelper.createExamPaperPdf(
                context = context,
                schoolName = "${sessionData.userCity} Anadolu Lisesi",
                courseName = courseName,
                gradeLevel = "10. Sınıf",
                examName = "1. Dönem 1. Yazılı Sınavı",
                examContent = "Açık uçlu 5 adet senaryo sorusu ve rubrik puanlama anahtarı hazırlanmıştır."
            )
            return DrawerResult(
                replyText = report,
                actionSummary = "📝 Sınav Kağıdı Hazırlandı: $courseName",
                generatedPdfFile = file
            )
        }

        // 5. Tarih Dersi Anlatımı & İnteraktif Senaryo Sınavı / Test Hazırlığı
        if (lowerQuery.contains("ders anlat") || lowerQuery.contains("tarih anlat") || lowerQuery.contains("konuyu anlat") ||
            lowerQuery.contains("test hazırla") || lowerQuery.contains("soru sor") || lowerQuery.contains("sınav hazırla")) {

            val isTestOnly = lowerQuery.contains("test hazırla") || lowerQuery.contains("soru sor")
            val topic = query.replace(Regex("(?i)ders anlat|tarih anlat|konuyu anlat|test hazırla|soru sor|sınav hazırla|öğretmen gibi|bana|lütfen"), "").trim()
                .ifBlank { "Osmanlı Devleti Kuruluş Dönemi ve Gaza Siyaseti" }

            val reply = buildString {
                if (!isTestOnly) {
                    append("📖 **$patronPrefix, Tarih Konu Anlatımı:**\n")
                    append("🎓 **Konu:** $topic\n\n")
                    append("Sayın Hocam; $topic konusu Türkiye Yüzyılı Maarif Modelinde tarihsel empati, birincil kaynak analizi ve kronolojik düşünme becerileri ekseninde ele alınır:\n\n")
                    append("1. **Tarihsel Arka Plan:** Bölgesel jeopolitik dengeler, beylikler arası ilişkiler ve gaza ruhunun teşkilatlanmadaki rolü temel belirleyicidir.\n")
                    append("2. **Temel Kavramlar:** Gaza, İstimalet (Hoşgörü) Politikası, İskân Siyaseti ve Alperenlik Geleneği.\n")
                    append("3. **Medeniyet Çıktısı:** Sadece askeri zaferler değil, fethedilen topraklarda adil yönetim ve sosyal kurumların inşası kalıcılığı sağlamıştır.\n\n")
                    append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
                }

                append("✍️ **Ölçme - Değerlendirme & Açık Uçlu Senaryo Soruları:**\n\n")
                append("• **Soru 1 (Analiz):** Osmanlı Devleti'nin Rumeli'ye geçişinde uyguladığı İskân ve İstimalet politikalarının fetihlerin kalıcı olmasındaki rolünü gerekçelendirerek açıklayınız.\n\n")
                append("• **Soru 2 (Tarihsel Empati):** Dönemin bir tımarlı sipahisi veya ahisi olduğunuzu varsayarak, toplumsal düzenin sağlanmasındaki sorumluluklarınızı 2 madde halinde belirtiniz.\n\n")
                append("• **Soru 3 (Çoktan Seçmeli):** Aşağıdakilerden hangisi Kuruluş Dönemi teşkilatlanma çalışmalarından biri değildir?\n")
                append("   A) Yaya ve Müsellem ordusu  B) Divan Teşkilatı  C) Nizâm-ı Cedid  D) Tımar Sistemi\n")
                append("   *(Cevap: C - Nizâm-ı Cedid, III. Selim dönemine aittir)*\n\n")
                append("💡 _Bu sınavı resmi A4 PDF formatında yazdırmak için **'Sınav kağıdı hazırla'** demeniz yeterlidir._")
            }

            val speech = "$patronPrefix, $topic konusunun pedagojik anlatımını ve Maarif modeline uygun açık uçlu senaryo sorularını ekranınıza getirdim."

            return DrawerResult(
                replyText = reply,
                actionSummary = "🎓 Tarih Dersi & Sınavı: $topic",
                speechText = speech
            )
        }

        return null // Kütüphane / Gemini çekmecesine pasla
    }
}
