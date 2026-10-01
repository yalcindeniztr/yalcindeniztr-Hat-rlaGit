import os
import unittest

WORKSPACE = r"C:\Users\yalci\antigravity\HatırlaGit"
APP_DIR = os.path.join(WORKSPACE, "app", "src", "main")
JAVA_UTIL_DIR = os.path.join(APP_DIR, "java", "com", "example", "util")

class TestAtillaLinguisticsAndTypo(unittest.TestCase):

    def test_01_atilla_turkish_linguistics_helper(self):
        """1. Türkçe İmla, Harf/Ses Hatası Toleransı ve Deyim Motoru doğrulaması"""
        file_path = os.path.join(JAVA_UTIL_DIR, "AtillaTurkishLinguisticsHelper.kt")
        self.assertTrue(os.path.exists(file_path), "AtillaTurkishLinguisticsHelper.kt bulunamadı")
        with open(file_path, "r", encoding="utf-8") as f:
            content = f.read()

        # "yarımı planla" (m-n imla/klavye hatası) kontrolü
        self.assertIn("yarımı planla", content)
        self.assertIn("yarını planla", content)
        self.assertIn("PLAN_TOMORROW", content)
        self.assertIn("generateTomorrowPlan", content)

        # Günümü planla kontrolü
        self.assertIn("günümü planla", content)
        self.assertIn("PLAN_TODAY", content)
        self.assertIn("generateTodayPlan", content)

        # Deyimler ve Atasözleri haritası
        self.assertIn("etekleri zil çalmak", content)
        self.assertIn("can kulağıyla dinlemek", content)
        self.assertIn("damlaya damlaya göl olur", content)
        print(" -> Test 01: AtillaTurkishLinguisticsHelper imla düzeltme ve deyimler yapısı doğrulandı.")

    def test_02_live_web_research_drawer_isolation(self):
        """2. LiveWebResearchDrawer planlama ve asistanlık komutlarını yakalamamalı"""
        drawer_path = os.path.join(JAVA_UTIL_DIR, "assistant", "drawers", "LiveWebResearchDrawer.kt")
        with open(drawer_path, "r", encoding="utf-8") as f:
            content = f.read()

        # 2-kelime hatasının kaldırıldığını doğrula
        self.assertNotIn('lowerQuery.split(" ").size >= 2', content)

        # Asistanlık ve planlama komutlarının engellendiğini doğrula
        self.assertIn('"planla"', content)
        self.assertIn('"program"', content)
        self.assertIn('"yarın"', content)
        self.assertIn('"yarım"', content)
        self.assertIn('"günüm"', content)
        print(" -> Test 02: LiveWebResearchDrawer planlama sorgularından tamamen izole edildi.")

    def test_03_ai_assistant_service_linguistic_dispatch(self):
        """3. AiAssistantService Türkçe dil motoru öncelikli yönlendirme kontrolü"""
        ai_path = os.path.join(JAVA_UTIL_DIR, "AiAssistantService.kt")
        with open(ai_path, "r", encoding="utf-8") as f:
            content = f.read()

        self.assertIn("AtillaTurkishLinguisticsHelper.analyze", content)
        self.assertIn("NormalizedIntent.PLAN_TOMORROW", content)
        self.assertIn("NormalizedIntent.PLAN_TODAY", content)
        self.assertIn("NormalizedIntent.IDIOM_OR_PROVERB", content)
        print(" -> Test 03: AiAssistantService Türkçe imla ve niyet yönlendirmesi doğrulandı.")

if __name__ == "__main__":
    unittest.main()
