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
