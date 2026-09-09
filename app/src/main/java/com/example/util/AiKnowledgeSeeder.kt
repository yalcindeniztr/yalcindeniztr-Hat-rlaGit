package com.example.util

import android.content.Context
import com.example.data.AiKnowledgeEntity
import com.example.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AiKnowledgeSeeder {

    suspend fun seedIfNeeded(context: Context) = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(context)
            val allList = db.aiKnowledgeDao().getAllKnowledgeList()

            // 1. MEB Maarif Modeli
            if (allList.none { it.title.contains("Maarif", ignoreCase = true) }) {
                db.aiKnowledgeDao().insertKnowledge(
                    AiKnowledgeEntity(
                        title = "MEB Türkiye Yüzyılı Maarif Modeli (Eğitim Sistemi)",
                        content = """
Türkiye Yüzyılı Maarif Modeli, ezbere dayalı anlayış yerine beceri temelli ve değer odaklı bütüncül bir eğitim felsefesini esas alır.
Temel Esasları:
1. Beceriler Çerçevesi: Kavramsal beceriler, alan becerileri (disiplinlere özgü) ve sosyal-duygusal öğrenme becerileri.
2. Erdem-Değer-Eylem Modeli: Adalet, merhamet, dürüstlük, sorumluluk, vatanseverlik ve çalışkanlık gibi değerlerin derslerin içine organik olarak entegre edilmesi.
3. Okuryazarlık Türleri: Bilgi okuryazarlığı, dijital okuryazarlık, finansal okuryazarlık ve görsel okuryazarlık.
4. Ölçme ve Değerlendirme: Sonuç odaklı çoktan seçmeli ezber yerine, süreç odaklı, öğrencinin gelişimini izleyen biçimlendirici değerlendirme araçları ve açık uçlu sınav senaryoları esastır.
5. Farklılaştırma: Her öğrencinin hızına uygun zenginleştirme (ileri düzey) ve destekleme (pekiştirme) uygulamaları yer alır.
                        """.trimIndent(),
                        category = "OFFICIAL_LAW",
                        isOfficialVerified = true,
                        source = "T.C. Millî Eğitim Bakanlığı",
                        createdAt = System.currentTimeMillis()
                    )
                )
            }

            // 2. OGM Materyal ve MEB Dijital Eğitim Yenilikleri
            if (allList.none { it.title.contains("OGM Materyal", ignoreCase = true) }) {
                db.aiKnowledgeDao().insertKnowledge(
                    AiKnowledgeEntity(
                        title = "MEB OGM Materyal ve Dijital Eğitim Platformu",
                        content = """
MEB Ortaöğretim Genel Müdürlüğü (OGM Materyal) ve Dijital Eğitim Yenilikleri:
1. İnteraktif Ders Kitapları: 9, 10, 11 ve 12. sınıf tüm ders kitaplarının video, 3D animasyon ve sesli anlatımlarla zenginleştirilmiş dijital sürümleri.
2. Soru Bankası ve Denemeler: MEB Türkiye Yüzyılı Maarif Modeli beceri temelli soruları, kazanım kavrama testleri ve YKS (TYT-AYT) kamp soruları.
3. 3D Deney ve Modeller: Biyoloji, fizik ve kimya deneylerinin simülasyon ortamında yapılabilmesi; tarih ve coğrafya derslerinde sanal müze ve harita turları.
4. Çözüm Havuzu ve Etkinlikler: Ders kitaplarındaki hazırlık, ünite sonu ve sayfa içi etkinliklerinin örnek analitik çözümleri.
5. EBA ve ÖBA Entegrasyonu: Yapay zeka destekli bireysel çalışma planları ve öğretmen mesleki gelişim seminerleri.
                        """.trimIndent(),
                        category = "EDTECH_RESOURCES",
                        isOfficialVerified = true,
                        source = "MEB OGM Materyal Portalı (ogmmateryal.eba.gov.tr)",
                        createdAt = System.currentTimeMillis()
                    )
                )
            }

            // 3. MEB Sınıf Geçme Yönetmeliği
            if (allList.none { it.title.contains("Sınıf Geçme", ignoreCase = true) }) {
                db.aiKnowledgeDao().insertKnowledge(
                    AiKnowledgeEntity(
                        title = "MEB Ortaöğretim Sınıf Geçme ve Sınav Yönetmeliği",
                        content = """
MEB Ortaöğretim Kurumları Yönetmeliği (Resmî Gazete) Hükümleri:
1. Doğrudan Sınıf Geçme: Ders yılı sonunda tüm derslerden başarılı olan veya başarısız dersi bulunsa bile yıl sonu başarı puanı en az 50 olan öğrenciler doğrudan sınıf geçer.
2. Baraj Dersi Kuralı: Türk Dili ve Edebiyatı dersi baraj derstir. Yıl sonu başarı puanı 50 veya üzerinde olsa dahi bu dersten başarısız olan öğrenci sorumlu olarak üst sınıfa geçer.
3. Sorumlu Geçme Sınırı: Bir üst sınıfa en fazla 3 dersten sorumlu olarak geçilebilir. Alt sınıflar da dahil olmak üzere toplam sorumlu ders sayısı en fazla 6 olabilir. 6'dan fazla sorumlu dersi biriken öğrenci sınıf tekrarı yapar.
4. Devamsızlık Sınırları: Özürsüz devamsızlık süresi en fazla 10 gündür. Toplam devamsızlık süresi (özürlü + özürsüz) 30 günü aşan öğrenciler sınıf tekrarına kalır.
5. Sınav Uygulamaları: Sınavlar yazılı ve uygulamalı olarak ortak açık uçlu metinlerle yapılır.
                        """.trimIndent(),
                        category = "OFFICIAL_LAW",
                        isOfficialVerified = true,
                        source = "T.C. Resmî Gazete / MEB",
                        createdAt = System.currentTimeMillis()
                    )
                )
            }

            // 4. Lise Tarih Dersi Maarif Müfredatı
            if (allList.none { it.title.contains("Lise Tarih Müfredatı", ignoreCase = true) }) {
                db.aiKnowledgeDao().insertKnowledge(
                    AiKnowledgeEntity(
                        title = "Lise Tarih (9, 10, 11, 12) Maarif Müfredatı ve Üniteleri",
                        content = """
MEB Yeni Maarif Modeli Lise Tarih Dersi Kitapları ve Ünite Haritası:
• 9. Sınıf: Geçmişin İnşası ve Tarih Yazımı, İlk ve Orta Çağlarda Türk Dünyası, İslam Medeniyetinin Doğuşu, Türklerin İslamiyeti Kabulü, Türkiye Tarihi (Selçuklular ve Ahilik).
• 10. Sınıf: Yerleşme ve Devletleşme Sürecinde Selçuklu Türkiyesi, Beylikten Devlete Osmanlı Siyaseti (1302-1453), İskân ve İstimâlet, Askerler ve Savaşçılar, Osmanlı Medeniyeti, Dünya Gücü Osmanlı.
• 11. Sınıf: Değişen Dünya Dengeleri Karşısında Osmanlı (1595-1774), Değişim Çağında Avrupa ve Osmanlı, Denge Stratejisi (1774-1914), Devrimler Çağında Devlet-Toplum, Sermaye ve Emek.
• 12. Sınıf: 20. Yüzyıl Başlarında Osmanlı ve Dünya (I. Dünya Savaşı), Millî Mücadele (Kongreler, TBMM, Cepheler, Lozan), Atatürkçülük ve Türk İnkılabı, İki Savaş Arası Dönem.
                        """.trimIndent(),
                        category = "MAARIF_CURRICULUM",
                        isOfficialVerified = true,
                        source = "MEB Talim ve Terbiye Kurulu",
                        createdAt = System.currentTimeMillis()
                    )
                )
            }

            // 5. Lise Türk Dili ve Edebiyatı Maarif Müfredatı
            if (allList.none { it.title.contains("Lise Edebiyat Müfredatı", ignoreCase = true) }) {
                db.aiKnowledgeDao().insertKnowledge(
                    AiKnowledgeEntity(
                        title = "Lise Türk Dili ve Edebiyatı (9, 10, 11, 12) Maarif Müfredatı",
                        content = """
MEB Yeni Maarif Modeli Lise Türk Dili ve Edebiyatı Ders Kitapları ve Ünite Haritası:
• 9. Sınıf: Edebiyata Giriş, Hikâye (Olay ve Durum Hikâyesi), Şiir (Ölçü, Kafiye, Redif, Âhenk), Masal/Fabl, Roman, Tiyatro, Biyografi.
• 10. Sınıf: Tarih ve Din İlişkisi, Dede Korkut ve Halk Hikâyeleri, İslamiyet Öncesi ve Geçiş Dönemi Şiiri (Kutadgu Bilig, Dîvânu Lugâti't-Türk), Destan ve Efsaneler, İlk Yerli Romanlar.
• 11. Sınıf: Edebiyat ve Toplum, Cumhuriyet Dönemi Hikâyeciliği, Saf Şiir ve Millî Edebiyat zevki, Makale, Fıkra, Sohbet, Roman (Milli Edebiyat ve Cumhuriyet).
• 12. Sınıf: Felsefe ve Psikoloji İlişkisi, Küçürek Hikâye, Cumhuriyet Dönemi Şiir Akımları (İkinci Yeni, Toplumcu Gerçekçiler), Modernist ve Postmodernist Roman, Nutuk (Söylev).
                        """.trimIndent(),
                        category = "MAARIF_CURRICULUM",
                        isOfficialVerified = true,
                        source = "MEB Talim ve Terbiye Kurulu",
                        createdAt = System.currentTimeMillis()
                    )
                )
            }

            // 6. ŞÖK Mevzuatı
            if (allList.none { it.title.contains("ŞÖK Mevzuatı", ignoreCase = true) }) {
                db.aiKnowledgeDao().insertKnowledge(
                    AiKnowledgeEntity(
                        title = "MEB Şube Öğretmenler Kurulu (ŞÖK) Mevzuatı ve Gündemi",
                        content = """
MEB Ortaöğretim Kurumları Yönetmeliği Madde 36, 57 ve 58 uyarınca ŞÖK Esasları:
1. Kurul Üyeleri: Şube rehber öğretmeni başkanlığında, şubede derse giren tüm branş öğretmenleri ve rehber öğretmen.
2. Toplanma Zamanları: 1. Dönem başı (Ekim), 2. Dönem başı (Şubat) ve Ders yılı sonu (Haziran).
3. Resmi 8 Gündem Maddesi: Açılış ve yoklama; önceki kararların değerlendirilmesi; akademik başarı (Edebiyat 70 barajı); devamsızlık takibi (10 gün özürsüz, 30 gün toplam); rehberlik ve BEP; Maarif değerler eğitimi; başarıyı artırıcı tedbirler; dilek ve kapanış.
                        """.trimIndent(),
                        category = "OFFICIAL_LAW",
                        isOfficialVerified = true,
                        source = "MEB Ortaöğretim Kurumları Yönetmeliği",
                        createdAt = System.currentTimeMillis()
                    )
                )
            }

            // 7. Günlük TV Rehberi
            if (allList.none { it.title.contains("Günlük TV Rehberi", ignoreCase = true) }) {
                db.aiKnowledgeDao().insertKnowledge(
                    AiKnowledgeEntity(
                        title = "Türkiye Günlük TV Dizileri ve Yayın Akışı Rehberi",
                        content = """
Türkiye Ulusal TV Kanalları Prime-Time (Saat 20:00) Günlük Dizi Akışı:
• Pazartesi: Kızıl Goncalar (NOW), Kudüs Fatihi Selahaddin Eyyubi (TRT 1), MasterChef (TV8).
• Salı: Mehmed: Fetihler Sultanı (TRT 1), Bahar (Show TV), Gizli Bahçe (NOW).
• Çarşamba: Kuruluş Osman (ATV), Sandık Kokusu (Show TV), Sahipsizler (Star TV).
• Perşembe: Hudutsuz Sevda (NOW), İnci Taneleri (Kanal D), Siyah Kalp (Show TV).
• Cuma: Kızılcık Şerbeti (Show TV), Yalı Çapkını (Star TV), Arka Sokaklar (Kanal D), Kara Ağaç Destanı (TRT 1).
• Cumartesi: Gönül Dağı (TRT 1), Kardeşlerim (ATV), Yabani (NOW).
• Pazar: Teşkilat (TRT 1), Deha (Show TV), Kirli Sepeti (NOW).
                        """.trimIndent(),
                        category = "TV_GUIDE",
                        isOfficialVerified = true,
                        source = "RTÜK & Ulusal TV Yayın Akışları",
                        createdAt = System.currentTimeMillis()
                    )
                )
            }

            // 8. Türk Mutfak Sanatı ve Sulu Yemek Püf Noktaları
            if (allList.none { it.title.contains("Türk Mutfak", ignoreCase = true) }) {
                db.aiKnowledgeDao().insertKnowledge(
                    AiKnowledgeEntity(
                        title = "Türk Mutfak Sanatı ve Sulu Yemek Püf Noktaları",
                        content = """
Geleneksel Türk Mutfağı Ustalarından Püf Noktaları:
1. Kuru Fasulye Sırrı: Bir gece önceden ıslatılır. Bol tereyağında soğan kavrulur, et önce mühürlenir. Kısık ateşte özleşmesi sağlanır.
2. Sulu Tas Kebabı & Güveç: Kuşbaşı dana/kuzu eti önce suyunu çekene kadar mühürlenir, arpacık soğan ve patatesle kısık ateşte pişirilir.
3. Karadeniz ve Samsun Pidesi: Mayalı hamur ince açılır, iç harcı koyulduktan sonra fırından çıkar çıkmaz hakiki Trabzon tereyağı sürülür.
4. Çorbalarda Terbiye Usulü: Yayla ve mercimek çorbalarında yoğurt-yumurta sarısı terbiyesi, çorbanın sıcak suyuyla temperlenerek kesilmeden eklenir.
                        """.trimIndent(),
                        category = "CULINARY_ARTS",
                        isOfficialVerified = true,
                        source = "Geleneksel Türk Mutfağı Külliyatı",
                        createdAt = System.currentTimeMillis()
                    )
                )
            }

            // 9. Python YouTube ve Google Asistan Köprüsü
            if (allList.none { it.title.contains("Python YouTube", ignoreCase = true) }) {
                db.aiKnowledgeDao().insertKnowledge(
                    AiKnowledgeEntity(
                        title = "Python YouTube ve Google Asistan Köprüsü",
                        content = """
Python ile YouTube Şarkı Çalma ve Google Asistan Entegrasyon Kılavuzu:
1. Kütüphaneler: 'pip install pywhatkit SpeechRecognition pyttsx3'
2. YouTube'da Şarkı Açma:
   import pywhatkit
   pywhatkit.playonyt("Şarkı Adı")
3. Tarayıcı Alternatifi:
   import webbrowser, urllib.parse
   webbrowser.open(f"https://www.youtube.com/results?search_query={urllib.parse.quote_plus('Şarkı Adı')}")
4. Google Asistan Köprüsü:
   webbrowser.open("https://assistant.google.com/")
5. Telefonda Çalıştırma: Pydroid 3 veya Termux üzerinden 'python youtube_assistant_bridge.py' komutuyla doğrudan çalıştırılabilir.
                        """.trimIndent(),
                        category = "PYTHON_BRIDGE",
                        isOfficialVerified = true,
                        source = "HatırlaGit Jarvis Geliştirici Kütüphanesi",
                        createdAt = System.currentTimeMillis()
                    )
                )
            }

            // 10. Jarvis Akıllı Uygulama Başlatma ve Cihaz Kontrolü
            if (allList.none { it.title.contains("Jarvis Akıllı Uygulama", ignoreCase = true) }) {
                db.aiKnowledgeDao().insertKnowledge(
                    AiKnowledgeEntity(
                        title = "Jarvis Akıllı Uygulama Başlatma ve Cihaz Kontrolü",
                        content = """
Jarvis / Usta Sesli Uygulama ve Cihaz Başlatma Rehberi:
• YouTube: 'YouTube'dan [şarkı/video] aç' dendiğinde ilgili parçayı oynatır.
• Google Asistan: 'Google Asistan'ı aç' veya 'Asistana bağlan' dendiğinde sesli asistanı açar.
• WhatsApp: 'WhatsApp'ı aç' veya 'mesajlara gir' komutuyla başlatır.
• Kamera & Galeri: 'Kamerayı aç' veya 'Fotoğrafları göster' ile devreye girer.
• Harita & Navigasyon: 'Haritayı aç' ile Google Haritalar'ı başlatır.
• Saat & Alarm: 'Alarmları aç' veya 'Saat uygulamasını aç' ile saat yöneticisini açar.
• Akıllı Süpürge: 'Süpürgeyi çalıştır' dendiğinde Roborock veya Mi Home uygulamasını başlatır.
• Takvim & Ajanda: 'Takvimi aç' ile etkinlik takvimini gösterir.
                        """.trimIndent(),
                        category = "JARVIS_APP_CONTROL",
                        isOfficialVerified = true,
                        source = "HatırlaGit Jarvis İşletim Protokolü",
                        createdAt = System.currentTimeMillis()
                    )
                )
            }

            // 11. 657 Sayılı Devlet Memurları Kanunu ve Güncel Yönetmelikler
            if (allList.none { it.title.contains("657 Sayılı Devlet Memurları", ignoreCase = true) }) {
                db.aiKnowledgeDao().insertKnowledge(
                    AiKnowledgeEntity(
                        title = "657 Sayılı Devlet Memurları Kanunu ve İzin/Disiplin Yönetmeliği",
                        content = """
657 Sayılı Devlet Memurları Kanunu ve Güncel Memur Mevzuatı Esasları:
1. Temel İlkeler: Sınıflandırma, Kariyer ve Liyakat ilkeleri esastır.
2. İzin Hakları:
   • Yıllık İzin: Hizmeti 1 yıldan 10 yıla kadar (10 yıl dahil) olan memurlar için 20 gün; hizmeti 10 yıldan fazla olanlar için 30 gündür. Zorunlu hallerde gidiş-dönüş için 4 güne kadar yol izni verilebilir.
   • Mazeret İzni: Kadın memura doğum öncesi 8, doğum sonrası 8 hafta analık izni; çoğul gebelikte doğum öncesine 2 hafta eklenir. Erkek memura eşinin doğumu nedeniyle 10 gün babalık izni verilir. Memurun isteği üzerine; kendisinin veya çocuğunun evlenmesi, annesi, babası, eşi, çocuğu veya kardeşinin vefatı halinde 7 gün izin verilir.
   • Süt İzni: Doğum sonrası analık izni bitiminden itibaren ilk 6 ayda günde 3 saat, ikinci 6 ayda günde 1.5 saat süt izni verilir.
   • Hastalık ve Refakat İzni: Memurun bakmakla yükümlü olduğu veya refakat etmediği takdirde hayatı tehlikeye girecek ana, baba, eş ve çocukları ile kardeşlerinden birinin ağır bir kaza geçirmesi veya tedavisi uzun süren bir hastalığının bulunması hallerinde 3 aya kadar refakat izni verilir, gerektiğinde bir katına kadar uzatılabilir.
3. Disiplin Cezaları ve Savunma Hakkı:
   • Cezalar: Uyarma, Kınama, Aylıktan Kesme, Kademe İlerlemesinin Durdurulması ve Devlet Memurluğundan Çıkarma.
   • Savunma Hakkı: Savunma için en az 7 gün süre tanınmadan hiçbir disiplin cezası verilemez.
4. Ödev ve Sorumluluklar: Tarafsızlık ve devlete bağlılık, davranış ve işbirliği, amir durumda olan devlet memurlarının görev ve sorumlulukları.
                        """.trimIndent(),
                        category = "OFFICIAL_LAW",
                        isOfficialVerified = true,
                        source = "T.C. Cumhurbaşkanlığı Mevzuat Bilgi Sistemi (657 Sayılı Kanun)",
                        createdAt = System.currentTimeMillis()
                    )
                )
            }

            // 12. 7354 Sayılı Öğretmenlik Meslek Kanunu (ÖMK) ve Kariyer Basamakları
            if (allList.none { it.title.contains("Öğretmenlik Meslek Kanunu", ignoreCase = true) }) {
                db.aiKnowledgeDao().insertKnowledge(
                    AiKnowledgeEntity(
                        title = "7354 Sayılı Öğretmenlik Meslek Kanunu (ÖMK) ve Haklar",
                        content = """
7354 Sayılı Öğretmenlik Meslek Kanunu ve Güncel Düzenlemeler:
1. Kariyer Basamakları:
   • Öğretmen: Adaylık sürecini veya Millî Eğitim Akademisi programını başarıyla tamamlayanlar.
   • Uzman Öğretmen: Öğretmenlikte en az 10 yıl hizmeti bulunan, mesleki gelişim eğitimini tamamlayan ve kademe ilerlemesi cezası bulunmayan öğretmenler uzman öğretmen unvanı ve ek tazminat alır.
   • Başöğretmen: Uzman öğretmenlikte en az 10 yıl hizmeti bulunan öğretmenler başöğretmen unvanı ve en üst derece tazminat alır.
2. Öğretmene Karşı Şiddete Karşı Ağırlaştırılmış Yaptırımlar:
   • Görevi başındaki veya görevi sebebiyle öğretmene, eğitim çalışanına karşı işlenen kasten yaralama, tehdit, hakaret ve direnme suçlarında cezalar yarı oranında (%50) artırılır ve hapis cezaları ertelenemez.
3. Hak ve Güvenceler: Mesleki bağımsızlık, akademik özgürlük, pedagojik rehberlik ve görev güvencesi kanunla teminat altındadır.
                        """.trimIndent(),
                        category = "TEACHER_LAW",
                        isOfficialVerified = true,
                        source = "T.C. Resmî Gazete / MEB (7354 Sayılı Kanun)",
                        createdAt = System.currentTimeMillis()
                    )
                )
            }

            // 13. 4688 Sayılı Kamu Görevlileri Sendikaları ve Toplu Sözleşme Kanunu
            if (allList.none { it.title.contains("Sendikal", ignoreCase = true) }) {
                db.aiKnowledgeDao().insertKnowledge(
                    AiKnowledgeEntity(
                        title = "4688 Sayılı Sendika Kanunu, Sendikal Haklar ve İşlemler",
                        content = """
Öğretmenler ve Eğitim Çalışanları İçin Sendikal Haklar Rehberi (4688 Sayılı Kanun):
1. Sendika Üyeliği:
   • 3 nüsha üyelik başvuru formu doldurulur ve okul/kurum evrakına teslim edilir. Bir nüshası sendikaya gönderilir. Üyelik aidatı maaştan bordro yoluyla kesilir.
2. Sendikadan Çekilme (İstifa):
   • Memur 3 nüsha istifa formunu doldurarak okul idaresine teslim eder. Evrak kayıt numarası alındıktan sonra 30 günün bitimiyle istifa hukuken kesinleşir.
3. Sendikal İzinler ve Güvenceler:
   • İlçe Temsilcisi: Haftada 4 saat veya yetkili sendika için tam gün izin hakkı.
   • İl Yönetim Kurulu: Haftada 1 tam gün sendikal izinli sayılır.
   • Sendika Yöneticisi Güvencesi: Sendika yöneticileri rızaları dışında başka bir ile veya ilçeye resen atanamaz.
4. Eylem ve İş Bırakma Güvencesi (Hukuki Çerçeve):
   • Anayasa Mahkemesi, Danıştay ve AİHM yerleşik içtihatlarına göre; yetkili sendikanın aldığı meşru karar doğrultusunda 1 günlük veya süreli iş bırakma eylemine katılan memura 'göreve gelmemek' suçundan disiplin cezası verilemez. Bu eylem anayasal sendikal hak kapsamındadır.
5. Toplu Sözleşme İkramiyesi: Sendikalı kamu görevlilerine yılda 4 kez (Ocak, Nisan, Temmuz, Ekim) toplu sözleşme ikramiyesi ödenir.
                        """.trimIndent(),
                        category = "SYNDICAL_RIGHTS",
                        isOfficialVerified = true,
                        source = "4688 Sayılı Kanun / Danıştay ve AYM Kararları",
                        createdAt = System.currentTimeMillis()
                    )
                )
            }

            // 14. MEB Yönetici ve Öğretmenlerinin Ders ve Ek Ders Yönetmeliği
            if (allList.none { it.title.contains("Ek Ders Yönetmeliği", ignoreCase = true) }) {
                db.aiKnowledgeDao().insertKnowledge(
                    AiKnowledgeEntity(
                        title = "MEB Yönetici ve Öğretmenlerinin Ek Ders ve Ücret Yönetmeliği",
                        content = """
MEB Ders ve Ek Ders Saatlerine İlişkin Karar Hükümleri:
1. Maaş Karşılığı ve Zorunlu Ek Ders:
   • Branş Öğretmenleri: Haftada 15 saat maaş karşılığı, 15 saate kadar zorunlu ek ders, 6 saate kadar isteğe bağlı ek ders (Toplam haftalık max 30 saat girebilir).
   • Sınıf ve Okul Öncesi Öğretmenleri: Haftada 18 saat maaş karşılığı, 12 saat zorunlu ek ders.
2. Hazırlık ve Planlama Görevi:
   • Okutulan her 10 saat ders için 1 saat ek ders verilir (Haftalık en fazla 3 saat).
3. Nöbet Görevi:
   • Okulda fiilen tutulan nöbet görevi için haftada 3 saat ek ders ücreti ödenir.
4. Sosyal Kulüp ve Rehberlik:
   • Sınıf rehber öğretmenliği veya eğitsel kulüp yürüten öğretmenlere haftada 2 saat ek ders ücreti ödenir.
5. Destekleme ve Yetiştirme Kursları (DYK):
   • Hafta içi veya hafta sonu DYK kurslarında fiilen okutulan derslerin ek ders ücreti %100 artırımlı olarak ödenir.
6. Sınav Görevleri: MEB merkezi sınavları (LGS, bursluluk, e-Sınav) ve ÖSYM sınavlarında oturum bazlı sınav ücreti ödenir.
                        """.trimIndent(),
                        category = "TEACHER_FINANCIALS",
                        isOfficialVerified = true,
                        source = "2006/11350 Sayılı Bakanlar Kurulu Kararı / MEB",
                        createdAt = System.currentTimeMillis()
                    )
                )
            }

            // 15. MEB BEP (Bireyselleştirilmiş Eğitim), Rehberlik ve Zümre Tutanakları
            if (allList.none { it.title.contains("BEP ve Zümre", ignoreCase = true) }) {
                db.aiKnowledgeDao().insertKnowledge(
                    AiKnowledgeEntity(
                        title = "MEB BEP (Özel Eğitim), Rehberlik ve Zümre İşlemleri",
                        content = """
Öğretmenler İçin Özel Eğitim, BEP ve Zümre Rehberi:
1. BEP (Bireyselleştirilmiş Eğitim Programı):
   • RAM (Rehberlik ve Araştırma Merkezi) raporu bulunan kaynaştırma/bütünleştirme öğrencileri için okul BEP Geliştirme Birimi toplanır.
   • Ders öğretmeni öğrencinin eğitsel performans düzeyine uygun BEP planını hazırlar.
   • Yazılı sınavlar öğrencinin BEP kazanımlarına göre ayrı açık uçlu sorularla yapılır.
2. Zümre Öğretmenler Kurulu Tutanakları:
   • Sene başı, 2. dönem başı ve sene sonu olmak üzere yılda en az 3 kez toplanır.
   • Yıllık planlar, ortak sınav tarihleri ve konu soru dağılım tabloları, Maarif Modeli beceri temelli öğrenme süreçleri zümre tutanaklarında karara bağlanır.
                        """.trimIndent(),
                        category = "TEACHING_PEDAGOGY",
                        isOfficialVerified = true,
                        source = "MEB Özel Eğitim Hizmetleri Yönetmeliği & TTKB",
                        createdAt = System.currentTimeMillis()
                    )
                )
            }

            // 16. Türkiye Gezi ve Seyahat Rehberi: 81 İl ve Öne Çıkan Rotalar
            if (allList.none { it.title.contains("Türkiye Gezi ve Seyahat", ignoreCase = true) }) {
                db.aiKnowledgeDao().insertKnowledge(
                    AiKnowledgeEntity(
                        title = "Türkiye Gezi ve Seyahat Rehberi: Bölgeler ve Tarihi Rotalar",
                        content = """
Türkiye 7 Bölge Gezi Rotaları ve Öne Çıkan Seyahat Durakları:
1. Marmara Bölgesi: İstanbul (Tarihi Yarımada, Ayasofya, Sultanahmet, Boğaz, Galata, Topkapı), Edirne (Selimiye Camii, Meriç Köprüsü), Bursa (Ulu Cami, Cumalıkızık, Uludağ), Çanakkale (Gelibolu Şehitliği, Truva Antik Kenti, Assos), Balıkesir (Ayvalık, Cunda Adası).
2. Ege Bölgesi: İzmir (Efes Antik Kenti, Şirince, Çeşme, Bergama), Aydın (Kuşadası, Afrodisias, Milet, Didim), Muğla (Bodrum Kalesi, Fethiye Ölüdeniz, Saklıkent, Dalyan Kral Mezarları), Denizli (Pamukkale Travertenleri, Hierapolis), Manisa (Sardes Antik Kenti, Spil Dağı).
3. Akdeniz Bölgesi: Antalya (Kaleiçi, Aspendos, Perge, Düden ve Manavgat Şelaleleri, Alanya Kalesi, Olympos, Kaş, Kalkan), Mersin (Kızkalesi, Cennet-Cehennem Obrukları), Adana (Taşköprü, Sabancı Merkez Camii), Hatay (Antakya Arkeoloji Müzesi, Aziz Piyer Kilisesi, Harbiye Şelaleleri).
4. İç Anadolu Bölgesi: Ankara (Anıtkabir, Anadolu Medeniyetleri Müzesi, Hacı Bayram Veli Camii, Gordion), Nevşehir (Kapadokya, Göreme Açık Hava Müzesi, Derinkuyu Yeraltı Şehri, Ürgüp, Uçhisar), Konya (Mevlana Müzesi, Çatalhöyük, Beyşehir Gölü), Sivas (Divriği Ulu Camii, Çifte Minareli Medrese), Eskişehir (Odunpazarı, Sazova Parkı).
5. Karadeniz Bölgesi: Trabzon (Sümela Manastırı, Ayasofya, Uzungöl), Rize (Ayder Yaylası, Zilkale, Fırtına Deresi, Pokut Yaylası), Samsun (Bandırma Vapuru Müzesi, Amisos Tepesi, Şahinkaya Kanyonu), Karabük (Safranbolu Tarihi Konakları), Kastamonu (Kastamonu Kalesi, Ilgaz Dağı), Amasya (Kral Kaya Mezarları, Yalıboyu Evleri), Artvin (Karagöl, Mençuna Şelalesi).
6. Doğu Anadolu Bölgesi: Van (Van Gölü, Akdamar Adası ve Kilisesi, Van Kalesi, Muradiye Şelalesi), Ağrı (İshak Paşa Sarayı, Doğubayazıt), Kars (Ani Harabeleri, Çıldır Gölü, Kars Kalesi), Erzurum (Çifte Minareli Medrese, Yakutiye, Palandöken), Erzincan (Girlevik Şelalesi, Kemaliye Karanlık Kanyon), Malatya (Arslantepe Höyüğü).
7. Güneydoğu Anadolu Bölgesi: Şanlıurfa (Göbeklitepe, Balıklıgöl, Harran Kümbet Evleri, Halfeti), Gaziantep (Zeugma Mozaik Müzesi, Gaziantep Kalesi, Bakırcılar Çarşısı), Mardin (Deyrulzafaran Manastırı, Kasımiye Medresesi, Dara Antik Kenti, Eski Mardin Taş Evleri), Diyarbakır (Diyarbakır Kalesi ve Surları, Hevsel Bahçeleri, Ulu Cami, Ongözlü Köprü), Adıyaman (Nemrut Dağı Heykelleri, Cendere Köprüsü).
                        """.trimIndent(),
                        category = "TURKEY_TRAVEL",
                        isOfficialVerified = true,
                        source = "T.C. Kültür ve Turizm Bakanlığı Seyahat Portalı",
                        createdAt = System.currentTimeMillis()
                    )
                )
            }

            // 17. Türkiye UNESCO Dünya Mirası Eserleri ve Kültür Varlıkları
            if (allList.none { it.title.contains("UNESCO Dünya Mirası", ignoreCase = true) }) {
                db.aiKnowledgeDao().insertKnowledge(
                    AiKnowledgeEntity(
                        title = "Türkiye UNESCO Dünya Mirası Listesindeki Eserler ve Anıtlar",
                        content = """
Türkiye'nin UNESCO Dünya Mirası Listesi'nde Yer Alan Başlıca Kültürel Varlıkları:
1. Göbeklitepe (Şanlıurfa): M.Ö. 10.000'e uzanan, insanlık tarihinin bilinen en eski anıtsal tapınak kompleksi. T biçimli steller üzerinde hayvan kabartmaları bulunur.
2. Efes Antik Kenti (İzmir): Celsus Kütüphanesi, Artemis Tapınağı, Büyük Tiyatro ve Meryem Ana Evi'ni barındıran Helenistik ve Roma döneminin metropolü.
3. Kapadokya ve Göreme Millî Parkı (Nevşehir): Peri bacaları, tüf kayalara oyulmuş manastırlar, kiliseler ve Derinkuyu/Kaymaklı yeraltı şehirleri.
4. Nemrut Dağı (Adıyaman): Kommagene Krallığı hükümdarı I. Antiochos'un tanrılar panteonunu birleştiren devasa anıt heykelleri ve tümülüsü.
5. Divriği Ulu Camii ve Darüşşifası (Sivas): Anadolu Selçuklu / Mengücekli şaheseri; taş işçiliğindeki 'gölge namaz kılan insan' silüeti ve eşsiz bezemeleriyle İslam mimarisinin zirvesidir.
6. Çatalhöyük Neolitik Kenti (Konya): M.Ö. 7400'lere dayanan, kapısız ve sokaksız, çatılardan girilen evleriyle ilk yerleşik kentleşme modeli.
7. Pamukkale ve Hierapolis (Denizli): Kalsiyum oksitli termal suların oluşturduğu beyaz travertenler ve antik termal hamam kompleksi.
8. Afrodisias (Aydın): Aşk ve güzellik tanrıçası Afrodit'e adanan, antik dünyanın en büyük mermer heykelcilik okulu ve stadyumu.
9. Selimiye Camii ve Külliyesi (Edirne): Mimar Sinan'ın 'Ustalık eserim' dediği, 4 minareli, muazzam kubbeli Türk-İslam mimarisinin başyapıtı.
10. Ani Arkeolojik Alanı (Kars): Orta Çağ İpek Yolu üzerinde 'Binbir Kiliseli Şehir' olarak anılan surlar, katedral ve Selçuklu Menûçihr Camii.
11. Safranbolu Şehri (Karabük): Geleneksel Osmanlı ahşap sivil mimarisini, han, hamam ve çeşmelerini özgün dokusuyla koruyan müze kent.
12. Truva Arkeolojik Alanı (Çanakkale): Homeros'un İlyada Destanı'na konu olan 9 farklı medeniyet katmanına sahip efsanevi antik kent.
13. Bursa ve Cumalıkızık: Osmanlı İmparatorluğu'nun ilk başkenti, külliyeleri, İpek Hanı ve yaşayan 700 yıllık erken Osmanlı köyü Cumalıkızık.
14. Bergama Çok Katmanlı Kültürel Peyzajı (İzmir): Parşömenin anavatanı, antik dünyanın en dik tiyatrosu ve Asklepion sağlık merkezi.
15. Diyarbakır Kalesi ve Hevsel Bahçeleri: Çin Seddi'nden sonra dünyanın en uzun ve sağlam bazalt taş surları ile Dicle Nehri kıyısındaki tarihi bahçeler.
16. Arslantepe Höyüğü (Malatya): İlk devlet ve bürokrasi sisteminin, kerpiç sarayın ve en eski bronz kılıçların bulunduğu höyük.
17. Gordion (Ankara): Frigya Krallığı'nın başkenti; Kral Midas'ın mezarı (tümülüs) ve Gordion düğümü efsanesinin beşiği.
                        """.trimIndent(),
                        category = "CULTURAL_HERITAGE",
                        isOfficialVerified = true,
                        source = "UNESCO & T.C. Kültür Varlıkları ve Müzeler Genel Müdürlüğü",
                        createdAt = System.currentTimeMillis()
                    )
                )
            }

            // 18. Türkiye Coğrafyası: 7 Bölge, Dağlar, Akarsular, Göller ve Ovalar
            if (allList.none { it.title.contains("Türkiye Coğrafyası", ignoreCase = true) }) {
                db.aiKnowledgeDao().insertKnowledge(
                    AiKnowledgeEntity(
                        title = "Türkiye Fiziki ve Beşeri Coğrafyası: Dağlar, Akarsular, Göller",
                        content = """
Türkiye'nin Coğrafi Özellikleri, Doğal Varlıkları ve İklim Haritası:
1. Dağ Silsileleri ve Volkanik Dağlar:
   • Kuzey Anadolu Dağları: Karadeniz boyunca uzanır (Kaçkarlar 3932 m, Küre Dağları, Ilgaz Dağı, Köroğlu Dağları).
   • Toros Dağları: Akdeniz kuşağında uzanır (Batı, Orta ve Güneydoğu Toroslar; Aladağlar, Bolkar Dağları, Beydağları).
   • Volkanik Dağlar: Ağrı Dağı (5137 m - Türkiye'nin en yüksek zirvesi), Erciyes Dağı (3917 m), Hasan Dağı, Süphan Dağı, Nemrut Volkanı (Krater Gölü ile meşhur), Kula Volkanları (Türkiye'nin en genç volkanik sahası - Jeopark).
   • Buzul Dağları: Cilo (Reşko Zirvesi 4135 m) Dağları.
2. Başlıca Akarsular ve Havzalar:
   • Karadeniz'e Dökülenler: Kızılırmak (Türkiye sınırları içindeki en uzun nehir - 1355 km), Yeşilırmak, Sakarya Nehri, Çoruh Nehri (Rafting cenneti).
   • Basra Körfezi'ne Dökülenler: Fırat ve Dicle Nehirleri (Mezopotamya'ya hayat veren can damarları).
   • Hazar Denizi'ne (Kapalı Havza) Dökülenler: Aras ve Kura Nehirleri.
   • Akdeniz'e Dökülenler: Seyhan, Ceyhan, Göksu, Manavgat, Aksu, Dalaman.
   • Ege Denizi'ne Dökülenler: Büyük Menderes, Küçük Menderes, Gediz, Bakırçay, Meriç Nehri (Türkiye-Yunanistan sınırı).
3. Göller ve Sulak Alanlar:
   • Tektonik Göller: Tuz Gölü (Türkiye'nin 2. büyük gölü), Beyşehir Gölü (En büyük tatlı su gölü), Eğirdir, Burdur, İznik, Sapanca, Manyas (Kuş Gölü), Hazar Gölü.
   • Volkanik ve Set Gölleri: Van Gölü (Türkiye'nin en büyük gölü - 3713 km², sodalı), Nemrut Krater Gölü, Çıldır Gölü (Kışın buz tutan göl), Uzungöl, Abant ve Yedigöller.
4. İklim Kuşakları: Karadeniz İklimi (Her mevsim yağışlı, ılıman), Akdeniz İklimi (Yazları sıcak ve kurak, kışları ılık ve yağışlı - maki bitki örtüsü), Karasal İklim (Yazları sıcak ve kurak, kışları soğuk ve kar yağışlı - bozkır/step bitki örtüsü).
                        """.trimIndent(),
                        category = "GEOGRAPHY_RESOURCES",
                        isOfficialVerified = true,
                        source = "Harita Genel Müdürlüğü & TÜİK Coğrafi İstatistikleri",
                        createdAt = System.currentTimeMillis()
                    )
                )
            }

            // 19. Türk Kültürü, Medeniyetler Kronolojisi ve Yöresel Mutfak Mirası
            if (allList.none { it.title.contains("Medeniyetler Kronolojisi", ignoreCase = true) }) {
                db.aiKnowledgeDao().insertKnowledge(
                    AiKnowledgeEntity(
                        title = "Anadolu Medeniyetleri Kronolojisi, Kültür ve Coğrafi İşaretli Lezzetler",
                        content = """
Anadolu Tarihi Medeniyetler Kronolojisi ve Gastronomi Mirası:
1. Anadolu Medeniyetleri Sıralaması:
   • Paleolitik & Neolitik: Göbeklitepe, Çatalhöyük, Hacılar, Çayönü.
   • Tunç ve Demir Çağı: Hattiler, Hititler (Hattuşa - Kadeş Antlaşması), Frigler (Gordion - Kral Midas), Lidyalılar (Sardes - İlk madeni para), Urartular (Tuşpa/Van - Su kanalları), İyonlar (Efes, Milet - Felsefe ve bilim).
   • Klasik ve Orta Çağ: Persler, Büyük İskender ve Helenistik Krallıklar, Roma İmparatorluğu, Doğu Roma (Bizans).
   • Türk-İslam Dönemi: Büyük Selçuklu, Anadolu Selçuklu Devleti (Konya başkent, kervansaraylar, Ahilik teşkilatı), Anadolu Beylikleri (Karamanoğulları, Candaroğulları, Karesioğulları, Dulkadiroğulları vb.), Osmanlı İmparatorluğu (1299-1922) ve Türkiye Cumhuriyeti (1923-Günümüz).
2. Coğrafi İşaretli Tescilli Türk Lezzetleri:
   • Güneydoğu & Akdeniz: Gaziantep Baklavası ve Beyranı, Şanlıurfa Urfa Kebabı ve Çiğköftesi, Hatay Künefesi ve Tepsi Kebabı, Adana Kebabı, Mersin Tantunisi, Kahramanmaraş Dövme Dondurması.
   • İç Anadolu: Kayseri Mantısı ve Pastırması, Konya Etliekmeği, Ankara Tavası, Sivas Köftesi, Nevşehir Testi Kebabı.
   • Karadeniz: Trabzon Akçaabat Köftesi ve Vakfıkebir Ekmeği, Samsun Bafra ve Terme Pidesi, Rize Muhlaması (Kuymak), Çorum Leblebisi.
   • Ege & Marmara: Bursa İskender Kebabı ve Kestane Şekeri, İzmir Kumrusu ve Boyozu, Aydın İnciri, Balıkesir Susurluk Tostu ve Ayranı, Afyonkarahisar Sucuğu ve Kaymağı, Edirne Tava Ciğeri.
   • Doğu Anadolu: Erzurum Cağ Kebabı ve Oltu Taşı, Van Kahvaltısı ve Otlu Peyniri, Malatya Kayısısı, Kars Kaşarı ve Gravyeri.
                        """.trimIndent(),
                        category = "TURKISH_CULTURE",
                        isOfficialVerified = true,
                        source = "Türk Patent ve Marka Kurumu & Kültür Portalı",
                        createdAt = System.currentTimeMillis()
                    )
                )
            }

            // 20. MEB Öğretmen Atama, Sosyal Etkinlikler ve Okul Gezileri Genelgesi
            if (allList.none { it.title.contains("Atama ve Sosyal Etkinlikler", ignoreCase = true) }) {
                db.aiKnowledgeDao().insertKnowledge(
                    AiKnowledgeEntity(
                        title = "MEB Öğretmen Atama, Yer Değiştirme, Sosyal Etkinlikler ve Okul Gezileri Mevzuatı",
                        content = """
Öğretmenler İçin Atama, Gezi Onayları ve Sosyal Etkinlikler Rehberi:
1. Öğretmen Atama ve Yer Değiştirme Yönetmeliği:
   • Hizmet Puanı: Görev yapılan il/ilçenin hizmet alanına (1-6. hizmet alanları), zorunlu çalışma süresine, başarı belgelerine (Teşekkür, Takdir, Üstün Başarı), yüksek lisans/doktora ve DYK kurs görevlerine göre her ay otomatik puan eklenir.
   • Mazeret Tayinleri: Eş durumu (aile birliği mazereti), sağlık mazereti ve can güvenliği mazeretine bağlı yer değiştirmeler her yıl yarıyıl (Ocak-Şubat) ve yaz tatili (Ağustos) dönemlerinde 2 aşamalı olarak yapılır.
   • İsteğe Bağlı İl İçi ve İl Dışı Atama: Bulunulan eğitim kurumunda en az 3 yıllık çalışma süresini tamamlayan öğretmenler her yıl Mayıs-Haziran aylarında hizmet puanı üstünlüğüne göre tercih yapabilir.
2. MEB Sosyal Etkinlikler Yönetmeliği ve Okul Gezileri Çerçeve Yönergesi:
   • Okul Dışı Gezi Onay Süreci: İl içi gezilerde gezi tarihinden en az 7 gün önce okul müdürlüğüne; il dışı gezilerde ise en az 15 gün önce il/ilçe millî eğitim müdürlüğüne onay dosyası sunulur.
   • Gerekli Belgeler: Gezi Planı, Kafile Listesi, Veli İzin Onay Belgeleri, Araç Uygunluk Belgesi (TÜVTÜRK muayene, zorunlu koltuk ferdi kaza sigortası, D2 yetki belgesi), Şoför Ehliyet ve SRC/Psikoteknik belgeleri.
   • Görevli Öğretmen Oranı: Her 10 öğrenci için en az 1 refakatçi öğretmen görevlendirilir; kafile başkanı bir müdür yardımcısı veya kıdemli öğretmendir.
3. Resmi Yazışma, DYS ve Dilekçe Hakkı:
   • 3071 Sayılı Dilekçe Hakkının Kullanılması Kanunu uyarınca memurların idareye verdikleri dilekçelere en geç 30 gün içinde gerekçeli cevap verilmesi kanuni zorunluluktur.
   • DYS (Doküman Yönetim Sistemi) üzerinden gelen resmi yazılar ve tebellüğ belgeleri yasal tebligat niteliğindedir.
                        """.trimIndent(),
                        category = "TEACHER_OFFICIAL_GUIDE",
                        isOfficialVerified = true,
                        source = "MEB Tebliğler Dergisi & Resmî Gazete",
                        createdAt = System.currentTimeMillis()
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
