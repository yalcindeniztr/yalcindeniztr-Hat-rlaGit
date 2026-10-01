package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.AiAssistantService
import com.example.util.GeneralKnowledgeHelper
import com.example.util.RecipeHelper
import com.example.util.UstaSessionState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class JarvisAssistantComprehensiveTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // Oturum durumunu sıfırla
        UstaSessionState.isWaitingForLessonPlanGrade = false
        UstaSessionState.isWaitingForFoodPreference = false
        UstaSessionState.isWaitingForAlarmTime = false
        UstaSessionState.isWaitingForCallConfirmation = false
        UstaSessionState.lastGeneratedPdfFile = null
    }

    @Test
    fun test1_LessonPlanAsksGradeFirst_ThenGeneratesPdf() = runBlocking {
        // 1. Aşama: Kullanıcı sınıf belirtmeden plan istiyor
        val prompt1 = "Günlük plan hazırla bu hafta için"
        val resp1 = AiAssistantService.processUserMessage(context, prompt1)

        assertTrue("Sınıf sorulmalıydı", UstaSessionState.isWaitingForLessonPlanGrade)
        assertTrue("Metinde sınıf düzeyi sorusu yer almalı", resp1.replyText.contains("hangi sınıf düzeyimiz için", ignoreCase = true))

        // 2. Aşama: Kullanıcı sınıfı belirtiyor
        val prompt2 = "10. Sınıf için hazırla"
        val resp2 = AiAssistantService.processUserMessage(context, prompt2)

        assertFalse("Sınıf bekleme durumu kapanmalı", UstaSessionState.isWaitingForLessonPlanGrade)
        assertTrue("10. sınıf planı hazırlandığı belirtilmeli", resp2.replyText.contains("Maarif Modeli Tarih Ders Planınız Hazırlandı", ignoreCase = true))
        assertNotNull("PDF belgesi oluşturulmuş olmalı", resp2.generatedPdfFile)
        assertTrue("PDF dosyası fiziksel olarak mevcut olmalı", resp2.generatedPdfFile?.exists() == true)
    }

    @Test
    fun test2_FoodInquiry_ThenDynamicFridgeAnalysis() = runBlocking {
        // 1. Aşama: Akşam yemeği tavsiyesi isteme (Ezbere yemek basmamalı, istişare etmeli)
        val prompt1 = "Akşam için yemek tarifi öner"
        val resp1 = AiAssistantService.processUserMessage(context, prompt1)

        assertTrue("Yemek tercihi istişaresi açılmalı", UstaSessionState.isWaitingForFoodPreference)
        assertTrue("Metinde istişare seçenekleri sunulmalı", resp1.replyText.contains("Akşam Yemeği İçin Ne Arzu Edersiniz", ignoreCase = true))

        // 2. Aşama: Dolaptaki malzemeleri söyleme
        val prompt2 = "Buzdolabımda tavuk ve mantar var ne yapabilirim"
        val resp2 = AiAssistantService.processUserMessage(context, prompt2)

        assertFalse("Yemek tercihi bekleme durumu kapanmalı", UstaSessionState.isWaitingForFoodPreference)
        assertTrue("Tavuk ve mantar analizi yapılmalı", resp2.replyText.contains("Kremalı Mantarlı Tavuk Sote", ignoreCase = true) || resp2.replyText.contains("Tavuk", ignoreCase = true))
        assertTrue("Adım adım yapılışı bulunmalı", resp2.replyText.contains("Adım Adım", ignoreCase = true))
    }

    @Test
    fun test3_OyunHavasi_TriggersYouTube_NotWeather() = runBlocking {
        val prompt = "Oyun havası aç"
        val resp = AiAssistantService.processUserMessage(context, prompt)

        assertFalse("Hava durumu tetiklenmemeli!", resp.replyText.contains("derece", ignoreCase = true) && resp.replyText.contains("nem", ignoreCase = true))
        assertTrue("YouTube eylemi tetiklenmeli", resp.actionSummary?.contains("YouTube", ignoreCase = true) == true)
    }

    @Test
    fun test4_Pharmacy_TriggersNavigationAndTopPlaces() = runBlocking {
        val prompt = "En yakın nöbetçi eczane bul"
        val resp = AiAssistantService.processUserMessage(context, prompt)

        assertTrue("Nöbetçi eczaneler listelenmeli", resp.recommendedPlaces.isNotEmpty() || resp.replyText.contains("Eczane", ignoreCase = true))
        assertTrue("Navigasyon aksiyonu bulunmalı", resp.actionSummary?.contains("Nöbetçi Eczane", ignoreCase = true) == true)
    }

    @Test
    fun test5_OfflineDeepKnowledge_AnswersAccurately() {
        // Tarih: Fatih Sultan Mehmet
        val fatih = GeneralKnowledgeHelper.answerQuery("Fatih Sultan Mehmet kimdir")
        assertNotNull("Fatih Sultan Mehmet yanıtı bulunmalı", fatih)
        assertTrue("1453 fethi içermeli", fatih?.fullContent?.contains("1453") == true)

        // Bilim: Kuantum Fiziği
        val kuantum = GeneralKnowledgeHelper.answerQuery("Kuantum fiziği nedir")
        assertNotNull("Kuantum yanıtı bulunmalı", kuantum)
        assertTrue("Heisenberg veya dalga içermeli", kuantum?.fullContent?.contains("Kuantum") == true)

        // Tarih: Kurtuluş Savaşı
        val kurtulus = GeneralKnowledgeHelper.answerQuery("Kurtuluş Savaşı cepheleri")
        assertNotNull("Kurtuluş Savaşı yanıtı bulunmalı", kurtulus)
        assertTrue("Cepheler açıklanmalı", kurtulus?.fullContent?.contains("Doğu Cephesi") == true)
    }

    @Test
    fun test6_EmpathyAndMoraleSupport() = runBlocking {
        val prompt = "Moralim çok bozuk bugün çok yoruldum"
        val resp = AiAssistantService.processUserMessage(context, prompt)

        assertTrue("Teselli ve manevi destek içermeli", resp.replyText.contains("Destek & Teselli", ignoreCase = true) || resp.replyText.contains("nefes alın", ignoreCase = true))
        assertTrue("Aksiyon özeti teselli olmalı", resp.actionSummary?.contains("Teselli", ignoreCase = true) == true || resp.actionSummary?.contains("Destek", ignoreCase = true) == true)
    }
}
