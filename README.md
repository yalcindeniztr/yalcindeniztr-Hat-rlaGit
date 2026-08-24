# HatırlaGit - Hayatın Akışını Yakala ⏰📌

> **Modern, Yaşlı Dostu, Çevrimdışı ve AES-256 Şifrelemeli Randevu & Yaşam Asistanı Android Uygulaması**

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-purple.svg)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-brightgreen.svg)](https://developer.android.com/jetpack/compose)
[![Room Database](https://img.shields.io/badge/Room-SQLite%20Encrypted-blue.svg)](https://developer.android.com/training/data-storage/room)
[![License](https://img.shields.io/badge/License-MIT-orange.svg)](LICENSE)
[![GitHub](https://img.shields.io/badge/GitHub-yalcindeniztr%2FHat--rlaGit-black.svg)](https://github.com/yalcindeniztr/Hat-rlaGit.git)

---

## 🌟 Öne Çıkan Özellikler

- 🎙️ **Sesli Yazım & Konuşarak Not Alma (Speech-to-Text):**  
  Yaşlılar ve hızlı not almak isteyenler için tek dokunuşla çalışan Türkçe sesli tanıma entegrasyonu. Arama, not alma ve hatırlatıcı alanlarını sadece konuşarak zahmetsizce doldurma imkanı.
- 📝 **Ana Ekranda "Hızlı Not Al" Kartı:**  
  Giriş sayfasında yer alan tek dokunuşlu hızlı not paneli sayesinde saniyeler içinde not kaydedebilir, önceden tanımlı zaman butonlarıyla (+30 Dk, +1 Saat, Yarın Sabah) otomatik alarm kurabilirsiniz.
- ➕ **Dinamik Özel Kategori Oluşturma & Yönetimi:**  
  Kullanıcılar diledikleri sayıda yeni kategori oluşturabilir, özel renk paleti, 15+ tematik ikon ve kategoriye özel bilgi alanları tanımlayabilir.
- 👵 **Yaşlı Dostu Erişilebilirlik Modu (Büyük & Ekstra Büyük Yazı Ayarı):**  
  Tek tıkla tüm uygulama tipografisini %122 veya %142 oranında büyüterek ileri yaştaki bireyler ve görme güçlüğü çekenler için kristal netliğinde okunabilirlik sağlar.
- 📱 **2 Sütunlu Görünür Kategori Kutucukları:**  
  Randevular ekranında sağa-sola kaydırma karmaşası olmadan, tüm 10 kategori kutucuğu 2 sütun halinde görünür ve geniş dokunma alanlarıyla kolayca seçilebilir.
- ✨ **3D Kabartmalı Canlı Tasarım:**  
  Derinlikli ışık/gölge katmanları, 3D yükseltilmiş dairesel orta ekleme butonu (+), canlı istatistik kartları ve yüksek kontrastlı renk paleti.
- 🔐 **Yerel ve AES-256 Şifreli Güvenlik:**  
  Kullanıcı takma adı, güvenlik PIN kodu ve özel randevu notları Android Keystore destekli AES-256-GCM ile donanım düzeyinde şifrelenir.
- ⏰ **Hassas Alarm Sistemi (Exact AlarmManager & Boot Recovery):**  
  Cihaz yeniden başlatılsa dahi silinmeyen, tam zamanında çalan yüksek öncelikli sesli ve titreşimli randevu bildirimleri.
- 📂 **CSV Dışa Aktarma & Tek Tıkla Sıfırlama:**  
  Randevu listesini Excel uyumlu CSV dosyası olarak dışa aktarma ve tek dokunuşla tüm verileri güvenle sıfırlama imkanı.
- 👤 **Kişiselleştirilebilir Profil:**  
  Fotoğraf galerisinden profil resmi yükleme, kullanıcı adı düzenleme ve anlık kategori istatistikleri.
- 🚀 **Akıllı Google Play Güncelleme Denetleyicisi (Yalnızca Yeni Sürüm Çıktığında):**  
  Her açılışta gereksiz ve sahte güncelleme uyarıları vermez. Sadece gerçekten yeni bir sürüm yayınlandığında şık bir bildirim penceresi sunulur; kullanıcı "Daha Sonra" dediğinde aynı sürüm için tekrar rahatsız etmez. Profil ekranından istendiğinde anlık denetleme yapılabilir.
- ⭐ **Haftalık Akıllı Değerlendirme & Puanlama:**  
  Kullanıcı deneyimini bölmeden en fazla haftada 1 kez Google Play 5 yıldız değerlendirme penceresi gösterilir. Kullanıcı oy verdiğinde sistem durumu DataStore'a kaydeder ve uyarı kalıcı olarak bir daha gösterilmez.
- 📢 **Esnek Reklam Alanları (Header & Geçiş / Interstitial):**  
  Üst sponsorlu banner ve eylem sonrası geri sayımlı geçiş reklamı slotları önceden entegre edilmiş olup, ileride Google AdMob veya aracı reklam ağları tek tıkla aktif edilebilir.

---

## 🗂️ 10 Tematik Randevu Kategorisi

1. 🏥 **Sağlık & Klinik:** Doktor randevuları, tahlil günleri, aşı ve kontrol takvimleri.
2. 💊 **İlaç & Tedavi:** Düzenli ilaç saatleri, reçete yenileme ve pansuman takibi.
3. ⚖️ **Hukuk & Resmi:** Duruşma günleri, noter, nüfus müdürlüğü ve tapu randevuları.
4. 🏃 **Günlük Yaşam:** Egzersiz, yürüyüş, fatura son ödeme ve su hatırlatmaları.
5. 👥 **Sosyal & İlişkiler:** Doğum günleri, buluşmalar, aile ziyaretleri ve yıldönümleri.
6. 🛍️ **Finans & Alışveriş:** Kredi kartı ödemeleri, pazar alışverişleri ve bütçe planları.
7. 📍 **Konum & Navigasyon:** Şehir dışı seyahatler, bilet saatleri ve adres hatırlatıcıları.
8. 💼 **Kariyer & Projeler:** İş toplantıları, teslim tarihleri ve mülakatlar.
9. 🚗 **Araç & Teknoloji:** Muayene, kasko/sigorta, periyodik bakım ve servis randevuları.
10. 🔔 **Genel Hatırlatıcı:** Tüm diğer serbest notlar ve hızlı alarmlar.

---

## 🏗️ Teknoloji Yığını ve Mimari

- **Dil:** %100 Modern Kotlin
- **Kullanıcı Arayüzü:** Jetpack Compose + Material Design 3 (M3)
- **Durum Yönetimi:** MVVM (Model-View-ViewModel), StateFlow, Coroutines & Flow
- **Yerel Veritabanı:** Room Database (SQLite Entity & DAO)
- **Tercih Deposu:** Jetpack DataStore Preferences (Şifrelenmiş)
- **Alarm & Bildirim:** Android AlarmManager (`SCHEDULE_EXACT_ALARM`), NotificationManager, BroadcastReceiver (`BootReceiver`, `AlarmReceiver`)
- **Görsel Yükleme:** Coil Compose (Photo Picker entegrasyonu)

---

## 🚀 Kurulum ve Çalıştırma

1. Projeyi klonlayın:
```bash
git clone https://github.com/yalcindeniztr/Hat-rlaGit.git
cd Hat-rlaGit
```

2. Android Studio (Ladybug veya üstü) ile projeyi açın.
3. Gradle senkronizasyonunu tamamlayın (`Sync Project with Gradle Files`).
4. Android 8.0 (API 26) veya üzeri bir cihazda ya da emülatörde çalıştırın:
```bash
./gradlew installDebug
```

---

## 🔒 Gizlilik Politikası

HatırlaGit, **%100 Çevrimdışı (Offline-First)** prensibiyle çalışır. Kişisel verileriniz hiçbir harici sunucuya veya üçüncü tarafa iletilmez.  
Detaylı gizlilik sözleşmesi için [PRIVACY_POLICY.md](PRIVACY_POLICY.md) dosyasını inceleyebilirsiniz.

---

## 📬 İletişim & Katkı

- **Geliştirici:** Yalçın Deniz
- **E-Posta:** [yalcindeniztr@gmail.com](mailto:yalcindeniztr@gmail.com)
- **GitHub Deposu:** [https://github.com/yalcindeniztr/Hat-rlaGit.git](https://github.com/yalcindeniztr/Hat-rlaGit.git)
