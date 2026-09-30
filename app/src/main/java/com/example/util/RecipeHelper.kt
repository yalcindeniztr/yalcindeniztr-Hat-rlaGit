package com.example.util

import android.content.Context
import com.example.data.AppDatabase
import com.example.data.ReminderEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RecipeItem(
    val title: String,
    val category: String,
    val prepTime: String,
    val cookTime: String,
    val ingredients: List<String>,
    val steps: List<String>,
    val tips: String,
    val youtubeQuery: String
)

object RecipeHelper {

    private val recipes: List<RecipeItem> = listOf(
        RecipeItem(
            title = "Geleneksel Kuru Fasulye",
            category = "Ana Yemek / Bakliyat",
            prepTime = "15 Dk (1 Gece Islatma)",
            cookTime = "45 Dk",
            ingredients = listOf(
                "2 su bardağı kuru fasulye (önceden ıslatılmış)",
                "1 adet büyük boy soğan",
                "1 yemek kaşığı domates salçası",
                "1 tatlı kaşığı biber salçası",
                "200g kuşbaşı dana eti veya sucuk (isteğe bağlı)",
                "3 yemek kaşığı tereyağı veya sıvı yağ",
                "1 çay kaşığı toz kırmızı biber, kimyon, tuz"
            ),
            steps = listOf(
                "Tencerede tereyağını eritin, yemeklik doğranmış soğanları pembeleşinceye kadar kavurun.",
                "Eti ekleyip suyunu salıp çekene kadar soteleyin. Salçaları ekleyip kokusu çıkana kadar kavurun.",
                "Süzülen fasulyeleri tencereye ekleyip 2-3 dakika karıştırın.",
                "Üzerini 2 parmak geçecek kadar sıcak su ilave edin, baharatları serpin.",
                "Kısık ateşte fasulyeler lokum gibi yumuşayana kadar yaklaşık 40-45 dakika pişirin."
            ),
            tips = "Fasulyenin gazını almak için haşlama suyuna bir tutam kimyon ekleyebilirsiniz.",
            youtubeQuery = "Kuru Fasulye Tarifi Nefis Yemek Tarifleri"
        ),
        RecipeItem(
            title = "Fırında Hakiki Karnıyarık",
            category = "Ana Yemek / Sebze",
            prepTime = "20 Dk",
            cookTime = "35 Dk",
            ingredients = listOf(
                "6 adet orta boy patlıcan",
                "300g orta yağlı dana kıyma",
                "2 adet kuru soğan",
                "2 adet yeşil biber, 1 adet domates",
                "2 diş sarımsak",
                "1 yemek kaşığı domates salçası, karabiber, pul biber, tuz",
                "Kızartmak için sıvı yağ"
            ),
            steps = listOf(
                "Patlıcanları alacalı soyup tuzlu suda 20 dakika bekletin, kurulayıp kızgın yağda hafifçe kızartın.",
                "Tavada soğan, sarımsak, kıyma ve biberleri kavurun. Salça ve baharatları ekleyerek iç harcı hazırlayın.",
                "Fırın tepsisine dizdiğiniz patlıcanların ortasını bıçakla yarıp tatlı kaşığıyla hafifçe açın.",
                "Hazırladığınız kıymalı harcı içlerine paylaştırın, üzerlerine domates ve biber dilimleri koyun.",
                "Salçalı sıcak su sosunu tepsiye döküp 190 derece fırında 25-30 dakika pişirin."
            ),
            tips = "Patlıcanların fazla yağ çekmemesi için kızartmadan önce havlu kağıtla iyice kurulayın.",
            youtubeQuery = "Karnıyarık Tarifi Nasıl Yapılır"
        ),
        RecipeItem(
            title = "Lokanta Usulü Süzme Mercimek Çorbası",
            category = "Çorbalar",
            prepTime = "10 Dk",
            cookTime = "25 Dk",
            ingredients = listOf(
                "1.5 su bardağı kırmızı mercimek",
                "1 adet orta boy patates, 1 adet havuç, 1 adet soğan",
                "1 yemek kaşığı un, 2 yemek kaşığı tereyağı",
                "6 su bardağı sıcak su veya et suyu",
                "Tuz, karabiber, zerdeçal (renk için)",
                "Üzeri için: Tereyağı ve pul biber"
            ),
            steps = listOf(
                "Tencerede tereyağında soğanı ve unu kokusu çıkana kadar 2 dakika kavurun.",
                "Doğranmış havuç, patates ve yıkanmış mercimeği ekleyin.",
                "Sıcak suyu döküp sebzeler yumuşayana kadar orta ateşte kaynatın.",
                "Pürüzsüz kıvama gelene kadar el blenderından geçirin.",
                "Üzerine kızdırılmış tereyağlı pul biber sosunu gezdirip limon ile servis yapın."
            ),
            tips = "İçine ekleyeceğiniz çeyrek çay kaşığı zerdeçal, lokantalardaki o iştah açıcı altın sarısı rengi verir.",
            youtubeQuery = "Lokanta Usulü Mercimek Çorbası Tarifi"
        ),
        RecipeItem(
            title = "Usta İşi Anne Köftesi",
            category = "Ana Yemek / Et",
            prepTime = "15 Dk (30 Dk Dinlendirme)",
            cookTime = "15 Dk",
            ingredients = listOf(
                "500g dana döş kıyma",
                "1 adet rendelenip suyu sıkılmış soğan",
                "1 adet yumurta",
                "3 yemek kaşığı galeta unu veya bayat ekmek içi",
                "2 diş ezilmiş sarımsak",
                "1 tatlı kaşığı kimyon, 1 çay kaşığı karabiber, 1 tatlı kaşığı tuz",
                "Yarım demet ince kıyılmış maydanoz"
            ),
            steps = listOf(
                "Tüm malzemeleri derin bir kaba alıp en az 10 dakika boyunca özleşene kadar yoğurun.",
                "Harcı streçleyip buzdolabında en az 30 dakika dinlendirin.",
                "Ceviz büyüklüğünde parçalar koparıp avuç içinde yassı köfte şekli verin.",
                "İyice ısınmış döküm tavada veya ızgarada her iki tarafını da mühürleyerek pişirin."
            ),
            tips = "Köfte harcına ekleyeceğiniz 2 yemek kaşığı maden suyu köftenin içinin yumuşacık ve sulu kalmasını sağlar.",
            youtubeQuery = "En Lezzetli Anne Köftesi Tarifi"
        ),
        RecipeItem(
            title = "Geleneksel Fırın Sütlaç",
            category = "Tatlılar",
            prepTime = "10 Dk",
            cookTime = "35 Dk",
            ingredients = listOf(
                "1 litre tam yağlı süt",
                "1 çay bardağı pirinç",
                "1 su bardağı toz şeker",
                "2 yemek kaşığı nişasta (yarım çay bardağı sütle açılmış)",
                "1 paket vanilya",
                "1 adet yumurta sarısı (üzerinin kızarması için)"
            ),
            steps = listOf(
                "Pirinçleri 2 su bardağı suda suyunu çekene kadar haşlayın.",
                "Sütü ve şekeri tencereye ekleyip kaynamaya bırakın.",
                "Ayrı bir kapta sütle açılmış nişasta ve yumurta sarısını çırpıp ılıtarak tencereye ekleyin.",
                "Koyulaşana kadar 5 dakika karıştırıp vanilyayı ilave edin.",
                "Güveç kaplarına paylaştırıp fırın tepsisine soğuk su dökerek 200 derece fırının üst ızgarasında üzeri kızarana kadar pişirin."
            ),
            tips = "Tepsinin tabanına soğuk su koymak güveçlerin tabanının kurumasını önler.",
            youtubeQuery = "Fırın Sütlaç Tarifi Tam Kıvamında"
        ),
        RecipeItem(
            title = "Kusursuz Türk Menemeni",
            category = "Kahvaltı / Pratik",
            prepTime = "5 Dk",
            cookTime = "10 Dk",
            ingredients = listOf(
                "3 adet sulu tarla domatesi (kabukları soyulmuş, küp doğranmış)",
                "3 adet sivri yeşil biber",
                "3 adet köy yumurtası",
                "2 yemek kaşığı tereyağı",
                "Tuz, pul biber, isteğe göre kaşar peyniri"
            ),
            steps = listOf(
                "Bakır tavada tereyağını eritin ve doğranmış biberleri hafif kavurun.",
                "Doğranmış domatesleri ekleyin ve tavanın kapağını kapatıp domatesler eriyene kadar pişirin.",
                "Yumurtaları tavanın içine kırın, sarılarını hafifçe dağıtıp beyazlarıyla kaynaşmasını sağlayın.",
                "Yumurtalar çok kurumadan sulu kıvamdayken ocaktan alın ve sıcak servis yapın."
            ),
            tips = "Menemeni ocaktan almadan 1 dakika önce ateşi kapatın; tavanın kendi sıcağıyla yumurta tam kıvamına ulaşır.",
            youtubeQuery = "Hakiki Menemen Nasıl Yapılır"
        )
    )

    fun findRecipeOrRecommend(query: String): RecipeItem {
        val lower = query.lowercase(Locale("tr", "TR"))
        return recipes.firstOrNull { item ->
            val titleLower = item.title.lowercase(Locale("tr", "TR"))
            titleLower.contains(lower) || lower.contains(titleLower.take(6)) ||
            (lower.contains("fasulye") && titleLower.contains("fasulye")) ||
            (lower.contains("karnıyarık") && titleLower.contains("karnıyarık")) ||
            (lower.contains("çorba") && titleLower.contains("çorba")) ||
            (lower.contains("köfte") && titleLower.contains("köfte")) ||
            (lower.contains("sütlaç") && titleLower.contains("sütlaç")) ||
            (lower.contains("tatlı") && titleLower.contains("sütlaç")) ||
            (lower.contains("menemen") && titleLower.contains("menemen"))
        } ?: recipes.random()
    }

    fun findRecipeByIngredients(query: String): Pair<RecipeItem, List<String>> {
        val lower = query.lowercase(Locale("tr", "TR"))
        var bestRecipe = recipes.first()
        var maxScore = -1
        var bestMissing = listOf<String>()

        for (recipe in recipes) {
            var score = 0
            val missing = mutableListOf<String>()
            for (ing in recipe.ingredients) {
                val ingLower = ing.lowercase(Locale("tr", "TR"))
                val words = ingLower.split(" ").filter { it.length > 2 }
                val hasMatch = words.any { lower.contains(it) }
                if (hasMatch) {
                    score += 2
                } else {
                    missing.add(ing.replace(Regex("""^\d+.*?(adet|su bardağı|tatlı kaşığı|yemek kaşığı|çay kaşığı|litre|paket|demet|gram|g)\s*"""), "").trim())
                }
            }
            if (score > maxScore) {
                maxScore = score
                bestRecipe = recipe
                bestMissing = missing
            }
        }
        return Pair(bestRecipe, bestMissing)
    }

    suspend fun addMissingToShoppingList(context: Context, items: List<String>): Int {
        if (items.isEmpty()) return 0
        val db = AppDatabase.getDatabase(context)
        val now = System.currentTimeMillis()
        val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr", "TR")).format(Date(now))
        var count = 0
        items.take(6).forEach { rawItem ->
            val cleanItem = rawItem.take(40).trim()
            if (cleanItem.isNotBlank()) {
                val rem = ReminderEntity(
                    category = "SHOPPING",
                    title = "🛒 $cleanItem",
                    dueDatetime = dateStr,
                    dueDateMillis = now,
                    customNote = "Yemek tarifi için gereken eksik malzeme.",
                    isFavorite = false,
                    encryptedMetadata = "{}",
                    actionStep = "NOTE_SAVED"
                )
                db.reminderDao().insertReminder(rem)
                count++
            }
        }
        return count
    }

    fun getRecipeBriefing(recipe: RecipeItem, patronPrefix: String): Pair<String, String> {
        val text = buildString {
            append("🍲 **$patronPrefix, İşte Sizin İçin Seçtiğim Tarif: ${recipe.title}**\n")
            append("⏱️ Hazırlık: ${recipe.prepTime} | Pişirme: ${recipe.cookTime} | Kategori: ${recipe.category}\n\n")
            
            append("🛒 **Gerekli Malzemeler:**\n")
            recipe.ingredients.forEach { ing ->
                append(" • $ing\n")
            }
            append("\n👩‍🍳 **Adım Adım Yapılışı:**\n")
            recipe.steps.forEachIndexed { idx, st ->
                append(" ${idx + 1}. $st\n")
            }
            append("\n💡 **Usta Püf Noktası:** ${recipe.tips}\n\n")
            append("🎬 **Videolu Anlatım:** Dilerseniz _'YouTube'da videolu tarifini aç'_ diyerek yapılış videosunu hemen izleyebilirsiniz!\n")
            append("📝 _Eksik malzemeleri alışveriş listenize kaydetmemi isterseniz 'Eksikleri alışveriş listeme ekle' demeniz yeterlidir._")
        }

        val speech = "$patronPrefix, sizin için ${recipe.title} tarifini hazırladım. ${recipe.tips.take(120)} Dilerseniz videosunu YouTube'da hemen açabilirim."

        return Pair(text, speech)
    }

    fun getInteractiveFoodInquiry(patronPrefix: String): Pair<String, String> {
        val text = buildString {
            append("🍽️ **$patronPrefix, Akşam Yemeği İçin Ne Arzu Edersiniz?**\n\n")
            append("Sizin için en uygun lezzeti belirleyebilmem için bana küçük bir ipucu verebilir misiniz?\n\n")
            append("• **Hafif & Pratik:** Sebze yemeği veya lezzetli bir çorba mı istersiniz?\n")
            append("• **Doyurucu:** Fırında tavuk, köfte veya kıymalı/etli bir ana yemek mi olsun?\n")
            append("• **Dolap Analizi:** Veya doğrudan _'Dolabımda tavuk, patates ve kaşar var'_ derseniz, elinizdeki malzemelerle anında nefis bir şef menüsü çıkarabilirim!")
        }
        val speech = "$patronPrefix, bu akşam için hafif bir sebze mi, doyurucu bir et veya tavuk yemeği mi arzu edersiniz? Dilerseniz dolabınızdaki malzemeleri söyleyin, ona göre hemen tarif çıkarayım."
        return Pair(text, speech)
    }

    fun analyzeFridgeAndSuggest(userText: String, patronPrefix: String): Pair<String, String> {
        val clean = userText.lowercase(Locale("tr", "TR"))
        val detectedIngredients = mutableListOf<String>()

        val ingredientMap = mapOf(
            "tavuk" to "Tavuk Eti",
            "kıyma" to "Dana Kıyma",
            "et" to "Kuşbaşı Et",
            "köfte" to "Köfte",
            "mantar" to "Mantar",
            "patates" to "Patates",
            "patlıcan" to "Patlıcan",
            "domates" to "Domates",
            "biber" to "Biber",
            "soğan" to "Kuru Soğan",
            "sarımsak" to "Sarımsak",
            "kaşar" to "Kaşar Peyniri",
            "peynir" to "Peynir",
            "yumurta" to "Yumurta",
            "makarna" to "Makarna",
            "pirinç" to "Pirinç",
            "bulgur" to "Bulgur",
            "kabak" to "Kabak",
            "havuç" to "Havuç",
            "ıspanak" to "Ispanak",
            "fasulye" to "Fasulye",
            "mercimek" to "Mercimek"
        )

        for ((key, label) in ingredientMap) {
            if (clean.contains(key)) {
                detectedIngredients.add(label)
            }
        }

        // Akıllı Eşleştirme & Dinamik Sentez
        val recipeTitle: String
        val cookMinutes: Int
        val steps = mutableListOf<String>()
        val tips: String
        val youtubeSearch: String

        if (clean.contains("tavuk") && clean.contains("mantar")) {
            recipeTitle = "Kremalı Mantarlı Tavuk Sote"
            cookMinutes = 25
            steps.addAll(listOf(
                "Tavuk göğsünü kuşbaşı, mantarları ise jülyen doğrayın.",
                "Geniş tavada zeytinyağında tavukları suyunu salıp çekene kadar soteleyin.",
                "Mantarları ve yemeklik doğranmış soğanı ekleyip yüksek ateşte 5 dakika sotelemeye devam edin.",
                "İsteğe göre sıvı krema veya 1 kaşık un ile 1 bardak süt ekleyin, karabiber ve kekik serpin.",
                "Sos kıvam alıp hafif koyulaşınca ocaktan alın, sıcak sıcak servis yapın."
            ))
            tips = "Mantarların kararmaması ve su salmaması için tavayı iyice ısıtıp yüksek ateşte soteleyin."
            youtubeSearch = "Kremalı Mantarlı Tavuk Sote Nefis Yemek Tarifleri"
        } else if (clean.contains("tavuk") && clean.contains("patates")) {
            recipeTitle = "Fırında Soslu Tavuk ve Patates"
            cookMinutes = 40
            steps.addAll(listOf(
                "Tavukları ve elma dilim doğranmış patatesleri geniş bir kaba alın.",
                "Bir kapta 1 kaşık yoğurt, salça, zeytinyağı, kekik, pul biber ve sarımsağı karıştırarak sosu hazırlayın.",
                "Sosu tavuk ve patateslerin üzerine döküp güzelce harmanlayın.",
                "Fırın tepsisine dizip önceden ısıtılmış 200 derece fırında nar gibi kızarana kadar pişirin."
            ))
            tips = "Sosun içine yarım çay kaşığı bal veya bir tutam kekik eklemek fırında harika karamelize bir lezzet verir."
            youtubeSearch = "Fırında Soslu Tavuk Patates Tarifi"
        } else if (clean.contains("kıyma") && clean.contains("patates")) {
            recipeTitle = "Tencerede Kıymalı Patates Yemeği"
            cookMinutes = 30
            steps.addAll(listOf(
                "Tencerede tereyağında soğanı ve kıymayı suyunu çekene kadar kavurun.",
                "1 yemek kaşığı domates salçası ekleyip kokusu çıkana kadar karıştırın.",
                "Küp küp doğranmış patatesleri tencereye ekleyip 2 dakika çevirin.",
                "Üzerini 1 parmak geçecek kadar sıcak su, tuz ve karabiber ilave edin.",
                "Kapağı kapalı olarak kısık ateşte patatesler yumuşayana kadar yaklaşık 25 dakika pişirin."
            ))
            tips = "Patateslerin dağılmaması için pişirme esnasında fazla karıştırmamaya özen gösterin."
            youtubeSearch = "Kıymalı Patates Yemeği Tarifi"
        } else if (clean.contains("makarna")) {
            recipeTitle = "Özel Soslu Gurme Makarna"
            cookMinutes = 15
            steps.addAll(listOf(
                "Makarnayı bol tuzlu kaynar suda 8-9 dakika 'al dente' kıvamında haşlayın.",
                "Ayrı bir tavada zeytinyağı, ezilmiş sarımsak, domates sosu veya dolaptaki sebzeleri soteleyin.",
                "Haşlama suyundan 1 kepçe ayırıp sosa ekleyin, bu işlem sosun makarnaya mükemmel tutunmasını sağlar.",
                "Süzülen makarnayı sos tavasına aktarıp 1 dakika harmanlayın, üzerine peynir rendeleyerek servis edin."
            ))
            tips = "Makarnayı asla sudan geçirmeyin, üzerindeki nişasta sosu emmesi için en kritik unsurdur."
            youtubeSearch = "Lezzetli Makarna Sosu Tarifi"
        } else if (clean.contains("patlıcan")) {
            recipeTitle = "Zeytinyağlı Hafif Şakşuka & Sebze Kızartması"
            cookMinutes = 25
            steps.addAll(listOf(
                "Patlıcanları alacalı soyup küp küp doğrayın ve tuzlu suda 15 dakika bekletip kurulayın.",
                "Patlıcan ve biberleri hafif yağda veya fırında altın sarısı olana kadar pişirin.",
                "Ayrı tavada rendelenmiş domates, sarımsak, zeytinyağı ve bir tutam şekeri kaynatarak nefis bir sos hazırlayın.",
                "Sebzeleri servis tabağına alıp üzerine sıcak domates sosunu gezdirin."
            ))
            tips = "Domates sosuna ekleyeceğiniz 1 çay kaşığı sirke veya elma sirkesi sosun lezzetini ikiye katlar."
            youtubeSearch = "Hakiki Şakşuka Tarifi Nefis Yemek"
        } else {
            // Genel Malzeme Sentezi
            val mainIngredient = detectedIngredients.firstOrNull() ?: "Sebze & Temel Malzemeler"
            recipeTitle = "Eldeki Malzemelerle: Usta Usulü $mainIngredient Güveci"
            cookMinutes = 30
            steps.addAll(listOf(
                "Elinizdeki malzemeleri (${detectedIngredients.joinToString(", ").ifBlank { "Dolaptaki sebzeler" }}) uygun boyutlarda doğrayın.",
                "Tencerede zeytinyağı ile soğan ve sarımsağı soteleyin, salça ekleyin.",
                "Sırasıyla sert sebzelerden başlayarak malzemeleri ekleyin ve hafifçe çevirin.",
                "Sıcak su ve sevdiğiniz baharatları ilave edip kısık ateşte kapağı kapalı olarak pişmeye bırakın."
            ))
            tips = "Yemeği ocaktan aldıktan sonra 10 dakika dinlendirirseniz lezzetler birbirine tam geçer."
            youtubeSearch = "Pratik Ev Yemekleri Tarifleri"
        }

        val text = buildString {
            append("👨‍🍳 **$patronPrefix, Dolabınızdaki Malzemeleri Analiz Ettim!**\n")
            if (detectedIngredients.isNotEmpty()) {
                append("📋 **Tespit Edilen Malzemeler:** ${detectedIngredients.joinToString(" • ")}\n\n")
            }
            append("✨ **Önerilen Menü: $recipeTitle**\n")
            append("⏱️ Pişirme Süresi: ~$cookMinutes Dakika | Pratik & Lezzetli\n\n")
            append("👩‍🍳 **Adım Adım Hazırlanışı:**\n")
            steps.forEachIndexed { idx, st ->
                append(" ${idx + 1}. $st\n")
            }
            append("\n💡 **Şefin Püf Noktası:** $tips\n\n")
            append("🎬 _'YouTube'da videolu tarifini aç'_ diyerek bu yemeğin hazırlanışını ekranda izleyebilirsiniz.")
        }

        val speech = "$patronPrefix, dolabınızdaki malzemeleri analiz ettim ve sizin için $recipeTitle tarifini hazırladım. $tips Dilerseniz yapılış videosunu YouTube'da hemen açabilirim."

        return Pair(text, speech)
    }
}
