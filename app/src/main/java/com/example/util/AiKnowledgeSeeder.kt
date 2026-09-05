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
4. Ölçme ve Değerlendirme: Sonuç odaklı çoktan seçmeli ezber yerine, süreç odaklı, öğrencinin gelişimini izleyen biçimlendirici değerlendirme araçları esastır.
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

            // 4. İlk Yardım, Afet ve Acil Durum Rehberi
            if (allList.none { it.title.contains("İlk Yardım", ignoreCase = true) }) {
                db.aiKnowledgeDao().insertKnowledge(
                    AiKnowledgeEntity(
                        title = "İlk Yardım, Afet Bilinci ve Acil Durum Rehberi",
                        content = """
T.C. Sağlık Bakanlığı ve AFAD İlk Yardım Protokolü:
1. Acil Çağrı Tek Numara: Türkiye genelinde tüm acil durumlar için tek numara 112'dir (Ambulans, Polis, İtfaiye, Jandarma, Orman Yangını, Sahil Güvenlik).
2. Heimlich Manevrası (Soluk Borusu Tıkanması):
   - Tam Tıkanma: Kişi nefes alamaz, konuşamaz, boğazını tutar ve morarır.
   - Müdahale: Hastanın arkasına geçilir. Bir el yumruk yapılıp göbek deliği ile göğüs kemiği arasına yerleştirilir. Diğer elle yumruk kavranır, içe ve yukarı doğru 5 kez kuvvetle bastırılır.
3. Yanık Müdahalesi: Yanan bölge en az 15-20 dakika ılık/serin akar su altında tutulur. Yanığa ASLA diş macunu, yoğurt, salça sürülmez! Temiz, nemli bir bezle örtülür.
4. Deprem Anı Hareket Tarzı (Çök-Kapan-Tutun): Sağlam bir eşyanın (koltuk, baza) yanına çökülür, baş ve boyun korunarak kapanılır, sarsıntı bitene kadar sağlam nesneye tutunulur. Asansör ve merdivenler ASLA kullanılmaz.
                        """.trimIndent(),
                        category = "HEALTH_EMERGENCY",
                        isOfficialVerified = true,
                        source = "T.C. Sağlık Bakanlığı & AFAD",
                        createdAt = System.currentTimeMillis()
                    )
                )
            }

            // 5. Tüketici Hakları ve Hakem Heyeti Rehberi
            if (allList.none { it.title.contains("Tüketici Hakları", ignoreCase = true) }) {
                db.aiKnowledgeDao().insertKnowledge(
                    AiKnowledgeEntity(
                        title = "6502 Sayılı Tüketici Kanunu ve Hakem Heyeti Rehberi",
                        content = """
T.C. Ticaret Bakanlığı 6502 Sayılı Tüketicinin Korunması Kanunu:
1. Cayma Hakkı (İnternetten Alışveriş): Mesafeli sözleşmelerde tüketici, hiçbir gerekçe göstermeksizin ve cezai şart ödemeksizin 14 (on dört) gün içinde malı iade etme (cayma) hakkına sahiptir.
2. Ayıplı Malda Seçimlik Haklar: Malın ayıplı çıkması durumunda tüketici şu 4 haktan birini talep edebilir:
   - Sözleşmeden dönme (ücret iadesi),
   - Satış bedelinden indirim isteme,
   - Aşırı bir masraf gerektirmediği takdirde ücretsiz onarım,
   - Ayıpsız misli ile değiştirilmesini isteme.
3. Tüketici Hakem Heyeti (THH): Belirli parasal sınırların altındaki uyuşmazlıklarda e-Devlet üzerinden (TUBİS) masrafsız olarak başvuru yapılabilir. Kararları bağlayıcı ve ilam niteliğindedir.
                        """.trimIndent(),
                        category = "OFFICIAL_LAW",
                        isOfficialVerified = true,
                        source = "T.C. Ticaret Bakanlığı",
                        createdAt = System.currentTimeMillis()
                    )
                )
            }

            // 6. Türk Mutfak Kültürü ve Pişirme Sanatı
            if (allList.none { it.title.contains("Türk Mutfak", ignoreCase = true) }) {
                db.aiKnowledgeDao().insertKnowledge(
                    AiKnowledgeEntity(
                        title = "Türk Mutfak Sanatı ve Sulu Yemek Püf Noktaları",
                        content = """
Geleneksel Türk Mutfağı Ustalarından Püf Noktaları:
1. Kuru Fasulye Sırrı: Bir gece önceden ıslatılır. Soğan bolca tereyağında kavrulur, domates ve biber salçası eklenir. Etli yapılacaksa et önce suyunu çekene kadar mühürlenir. Pişerken köpüğü alınır, kısık ateşte özleşmesi sağlanır.
2. Sulu Tas Kebabı & Güveç: Kuşbaşı dana veya kuzu eti önce yüksek ateşte mühürlenip suyunu çektirilir. Arpacık soğan, sarımsak, havuç ve patatesle zenginleştirilir.
3. Karadeniz ve Samsun Pidesi: Mayalı hamur ince açılır, iç harcı (kıymalı, çökelekli, kavurmalı) koyulduktan sonra kenarları burgu şeklinde kapatılır. Fırından çıkar çıkmaz bol hakiki Trabzon tereyağı sürülür.
4. Çorbalarda Terbiye Usulü: Yayla ve mercimek çorbalarında yoğurt ve yumurta sarısı terbiyesi, çorbanın sıcak suyundan yavaş yavaş karıştırılarak ılıtılıp (temperleme) eklenir ki kesilmesin.
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
