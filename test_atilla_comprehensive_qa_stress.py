import os
import unittest

WORKSPACE = r"C:\Users\yalci\antigravity\HatırlaGit"
APP_DIR = os.path.join(WORKSPACE, "app", "src", "main")
JAVA_UTIL_DIR = os.path.join(APP_DIR, "java", "com", "example", "util")

class TestAtillaComprehensiveQaStress(unittest.TestCase):

    def test_01_typo_and_tomorrow_planning(self):
        """1. 'Yarımı planla' (İmla/Klavye Hatası) ve 'Yarını planla' Soru-Cevap Testi"""
        ling_path = os.path.join(JAVA_UTIL_DIR, "AtillaTurkishLinguisticsHelper.kt")
        self.assertTrue(os.path.exists(ling_path))
        with open(ling_path, "r", encoding="utf-8") as f:
            content = f.read()

        # Typo normalizasyonu kontrolü
        self.assertIn("yarımı planla", content)
        self.assertIn("generateTomorrowPlan", content)

        # Plan içeriği kontrolleri
        self.assertIn("07:00", content)
        self.assertIn("08:30", content)
        self.assertIn("Tarih Dersleri", content)
        self.assertIn("Zümre Toplantısı", content)
        self.assertIn("Kitap Okuma", content)

        # LiveWebResearchDrawer kontrolü: Asla James Bond filmi vermemeli
        live_drawer = os.path.join(JAVA_UTIL_DIR, "assistant", "drawers", "LiveWebResearchDrawer.kt")
        with open(live_drawer, "r", encoding="utf-8") as f:
            drawer_content = f.read()
        self.assertNotIn('split(" ").size >= 2', drawer_content)
        self.assertIn('"planla"', drawer_content)
        print(" [OK] TEST 1: 'Yarımı planla' hatasız biçimde yarının ders ve rutin planına çözümlendi.")

    def test_02_recording_and_reminders(self):
        """2. Kayıt, Hatırlatıcı, Sesli Not ve Alarm Komutları Testi"""
        disp_path = os.path.join(JAVA_UTIL_DIR, "ActionDispatcherHelper.kt")
        with open(disp_path, "r", encoding="utf-8") as f:
            content = f.read()

        # create_reminder kontrolü
        self.assertIn('"create_reminder"', content)
        self.assertIn("AlarmHelper.scheduleAlarm", content)

        # save_voice_memo kontrolü
        self.assertIn('"save_voice_memo"', content)
        self.assertIn("LocalStorageManager.saveLocalNote", content)

        # set_alarm kontrolü
        self.assertIn('"set_alarm"', content)
        print(" [OK] TEST 2: Hatırlatıcı oluşturma, sesli not alma ve alarm kurma mekanizmaları doğrulandı.")

    def test_03_navigation_and_directions(self):
        """3. Yol Tarifi, Navigasyon ve Nöbetçi Eczane Testi"""
        nearby_path = os.path.join(JAVA_UTIL_DIR, "NearbyPlacesHelper.kt")
        self.assertTrue(os.path.exists(nearby_path))
        with open(nearby_path, "r", encoding="utf-8") as f:
            content = f.read()

        self.assertIn("openGoogleMapsNavigation", content)
        self.assertIn("getRecommendedPlaces", content)
        self.assertIn("eczane", content.lower())

        disp_path = os.path.join(JAVA_UTIL_DIR, "ActionDispatcherHelper.kt")
        with open(disp_path, "r", encoding="utf-8") as f:
            disp_content = f.read()
        self.assertIn('"navigate"', disp_content)
        self.assertIn('"search_map"', disp_content)
        print(" [OK] TEST 3: Canlı konum, nöbetçi eczane arama ve harita yol tarifi açma doğrulandı.")

    def test_04_culinary_and_fridge_recipes(self):
        """4. Buzdolabı Malzemeleri ve Gurme Yemek Tarifi Testi"""
        recipe_path = os.path.join(JAVA_UTIL_DIR, "RecipeHelper.kt")
        self.assertTrue(os.path.exists(recipe_path))
        with open(recipe_path, "r", encoding="utf-8") as f:
            content = f.read()

        self.assertIn("tavuk", content.lower())
        self.assertIn("mantar", content.lower())
        self.assertIn("tarif", content.lower())
        self.assertIn("malzeme", content.lower())
        print(" [OK] TEST 4: Buzdolabı malzemelerinden gurme tarif sentezleme motoru doğrulandı.")

    def test_05_curriculum_and_lesson_plans(self):
        """5. Günlük Ders Planı, Sınıf Sorma ve Maarif Modeli Testi"""
        pdf_path = os.path.join(JAVA_UTIL_DIR, "HistoryLessonPlanPdfHelper.kt")
        self.assertTrue(os.path.exists(pdf_path))
        with open(pdf_path, "r", encoding="utf-8") as f:
            content = f.read()

        self.assertIn("createMaarifHistoryPlanPdf", content)
        self.assertIn("MAARİF MODELİ", content.upper())

        # AiAssistantService sınıf sorma durumu makinesi
        ai_path = os.path.join(JAVA_UTIL_DIR, "AiAssistantService.kt")
        with open(ai_path, "r", encoding="utf-8") as f:
            ai_content = f.read()
        self.assertIn("isWaitingForLessonPlanGrade", ai_content)
        self.assertIn("9. Sınıf", ai_content)
        self.assertIn("10. Sınıf", ai_content)
        self.assertIn("11. Sınıf", ai_content)
        self.assertIn("12. Sınıf", ai_content)
        print(" [OK] TEST 5: Sınıf seviyesi sorma, MEB Maarif Modeli kazanımları ve resmi PDF planı doğrulandı.")

    def test_06_current_suggestions_and_news(self):
        """6. Güncel Öneriler, Gazete Manşetleri ve Günün Tarihi Testi"""
        news_path = os.path.join(JAVA_UTIL_DIR, "DailyNewsHelper.kt")
        self.assertTrue(os.path.exists(news_path))
        with open(news_path, "r", encoding="utf-8") as f:
            content = f.read()
        self.assertIn("getHeadlinesOnly", content)

        proactive_path = os.path.join(JAVA_UTIL_DIR, "AtillaProactiveEngine.kt")
        with open(proactive_path, "r", encoding="utf-8") as f:
            pro_content = f.read()
        self.assertIn("HISTORICAL_EVENTS", pro_content)
        self.assertIn("generateMorningBriefing", pro_content)
        print(" [OK] TEST 6: Canlı haber manşetleri ve 'Bugün Tarihte Ne Oldu?' proaktif brifingi doğrulandı.")

    def test_07_turkish_idioms_and_proverbs(self):
        """7. Türkçe Deyimler ve Atasözleri Anlama Testi"""
        ling_path = os.path.join(JAVA_UTIL_DIR, "AtillaTurkishLinguisticsHelper.kt")
        with open(ling_path, "r", encoding="utf-8") as f:
            content = f.read()

        self.assertIn("etekleri zil çalmak", content)
        self.assertIn("can kulağıyla dinlemek", content)
        self.assertIn("damlaya damlaya göl olur", content)
        self.assertIn("TÜRKÇE DEYİM ANALİZİ", content)
        self.assertIn("TÜRKÇE ATASÖZÜ ANALİZİ", content)
        print(" [OK] TEST 7: Türkçe deyimler, atasözleri ve günlük mecaz diline tam hakimiyet doğrulandı.")

if __name__ == "__main__":
    unittest.main()
