package com.example

import com.example.util.GeneralKnowledgeHelper
import com.example.util.RecipeHelper
import org.junit.Assert.*
import org.junit.Test

class GeneralKnowledgeUnitTest {

    @Test
    fun testOfflineHistoryKnowledge_FatihSultanMehmet() {
        val answer = GeneralKnowledgeHelper.answerQuery("Fatih Sultan Mehmet kimdir")
        assertNotNull("Fatih Sultan Mehmet bilgisi bulunmalı", answer)
        assertEquals("TARİH", answer?.category)
        assertTrue("1453 İstanbul fethi yer almalı", answer?.fullContent?.contains("1453") == true)
        assertTrue("Orta Çağ ve Yeni Çağ geçmeli", answer?.fullContent?.contains("Orta Çağ") == true)
    }

    @Test
    fun testOfflineHistoryKnowledge_KurtulusSavasi() {
        val answer = GeneralKnowledgeHelper.answerQuery("Kurtuluş Savaşı cepheleri")
        assertNotNull("Kurtuluş Savaşı bilgisi bulunmalı", answer)
        assertEquals("TARİH", answer?.category)
        assertTrue("Doğu Cephesi bulunmalı", answer?.fullContent?.contains("Doğu Cephesi") == true)
        assertTrue("Batı Cephesi bulunmalı", answer?.fullContent?.contains("Batı Cephesi") == true)
        assertTrue("Lozan Barış Antlaşması bulunmalı", answer?.fullContent?.contains("Lozan") == true)
    }

    @Test
    fun testOfflineHistoryKnowledge_Canakkale() {
        val answer = GeneralKnowledgeHelper.answerQuery("Çanakkale Zaferi hakkında bilgi ver")
        assertNotNull("Çanakkale Zaferi bilgisi bulunmalı", answer)
        assertEquals("TARİH", answer?.category)
        assertTrue("18 Mart Deniz Zaferi bulunmalı", answer?.fullContent?.contains("18 Mart") == true)
        assertTrue("Anafartalar veya Mustafa Kemal bulunmalı", answer?.fullContent?.contains("Mustafa Kemal") == true)
    }

    @Test
    fun testOfflineScienceKnowledge_Kuantum() {
        val answer = GeneralKnowledgeHelper.answerQuery("Kuantum fiziği nedir")
        assertNotNull("Kuantum bilgisi bulunmalı", answer)
        assertEquals("BİLİM", answer?.category)
        assertTrue("Max Planck veya Heisenberg bulunmalı", answer?.fullContent?.contains("Planck") == true || answer?.fullContent?.contains("Heisenberg") == true)
        assertTrue("Dalga-Parçacık ikiliği bulunmalı", answer?.fullContent?.contains("Dalga-Parçacık") == true)
    }

    @Test
    fun testOfflineScienceKnowledge_Dna() {
        val answer = GeneralKnowledgeHelper.answerQuery("DNA ve genetik yapı nedir")
        assertNotNull("DNA bilgisi bulunmalı", answer)
        assertEquals("BİLİM", answer?.category)
        assertTrue("Adenin, Timin, Guanin, Sitozin geçmeli", answer?.fullContent?.contains("Adenin") == true)
    }

    @Test
    fun testRecipeInquiryAndFridgeSynthesis() {
        // 1. Akşam yemeği istişaresi
        val (inquiryText, inquirySpeech) = RecipeHelper.getInteractiveFoodInquiry("Sayın Hocam")
        assertTrue("İstişare başlığı içermeli", inquiryText.contains("Akşam Yemeği İçin Ne Arzu Edersiniz"))
        assertTrue("Hafif ve doyurucu seçenekleri sunmalı", inquiryText.contains("Hafif & Pratik") && inquiryText.contains("Doyurucu"))

        // 2. Dolaptaki malzemelere göre dinamik analiz
        val (recipeText, recipeSpeech) = RecipeHelper.analyzeFridgeAndSuggest("Buzdolabında tavuk ve mantar var", "Sayın Hocam")
        assertTrue("Kremalı Mantarlı Tavuk Sote önerilmeli", recipeText.contains("Kremalı Mantarlı Tavuk Sote"))
        assertTrue("Şefin püf noktası bulunmalı", recipeText.contains("Şefin Püf Noktası"))
        assertTrue("Adım adım yapılışı bulunmalı", recipeText.contains("Adım Adım"))

        // 3. Başka bir malzeme kombinasyonu: Kıyma ve Patates
        val (recipeKiyma, _) = RecipeHelper.analyzeFridgeAndSuggest("Elimde kıyma ve patates var", "Sayın Hocam")
        assertTrue("Kıymalı Patates Yemeği önerilmeli", recipeKiyma.contains("Kıymalı Patates"))
    }
}
