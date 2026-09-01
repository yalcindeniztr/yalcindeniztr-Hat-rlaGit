# HatırlaGit - Gizlilik Politikası ve Kullanım Koşulları (Privacy Policy)

**Son Güncelleme:** 1 Eylül 2026  
**Uygulama Sürümü:** 1.0.6 (v1.0.6)  
**Uygulama Adı:** HatırlaGit (Kişisel Yaşam & Yapay Zeka Asistanı)  
**Resmi GitHub Deposu:** [https://github.com/yalcindeniztr/yalcindeniztr-Hat-rlaGit.git](https://github.com/yalcindeniztr/yalcindeniztr-Hat-rlaGit.git)  
**İletişim & Geliştirici:** yalcindeniztr@gmail.com  

---

## 1. Genel Bakış ve Gizlilik Taahhüdü

**HatırlaGit**, kullanıcıların randevularını, fatura ve resmi işlemlerini, park yerlerini, ezan vakitlerini ve günlük planlarını güvenli, pratik ve çevrimdışı öncelikli (**offline-first**) bir şekilde yönetmeleri amacıyla geliştirilmiş **akıllı bir kişisel yaşam asistanıdır**.

Gizliliğiniz ve veri egemenliğiniz bizim için en temel ilkedir. **HatırlaGit, kullanıcı verilerini herhangi bir uzak sunucuya, merkezi veritabanına veya reklam ağlarına aktarmaz, satmaz ve işlemez.** Tüm verileriniz yalnızca sizin fiziksel cihazınızda yerel olarak saklanır.

---

## 2. Toplanan Veriler ve Güvenlik Mimarisi

### 2.1. Yerel Veri Depolama (Room SQLite & Encrypted DataStore)
- **Randevu ve Hatırlatıcı Bilgileri:** Randevu başlığı, kategori, tarih, saat, önem derecesi ve kullanıcı tarafından girilen özel notlar.
- **Yapay Zeka Asistan Kütüphanesi (Local Knowledge Base):** Kullanıcının asistana öğrettiği özel notlar, tercihler ve kullanıcı adı yalnızca cihaz içi SQLite veritabanında (`ai_knowledge`) tutulur.
- **Araç Park Yeri & Lokasyonlar:** "Arabam Nerede?" ve "Kayıtlı Konumlar" özellikleri ile kaydedilen enlem/boylam koordinatları.
- **Kullanıcı Tercihleri:** Takma ad, PIN kodu, profil fotoğrafı yolu ve seçilen yazı boyutu.

### 2.2. Donanım Korumalı AES-256-GCM Şifreleme
- Kullanıcı takma adı, güvenlik PIN kodu, özel notlar ve kullanıcı tarafından girilen **Yapay Zeka API Anahtarları (Bring Your Own Key - BYOK)**, Android Keystore donanım güvenlik modülü kullanılarak **AES-256-GCM** standardı ile yerel olarak şifrelenir.
- Cihazınıza fiziksel olarak erişilse dahi, donanım anahtarı olmadan verileriniz çözülemez ve okunamaz.

---

## 3. Yapay Zeka (AI) ve API Güvenliği Politikası

1. **Kendi API Anahtarını Kullanma (BYOK):**  
   Kullanıcı dilerse Google Gemini veya OpenAI API anahtarını Profil ekranından ekleyebilir. Bu anahtar **asla geliştiricinin veya üçüncü tarafların sunucularına gönderilmez**, yalnızca cihazda şifreli tutulur ve doğrudan ilgili AI API uç noktasına şifreli TLS 1.3 protokolü ile bağlanır.
2. **Çevrimdışı Yapay Zeka Motoru:**  
   API anahtarı girilmediğinde, HatırlaGit %100 çevrimdışı yerel akıllı motoruyla çalışmaya devam eder.
3. **Veri Eğitimi Yok:**  
   Kullanıcı sohbetleri ve randevuları hiçbir genel yapay zeka modelinin eğitim havuzuna aktarılmaz.

---

## 4. İstenen İzinler ve Kullanım Amaçları

HatırlaGit, yalnızca uygulamanın temel fonksiyonlarını yerine getirebilmesi için gerekli olan minimum izinleri talep eder:

1. **`POST_NOTIFICATIONS` (Bildirim İzni):**  
   Yaklaşan randevularınızı, ilaç vakitlerinizi ve takvim hatırlatmalarınızı ekranınızda bildirim olarak gösterebilmek için kullanılır.
2. **`SCHEDULE_EXACT_ALARM` & `USE_EXACT_ALARM` (Tam Zamanlı Alarm İzni):**  
   Randevu saatiniz geldiğinde, sistem pil tasarrufu kısıtlamalarına takılmadan tam vaktinde alarm ve sesli uyarı çalabilmek için gereklidir.
3. **`RECORD_AUDIO` (Mikrofon İzni):**  
   Sesli not alma, sesli randevu oluşturma ve Yapay Zeka Asistanı ile sesli konuşma amacıyla kullanılır. Ses kayıtları cihazda saklanmaz, anlık olarak metne dönüştürülür.
4. **`CAMERA` (Kamera İzni - Opsiyonel):**  
   Google ML Kit ile fatura, fiş veya randevu belgelerinin fotoğrafını çekerek son ödeme tarihini ve tutarını otomatik okumak (OCR) için kullanılır. Fotoğraflar sunucuya yüklenmez, cihaz üzerinde anlık işlenir.
5. **`ACCESS_FINE_LOCATION` & `ACCESS_COARSE_LOCATION` (Konum İzni):**  
   "Arabam Nerede?", "Ezan Vakitleri" ve asistana sorulan *"En yakın nöbetçi eczane / hastane / otopark nerede?"* sorgularında en yakın 3 önemli yeri bulmak ve Google Haritalar yol tarifi sunmak için kullanılır. Konum geçmişi tutulmaz.
6. **`RECEIVE_BOOT_COMPLETED` (Cihaz Başlatma İzni):**  
   Cihazınız yeniden başlatıldığında aktif randevu alarmlarınızı arka planda güvenle yeniden kurar.

---

## 5. Üçüncü Taraf Hizmetleri ve Veri Paylaşımı

- **Sunucusuz ve Sıfır Takip:** HatırlaGit kullanıcı verilerini toplayan hiçbir analitik takip aracı (Firebase Analytics, Mixpanel vb.) barındırmaz.
- **Google Haritalar & Telefon Araması:** En yakın hastane veya nöbetçi eczane için yol tarifi istendiğinde, işlem kullanıcının onayıyla cihazın yüklü Google Haritalar veya Telefon uygulamasına devredilir.
- **Sıfır Veri Satışı:** Verileriniz asla satılmaz, kiralanmaz ve ticari amaçla işlenmez.

---

## 6. Veri Kontrolü, Yedekleme ve Silme Hakkı (KVKK & GDPR)

Kullanıcı kendi verileri üzerinde %100 tam tasarruf ve denetim hakkına sahiptir:
- **PDF ve Excel / CSV Olarak Dışa Aktarma:** Profil sekmesinden tüm randevularınızı ve bütçe dökümünüzü tek tıkla resmi PDF veya Excel tablosu olarak cihazınıza kaydedebilir veya paylaşabilirsiniz.
- **Güvenli Çevrimdışı QR Kod ile Cihaz Aktarımı:** İnternetsiz olarak eski telefondan yeni telefona verilerinizi QR kod okutarak kayıpsız aktarabilirsiniz.
- **Tek Tıkla Kalıcı Silme:** Profil ekranındaki *"Tüm Verileri Sil"* seçeneği ile veritabanındaki tüm kayıtları, yapay zeka hafızasını, şifrelenmiş anahtarları ve kurulmuş alarmları anında kalıcı olarak silebilirsiniz.
- Uygulama cihazdan kaldırıldığında, Android işletim sistemi tarafından uygulamaya ait tüm yerel veriler otomatik ve kalıcı olarak temizlenir.

---

## 7. Sorumluluk Reddi (Disclaimer)

HatırlaGit kişisel bir ajanda, hatırlatma ve yapay zeka asistanı aracıdır. Sağlık, nöbetçi eczane ve ilaç kategorileri bilgilendirme ve hatırlatma amaçlı olup, tıbbi teşhis, tedavi veya profesyonel hekim tavsiyesi niteliği taşımaz. Resmi ve yasal işlemlerde kamu kurumlarının birincil resmi kanalları (.gov.tr) esas alınmalıdır.

---

## 8. İletişim

Gizlilik politikamız veya HatırlaGit uygulaması ile ilgili her türlü soru, öneri ve talepleriniz için:

- **Geliştirici:** Yalçın Deniz  
- **E-Posta:** [yalcindeniztr@gmail.com](mailto:yalcindeniztr@gmail.com)  
- **GitHub:** [https://github.com/yalcindeniztr/yalcindeniztr-Hat-rlaGit.git](https://github.com/yalcindeniztr/yalcindeniztr-Hat-rlaGit.git)  

---

# English Summary: Privacy Policy

**HatırlaGit** is a 100% offline-first, privacy-focused life and AI assistant Android application.
- **Zero Server Telemetry:** All reminders, AI local knowledge, audio transcriptions, and locations are stored exclusively on your device using encrypted Room SQLite and DataStore.
- **Hardware AES-256-GCM Encryption:** User nick, PIN codes, and Bring-Your-Own-Key (BYOK) AI API keys are hardware-encrypted via Android Keystore.
- **Camera & Microphone Permissions:** Used exclusively for on-device OCR bill parsing (Google ML Kit) and on-device speech-to-text.
- **Location Services:** Used locally for prayer times, car parking spot, and nearby emergency places (duty pharmacies, hospitals, parking lots).
- **User Ownership:** Export data anytime via PDF/CSV, transfer via secure offline QR code, or erase all data permanently with one click.
- **Repository:** [https://github.com/yalcindeniztr/yalcindeniztr-Hat-rlaGit.git](https://github.com/yalcindeniztr/yalcindeniztr-Hat-rlaGit.git).
