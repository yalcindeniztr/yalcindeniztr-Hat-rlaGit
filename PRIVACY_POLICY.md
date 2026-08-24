# HatırlaGit - Gizlilik Politikası ve Kullanım Koşulları (Privacy Policy)

**Son Güncelleme:** 20 Ağustos 2026  
**Uygulama Adı:** HatırlaGit (Hayatın Akışını Yakala)  
**Resmi GitHub Deposu:** [https://github.com/yalcindeniztr/Hat-rlaGit.git](https://github.com/yalcindeniztr/Hat-rlaGit.git)  
**İletişim & Geliştirici:** yalcindeniztr@gmail.com  

---

## 1. Genel Bakış ve Gizlilik Taahhüdü

**HatırlaGit**, kullanıcıların günlük randevularını, sağlık/ilaç takiplerini, fatura ve resmi daire işlemlerini güvenli, pratik ve çevrimdışı bir şekilde yönetmeleri amacıyla geliştirilmiş **yerel (offline-first) bir kişisel yaşam asistanıdır**.

Gizliliğiniz ve veri güvenliğiniz bizim için en temel ilkedir. **HatırlaGit, kullanıcı verilerini herhangi bir uzak sunucuya, üçüncü taraf veri merkezlerine veya reklam ağlarına aktarmaz, saklamaz ve işlemez.** Tüm verileriniz yalnızca sizin fiziksel cihazınızda yerel olarak saklanır.

---

## 2. Toplanan Veriler ve Saklama Yöntemi

### 2.1. Yerel Veri Depolama (Room SQLite & DataStore)
- **Randevu ve Hatırlatıcı Bilgileri:** Randevu başlığı, kategori, tarih, saat, önem derecesi ve kullanıcı tarafından girilen özel notlar.
- **Araç Park Yeri Konumu:** "Arabam Nerede?" özelliği kullanıldığında kaydedilen son park konumunuz (Enlem/Boylam).
- **Kullanıcı Tercihleri & Profil:** Takma ad (Nick), şifreleme PIN kodu, profil fotoğrafı yolu ve seçilen yazı boyutu (Normal / Büyük / Ekstra Büyük Yaşlı Modu).

### 2.2. Uçtan Uca Donanım Destekli AES-256 Şifreleme
- Kullanıcı takma adı, güvenlik PIN kodu ve kritik notlar, Android Keystore sistemi kullanılarak **AES-256-GCM** standardı ile yerel olarak şifrelenir.
- Cihazınıza fiziksel olarak erişilse dahi, şifrelenmiş veriler anahtar olmadan okunamaz.

---

## 3. İstenen İzinler ve Kullanım Amaçları

HatırlaGit, yalnızca uygulamanın temel fonksiyonlarını yerine getirebilmesi için gerekli olan minimum izinleri talep eder:

1. **`POST_NOTIFICATIONS` (Bildirim Gönderme İzni):**  
   Yaklaşan randevularınızı, ilaç vakitlerinizi ve takvim hatırlatmalarınızı ekranınızda bildirim olarak gösterebilmek için kullanılır.
2. **`SCHEDULE_EXACT_ALARM` & `USE_EXACT_ALARM` (Tam Zamanlı Alarm İzni):**  
   Randevu saatiniz geldiğinde, sistem pil tasarrufu kısıtlamalarına takılmadan tam vaktinde alarm ve sesli uyarı çalabilmek için gereklidir.
3. **`RECEIVE_BOOT_COMPLETED` (Cihaz Başlatma İzni):**  
   Telefonunuzu veya tabletinizi kapatıp açtığınızda, kaybolmaması gereken aktif randevu alarmlarınızı otomatik olarak arka planda yeniden kurar.
4. **Fotoğraf Seçici (Photo Picker) / Galeri Erişimi:**  
   Kullanıcının profil resmi belirleyebilmesi için yalnızca kullanıcının seçtiği görsele yerel olarak erişir. Fotoğraflarınız asla internete yüklenmez.
5. **Konum İzinleri (`ACCESS_FINE_LOCATION` & `ACCESS_COARSE_LOCATION`):**  
   "Arabam Nerede?" ve "Ezan Vakitleri" (otomatik bulma) özelliklerinde bulunduğunuz konumu (GPS) tespit etmek için kullanılır. Konum verileriniz hiçbir sunucuya gönderilmez, tamamen cihazınızda yerel olarak saklanır.

---

## 4. Üçüncü Taraf Hizmetleri ve Veri Paylaşımı

- **Sunucusuz Mimari:** HatırlaGit herhangi bir sunucu veritabanı veya bulut depolama kullanmaz.
- **Reklamsız ve Takipsiz:** Uygulama içerisinde hiçbir analitik takip aracı (Google Analytics, Firebase Analytics vb.) veya reklam kütüphanesi (AdMob vb.) yer almaz.
- **Sıfır Veri Satışı:** Verileriniz asla satılmaz, kiralanmaz ve ticari amaçla işlenmez.

---

## 5. Yaşlı Dostu Tasarım ve Erişilebilirlik

HatırlaGit, ileri yaştaki bireylerin ve görme güçlüğü çeken kullanıcıların teknolojiden rahatça faydalanabilmesi için özel olarak optimize edilmiştir:
- **Ayarlanabilir Büyük Yazı Modu:** Standart, Büyük (%122) ve Ekstra Büyük (%142) yazı ölçekleme seçeneği.
- **Kaydırmasız 2 Sütunlu Kategori Kutucukları:** Sağa-sola kaydırma zorunluluğunu ortadan kaldıran, tek bakışta görülebilen ve rahat dokunulabilen 2 sütunlu kare seçim alanları.
- **Yüksek Kontrast & 3D Kabartma Butonlar:** Kolay ayırt edilebilir renkler, derinlikli butonlar ve net tipografi.

---

## 6. Veri Kontrolü ve Silme Hakkı (KVKK & GDPR)

Kullanıcı kendi verileri üzerinde tam kontrole sahiptir:
- **CSV Formatında Dışa Aktarma:** Profil sayfasındaki *"Tüm Verileri Dışa Aktar"* butonunu kullanarak randevularınızı dilediğiniz an Excel/CSV dosyası olarak yedekleyebilirsiniz.
- **Tek Tıkla Kalıcı Silme:** Profil ekranındaki *"Tüm Verileri Güvenle Sıfırla"* seçeneği ile veritabanındaki tüm kayıtları, şifreleri ve kurulmuş alarmları anında geri dönüşsüz olarak silebilirsiniz.
- Uygulamayı cihazınızdan kaldırdığınızda, cihaza ait tüm yerel veriler otomatik olarak tamamen silinir.

---

## 7. Sorumluluk Reddi (Disclaimer)

HatırlaGit kişisel bir ajanda ve zaman hatırlatma aracıdır. Sağlık ve ilaç kategorileri bilgilendirme amaçlı olup, tıbbi teşhis, tedavi veya profesyonel hekim tavsiyesi niteliği taşımaz. Kullanıcıların kritik sağlık ve yasal işlemlerinde birincil resmi kanallara ve uzmanlara başvurmaları önerilir.

---

## 8. İletişim

Gizlilik politikamız veya HatırlaGit uygulaması ile ilgili her türlü soru, öneri ve geri bildirimleriniz için geliştirici ile iletişime geçebilirsiniz:

- **E-Posta:** [yalcindeniztr@gmail.com](mailto:yalcindeniztr@gmail.com)  
- **GitHub:** [https://github.com/yalcindeniztr/Hat-rlaGit.git](https://github.com/yalcindeniztr/Hat-rlaGit.git)  

---

# English Summary: Privacy Policy

**HatırlaGit** is an offline-first, privacy-focused life and appointment assistant Android application.
- **100% Offline & Local:** All appointments, alarms, notes, and profile settings are stored exclusively on your device using encrypted Room SQLite and DataStore.
- **No Remote Tracking:** No user data is sent to external servers, analytics trackers, or third-party advertisers.
- **Hardware AES-256 Encryption:** User nick and PIN codes are encrypted via Android Keystore.
- **Permissions Used:** Notification and Exact Alarm permissions are solely used for triggering timely reminders.
- **User Control:** Users can export data via CSV or wipe all data permanently at any time.
- **Open Source:** Available at [https://github.com/yalcindeniztr/Hat-rlaGit.git](https://github.com/yalcindeniztr/Hat-rlaGit.git).
