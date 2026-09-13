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
               lowerQuery.contains("şök") ||
               lowerQuery.contains("sınav kağıdı") ||
               lowerQuery.contains("açık uçlu sınav") ||
               lowerQuery.contains("657") ||
               lowerQuery.contains("ömk") ||
               lowerQuery.contains("ek ders") ||
               lowerQuery.contains("bep") ||
               lowerQuery.contains("zümre") ||
               lowerQuery.contains("sendika")
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
            val (_, report) = MebDocumentHelper.createAnnualPlanPdf(
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
                actionSummary = "📋 Yıllık Plan Hazırlandı: $courseName ($grade)"
            )
        }

        if (lowerQuery.contains("şök")) {
            val className = "10-A"
            val (_, report) = MebDocumentHelper.createSokMeetingPdf(
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
                actionSummary = "📑 ŞÖK Tutanağı Hazırlandı: $className"
            )
        }

        if (lowerQuery.contains("sınav kağıdı") || lowerQuery.contains("açık uçlu sınav")) {
            val isEdebiyat = lowerQuery.contains("edebiyat")
            val courseName = if (isEdebiyat) "Türk Dili ve Edebiyatı" else "Tarih"
            val (_, report) = MebDocumentHelper.createExamPaperPdf(
                context = context,
                schoolName = "${sessionData.userCity} Anadolu Lisesi",
                courseName = courseName,
                gradeLevel = "10. Sınıf",
                examName = "1. Dönem 1. Yazılı Sınavı",
                examContent = "Açık uçlu 5 adet senaryo sorusu ve rubrik puanlama anahtarı hazırlanmıştır."
            )
            return DrawerResult(
                replyText = report,
                actionSummary = "📝 Sınav Kağıdı Hazırlandı: $courseName"
            )
        }

        return null // Kütüphane / Gemini çekmecesine pasla
    }
}
