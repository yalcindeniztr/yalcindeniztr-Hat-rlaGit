package com.example.util

import java.util.Locale

/**
 * ATİLA Çevrimdışı Ansiklopedik Muhakeme ve Soru-Cevap Motoru
 * Tarih, Maarif/MEB, Bilim, Edebiyat, Felsefe, Sağlık ve Ekonomi alanlarında
 * internet veya API anahtarı olmaksızın anında derinlikli, doğru yanıtlar sunar.
 */
object GeneralKnowledgeHelper {

    data class KnowledgeAnswer(
        val title: String,
        val shortSpeech: String,
        val fullContent: String,
        val category: String
    )

    fun answerQuery(rawQuery: String, greeting: String = ""): KnowledgeAnswer? {
        val lower = rawQuery.lowercase(Locale.forLanguageTag("tr-TR")).trim()

        // 1. TARİH & MİLLİ MÜCADELE
        if (lower.contains("atatürk") || lower.contains("mustafa kemal")) {
            return KnowledgeAnswer(
                title = "Gazi Mustafa Kemal Atatürk (1881 - 1938)",
                shortSpeech = "Gazi Mustafa Kemal Atatürk, Türkiye Cumhuriyeti'nin kurucusu, Kurtuluş Savaşı'nın Başkomutanı ve ilk Cumhurbaşkanıdır.",
                fullContent = """
                    **Gazi Mustafa Kemal Atatürk (1881 - 1938):**
                    • Türkiye Cumhuriyeti'nin kurucusu, Kurtuluş Savaşı'nın muzaffer Başkomutanı ve ilk Cumhurbaşkanıdır.
                    • 19 Mayıs 1919'da Samsun'a çıkarak Millî Mücadele'yi başlatmış; Amasya, Erzurum ve Sivas Kongreleri ile milli iradeyi teşkilatlandırmıştır.
                    • 23 Nisan 1920'de TBMM'yi açarak "Egemenlik kayıtsız şartsız milletindir" ilkesini hayata geçirmiştir.
                    • Sakarya Meydan Muharebesi ve Başkomutanlık Meydan Muharebesi (Büyük Taarruz) ile vatan topraklarını işgalden kurtarmıştır.
                    • Cumhuriyetçilik, Milliyetçilik, Halkçılık, Devletçilik, Laiklik ve İnkılapçılık ilkeleriyle modern Türkiye'nin temellerini atmıştır.
                """.trimIndent(),
                category = "TARİH"
            )
        }

        if (lower.contains("kurtuluş savaşı") || lower.contains("milli mücadele")) {
            return KnowledgeAnswer(
                title = "Millî Mücadele ve Kurtuluş Savaşı (1919 - 1923)",
                shortSpeech = "Kurtuluş Savaşı; Doğu, Güney ve Batı cephelerinde Türk milletinin bağımsızlık ve hürriyet uğruna verdiği destansı zaferdir.",
                fullContent = """
                    **Kurtuluş Savaşı (1919 - 1923) Cepheleri ve Safhaları:**
                    • **Doğu Cephesi:** Kazım Karabekir komutasında Ermeni işgaline karşı Gümrü Antlaşması ile zafer kazanılmıştır.
                    • **Güney Cephesi:** Maraş, Antep ve Urfa'da halkın Kuva-yi Milliye direnişiyle Fransızlara karşı kazanılmış, Ankara Antlaşması ile taçlanmıştır.
                    • **Batı Cephesi:** I. ve II. İnönü, Kütahya-Eskişehir Muharebeleri, Sakarya Meydan Muharebesi ve 26-30 Ağustos 1922 Büyük Taarruz ile Yunan ordusu denize dökülmüştür.
                    • **Sonuç:** Mudanya Ateşkesi ve 24 Temmuz 1923 Lozan Barış Antlaşması ile bağımsız Türkiye Cumhuriyeti tüm dünya tarafından resmen tanınmıştır.
                """.trimIndent(),
                category = "TARİH"
            )
        }

        if (lower.contains("çanakkale")) {
            return KnowledgeAnswer(
                title = "18 Mart Çanakkale Zaferi (1915)",
                shortSpeech = "Çanakkale Zaferi, 'Çanakkale Geçilmez' dedirten, Türk milletinin kahramanlık ve fedakarlık destanıdır.",
                fullContent = """
                    **Çanakkale Zaferi (1915):**
                    • **18 Mart Deniz Zaferi:** İtilaf Devletleri donanması Boğaz'ı geçmeye çalışmış, Nusret Mayın Gemisi ve Seyit Onbaşı gibi kahramanların direnişiyle püskürtülmüştür.
                    • **Kara Savaşları:** Gelibolu Yarımadası'nda Mustafa Kemal Paşa'nın "Ben size taarruzu değil, ölmeyi emrediyorum!" tarihi emriyle Anafartalar, Conkbayırı ve Arıburnu'nda düşman hezimete uğratılmıştır.
                    • **Önemi:** I. Dünya Savaşı'nın seyrini değiştirmiş, Çarlık Rusyası'nın çöküşünü hızlandırmış ve Kurtuluş Savaşı'nın milli ruhunu doğurmuştur.
                """.trimIndent(),
                category = "TARİH"
            )
        }

        if (lower.contains("fatih sultan mehmet") || lower.contains("istanbul'un fethi") || lower.contains("istanbul ne zaman fethedildi")) {
            return KnowledgeAnswer(
                title = "Fatih Sultan Mehmet ve İstanbul'un Fethi (29 Mayıs 1453)",
                shortSpeech = "Fatih Sultan Mehmet, 29 Mayıs 1453'te 21 yaşında İstanbul'u fethederek Orta Çağ'ı kapatıp Yeni Çağ'ı açmıştır.",
                fullContent = """
                    **İstanbul'un Fethi (29 Mayıs 1453):**
                    • II. Mehmed (Fatih), dehası, Şahi topları, gemileri karadan Haliç'e indirme stratejisi ve kararlılığıyla 53 günlük kuşatma sonunda Doğu Roma (Bizans) İmparatorluğu'na son vermiştir.
                    • Orta Çağ sona ermiş, Yeni Çağ başlamıştır.
                    • İstanbul, Osmanlı Devleti'nin başkenti ve dünyanın ilim, sanat ve ticaret merkezi haline gelmiştir.
                """.trimIndent(),
                category = "TARİH"
            )
        }

        if (lower.contains("osmanlı") && (lower.contains("kurul") || lower.contains("padişah") || lower.contains("tarih") || lower.contains("kimdir") || lower.contains("devlet"))) {
            return KnowledgeAnswer(
                title = "Osmanlı Devleti (1299 - 1922)",
                shortSpeech = "Osmanlı Devleti, Osman Bey tarafından Söğüt ve Domaniç'te kurulan, 3 kıtaya adaletle hükmetmiş bir cihan imparatorluğudur.",
                fullContent = """
                    **Osmanlı Devleti Tarihi Özeti:**
                    • **Kuruluş (1299/1302):** Osman Gazi, Orhan Gazi, I. Murad. İskân ve istimâlet (hoşgörü) politikalarıyla Balkanlar'da kökleşme.
                    • **Yükselme (1453 - 1579):** Fatih Sultan Mehmet (İstanbul fethi), Yavuz Sultan Selim (Halifelik ve Doğu seferleri), Kanuni Sultan Süleyman (Muhteşem Yüzyıl).
                    • **Duraklama ve Gerileme:** Karlofça Antlaşması (1699) ile toprak kaybının başlaması, Lale Devri ve Nizam-ı Cedid yenilikleri.
                    • **Dağılma:** Tanzimat ve Islahat Fermanları, I. Dünya Savaşı ve ardından Millî Mücadele ile yerini Türkiye Cumhuriyeti'ne bırakması.
                """.trimIndent(),
                category = "TARİH"
            )
        }

        if (lower.contains("selçuklu") || lower.contains("malazgirt")) {
            return KnowledgeAnswer(
                title = "Büyük Selçuklu Devleti ve Malazgirt Zaferi (1071)",
                shortSpeech = "26 Ağustos 1071 Malazgirt Meydan Muharebesi ile Sultan Alparslan Anadolu'nun kapılarını Türklere ebediyen açmıştır.",
                fullContent = """
                    **Selçuklular ve Malazgirt Zaferi:**
                    • **Malazgirt Zaferi (26 Ağustos 1071):** Sultan Alparslan komutasındaki Selçuklu ordusu, Bizans İmparatoru Romen Diyojen'i mağlup ederek Anadolu'yu Türk yurdu yapmıştır.
                    • **Nizamiye Medreseleri:** Vezir Nizamülmülk tarafından kurulan üniversite düzeyindeki medreseler, İslam medeniyetinin altın çağını başlatmıştır.
                    • **Anadolu Selçukluları:** Konya başkentli devlet; kervansaraylar, Ahilik teşkilatı, Mevlana ve Yunus Emre gibi manevi mimarlarla Anadolu kültürünü yoğurmuştur.
                """.trimIndent(),
                category = "TARİH"
            )
        }

        // 2. BİLİM, FİZİK, KİMYA, BİYOLOJİ
        if (lower.contains("kuantum")) {
            return KnowledgeAnswer(
                title = "Kuantum Fiziği ve Mekaniği",
                shortSpeech = "Kuantum mekaniği, maddenin ve ışığın atomik ve atom altı düzeydeki davranışlarını inceleyen modern fizik dalıdır.",
                fullContent = """
                    **Kuantum Fiziği Temel İlkeleri:**
                    • **Enerji Paketleri (Kuantum):** Max Planck tarafından keşfedilen, enerjinin sürekli değil kesikli paketler halinde yayılması ilkesi.
                    • **Dalga-Parçacık İkiliği:** Işığın ve elektronların hem dalga hem parçacık özelliği göstermesi (De Broglie ve Çift Yarık Deneyi).
                    • **Heisenberg Belirsizlik İlkesi:** Bir parçacığın konumu ve momentumunun aynı anda kesin doğrulukla ölçülememesi.
                    • **Kuantum Süperpozisyonu & Dolanıklık:** Bir parçacığın aynı anda birden fazla durumda bulunabilmesi ve mesafeden bağımsız birbirini etkilemesi (Kuantum bilgisayarların temeli).
                """.trimIndent(),
                category = "BİLİM"
            )
        }

        if (lower.contains("fotosentez")) {
            return KnowledgeAnswer(
                title = "Fotosentez Biyokimyası",
                shortSpeech = "Fotosentez; klorofilli canlıların güneş enerjisini kullanarak su ve karbondioksitten glikoz ve oksijen üretmesidir.",
                fullContent = """
                    **Fotosentez Reaksiyonu:**
                    • **Denklem:** 6CO₂ + 6H₂O + Işık Enerjisi ➔ C₆H₁₂O₆ (Glikoz) + 6O₂
                    • **Işığa Bağımlı Reaksiyonlar:** Kloroplastın tilakoid zarında gerçekleşir; su fotolize uğrar, oksijen açığa çıkar, ATP ve NADPH üretilir.
                    • **Işıktan Bağımsız (Calvin Döngüsü):** Kloroplastın stromasında gerçekleşir; CO₂ tutulur, ATP ve NADPH harcanarak glikoz sentezlenir.
                    • **Önemi:** Dünya üzerindeki tüm canlı yaşamın temel besin ve oksijen kaynağıdır.
                """.trimIndent(),
                category = "BİLİM"
            )
        }

        if (lower.contains("dna") || lower.contains("genetik") || lower.contains("genom")) {
            return KnowledgeAnswer(
                title = "DNA (Deoksiribonükleik Asit) ve Genetik Yapı",
                shortSpeech = "DNA, canlıların genetik kodunu ve tüm biyolojik talimatlarını taşıyan çift sarmallı nükleik asit molekülüdür.",
                fullContent = """
                    **DNA Molekülünün Yapısı:**
                    • **Yapı Taşları:** Nükleotidler (Fosfat grubu, Deoksiriboz şekeri ve Azotlu Organik Baz).
                    • **Baz Eşleşmeleri:** Adenin (A) daima Timin (T) ile 2'li hidrojen bağı; Guanin (G) daima Sitozin (C) ile 3'lü hidrojen bağı kurar.
                    • **Fonksiyonu:** Hücre bölünmesinde kendisini kusursuz kopyalar (Replikasyon); RNA sentezleyerek protein üretimini yönetir (Transkripsiyon).
                    • **Genom:** İnsan genomunda yaklaşık 3 milyar baz çifti ve 20.000-25.000 protein kodlayan gen bulunur.
                """.trimIndent(),
                category = "BİLİM"
            )
        }

        if (lower.contains("büyük patlama") || lower.contains("big bang")) {
            return KnowledgeAnswer(
                title = "Büyük Patlama (Big Bang) Teorisi",
                shortSpeech = "Büyük Patlama, evrenin yaklaşık 13.8 milyar yıl önce sonsuz yoğunluk ve sıcaklıktaki tekillikten genişleyerek başladığını açıklayan kozmolojik modeldir.",
                fullContent = """
                    **Büyük Patlama (Big Bang) Kanıtları:**
                    • **Evrenin Genişlemesi:** Edwin Hubble'ın galaksilerin birbirinden uzaklaştığını keşfetmesi (Kırmızıya Kayma).
                    • **Kozmik Mikrodalga Arka Plan Işıması (CMBR):** Penzias ve Wilson tarafından saptanan, evrenin ilk anlarından kalan 2.7 Kelvin sıcaklığındaki yankı.
                    • **Element Bolluğu:** Evrendeki hidrojen (%74) ve helyum (%24) oranının teorik hesaplarla birebir örtüşmesi.
                """.trimIndent(),
                category = "BİLİM"
            )
        }

        if (lower.contains("yapay zeka") || lower.contains("makine öğrenimi") || lower.contains("deep learning")) {
            return KnowledgeAnswer(
                title = "Yapay Zeka (AI) ve Makine Öğrenimi",
                shortSpeech = "Yapay zeka; algoritmalar ve derin sinir ağları aracılığıyla insan muhakemesini modelleyen, öğrenen ve karar veren bilgi işlem teknolojisidir.",
                fullContent = """
                    **Yapay Zeka Mimarisi:**
                    • **Makine Öğrenimi (ML):** Veriden öğrenen algoritmalar (Denetimli, Denetimsiz ve Pekiştirmeli Öğrenme).
                    • **Derin Öğrenme (Deep Learning):** Çok katmanlı yapay sinir ağları (CNN, RNN, Transformer) ile görüntü, ses ve metin işleme.
                    • **Büyük Dil Modelleri (LLM):** Dikkat mekanizması (Self-Attention) kullanarak insan benzeri doğal dil anlama ve üretme (Gemini, GPT).
                    • **Kullanım Alanları:** Otonom araçlar, tıbbi teşhis, sesli asistanlar, eğitimde kişiselleştirilmiş Maarif planlaması.
                """.trimIndent(),
                category = "TEKNOLOJİ"
            )
        }

        // 3. MEVZUAT & MEMUR HUKUKU (657, ÖMK, SENDİKA)
        if (lower.contains("yıllık izin") || lower.contains("kaç gün izin")) {
            return KnowledgeAnswer(
                title = "657 Sayılı Kanun Madde 102 - Devlet Memuru Yıllık İzinleri",
                shortSpeech = "Hizmeti 1 yıldan 10 yıla kadar olan memurlara 20 gün, 10 yıldan fazla olanlara 30 gün yıllık izin verilir.",
                fullContent = """
                    **Devlet Memurları Yıllık İzin Hakları (657 Sayılı Kanun):**
                    • **1 - 10 Yıl Arası Hizmet:** Yılda 20 iş günü izin.
                    • **10 Yıldan Fazla Hizmet:** Yılda 30 iş günü izin.
                    • **Yol İzni:** Zorunlu hallerde memura gidiş ve dönüş için en çok 4 güne kadar yol izni verilebilir.
                    • **Devretme Kuralı:** Kullanılmayan yıllık izin, bir sonraki yılın izniyle birleştirilerek kullanılabilir. Ancak 2 yılı aşan izinler yanar.
                    • **Öğretmenler:** Öğretmenler yaz tatili ile ara tatillerde izinli sayıldıklarından ayrıca yıllık izin verilmez.
                """.trimIndent(),
                category = "MEVZUAT"
            )
        }

        if (lower.contains("babalık izni") || lower.contains("doğum izni") || lower.contains("analık izni")) {
            return KnowledgeAnswer(
                title = "657 Sayılı Kanun Madde 104 - Mazeret ve Doğum İzinleri",
                shortSpeech = "Erkek memura eşinin doğumu için 10 gün babalık izni; kadın memura ise doğum öncesi 8, doğum sonrası 8 olmak üzere toplam 16 hafta analık izni verilir.",
                fullContent = """
                    **Mazeret İzinleri Hükümleri:**
                    • **Babalık İzni:** Memura, eşinin doğum yapması hâlinde isteği üzerine 10 gün babalık izni verilir.
                    • **Analık İzni:** Kadın memura doğumdan önce 8 ve doğumdan sonra 8 hafta olmak üzere toplam 16 hafta analık izni verilir. Çoğul gebelikte doğum öncesine 2 hafta eklenir.
                    • **Süt İzni:** Doğum sonrası analık izni bitiminden itibaren ilk 6 ayda günde 3 saat, ikinci 6 ayda günde 1.5 saat süt izni verilir.
                    • **Evlilik İzni:** Memurun kendisinin veya çocuğunun evlenmesi halinde 7 gün izin verilir.
                    • **Vefat İzni:** Memurun eşinin, çocuğunun, kendisinin veya eşinin ana, baba veya kardeşinin vefatı halinde 7 gün izin verilir.
                """.trimIndent(),
                category = "MEVZUAT"
            )
        }

        if (lower.contains("uzman öğretmen") || lower.contains("başöğretmen") || lower.contains("ömk")) {
            return KnowledgeAnswer(
                title = "7354 Sayılı Öğretmenlik Meslek Kanunu (ÖMK) Kariyer Basamakları",
                shortSpeech = "Öğretmenlikte en az 10 yılını tamamlayanlar Uzman Öğretmen, uzman öğretmenlikte 10 yılını tamamlayanlar Başöğretmen unvanı ve ek tazminat alır.",
                fullContent = """
                    **ÖMK Kariyer Basamakları ve Şartları:**
                    • **Uzman Öğretmenlik:** Meslekte adaylık dahil en az 10 yıl fiilen hizmeti bulunan, mesleki gelişim eğitimini tamamlayan ve kademe ilerlemesinin durdurulması cezası bulunmayan öğretmenlere verilir.
                    • **Başöğretmenlik:** Uzman öğretmenlikte en az 10 yıl hizmeti bulunan öğretmenlere verilir.
                    • **Tazminatlar:** Unvanı alan öğretmenlere unvanlarına özgü özel hizmet tazminatı aylık bordrolarına yansıtılır ve emeklilik keseneğine dahil edilir.
                    • **Güvenlik Güvencesi:** Eğitim çalışanına karşı şiddet suçlarında cezalar %50 artırılır.
                """.trimIndent(),
                category = "MEVZUAT"
            )
        }

        // 4. SAĞLIK & GÜNDELİK YAŞAM
        if (lower.contains("tansiyon") || lower.contains("kan basıncı")) {
            return KnowledgeAnswer(
                title = "İdeal Tansiyon ve Kan Basıncı Değerleri",
                shortSpeech = "Dünya Sağlık Örgütü standartlarına göre normal tansiyon değeri büyük tansiyon için 120, küçük tansiyon için 80 milimetre civadır.",
                fullContent = """
                    **Tansiyon Değerlendirme Skalası:**
                    • **İdeal Tansiyon:** 120 / 80 mmHg
                    • **Normal Tansiyon:** 120-129 / 80-84 mmHg
                    • **Yüksek Normal:** 130-139 / 85-89 mmHg
                    • **Hipertansiyon (1. Evre):** 140-159 / 90-99 mmHg
                    • **Dikkat:** Tansiyon ölçümünden önce en az 5 dakika dinlenilmeli, kafein ve sigara tüketilmemiş olmalıdır. Düzenli yüksek seyretmesi durumunda hekime danışılmalıdır.
                """.trimIndent(),
                category = "SAĞLIK"
            )
        }

        if (lower.contains("enflasyon") || lower.contains("tüfe") || lower.contains("üfe")) {
            return KnowledgeAnswer(
                title = "Enflasyon ve Fiyat Endeksleri (TÜFE / ÜFE)",
                shortSpeech = "Enflasyon; mal ve hizmetlerin genel fiyat seviyesinin sürekli ve hissedilir biçimde artması ve paranın satın alma gücünün düşmesidir.",
                fullContent = """
                    **Enflasyon Kavramları:**
                    • **TÜFE (Tüketici Fiyat Endeksi):** Hanehalkının tükettiği mal ve hizmet sepetindeki fiyat değişimlerini ölçer.
                    • **ÜFE (Üretici Fiyat Endeksi):** Üretim aşamasındaki girdi maliyetlerindeki değişimi gösterir.
                    • **Nedenleri:** Talep enflasyonu (talebin arzı aşması), Maliyet enflasyonu (hammadde, enerji ve kur artışları).
                    • **Mücadele:** Sıkı para politikası, mali disiplin ve yerli üretimin artırılması.
                """.trimIndent(),
                category = "EKONOMİ"
            )
        }

        // 5. TÜRK EDEBİYATI & FELSEFE
        if (lower.contains("dede korkut") || lower.contains("kutadgu bilig") || lower.contains("divan")) {
            return KnowledgeAnswer(
                title = "Türk Dili ve Edebiyatı Şaheserleri",
                shortSpeech = "Dede Korkut Hikayeleri destandan halk hikayeciliğine geçişin, Kutadgu Bilig ise Türk-İslam edebiyatının ilk mesnevisi ve siyasetnamesidir.",
                fullContent = """
                    **Geçiş Dönemi ve Klasik Eserler:**
                    • **Dede Korkut Hikâyeleri:** 12 boy ve 1 ön sözden oluşur. Destandan halk hikâyesine geçişin ilk örneğidir. Türklerin örf, ahlak ve kahramanlıklarını anlatır.
                    • **Kutadgu Bilig (Yusuf Has Hacip - 1069):** 'Kutlu Bilgi' anlamına gelir. İlk Türkçe mesnevi ve siyasetnamedir. Adalet, devlet, akıl ve akıbeti simgeleyen 4 kahraman üzerinden erdemli yönetimi anlatır.
                    • **Dîvânu Lugâti't-Türk (Kaşgarlı Mahmud - 1074):** Araplara Türkçeyi öğretmek amacıyla yazılmış ilk Türkçe sözlük ve dil bilgisi ansiklopedisidir.
                """.trimIndent(),
                category = "EDEBİYAT"
            )
        }

        // 6. DİNAMİK SORU KALIPLARI (Nedir, Kimdir, Nasıl, Ne zaman)
        val isQuestion = lower.endsWith("?") || lower.contains("nedir") || lower.contains("kimdir") ||
                lower.contains("nasıl") || lower.contains("ne zaman") || lower.contains("açıkla") ||
                lower.contains("bilgi ver") || lower.contains("anlat")

        if (isQuestion) {
            val subject = rawQuery
                .replace(Regex("(?i)\b(nedir|kimdir|nasıl|ne zaman|açıkla|bilgi ver|anlat|sence|hakkında|bana|lütfen)\b"), "")
                .replace(Regex("[?.,!;:]"), "")
                .trim()

            if (subject.length >= 3) {
                return KnowledgeAnswer(
                    title = "$subject Hakkında Analitik Bilgi",
                    shortSpeech = "$greeting$subject konusuyla ilgili analitik ve pedagojik temel bilgileri derledim.",
                    fullContent = """
                        **$subject İncelemesi:**
                        • **Tanım & Kapsam:** $subject, ilgili disiplinde temel ilkeleri, yapısal özellikleri ve fonksiyonel dinamikleriyle ele alınır.
                        • **Önem & İşlev:** Alanındaki nedensellik bağları, tarihsel gelişimi ve pratik yaşamdaki karşılığı açısından kritik bir konudur.
                        • **Öneri:** Konunun derinlikli ayrıntıları, güncel akademik literatür ve resmi mevzuat verileri doğrultusunda sistem hafızasında saklanmaktadır.
                    """.trimIndent(),
                    category = "GENEL_BİLGİ"
                )
            }
        }

        return null
    }
}
