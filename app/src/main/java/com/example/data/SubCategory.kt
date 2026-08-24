package com.example.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SubCategory(
    val id: String,
    val categoryKey: String, // e.g. "HEALTH", "LEGAL", or custom category name
    val name: String,
    val iconName: String = "Notifications",
    val defaultDescription: String = "",
    val defaultTimes: List<String> = listOf("09:00"),
    val suggestedInterval: String = "DAILY", // ONCE, DAILY, PRAYER_BASED, RANGE
    val isCustom: Boolean = false
)

object DefaultSubCategories {
    fun getDefaultsForCategory(categoryKey: String): List<SubCategory> {
        return when (categoryKey) {
            "BILLS_CARDS" -> listOf(
                SubCategory("b_water", "BILLS_CARDS", "Su Faturası", "WaterDrop", "Aylık su sayacı ve tüketim bedeli son ödeme tarihi.", listOf("10:00"), "ONCE"),
                SubCategory("b_electric", "BILLS_CARDS", "Elektrik Faturası", "Bolt", "Aylık elektrik enerjisi tüketimi ve son ödeme günü.", listOf("10:30"), "ONCE"),
                SubCategory("b_gas", "BILLS_CARDS", "Doğalgaz / Isınma Faturası", "LocalFireDepartment", "Kış/yaz doğalgaz faturası ve sayaç okuma son günü.", listOf("11:00"), "ONCE"),
                SubCategory("b_home_phone", "BILLS_CARDS", "Ev / İş Telefonu Faturası", "Phone", "Sabit hat ve ofis santral aylık abonelik faturası.", listOf("11:30"), "ONCE"),
                SubCategory("b_mobile_phone", "BILLS_CARDS", "Cep Telefonu Faturası", "Smartphone", "Aylık mobil hat tarifesi, ek internet ve paket ödemesi.", listOf("12:00"), "ONCE"),
                SubCategory("b_internet", "BILLS_CARDS", "İnternet & Fiber / TV Paketi", "Wifi", "Ev/ofis fiber internet, Dijital TV platform abonelikleri.", listOf("12:30"), "ONCE"),
                SubCategory("b_credit_card", "BILLS_CARDS", "Kredi Kartı Hesap Özeti / Ekstre", "CreditCard", "Kredi kartı ekstre son ödeme günü ve asgari tutar kontrolü.", listOf("14:00"), "ONCE"),
                SubCategory("b_home_insurance", "BILLS_CARDS", "Ev / Konut Sigortası", "HomeWork", "Yıllık konut, yangın ve eşya sigortası poliçe yenilemesi.", listOf("10:00"), "ONCE"),
                SubCategory("b_dask", "BILLS_CARDS", "DASK (Zorunlu Deprem Sigortası)", "Domain", "Yıllık zorunlu deprem sigortası poliçesi yenileme tarihi.", listOf("10:30"), "ONCE"),
                SubCategory("b_env_tax", "BILLS_CARDS", "Çevre Temizlik Vergisi (ÇTV)", "DeleteOutline", "Belediye çevre temizlik harcı ve vergisi ödeme günü.", listOf("11:00"), "ONCE"),
                SubCategory("b_property_tax", "BILLS_CARDS", "Emlak Vergisi (1. & 2. Taksit)", "AccountBalance", "Mayıs ve Kasım ayı belediye emlak vergisi taksitleri.", listOf("11:30"), "ONCE"),
                SubCategory("b_dues", "BILLS_CARDS", "Site & Apartman Aidatı", "ReceiptLong", "Aylık bina aidatı, yakıt ve ortak gider ödemesi.", listOf("09:30"), "DAILY")
            )
            "MY_CAR" -> listOf(
                SubCategory("car_traffic_ins", "MY_CAR", "Zorunlu Trafik Sigortası", "Security", "Yıllık zorunlu trafik sigortası poliçe bitiş ve yenileme tarihi.", listOf("10:00"), "ONCE"),
                SubCategory("car_kasko", "MY_CAR", "Kasko Sigortası Yenileme", "Shield", "Genişletilmiş araç kasko poliçesi bitiş ve yenileme günü.", listOf("10:30"), "ONCE"),
                SubCategory("car_muayene", "MY_CAR", "TÜVTÜRK Araç Muayenesi", "DirectionsCar", "Periyodik araç muayene istasyon randevusu ve egzoz kontrolü.", listOf("09:00"), "ONCE"),
                SubCategory("car_bakim", "MY_CAR", "Periyodik Bakım & Yağ Değişimi", "Build", "Motor yağı, filtreler, buji ve fren balata kontrolü.", listOf("10:00"), "ONCE"),
                SubCategory("car_mtv_1", "MY_CAR", "MTV 1. Taksit (Ocak Ayı)", "Payments", "Motorlu Taşıtlar Vergisi 1. taksit son ödeme günü.", listOf("11:00"), "ONCE"),
                SubCategory("car_mtv_2", "MY_CAR", "MTV 2. Taksit (Temmuz Ayı)", "Payments", "Motorlu Taşıtlar Vergisi 2. taksit son ödeme günü.", listOf("11:00"), "ONCE"),
                SubCategory("car_emisyon", "MY_CAR", "Egzoz Gazı Emisyon Pulu", "Eco", "Periyodik egzoz emisyon ölçümü ve onay süresi.", listOf("11:30"), "ONCE"),
                SubCategory("car_lastik", "MY_CAR", "Kışlık / Yazlık Lastik Değişimi", "TireRepair", "Mevsimlik lastik montajı, balans ve hava basınç kontrolü.", listOf("10:00"), "ONCE"),
                SubCategory("car_aku", "MY_CAR", "Akü Kontrolü & Değişimi", "BatteryChargingFull", "Akü voltaj seviyesi, kutup başı temizliği ve şarj kontrolü.", listOf("14:00"), "ONCE"),
                SubCategory("car_klima", "MY_CAR", "Klima Bakımı & Polen Filtresi", "AcUnit", "Araç klima gazı dolumu ve antibakteriyel hava kanalı temizliği.", listOf("15:00"), "ONCE")
            )
            "HEALTH" -> listOf(
                SubCategory("h_water", "HEALTH", "Su İçme Hatırlatıcısı", "WaterDrop", "Günde en az 2-2.5 litre su içmeyi unutmayın.", listOf("08:30", "11:30", "14:30", "17:30", "20:30"), "DAILY"),
                SubCategory("h_medicine", "HEALTH", "İlaç & Doz Alma", "Medication", "Doktorun reçete ettiği ilaçlarınızı tam saatinde alın.", listOf("08:00", "13:00", "19:00", "22:00"), "DAILY"),
                SubCategory("h_bp", "HEALTH", "Tansiyon Ölçüm & Kayıt", "MonitorHeart", "Sabah ve akşam dinlenik halde tansiyonunuzu ölçün.", listOf("09:00", "20:00"), "DAILY"),
                SubCategory("h_glucose", "HEALTH", "Kan Şekeri (Diyabet) Takibi", "Bloodtype", "Açlık ve tokluk şekerinizi düzenli takip edin.", listOf("07:30", "12:30", "19:30"), "DAILY"),
                SubCategory("h_doctor", "HEALTH", "Doktor & Hastane Randevusu", "LocalHospital", "Muayene saatinden 15 dakika önce hastanede olun.", listOf("10:00"), "ONCE"),
                SubCategory("h_eye", "HEALTH", "Göz Damlası & Bakımı", "Visibility", "Göz damlalarınızı hijyenik şekilde uygulayın.", listOf("08:00", "14:00", "20:00"), "DAILY"),
                SubCategory("h_teeth", "HEALTH", "Diş Fırçalama & Bakım", "CleanHands", "Günde en az iki defa 2 dakika fırçalayın.", listOf("08:30", "22:30"), "DAILY"),
                SubCategory("h_walk", "HEALTH", "Yürüyüş & Egzersiz Saati", "DirectionsWalk", "Günde 30-45 dakika hafif tempolu yürüyüş.", listOf("18:00"), "DAILY"),
                SubCategory("h_vaccine", "HEALTH", "Aşı & İğne Saati", "Vaccines", "Periyodik aşı ve enjeksiyon uygulaması.", listOf("11:00"), "ONCE"),
                SubCategory("h_lab", "HEALTH", "Kan Tahlili & Rapor Gösterme", "Biotech", "Tahlil sonuçlarını doktora gösterme zamanı.", listOf("14:00"), "ONCE"),
                SubCategory("h_screen", "HEALTH", "Ekran Molası (20-20-20)", "Visibility", "Uzağa bakıp gözlerinizi dinlendirin.", listOf("14:00"), "DAILY"),
                SubCategory("h_checkup", "HEALTH", "Genel Sağlık Taraması", "LocalHospital", "Yıllık check-up, göz ve diş randevusu.", listOf("10:00"), "ONCE")
            )
            "LEGAL" -> listOf(
                SubCategory("l_court", "LEGAL", "Duruşma & Mahkeme Günü", "Gavel", "Duruşma salonu önünde hazır bulunun.", listOf("09:30"), "ONCE"),
                SubCategory("l_notary", "LEGAL", "Noter & Vekaletname İşlemi", "FactCheck", "Gerekli evrak ve kimlik asıllarını yanınıza alın.", listOf("11:00"), "ONCE"),
                SubCategory("l_tax", "LEGAL", "Vergi & SGK Beyannamesi", "AccountBalance", "Son ödeme ve beyan tarihini kaçırmayın.", listOf("14:00"), "ONCE"),
                SubCategory("l_id_renew", "LEGAL", "Kimlik / Ehliyet / Pasaport Randevusu", "Badge", "Nüfus müdürlüğü randevu saati.", listOf("10:30"), "ONCE"),
                SubCategory("l_title_deed", "LEGAL", "Tapu & Kadastro Devir İşlemi", "HomeWork", "Tapu harç makbuzları ve randevu evrakları.", listOf("13:30"), "ONCE"),
                SubCategory("l_municipality", "LEGAL", "Belediye & İmar Başvurusu", "Domain", "Dilekçe ve evrak teslim zamanı.", listOf("10:00"), "ONCE"),
                SubCategory("l_contract", "LEGAL", "Sözleşme & İmza Randevusu", "Draw", "Sözleşme maddelerini kontrol edip imzalayın.", listOf("15:00"), "ONCE"),
                SubCategory("l_passport", "LEGAL", "Pasaport / Vize Yenileme", "Flight", "Belge geçerlilik süresini kontrol edin.", listOf("11:00"), "ONCE"),
                SubCategory("l_maintenance", "LEGAL", "Ev Bakımı (Kombi vs.)", "Build", "Kış/yaz öncesi cihaz bakımlarını yaptırın.", listOf("12:00"), "ONCE")
            )
            "FINANCE" -> listOf(
                SubCategory("f_card", "FINANCE", "Kredi Kartı Son Ödeme Günü", "CreditCard", "Asgari veya toplam borç ödemesi.", listOf("12:00"), "ONCE"),
                SubCategory("f_bills", "FINANCE", "Fatura Ödeme (Elektrik/Su/Gaz/Net)", "ReceiptLong", "Otomatik ödeme kontrolü ve manuel ödemeler.", listOf("11:00"), "ONCE"),
                SubCategory("f_rent", "FINANCE", "Kira & Site Aidatı Günü", "AccountBalanceWallet", "Ev sahibi veya bina yönetimi hesap transferi.", listOf("10:00"), "ONCE"),
                SubCategory("f_market", "FINANCE", "Haftalık Market & Pazar Alışverişi", "ShoppingCart", "İhtiyaç listesini kontrol ederek alışverişe çıkın.", listOf("17:00"), "DAILY"),
                SubCategory("f_loan", "FINANCE", "Banka Kredisi / Taksit Ödemesi", "Payments", "Aylık sabit kredi taksit tutarı.", listOf("13:00"), "ONCE"),
                SubCategory("f_gold", "FINANCE", "Borsa, Döviz & Altın Takibi", "TrendingUp", "Piyasa açılış/kapanış kontrolü.", listOf("09:30", "17:30"), "DAILY"),
                SubCategory("f_subs", "FINANCE", "Abonelik & Servis Kontrolü", "Event", "Kullanılmayan abonelikleri gözden geçirin.", listOf("19:00"), "ONCE"),
                SubCategory("f_bulk", "FINANCE", "Toplu Market & Stok", "ShoppingCart", "Bozulmayan ürünlerin aylık stoklanması.", listOf("11:00"), "ONCE")
            )
            "DAILY" -> listOf(
                SubCategory("d_cleaning", "DAILY", "Ev Temizliği & Çamaşır / Ütü", "CleaningServices", "Haftalık oda temizliği ve kıyafet bakımı.", listOf("10:00"), "DAILY"),
                SubCategory("d_plants", "DAILY", "Çiçek & Balkon Bitkileri Sulama", "Yard", "Toprağın nemini kontrol edip sulayın.", listOf("08:00", "19:00"), "DAILY"),
                SubCategory("d_pet", "DAILY", "Evcil Hayvan Besleme & Gezdirme", "Pets", "Yemek, taze su ve yürüyüş saati.", listOf("07:30", "18:30"), "DAILY"),
                SubCategory("d_trash", "DAILY", "Çöp & Geri Dönüşüm Çıkarma", "DeleteSweep", "Akşam çöp çıkarma saati.", listOf("20:00"), "DAILY"),
                SubCategory("d_cargo", "DAILY", "Kargo / Sipariş Teslimat Bekleme", "LocalShipping", "Kurye teslimat kodu ve zili takip edin.", listOf("13:00"), "ONCE"),
                SubCategory("d_cooking", "DAILY", "Yemek Pişirme & Menü Hazırlığı", "Restaurant", "Akşam yemeği hazırlık başlangıcı.", listOf("17:30"), "DAILY"),
                SubCategory("d_detox", "DAILY", "Dijital Detoks Saati", "Phone", "Uyumadan 1 saat önce telefonu bırakın.", listOf("22:30"), "DAILY"),
                SubCategory("d_plan", "DAILY", "Günün Özeti & Planlama", "MenuBook", "Ertesi günün yapılacaklarını planlayın.", listOf("21:00"), "DAILY"),
                SubCategory("d_sheets", "DAILY", "Nevresim & Çarşaf Değişimi", "Bed", "Hijyen için yatak örtülerini değiştirin.", listOf("10:00"), "ONCE")
            )
            "SOCIAL" -> listOf(
                SubCategory("s_birthday", "SOCIAL", "Doğum Günü Kutlaması", "Cake", "Tebrik mesajı atın veya hediye verin.", listOf("10:00"), "ONCE"),
                SubCategory("s_anniversary", "SOCIAL", "Yıldönümü Kutlaması", "Favorite", "Özel akşam planı ve hediyeleşme.", listOf("19:00"), "ONCE"),
                SubCategory("s_call_elders", "SOCIAL", "Akrabaları & Büyükleri Arama", "PhoneInTalk", "Hal hatır sormak için telefon edin.", listOf("18:00"), "DAILY"),
                SubCategory("s_meetup", "SOCIAL", "Dost & Aile Buluşması", "Groups", "Kafede veya evde kahve/yemek randevusu.", listOf("16:00"), "ONCE"),
                SubCategory("s_gift", "SOCIAL", "Hediye & Çiçek Siparişi", "CardGiftcard", "Zamanında teslimat için erken sipariş verin.", listOf("11:30"), "ONCE")
            )
            "VEHICLE" -> listOf(
                SubCategory("v_inspection", "VEHICLE", "TÜVTÜRK Araç Muayenesi", "DirectionsCar", "İstasyon randevu saati ve ruhsat kontrolü.", listOf("09:00"), "ONCE"),
                SubCategory("v_insurance", "VEHICLE", "Trafik Sigortası & Kasko Yenileme", "Security", "Poliçe bitiş tarihinden önce yenileyin.", listOf("10:00"), "ONCE"),
                SubCategory("v_oil", "VEHICLE", "Motor Yağı & Periyodik Bakım", "Build", "Kilometre bakımı, filtreler ve fren kontrolü.", listOf("10:30"), "ONCE"),
                SubCategory("v_fuel", "VEHICLE", "Yakıt Alma & Depo Kontrolü", "LocalGasStation", "Uygun istasyondan yakıt ikmali.", listOf("18:00"), "DAILY"),
                SubCategory("v_tire", "VEHICLE", "Lastik Değişimi & Hava Basıncı", "TireRepair", "Mevsimlik lastik değişimi veya PSI ölçümü.", listOf("11:00"), "ONCE"),
                SubCategory("v_ticket", "VEHICLE", "Uçak / Tren / Otobüs Sefer Saati", "ConfirmationNumber", "Kalkış saatinden önce terminalde olun.", listOf("07:00"), "ONCE")
            )
            "CAREER" -> listOf(
                SubCategory("c_meeting", "CAREER", "İş Toplantısı / Video Konferans", "VideoCameraFront", "Toplantı notları ve gündem maddeleri.", listOf("10:00", "14:30"), "DAILY"),
                SubCategory("c_deadline", "CAREER", "Proje / Görev Teslim Tarihi", "AssignmentTurnedIn", "Son kontrolleri yapıp teslim edin.", listOf("17:00"), "ONCE"),
                SubCategory("c_email", "CAREER", "Önemli E-posta / Teklif Gönderimi", "Mail", "Teklif dosyasını ekleyip yanıtlayın.", listOf("11:00"), "DAILY"),
                SubCategory("c_customer", "CAREER", "Müşteri / Satış Görüşmesi", "Handshake", "Sunum ve demo saati.", listOf("15:00"), "ONCE")
            )
            "PERSONAL" -> listOf(
                SubCategory("p_exam", "PERSONAL", "Ders Çalışma & Sınav Tarihi", "MenuBook", "Konu tekrarı ve test çözümü.", listOf("19:00", "21:00"), "DAILY"),
                SubCategory("p_reading", "PERSONAL", "Günlük Kitap Okuma (30 Dk)", "AutoStories", "Günün sakin anında 30 sayfa kitap.", listOf("21:30"), "DAILY"),
                SubCategory("p_language", "PERSONAL", "Yabancı Dil Pratiği & Kelime", "Translate", "Kelime kartları ve dinleme egzersizi.", listOf("20:00"), "DAILY"),
                SubCategory("p_meditation", "PERSONAL", "Meditasyon & Nefes Egzersizi", "SelfImprovement", "Zihni dinlendirme ve derin nefes.", listOf("07:00", "22:00"), "DAILY"),
                SubCategory("p_digicomp", "PERSONAL", "Dijital Temizlik & Yedek", "Sync", "Cihazları yedekleme, medyaları temizleme.", listOf("18:00"), "ONCE"),
                SubCategory("p_passwords", "PERSONAL", "Şifre & Güvenlik Check-up", "VpnKey", "Önemli hesap şifrelerini güncelleyin.", listOf("20:00"), "ONCE"),
                SubCategory("p_selfcare", "PERSONAL", "Kişisel Bakım Günü", "Spa", "Haftada bir gün cilt bakımı ve rahatlama.", listOf("15:00"), "ONCE")
            )
            "LOCATION" -> listOf(
                SubCategory("loc_travel", "LOCATION", "Kayıtlı Konuma Seyahat", "Navigation", "Trafik durumuna göre yola çıkış.", listOf("08:30"), "ONCE"),
                SubCategory("loc_break", "LOCATION", "Yol Üstü Mola Hatırlatıcısı", "Coffee", "2 saatte bir 15 dakika mola verin.", listOf("12:00", "16:00"), "DAILY")
            )
            "FAMILY_BUDGET" -> listOf(
                SubCategory("fb_grocery", "FAMILY_BUDGET", "Mutfak & Market Harcaması", "ShoppingCart", "Aylık veya haftalık mutfak bütçesi kontrolü.", listOf("19:00"), "DAILY"),
                SubCategory("fb_kids", "FAMILY_BUDGET", "Çocuk Masrafları & Okul", "Face", "Okul, servis, kırtasiye veya harçlık giderleri.", listOf("08:00"), "ONCE"),
                SubCategory("fb_savings", "FAMILY_BUDGET", "Aylık Birikim & Tasarruf", "Savings", "Gelirinizin belirli bir kısmını birikime ayırın.", listOf("10:00"), "ONCE"),
                SubCategory("fb_entertainment", "FAMILY_BUDGET", "Eğlence & Dışarıda Yemek", "Restaurant", "Hafta sonu aktiviteleri ve sosyal harcamalar.", listOf("18:00"), "ONCE"),
                SubCategory("fb_unexpected", "FAMILY_BUDGET", "Acil Durum & Beklenmedik Gider", "Warning", "Beklenmedik masraflar için ayrılan fon kontrolü.", listOf("12:00"), "ONCE")
            )
            else -> listOf(
                SubCategory("gen_alarm", categoryKey, "Genel Alarm & Hatırlatma", "Alarm", "Belirttiğiniz saatte uyarı verir.", listOf("09:00"), "ONCE"),
                SubCategory("gen_todo", categoryKey, "Yapılacaklar (To-Do) Maddesi", "Checklist", "Önemli görev tamamlama.", listOf("12:00"), "DAILY")
            )
        }
    }
}
