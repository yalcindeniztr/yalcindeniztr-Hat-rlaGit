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

            val hasMaarif = allList.any { it.title.contains("Maarif", ignoreCase = true) }
            val hasSinifGecme = allList.any { it.title.contains("Sınıf Geçme", ignoreCase = true) }

            if (!hasMaarif) {
                val maarifEntity = AiKnowledgeEntity(
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
                db.aiKnowledgeDao().insertKnowledge(maarifEntity)
            }

            if (!hasSinifGecme) {
                val sinifGecmeEntity = AiKnowledgeEntity(
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
                db.aiKnowledgeDao().insertKnowledge(sinifGecmeEntity)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
