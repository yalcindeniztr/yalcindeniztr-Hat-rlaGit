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

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
