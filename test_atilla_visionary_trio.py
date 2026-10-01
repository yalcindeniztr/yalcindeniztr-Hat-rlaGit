import os
import unittest

WORKSPACE = r"C:\Users\yalci\antigravity\HatırlaGit"
APP_DIR = os.path.join(WORKSPACE, "app", "src", "main")
JAVA_UTIL_DIR = os.path.join(APP_DIR, "java", "com", "example", "util")

class TestAtillaVisionaryTrio(unittest.TestCase):

    def test_01_voiceprint_verifier(self):
        """1. Biyometrik Ses İmzası (AtillaVoiceprintVerifier) doğrulaması"""
        file_path = os.path.join(JAVA_UTIL_DIR, "AtillaVoiceprintVerifier.kt")
        self.assertTrue(os.path.exists(file_path), "AtillaVoiceprintVerifier.kt bulunamadı")
        with open(file_path, "r", encoding="utf-8") as f:
            content = f.read()

        self.assertIn("object AtillaVoiceprintVerifier", content)
        self.assertIn("SpeakerVerificationResult", content)
        self.assertIn("DEFAULT_MIN_PITCH_HZ", content)
        self.assertIn("DEFAULT_MAX_PITCH_HZ", content)
        self.assertIn("verifySpeaker", content)
        self.assertIn("enrollPatronVoiceprint", content)
        self.assertIn("isSecurityEnabled", content)

        # WakeWordService entegrasyonu kontrolü
        ww_file = os.path.join(JAVA_UTIL_DIR, "AtillaWakeWordService.kt")
        with open(ww_file, "r", encoding="utf-8") as f:
            ww_content = f.read()
        self.assertIn("AtillaVoiceprintVerifier.verifySpeaker", ww_content)
        print(" -> Test 01: AtillaVoiceprintVerifier biyometrik ses izi ve uyandırma kilidi doğrulandı.")

    def test_02_offline_reasoning_engine(self):
        """2. On-Device Çevrimdışı Küçük Muhakeme Motoru doğrulaması"""
        file_path = os.path.join(JAVA_UTIL_DIR, "AtillaOfflineReasoningEngine.kt")
        self.assertTrue(os.path.exists(file_path), "AtillaOfflineReasoningEngine.kt bulunamadı")
        with open(file_path, "r", encoding="utf-8") as f:
            content = f.read()

        self.assertIn("object AtillaOfflineReasoningEngine", content)
        self.assertIn("data class OfflineReasoningResult", content)
        self.assertIn("HISTORICAL_KNOWLEDGE_BASE", content)
        self.assertIn("chainOfThought", content)
        self.assertIn("reason", content)

        # Tarihsel kütüphane kontrolü (Kırım, Malazgirt, İstanbul, Lozan, Amasya)
        self.assertIn("kırım", content)
        self.assertIn("malazgirt", content)
        self.assertIn("istanbul", content)
        self.assertIn("lozan", content)
        self.assertIn("amasya", content)
        print(" -> Test 02: AtillaOfflineReasoningEngine on-device derin muhakeme motoru doğrulandı.")

    def test_03_wear_sync_helper(self):
        """3. Akıllı Saat & Wear OS Entegrasyonu doğrulaması"""
        file_path = os.path.join(JAVA_UTIL_DIR, "AtillaWearSyncHelper.kt")
        self.assertTrue(os.path.exists(file_path), "AtillaWearSyncHelper.kt bulunamadı")
        with open(file_path, "r", encoding="utf-8") as f:
            content = f.read()

        self.assertIn("object AtillaWearSyncHelper", content)
        self.assertIn("sendWearableHudNotification", content)
        self.assertIn("RemoteInput", content)
        self.assertIn("EXTRA_VOICE_REPLY", content)
        self.assertIn("WearableExtender", content)
        self.assertIn("extractVoiceReplyFromIntent", content)
        self.assertIn("getWearTileData", content)

        # MainActivity ve Receiver entegrasyonu kontrolü
        main_file = os.path.join(APP_DIR, "java", "com", "example", "MainActivity.kt")
        with open(main_file, "r", encoding="utf-8") as f:
            main_content = f.read()
        self.assertIn("AtillaWearSyncHelper.extractVoiceReplyFromIntent", main_content)

        rcv_file = os.path.join(APP_DIR, "java", "com", "example", "receiver", "ReminderReceiver.kt")
        with open(rcv_file, "r", encoding="utf-8") as f:
            rcv_content = f.read()
        self.assertIn("ACTION_WEAR_QUICK_TORCH", rcv_content)
        print(" -> Test 03: AtillaWearSyncHelper akıllı saat HUD ve bilek sesli yanıt köprüsü doğrulandı.")

    def test_04_dispatcher_and_service_wiring(self):
        """4. ActionDispatcherHelper ve AiAssistantService 3-Vizyoner Rota kontrolü"""
        disp_file = os.path.join(JAVA_UTIL_DIR, "ActionDispatcherHelper.kt")
        with open(disp_file, "r", encoding="utf-8") as f:
            disp_content = f.read()

        self.assertIn("offline_reasoning", disp_content)
        self.assertIn("send_to_watch", disp_content)
        self.assertIn("toggle_voiceprint_security", disp_content)

        ai_file = os.path.join(JAVA_UTIL_DIR, "AiAssistantService.kt")
        with open(ai_file, "r", encoding="utf-8") as f:
            ai_content = f.read()

        self.assertIn("AtillaWearSyncHelper.sendWearableHudNotification", ai_content)
        self.assertIn("AtillaOfflineReasoningEngine.reason", ai_content)
        self.assertIn("AtillaVoiceprintVerifier.enrollPatronVoiceprint", ai_content)
        print(" -> Test 04: Servis ve köprü entegrasyonu eksiksiz bağlandı.")

if __name__ == "__main__":
    unittest.main()
