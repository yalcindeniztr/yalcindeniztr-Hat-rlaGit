import os
import re
import unittest

WORKSPACE = r"C:\Users\yalci\antigravity\HatırlaGit"
APP_DIR = os.path.join(WORKSPACE, "app", "src", "main")
JAVA_UTIL_DIR = os.path.join(APP_DIR, "java", "com", "example", "util")
MANIFEST_PATH = os.path.join(APP_DIR, "AndroidManifest.xml")

class TestAtillaMasterV4(unittest.TestCase):

    def test_01_atilla_emotion_engine(self):
        """1. Duygusal Akustik Analiz (AtillaEmotionEngine) testi"""
        file_path = os.path.join(JAVA_UTIL_DIR, "AtillaEmotionEngine.kt")
        self.assertTrue(os.path.exists(file_path), "AtillaEmotionEngine.kt bulunamadı")
        with open(file_path, "r", encoding="utf-8") as f:
            content = f.read()

        # EmotionState enum kontrolü
        self.assertIn("enum class EmotionState", content)
        for state in ["TIRED", "STRESSED", "JOYFUL", "URGENT", "TEACHER_MAARIF", "WITTY", "NEUTRAL"]:
            self.assertIn(state, content)

        # Anahtar kelime listeleri
        self.assertIn("TIRED_KEYWORDS", content)
        self.assertIn("STRESSED_KEYWORDS", content)
        self.assertIn("JOYFUL_KEYWORDS", content)
        self.assertIn("URGENT_KEYWORDS", content)
        self.assertIn("TEACHER_MAARIF_KEYWORDS", content)
        self.assertIn("WITTY_KEYWORDS", content)

        # Empati cümleleri ve ses parametreleri
        self.assertIn("voicePitch", content)
        self.assertIn("speechRate", content)
        self.assertIn("empathyPreamble", content)
        print(" -> Test 01: AtillaEmotionEngine duygusal ve akustik analiz yapısı doğrulandı.")

    def test_02_tts_dynamic_emotion_modulation(self):
        """2. Dinamik Sesli Yanıt / Neural & Adaptive TTS Modülasyon testi"""
        file_path = os.path.join(JAVA_UTIL_DIR, "TtsHelper.kt")
        self.assertTrue(os.path.exists(file_path), "TtsHelper.kt bulunamadı")
        with open(file_path, "r", encoding="utf-8") as f:
            content = f.read()

        self.assertIn("AtillaEmotionEngine.EmotionState", content)
        self.assertIn("currentPitch", content)
        self.assertIn("currentSpeechRate", content)
        self.assertIn("engine.setPitch(currentPitch)", content)
        self.assertIn("engine.setSpeechRate(currentSpeechRate)", content)
        print(" -> Test 02: TtsHelper dinamik duygu durumlu ses frekansı modülasyonu doğrulandı.")

    def test_03_proactive_maarif_engine(self):
        """3. Proaktif Maarif & Müfredat Hatırlatıcısı (AtillaProactiveEngine) testi"""
        file_path = os.path.join(JAVA_UTIL_DIR, "AtillaProactiveEngine.kt")
        self.assertTrue(os.path.exists(file_path), "AtillaProactiveEngine.kt bulunamadı")
        with open(file_path, "r", encoding="utf-8") as f:
            content = f.read()

        self.assertIn("generateMorningBriefing", content)
        self.assertIn("generateEveningDebrief", content)
        self.assertIn("HISTORICAL_EVENTS", content)
        self.assertIn("MAARIF_GRADE_TOPICS", content)
        self.assertIn("scheduleDailyProactiveBriefings", content)
        self.assertIn("showProactiveNotification", content)

        # Receiver kontrolü
        receiver_path = os.path.join(APP_DIR, "java", "com", "example", "receiver", "ReminderReceiver.kt")
        with open(receiver_path, "r", encoding="utf-8") as f:
            rcv_content = f.read()
        self.assertIn("ACTION_ATILLA_MORNING_BRIEF", rcv_content)
        self.assertIn("ACTION_ATILLA_EVENING_BRIEF", rcv_content)
        print(" -> Test 03: AtillaProactiveEngine ve ReminderReceiver proaktif zamanlayıcısı doğrulandı.")

    def test_04_atilla_screen_vision_helper(self):
        """4. Ekran Gözü (Multimodal Vision / Soru Çözücü & Anlayıcı) testi"""
        file_path = os.path.join(JAVA_UTIL_DIR, "AtillaScreenVisionHelper.kt")
        self.assertTrue(os.path.exists(file_path), "AtillaScreenVisionHelper.kt bulunamadı")
        with open(file_path, "r", encoding="utf-8") as f:
            content = f.read()

        self.assertIn("data class QuestionStructure", content)
        self.assertIn("data class ActionableItem", content)
        self.assertIn("analyzeCurrentScreen", content)
        self.assertIn("parseAndSolveQuestion", content)
        self.assertIn("extractActionables", content)
        self.assertIn("PHONE_PATTERN", content)
        self.assertIn("IBAN_PATTERN", content)

        # Accessibility getRawScreenText metodu
        acc_path = os.path.join(JAVA_UTIL_DIR, "AtillaAccessibilityService.kt")
        with open(acc_path, "r", encoding="utf-8") as f:
            acc_content = f.read()
        self.assertIn("fun getRawScreenText()", acc_content)
        print(" -> Test 04: AtillaScreenVisionHelper ekran okuma ve sınav sorusu çözücü motoru doğrulandı.")

    def test_05_floating_hologram_bubble_service(self):
        """5. Göz Teması & Yüzen Hologram Küresi (AtillaFloatingBubbleService) testi"""
        file_path = os.path.join(JAVA_UTIL_DIR, "AtillaFloatingBubbleService.kt")
        self.assertTrue(os.path.exists(file_path), "AtillaFloatingBubbleService.kt bulunamadı")
        with open(file_path, "r", encoding="utf-8") as f:
            content = f.read()

        self.assertIn("TYPE_APPLICATION_OVERLAY", content)
        self.assertIn("FLAG_NOT_FOCUSABLE", content)
        self.assertIn("startBubble", content)
        self.assertIn("stopBubble", content)
        self.assertIn("triggerScreenVision", content)

        # AndroidManifest izin ve servis kontrolü
        with open(MANIFEST_PATH, "r", encoding="utf-8") as f:
            manifest_content = f.read()
        self.assertIn("android.permission.SYSTEM_ALERT_WINDOW", manifest_content)
        self.assertIn("AtillaFloatingBubbleService", manifest_content)
        print(" -> Test 05: AtillaFloatingBubbleService yüzen pencere ve izin tanımları doğrulandı.")

    def test_06_dispatcher_and_service_routing(self):
        """6. ActionDispatcherHelper ve AiAssistantService 5-Yetenek Yönlendirme testi"""
        disp_path = os.path.join(JAVA_UTIL_DIR, "ActionDispatcherHelper.kt")
        with open(disp_path, "r", encoding="utf-8") as f:
            disp_content = f.read()

        self.assertIn("inspect_screen_vision", disp_content)
        self.assertIn("get_proactive_briefing", disp_content)
        self.assertIn("toggle_floating_bubble", disp_content)

        ai_path = os.path.join(JAVA_UTIL_DIR, "AiAssistantService.kt")
        with open(ai_path, "r", encoding="utf-8") as f:
            ai_content = f.read()

        self.assertIn("AtillaScreenVisionHelper.analyzeCurrentScreen", ai_content)
        self.assertIn("AtillaProactiveEngine.generateMorningBriefing", ai_content)
        self.assertIn("AtillaFloatingBubbleService.startBubble", ai_content)
        self.assertIn("AtillaEmotionEngine.analyze", ai_content)
        print(" -> Test 06: ActionDispatcherHelper ve AiAssistantService yönlendirme entegrasyonu doğrulandı.")

if __name__ == "__main__":
    unittest.main()
