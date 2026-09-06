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

            // 2. MEB Sınıf Geçme Yönetmeliği
            if (allList.none { it.title.contains("Sınıf Geçme", ignoreCase = true) }) {
                db.aiKnowledgeDao().insertKnowledge(
                    AiKnowledgeEntity(
                        title = "MEB Ortaöğretim Sınıf Geçme ve Sınav Yönetmeliği",
                        content = """
MEB Ortaöğretim Kurumları Yönetmeliği (Resmî Gazete) Hükümleri:
1. Doğrudan Sınıf Geçme: Ders yılı sonunda tüm derslerden başarılı olan veya başarısız dersi bulunsa bile yıl sonu başarı puanı en az 50 olan öğrenciler doğrudan sınıf geçer.
2. Baraj Dersi Kuralı: Türk Dili ve Edebiyatı dersi baraj derstir. Yıl sonu başarı puanı 50 veya üzerinde olsa dahi bu dersten başarısız olan öğrenci sorumlu olarak üst sınıfa geçer.
3. Sorumlu Geçme Sınırı: Bir üst sınıfa en fazla 3 dersten sorumlu olarak geçilebilir. Alt sınıflar da dahil olmak üzere toplam sorumlu ders sayısı en fazla 6 olabilir. 6'dan fazla sorumlu dersi biriken öğrenci sınıf tekrarı yapar.
4. Devamsızlık Sınırları: Özürsüz devamsızlık süresi en fazla 10 gündür. Toplam devamsızlık süresi (özürlü + özürsüz) 30 günü aşan öğrenciler, başarı puanları ne olursa olsun başarısız sayılarak sınıf tekrarına kalır.
5. Sınav Uygulamaları: Sınavlar yazılı ve uygulamalı olarak ortak metinlerle yapılır. Açık liseye geçişler istisnai durumlar dışında kısıtlanmıştır.
                        """.trimIndent(),
                        category = "OFFICIAL_LAW",
                        isOfficialVerified = true,
                        source = "T.C. Resmî Gazete / MEB",
                        createdAt = System.currentTimeMillis()
                    )
                )
            }

            // 3. Türk Tarihi ve Kültürel Miras
            if (allList.none { it.title.contains("Türk Tarihi", ignoreCase = true) }) {
                db.aiKnowledgeDao().insertKnowledge(
                    AiKnowledgeEntity(
                        title = "Türk Tarihi, Kültürü ve Edebi Şaheserler",
                        content = """
Türk Tarihi ve Kültür Külliyatı:
1. İlk Türk Devletleri: Asya Hun Devleti (Mete Han ve onluk ordu sistemi), Göktürkler (Orhun Abideleri - Bilge Kağan, Kül Tigin, Tonyukuk), Uygurlar (yerleşik hayata geçiş, matbaa ve şehircilik).
2. İslamiyet Sonrası Türk Edebiyatı Şaheserleri:
   - Kutadgu Bilig (Yusuf Has Hacib): Mutluluk veren bilgi, siyasetname türünün ilk örneği.
   - Dîvânu Lugâti't-Türk (Kaşgarlı Mahmud): İlk Türkçe sözlük, ansiklopedi ve harita.
   - Atabetü'l-Hakayık (Edib Ahmed Yükneki): Hakikatlerin eşiği, ahlak ve edep kitabı.
   - Dîvân-ı Hikmet (Hoca Ahmed Yesevi): Tasavvufi şiirler ve hikmetler.
3. Büyük Selçuklu ve Anadolu Selçuklu: Malazgirt Zaferi (1071), kervansaraylar, Ahilik teşkilatı (Ahi Evran - esnaf ahlakı ve mesleki dayanışma).
4. Osmanlı Devleti: 1299 Söğüt kuruluşu, 1453 İstanbul'un Fethi (Fatih Sultan Mehmed), adalet divanı, vakıf medeniyeti ve Mimar Sinan eserleri.
5. Millî Mücadele ve Cumhuriyet: 19 Mayıs 1919 Samsun'a çıkış (Gazi Mustafa Kemal Atatürk), Amasya, Erzurum ve Sivas Kongreleri, TBMM'nin Açılışı (23 Nisan 1920), Sakarya Meydan Muharebesi, Büyük Taarruz (30 Ağustos 1922) ve 29 Ekim 1923 Türkiye Cumhuriyeti'nin İlanı.
                        """.trimIndent(),
                        category = "CULTURE_HISTORY",
                        isOfficialVerified = true,
                        source = "Türk Tarih Kurumu & MEB Müfredatı",
                        createdAt = System.currentTimeMillis()
                    )
                )
            }

            // 4. Lise Tarih Dersi (9, 10, 11, 12. Sınıf) Maarif Müfredatı ve Ders Kitapları
            if (allList.none { it.title.contains("Lise Tarih Müfredatı", ignoreCase = true) }) {
                db.aiKnowledgeDao().insertKnowledge(
                    AiKnowledgeEntity(
                        title = "Lise Tarih (9, 10, 11, 12) Maarif Müfredatı ve Üniteleri",
                        content = """
MEB Yeni Maarif Modeli Lise Tarih Dersi Kitapları ve Ünite Haritası:
• 9. Sınıf Tarih:
  1. Ünite: Geçmişin İnşası ve Tarih Yazımı (Tarihin tanımı, yöntem, kaynak türleri, takvim sistemleri).
  2. Ünite: İlk ve Orta Çağlarda Türk Dünyası (Orta Asya coğrafyası, boylar konfederasyonu, kut inancı, töre).
  3. Ünite: İslam Medeniyetinin Doğuşu (Asr-ı Saadet, Dört Halife, Emeviler, Abbasiler, bilim havzaları).
  4. Ünite: Türklerin İslamiyeti Kabulü ve İlk Türk İslam Devletleri (Talas Savaşı, Karahanlılar, Gazneliler, Büyük Selçuklu).
  5. Ünite: Türkiye Tarihi (11-13. Yüzyıl) - Miryokefalon Zaferi, Anadolu Selçuklu, Kösedağ Savaşı ve Ahilik.

• 10. Sınıf Tarih:
  1. Ünite: Yerleşme ve Devletleşme Sürecinde Selçuklu Türkiyesi.
  2. Ünite: Beylikten Devlete Osmanlı Siyaseti (1302-1453) - Koyunhisar Savaşı, İskân ve İstimâlet politikası.
  3. Ünite: Devletleşme Sürecinde Savaşçılar ve Askerler (Tımarlı Sipahiler, Yeniçeri Ocağı, Kapıkulu).
  4. Ünite: Beylikten Devlete Osmanlı Medeniyeti (Divan-ı Hümayun, Medrese, Şeyhülislamlık, Tasavvuf).
  5. Ünite: Dünya Gücü Osmanlı (1453-1595) - Fatih, Yavuz, Kanuni Sultan Süleyman devirleri ve Preveze Zaferi.

• 11. Sınıf Tarih:
  1. Ünite: Değişen Dünya Dengeleri Karşısında Osmanlı Siyaseti (1595-1774) - Zitvatorok, Karlofça ve Küçük Kaynarca.
  2. Ünite: Değişim Çağında Avrupa ve Osmanlı - Rönesans, Reform, Aydınlanma ve Lale Devri.
  3. Ünite: Uluslararası İlişkilerde Denge Stratejisi (1774-1914) - Kırım Harbi, Berlin Antlaşması, Düyun-ı Umumiye.
  4. Ünite: Devrimler Çağında Değişen Devlet-Toplum İlişkileri - Tanzimat, Islahat ve Meşrutiyet.
  5. Ünite: Sermaye ve Emek, Sanayi İnkılabı ve Osmanlı Ekonomisi.

• 12. Sınıf T.C. İnkılap Tarihi ve Atatürkçülük:
  1. Ünite: 20. Yüzyıl Başlarında Osmanlı ve Dünya (Trablusgarp, Balkan Savaşları, I. Dünya Savaşı, Çanakkale).
  2. Ünite: Millî Mücadele (Kuvâ-yı Millîye, Amasya Genelgesi, Erzurum ve Sivas Kongreleri, TBMM, Misak-ı Millî).
  3. Ünite: Cephelerde Millî Mücadele (Doğu, Güney ve Batı Cepheleri, İnönü, Sakarya, Büyük Taarruz, Mudanya, Lozan).
  4. Ünite: Atatürkçülük ve Türk İnkılabı (Cumhuriyetçilik, Milliyetçilik, Halkçılık, Devletçilik, Laiklik, İnkılapçılık; Siyasi, Hukuki, Sosyal ve İktisadi İnkılaplar).
  5. Ünite: İki Savaş Arasındaki Dönemde Türkiye ve Dünya (Montrö Boğazlar Sözleşmesi, Hatay'ın Anavatana Katılması).
                        """.trimIndent(),
                        category = "MAARIF_CURRICULUM",
                        isOfficialVerified = true,
                        source = "MEB Talim ve Terbiye Kurulu",
                        createdAt = System.currentTimeMillis()
                    )
                )
            }

            // 5. Lise Türk Dili ve Edebiyatı (9, 10, 11, 12. Sınıf) Maarif Müfredatı ve Ders Kitapları
            if (allList.none { it.title.contains("Lise Edebiyat Müfredatı", ignoreCase = true) }) {
                db.aiKnowledgeDao().insertKnowledge(
                    AiKnowledgeEntity(
                        title = "Lise Türk Dili ve Edebiyatı (9, 10, 11, 12) Maarif Müfredatı",
                        content = """
MEB Yeni Maarif Modeli Lise Türk Dili ve Edebiyatı Ders Kitapları ve Ünite Haritası:
• 9. Sınıf Türk Dili ve Edebiyatı:
  1. Ünite: Giriş (Edebiyatın bilimlerle ilişkisi, dilin kullanımından doğan türler: ağız, şive, lehçe, argo, jargon).
  2. Ünite: Hikâye (Olay hikâyesi - Ömer Seyfettin, Durum hikâyesi - Sait Faik Abasıyanık, anlatıcı bakış açıları).
  3. Ünite: Şiir (Manzume ve şiir ayrımı, nazım birimi, hece/aruz ölçüsü, tam/yarım/zengin kafiye, redif, söz sanatları).
  4. Ünite: Masal ve Fabl (Evrensel masal tipleri, Keloğlan masalları, La Fontaine, Beydeba - Kelile ve Dimne).
  5. Ünite: Roman, Tiyatro ve Biyografi/Mektup/Günlük türleri.

• 10. Sınıf Türk Dili ve Edebiyatı:
  1. Ünite: Giriş (Edebiyatın tarih ve din ile ilişkisi, Türk edebiyatının ana dönemleri).
  2. Ünite: Hikâye (Dede Korkut Hikâyeleri, Kerem ile Aslı halk hikâyesi, Tanzimat dönemi hikâyeleri).
  3. Ünite: Şiir (İslamiyet öncesi: Koşuk, Sagu; Geçiş dönemi: Kutadgu Bilig, Dîvânu Lugâti't-Türk; Halk şiiri: Koşma, Semai; Divan şiiri: Gazel, Kaside, Şarkı).
  4. Ünite: Destan ve Efsane (Oğuz Kağan, Ergenekon, Manas, Türeyiş; İlyada, Şehname, Nibelungen).
  5. Ünite: Roman (İlk yerli roman: Taaşşuk-ı Tal'at ve Fitnat, Mai ve Siyah, Çalıkuşu).

• 11. Sınıf Türk Dili ve Edebiyatı:
  1. Ünite: Edebiyat ve Toplum İlişkisi.
  2. Ünite: Hikâye (Cumhuriyet Dönemi 1923-1960 hikâyeciliği: Memduh Şevket Esendal, Haldun Taner).
  3. Ünite: Şiir (Tanzimat'tan Cumhuriyet'e şiir gelenekleri: Saf Şiir - Ahmet Haşim, Yahya Kemal; Millî Edebiyat zevki).
  4. Ünite: Makale, Fıkra (Köşe Yazısı) ve Sohbet.
  5. Ünite: Roman (Yakup Kadri Karaosmanoğlu - Yaban, Reşat Nuri Güntekin, Ahmet Hamdi Tanpınar - Huzur).

• 12. Sınıf Türk Dili ve Edebiyatı:
  1. Ünite: Edebiyatın Felsefe ve Psikoloji ile İlişkisi.
  2. Ünite: Hikâye (1960-1980 arası ve 1980 sonrası modern hikâye, Küçürek / Minimal hikâye).
  3. Ünite: Şiir (Cumhuriyet Dönemi Şiiri: İkinci Yeni - Cemal Süreya, Edip Cansever; Toplumcu Gerçekçiler - Nazım Hikmet; Mistik-Dini Şiir - Sezai Karakoç).
  4. Ünite: Roman (Modernizm ve Postmodernizm: Oğuz Atay - Tutunamayanlar, Yusuf Atılgan - Aylak Adam, Orhan Pamuk).
  5. Ünite: Deneme, Tiyatro ve Nutuk (Söylev).
                        """.trimIndent(),
                        category = "MAARIF_CURRICULUM",
                        isOfficialVerified = true,
                        source = "MEB Talim ve Terbiye Kurulu",
                        createdAt = System.currentTimeMillis()
                    )
                )
            }

            // 6. Şube Öğretmenler Kurulu (ŞÖK) Mevzuatı ve Resmi Maddeleri
            if (allList.none { it.title.contains("ŞÖK Mevzuatı", ignoreCase = true) }) {
                db.aiKnowledgeDao().insertKnowledge(
                    AiKnowledgeEntity(
                        title = "MEB Şube Öğretmenler Kurulu (ŞÖK) Mevzuatı ve Gündemi",
                        content = """
MEB Ortaöğretim Kurumları Yönetmeliği Madde 36, 57 ve 58 uyarınca ŞÖK Esasları:
1. Kurul Üyeleri: Şube rehber öğretmeni başkanlığında, o şubede derse giren tüm branş öğretmenleri ve rehberlik öğretmeninden oluşur. Toplantılara okul müdürü veya ilgili müdür yardımcısı başkanlık eder.
2. Toplanma Zamanları: 1. Dönem başı (Ekim), 2. Dönem başı (Şubat) ve Ders yılı sonu (Haziran). İhtiyaç duyulması halinde ara toplantı yapılabilir.
3. Resmi Gündem Maddeleri:
   a) Açılış ve yoklama.
   b) Bir önceki ŞÖK kararlarının değerlendirilmesi.
   c) Şubenin akademik başarı durumu (Türk Dili ve Edebiyatı 70 barajı ve ders ortalamaları).
   d) Devam-devamsızlık durumu (Özürsüz 10 gün, toplam 30 gün sınırına yaklaşan öğrencilerin velilerine tebligat).
   e) Disiplin, rehberlik, BEP ve özel eğitim ihtiyacı olan öğrencilerin durumları.
   f) Maarif modeli değerler eğitimi ve sosyal etkinlik uygulamaları.
   g) Başarıyı artırıcı tedbirler ve zümre kararları (DYK kursları, telafi etütleri).
   h) Dilek ve temenniler, kapanış.
4. Kararların Niteliği: Alınan kararlar tutanakla kayıt altına alınır, okul müdürü tarafından onaylanarak yürürlüğe girer.
                        """.trimIndent(),
                        category = "OFFICIAL_LAW",
                        isOfficialVerified = true,
                        source = "MEB Ortaöğretim Kurumları Yönetmeliği",
                        createdAt = System.currentTimeMillis()
                    )
                )
            }

            // 7. Günlük TV Prime-Time Yayın Akışı Rehberi
            if (allList.none { it.title.contains("Günlük TV Rehberi", ignoreCase = true) }) {
                db.aiKnowledgeDao().insertKnowledge(
                    AiKnowledgeEntity(
                        title = "Türkiye Günlük TV Dizileri ve Yayın Akışı Rehberi",
                        content = """
Türkiye Ulusal Televizyon Kanalları Prime-Time (Saat 20:00) Günlük Dizi ve Program Akışı:
• Pazartesi:
  - NOW TV: Kızıl Goncalar (Dram/Gerilim - Özgü Namal, Özcan Deniz)
  - TRT 1: Kudüs Fatihi Selahaddin Eyyubi (Tarih/Aksiyon)
  - TV8: MasterChef Türkiye (Yarışma)

• Salı:
  - TRT 1: Mehmed: Fetihler Sultanı (Tarihi Dönem Dizisi - Serkan Çayoğlu)
  - Show TV: Bahar (Dram/Komedi - Demet Evgar, Buğra Gülsoy)
  - NOW TV: Gizli Bahçe (Dram)
  - TV8: MasterChef Türkiye

• Çarşamba:
  - ATV: Kuruluş Osman (Tarih/Aksiyon - Burak Özçivit)
  - Show TV: Sandık Kokusu (Aile/Dram - Özge Özpirinçci)
  - Star TV: Sahipsizler (Dram)
  - TV8: MasterChef Türkiye

• Perşembe:
  - NOW TV: Hudutsuz Sevda (Aksiyon/Dram - Deniz Can Aktaş, Miray Daner)
  - Kanal D: İnci Taneleri (Dram - Yılmaz Erdoğan, Hazar Ergüçlü)
  - Show TV: Siyah Kalp (Dram)

• Cuma:
  - Show TV: Kızılcık Şerbeti (Aile/Toplumsal Dram - Evrim Alasya, Barış Kılıç, Sıla Türkoğlu)
  - Star TV: Yalı Çapkını (Dram/Romantik - Afra Saraçoğlu, Mert Ramazan Demir)
  - Kanal D: Arka Sokaklar (Polisiye/Aksiyon - Zafer Ergin, Özgür Ozan)
  - TRT 1: Kara Ağaç Destanı (Dönem Dramı)

• Cumartesi:
  - TRT 1: Gönül Dağı (Samimi Anadolu/Aile - Berk Atan, Cihat Süvarioğlu, Semih Ertürk)
  - ATV: Kardeşlerim (Gençlik/Dram)
  - NOW TV: Yabani (Dram)
  - TV8: MasterChef Türkiye

• Pazar:
  - TRT 1: Teşkilat (İstihbarat/Aksiyon - Murat Yıldırım, Tolga Sarıtaş)
  - Show TV: Deha (Matematik/Aksiyon/Dram - Aras Bulut İynemli)
  - NOW TV: Kirli Sepeti (Dram/Entrika)
  - TV8: MasterChef Türkiye (Eleme Gecesi)
                        """.trimIndent(),
                        category = "TV_GUIDE",
                        isOfficialVerified = true,
                        source = "RTÜK & Ulusal TV Yayın Akışları",
                        createdAt = System.currentTimeMillis()
                    )
                )
            }

            // 8. Tüketici Hakları ve Hakem Heyeti Rehberi
            if (allList.none { it.title.contains("Tüketici Hakları", ignoreCase = true) }) {
                db.aiKnowledgeDao().insertKnowledge(
                    AiKnowledgeEntity(
                        title = "6502 Sayılı Tüketici Kanunu ve Hakem Heyeti Rehberi",
                        content = """
T.C. Ticaret Bakanlığı 6502 Sayılı Tüketicinin Korunması Kanunu:
1. Cayma Hakkı (İnternetten Alışveriş): Mesafeli sözleşmelerde tüketici, hiçbir gerekçe göstermeksizin ve cezai şart ödemeksizin 14 gün içinde malı iade etme hakkına sahiptir.
2. Ayıplı Malda Seçimlik Haklar: Ücret iadesi, indirim, ücretsiz onarım veya ayıpsız misli ile değişim talep edilebilir.
3. Tüketici Hakem Heyeti (THH): e-Devlet (TUBİS) üzerinden masrafsız başvuru yapılabilir. Kararları bağlayıcıdır.
                        """.trimIndent(),
                        category = "OFFICIAL_LAW",
                        isOfficialVerified = true,
                        source = "T.C. Ticaret Bakanlığı",
                        createdAt = System.currentTimeMillis()
                    )
                )
            }

            // 9. Türk Mutfak Sanatı ve Sulu Yemek Püf Noktaları
            if (allList.none { it.title.contains("Türk Mutfak", ignoreCase = true) }) {
                db.aiKnowledgeDao().insertKnowledge(
                    AiKnowledgeEntity(
                        title = "Türk Mutfak Sanatı ve Sulu Yemek Püf Noktaları",
                        content = """
Geleneksel Türk Mutfağı Ustalarından Püf Noktaları:
1. Kuru Fasulye Sırrı: Bir gece önceden ıslatılır. Bol tereyağında soğan kavrulur, et önce mühürlenir. Pişerken kısık ateşte özleşmesi sağlanır.
2. Sulu Tas Kebabı & Güveç: Kuşbaşı dana/kuzu eti önce suyunu çekene kadar mühürlenir, arpacık soğan ve patatesle zenginleştirilir.
3. Karadeniz ve Samsun Pidesi: Mayalı hamur ince açılır, iç harcı koyulduktan sonra kenarları burgu şeklinde kapatılır, fırından çıkar çıkmaz hakiki tereyağı sürülür.
4. Çorbalarda Terbiye Usulü: Yayla ve mercimek çorbalarında yoğurt-yumurta sarısı terbiyesi, çorbanın sıcak suyuyla temperlenerek kesilmeden eklenir.
                        """.trimIndent(),
                        category = "CULINARY_ARTS",
                        isOfficialVerified = true,
                        source = "Geleneksel Türk Mutfağı Külliyatı",
                        createdAt = System.currentTimeMillis()
                    )
                )
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
