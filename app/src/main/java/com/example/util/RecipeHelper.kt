package com.example.util

import android.content.Context
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
            append("🎬 **Videolu Anlatım:** Dilerseniz _'YouTube'da videolu tarifini aç'_ diyerek yapılış videosunu hemen izleyebilirsiniz!")
        }

        val speech = "$patronPrefix, sizin için ${recipe.title} tarifini hazırladım. ${recipe.tips.take(120)} Dilerseniz videosunu YouTube'da hemen açabilirim."

        return Pair(text, speech)
    }
}
