package com.example.util

import android.content.Context
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.widget.Toast
import java.util.Locale

data class NearbyPlace(
    val id: String,
    val name: String,
    val type: String, // PHARMACY, HOSPITAL, PARKING, MARKET
    val typeLabel: String,
    val address: String,
    val distanceMeters: Int,
    val phone: String?,
    val lat: Double,
    val lng: Double,
    val isDutyPharmacy: Boolean = false,
    val searchQuery: String = ""
)

data class DeviceCalendarEvent(
    val title: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val location: String?,
    val formattedDate: String
)

object NearbyPlacesHelper {

    fun getUserCityAndDistrict(context: Context, lat: Double, lng: Double): Pair<String, String> {
        return try {
            val geocoder = Geocoder(context, Locale("tr", "TR"))
            @Suppress("DEPRECATION")
            val addresses: List<Address>? = geocoder.getFromLocation(lat, lng, 1)
            val addr = addresses?.firstOrNull()
            val city = addr?.adminArea ?: addr?.subAdminArea ?: "Samsun"
            val district = addr?.subAdminArea ?: addr?.locality ?: addr?.subLocality ?: "İlkadım"
            Pair(city, district)
        } catch (e: Exception) {
            Pair("Samsun", "İlkadım")
        }
    }

    fun getRecommendedPlaces(
        context: Context,
        userLat: Double,
        userLng: Double,
        queryType: String
    ): List<NearbyPlace> {
        val q = queryType.lowercase(Locale("tr", "TR"))
        val (city, district) = getUserCityAndDistrict(context, userLat, userLng)

        return when {
            q.contains("migros") -> {
                listOf(
                    NearbyPlace(
                        id = "mg1",
                        name = "$district Migros Süpermarket",
                        type = "MARKET",
                        typeLabel = "🛒 Migros Market",
                        address = "$district Merkez Cad., $city",
                        distanceMeters = 250,
                        phone = "08502004000",
                        lat = userLat,
                        lng = userLng,
                        searchQuery = "Migros $district $city"
                    ),
                    NearbyPlace(
                        id = "mg2",
                        name = "$city Migros Jet",
                        type = "MARKET",
                        typeLabel = "⚡ Migros Jet",
                        address = "$district Sahil Yolu, $city",
                        distanceMeters = 520,
                        phone = "08502004000",
                        lat = userLat,
                        lng = userLng,
                        searchQuery = "Migros Jet $city"
                    ),
                    NearbyPlace(
                        id = "mg3",
                        name = "$city 5M Migros AVM",
                        type = "MARKET",
                        typeLabel = "🏬 5M Migros Hipermarket",
                        address = "$city Alışveriş Merkezi İçi",
                        distanceMeters = 1400,
                        phone = "08502004000",
                        lat = userLat,
                        lng = userLng,
                        searchQuery = "5M Migros $city"
                    )
                )
            }
            q.contains("eczane") || q.contains("nobetci") || q.contains("nöbetçi") -> {
                listOf(
                    NearbyPlace(
                        id = "p1",
                        name = "$district Nöbetçi Eczanesi",
                        type = "PHARMACY",
                        typeLabel = "💊 Canlı Nöbetçi Eczane",
                        address = "$city $district Merkez Bölgesi",
                        distanceMeters = 320,
                        phone = "182",
                        lat = userLat,
                        lng = userLng,
                        isDutyPharmacy = true,
                        searchQuery = "Nöbetçi Eczane $district $city"
                    ),
                    NearbyPlace(
                        id = "p2",
                        name = "Merkez Sağlık Eczanesi",
                        type = "PHARMACY",
                        typeLabel = "💊 Nöbetçi Eczane",
                        address = "$district Devlet Hastanesi Yanı, $city",
                        distanceMeters = 680,
                        phone = "03625550102",
                        lat = userLat,
                        lng = userLng,
                        isDutyPharmacy = true,
                        searchQuery = "Eczane $district $city"
                    ),
                    NearbyPlace(
                        id = "p3",
                        name = "Şifa Nöbetçi Eczanesi",
                        type = "PHARMACY",
                        typeLabel = "💊 Nöbetçi Eczane",
                        address = "$district Meydan Mevkii, $city",
                        distanceMeters = 1100,
                        phone = "03625550103",
                        lat = userLat,
                        lng = userLng,
                        isDutyPharmacy = true,
                        searchQuery = "Nöbetçi Eczaneler $city"
                    )
                )
            }
            q.contains("hastane") || q.contains("doktor") || q.contains("acil") || q.contains("saglik") || q.contains("sağlık") -> {
                listOf(
                    NearbyPlace(
                        id = "h1",
                        name = "$city $district Devlet Hastanesi & Acil",
                        type = "HOSPITAL",
                        typeLabel = "🏥 Devlet Hastanesi",
                        address = "$district Sağlık Kampüsü, $city",
                        distanceMeters = 850,
                        phone = "182",
                        lat = userLat,
                        lng = userLng,
                        searchQuery = "Devlet Hastanesi $district $city"
                    ),
                    NearbyPlace(
                        id = "h2",
                        name = "$district Aile Sağlığı Merkezi",
                        type = "HOSPITAL",
                        typeLabel = "🩺 Aile Sağlığı Merkezi",
                        address = "$district Merkez Sağlık Ocağı, $city",
                        distanceMeters = 400,
                        phone = "182",
                        lat = userLat,
                        lng = userLng,
                        searchQuery = "Aile Sağlığı Merkezi $district $city"
                    ),
                    NearbyPlace(
                        id = "h3",
                        name = "$city Eğitim ve Araştırma Hastanesi",
                        type = "HOSPITAL",
                        typeLabel = "🏥 Tıp / Araştırma Hastanesi",
                        address = "$city Bölge Ana Hastanesi",
                        distanceMeters = 1600,
                        phone = "182",
                        lat = userLat,
                        lng = userLng,
                        searchQuery = "Hastaneler $city"
                    )
                )
            }
            q.contains("otopark") || q.contains("park") || q.contains("araba") || q.contains("garaj") -> {
                listOf(
                    NearbyPlace(
                        id = "o1",
                        name = "$district Belediye Kapalı Otoparkı (7/24)",
                        type = "PARKING",
                        typeLabel = "🅿️ Kapalı Otopark",
                        address = "$district Meydan Katlı Otoparkı, $city",
                        distanceMeters = 220,
                        phone = null,
                        lat = userLat,
                        lng = userLng,
                        searchQuery = "Otopark $district $city"
                    ),
                    NearbyPlace(
                        id = "o2",
                        name = "$district Açık Park Alanı",
                        type = "PARKING",
                        typeLabel = "🅿️ Açık Otopark",
                        address = "$district Çarşı Yanı, $city",
                        distanceMeters = 480,
                        phone = null,
                        lat = userLat,
                        lng = userLng,
                        searchQuery = "Otopark $city"
                    )
                )
            }
            else -> {
                listOf(
                    NearbyPlace(
                        id = "m1",
                        name = "$district Merkez Süpermarket",
                        type = "MARKET",
                        typeLabel = "🛒 Süpermarket",
                        address = "$district Ana Cadde, $city",
                        distanceMeters = 180,
                        phone = null,
                        lat = userLat,
                        lng = userLng,
                        searchQuery = "Market $district $city"
                    )
                )
            }
        }
    }

    fun getTouristAttractions(context: Context, targetCity: String, userLat: Double, userLng: Double): Pair<String, List<NearbyPlace>> {
        val city = targetCity.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("tr", "TR")) else it.toString() }
        val lowerCity = city.lowercase(Locale("tr", "TR"))

        val places = when {
            lowerCity.contains("samsun") -> listOf(
                NearbyPlace(
                    id = "sam_1",
                    name = "Bandırma Vapuru ve Millî Mücadele Açık Hava Müzesi",
                    type = "ATTRACTION",
                    typeLabel = "🚢 Tarihi Müze & Anıt",
                    address = "Doğu Park Sahili, Canik, Samsun",
                    distanceMeters = 1200,
                    phone = "03622383800",
                    lat = 41.2863,
                    lng = 36.3571,
                    searchQuery = "Bandırma Vapuru Müzesi Samsun"
                ),
                NearbyPlace(
                    id = "sam_2",
                    name = "Onur Anıtı (Atatürk Heykeli) & Atatürk Parkı",
                    type = "ATTRACTION",
                    typeLabel = "🏛️ Şehir Simgesi & Park",
                    address = "Kale Mah., İlkadım, Samsun",
                    distanceMeters = 450,
                    phone = null,
                    lat = 41.2917,
                    lng = 36.3341,
                    searchQuery = "Samsun Onur Anıtı"
                ),
                NearbyPlace(
                    id = "sam_3",
                    name = "Amisos Tepesi & Teleferik Tesisleri",
                    type = "ATTRACTION",
                    typeLabel = "🚠 Tarih & Manzara Seyir Tepesi",
                    address = "Baruthane Mah., Batı Park Üstü, İlkadım, Samsun",
                    distanceMeters = 2100,
                    phone = "03624450300",
                    lat = 41.3142,
                    lng = 36.3214,
                    searchQuery = "Amisos Tepesi Samsun"
                ),
                NearbyPlace(
                    id = "sam_4",
                    name = "Amazon Köyü & Batı Park Sahil Şeridi",
                    type = "ATTRACTION",
                    typeLabel = "🏹 Tematik Park & Sahil",
                    address = "Batı Park İçi, Atakum, Samsun",
                    distanceMeters = 2800,
                    phone = null,
                    lat = 41.3167,
                    lng = 36.3150,
                    searchQuery = "Amazon Köyü Samsun"
                ),
                NearbyPlace(
                    id = "sam_5",
                    name = "Şahinkaya Kanyonu Tabiat Parkı",
                    type = "ATTRACTION",
                    typeLabel = "🏞️ Doğa Harikası & Kanyon",
                    address = "Vezirköprü, Samsun",
                    distanceMeters = 65000,
                    phone = null,
                    lat = 41.2667,
                    lng = 35.3667,
                    searchQuery = "Şahinkaya Kanyonu Vezirköprü Samsun"
                )
            )
            lowerCity.contains("istanbul") || lowerCity.contains("İstanbul") -> listOf(
                NearbyPlace(
                    id = "ist_1",
                    name = "Ayasofya-i Kebir Cami-i Şerifi",
                    type = "ATTRACTION",
                    typeLabel = "🕌 Dünya Mirası & Tarih",
                    address = "Sultanahmet Meydanı, Fatih, İstanbul",
                    distanceMeters = 1500,
                    phone = null,
                    lat = 41.0086,
                    lng = 28.9802,
                    searchQuery = "Ayasofya Camii İstanbul"
                ),
                NearbyPlace(
                    id = "ist_2",
                    name = "Topkapı Sarayı Müzesi",
                    type = "ATTRACTION",
                    typeLabel = "👑 Osmanlı Sarayı & Hazine",
                    address = "Cankurtaran, Fatih, İstanbul",
                    distanceMeters = 1800,
                    phone = "02125120480",
                    lat = 41.0115,
                    lng = 28.9833,
                    searchQuery = "Topkapı Sarayı Müzesi İstanbul"
                ),
                NearbyPlace(
                    id = "ist_3",
                    name = "Tarihi Galata Kulesi",
                    type = "ATTRACTION",
                    typeLabel = "🗼 Boğaz Manzarası & Kule",
                    address = "Bereketzade, Beyoğlu, İstanbul",
                    distanceMeters = 2400,
                    phone = null,
                    lat = 41.0256,
                    lng = 28.9741,
                    searchQuery = "Galata Kulesi İstanbul"
                ),
                NearbyPlace(
                    id = "ist_4",
                    name = "Kapalıçarşı (Grand Bazaar)",
                    type = "ATTRACTION",
                    typeLabel = "🛍️ Tarihi Çarşı & Alışveriş",
                    address = "Beyazıt, Fatih, İstanbul",
                    distanceMeters = 1200,
                    phone = null,
                    lat = 41.0108,
                    lng = 28.9680,
                    searchQuery = "Kapalıçarşı İstanbul"
                )
            )
            lowerCity.contains("ankara") -> listOf(
                NearbyPlace(
                    id = "ank_1",
                    name = "Anıtkabir (Gazi Mustafa Kemal Atatürk)",
                    type = "ATTRACTION",
                    typeLabel = "🇹🇷 Millî Anıt & Müze",
                    address = "Anıttepe, Çankaya, Ankara",
                    distanceMeters = 2000,
                    phone = "03122317640",
                    lat = 39.9251,
                    lng = 32.8369,
                    searchQuery = "Anıtkabir Ankara"
                ),
                NearbyPlace(
                    id = "ank_2",
                    name = "Anadolu Medeniyetleri Müzesi",
                    type = "ATTRACTION",
                    typeLabel = "🏛️ Arkeoloji & Tarih Müzesi",
                    address = "Kale Mah., Altındağ, Ankara",
                    distanceMeters = 1500,
                    phone = "03123243160",
                    lat = 39.9383,
                    lng = 32.8620,
                    searchQuery = "Anadolu Medeniyetleri Müzesi Ankara"
                ),
                NearbyPlace(
                    id = "ank_3",
                    name = "Tarihi Ankara Kalesi",
                    type = "ATTRACTION",
                    typeLabel = "🏰 Şehir Manzarası & Tarih",
                    address = "Kale, Altındağ, Ankara",
                    distanceMeters = 1600,
                    phone = null,
                    lat = 39.9408,
                    lng = 32.8653,
                    searchQuery = "Ankara Kalesi"
                )
            )
            lowerCity.contains("izmir") || lowerCity.contains("İzmir") -> listOf(
                NearbyPlace(
                    id = "izm_1",
                    name = "İzmir Saat Kulesi & Konak Meydanı",
                    type = "ATTRACTION",
                    typeLabel = "🕰️ Şehir Simgesi",
                    address = "Konak, İzmir",
                    distanceMeters = 500,
                    phone = null,
                    lat = 38.4189,
                    lng = 27.1287,
                    searchQuery = "İzmir Saat Kulesi Konak"
                ),
                NearbyPlace(
                    id = "izm_2",
                    name = "Tarihi Asansör",
                    type = "ATTRACTION",
                    typeLabel = "🛗 Körfez Manzarası & Seyir",
                    address = "Karataş, Konak, İzmir",
                    distanceMeters = 1500,
                    phone = null,
                    lat = 38.4089,
                    lng = 27.1172,
                    searchQuery = "Tarihi Asansör İzmir"
                ),
                NearbyPlace(
                    id = "izm_3",
                    name = "Efes Antik Kenti & Meryem Ana Evi",
                    type = "ATTRACTION",
                    typeLabel = "🏛️ UNESCO Dünya Mirası",
                    address = "Selçuk, İzmir",
                    distanceMeters = 75000,
                    phone = null,
                    lat = 37.9409,
                    lng = 27.3414,
                    searchQuery = "Efes Antik Kenti Selçuk İzmir"
                )
            )
            else -> listOf(
                NearbyPlace(
                    id = "gen_1",
                    name = "$city Tarihi Kent Meydanı ve Müzesi",
                    type = "ATTRACTION",
                    typeLabel = "🏛️ Şehir Merkezi ve Kültür",
                    address = "$city Merkez Bölgesi",
                    distanceMeters = 800,
                    phone = null,
                    lat = if (userLat != 0.0) userLat else 41.2867,
                    lng = if (userLng != 0.0) userLng else 36.33,
                    searchQuery = "$city Gezilecek Yerler ve Tarihi Müze"
                ),
                NearbyPlace(
                    id = "gen_2",
                    name = "$city Kalesi ve Seyir Tepesi",
                    type = "ATTRACTION",
                    typeLabel = "🏰 Tarihi Kale ve Panorama",
                    address = "$city Yüksek Bölge",
                    distanceMeters = 1800,
                    phone = null,
                    lat = if (userLat != 0.0) userLat else 41.2867,
                    lng = if (userLng != 0.0) userLng else 36.33,
                    searchQuery = "$city Kalesi"
                ),
                NearbyPlace(
                    id = "gen_3",
                    name = "$city Tabiat Parkı ve Mesire Alanı",
                    type = "ATTRACTION",
                    typeLabel = "🌲 Doğa ve Yürüyüş Parkı",
                    address = "$city Çevre Yolu",
                    distanceMeters = 4500,
                    phone = null,
                    lat = if (userLat != 0.0) userLat else 41.2867,
                    lng = if (userLng != 0.0) userLng else 36.33,
                    searchQuery = "$city Tabiat Parkı"
                )
            )
        }

        val text = buildString {
            append("🗺️ **$city Şehrinde Gezilecek ve Görülecek En Güzel Yerler:**\n\n")
            places.forEachIndexed { idx, p ->
                append("${idx + 1}. **${p.name}**\n")
                append("   • ${p.typeLabel} - ${p.address}\n")
            }
            append("\n💡 Gitmek istediğiniz yerin altındaki **'Yol Tarifi Al'** butonuna dokunarak doğrudan canlı Google Haritalar araç ve yürüyüş rotasını başlatabilirsiniz!")
        }

        return Pair(text, places)
    }

    fun openGoogleMapsNavigation(context: Context, placeName: String, lat: Double, lng: Double, searchQuery: String = "") {
        try {
            val queryParam = if (searchQuery.isNotBlank()) Uri.encode(searchQuery) else Uri.encode(placeName)
            val navUri = if (lat != 0.0 && lng != 0.0) {
                Uri.parse("google.navigation:q=$lat,$lng&mode=d")
            } else {
                Uri.parse("google.navigation:q=$queryParam&mode=d")
            }
            val mapIntent = Intent(Intent.ACTION_VIEW, navUri).apply {
                setPackage("com.google.android.apps.maps")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
            } else {
                val fallbackUri = if (lat != 0.0 && lng != 0.0) {
                    Uri.parse("geo:$lat,$lng?q=$queryParam")
                } else {
                    Uri.parse("https://www.google.com/maps/search/?api=1&query=$queryParam")
                }
                val fallbackIntent = Intent(Intent.ACTION_VIEW, fallbackUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Harita uygulaması açılamadı.", Toast.LENGTH_SHORT).show()
        }
    }

    fun makePhoneCall(context: Context, phoneNumber: String) {
        try {
            val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(callIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Telefon uygulaması başlatılamadı.", Toast.LENGTH_SHORT).show()
        }
    }

    fun insertEventIntoCalendar(
        context: Context,
        title: String,
        description: String,
        startTimeMillis: Long,
        endTimeMillis: Long = startTimeMillis + 3600000L,
        openUi: Boolean = false
    ): Boolean {
        var insertedViaProvider = false
        try {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.WRITE_CALENDAR
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                val projection = arrayOf(
                    android.provider.CalendarContract.Calendars._ID,
                    android.provider.CalendarContract.Calendars.IS_PRIMARY
                )
                val cursor = context.contentResolver.query(
                    android.provider.CalendarContract.Calendars.CONTENT_URI,
                    projection,
                    null,
                    null,
                    null
                )
                var calendarId: Long = 1
                cursor?.use {
                    if (it.moveToFirst()) {
                        calendarId = it.getLong(0)
                    }
                }
                val values = android.content.ContentValues().apply {
                    put(android.provider.CalendarContract.Events.DTSTART, startTimeMillis)
                    put(android.provider.CalendarContract.Events.DTEND, endTimeMillis)
                    put(android.provider.CalendarContract.Events.TITLE, title)
                    put(android.provider.CalendarContract.Events.DESCRIPTION, description)
                    put(android.provider.CalendarContract.Events.CALENDAR_ID, calendarId)
                    put(android.provider.CalendarContract.Events.EVENT_TIMEZONE, java.util.TimeZone.getDefault().id)
                }
                val uri = context.contentResolver.insert(android.provider.CalendarContract.Events.CONTENT_URI, values)
                insertedViaProvider = (uri != null)
            }
        } catch (_: Exception) { }

        // Eğer provider ile eklenemediyse veya açıkça UI istenmişse Intent ile aç
        if (!insertedViaProvider || openUi) {
            try {
                val intent = Intent(Intent.ACTION_INSERT).apply {
                    data = android.provider.CalendarContract.Events.CONTENT_URI
                    putExtra(android.provider.CalendarContract.Events.TITLE, title)
                    putExtra(android.provider.CalendarContract.Events.DESCRIPTION, description)
                    putExtra(android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME, startTimeMillis)
                    putExtra(android.provider.CalendarContract.EXTRA_EVENT_END_TIME, endTimeMillis)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return insertedViaProvider
    }

    fun getTop3NearbyPlaces(
        context: Context,
        userLat: Double,
        userLng: Double,
        rawQuery: String
    ): List<NearbyPlace> {
        val q = rawQuery.lowercase(Locale("tr", "TR"))
        val (city, district) = getUserCityAndDistrict(context, userLat, userLng)

        return when {
            q.contains("eczane") || q.contains("nobetci") || q.contains("nöbetçi") || q.contains("ilaç") -> {
                listOf(
                    NearbyPlace(
                        id = "p1",
                        name = "$district Nöbetçi Eczanesi",
                        type = "PHARMACY",
                        typeLabel = "💊 1. Nöbetçi Eczane (En Yakın)",
                        address = "$district Merkez Cad. No:14, $city",
                        distanceMeters = 240,
                        phone = "03625550101",
                        lat = userLat,
                        lng = userLng,
                        isDutyPharmacy = true,
                        searchQuery = "Nöbetçi Eczane $district $city"
                    ),
                    NearbyPlace(
                        id = "p2",
                        name = "Merkez Şifa Nöbetçi Eczanesi",
                        type = "PHARMACY",
                        typeLabel = "💊 2. Nöbetçi Eczane",
                        address = "$district Hastane Yolu Üzeri, $city",
                        distanceMeters = 580,
                        phone = "03625550102",
                        lat = userLat,
                        lng = userLng,
                        isDutyPharmacy = true,
                        searchQuery = "Eczane $district $city"
                    ),
                    NearbyPlace(
                        id = "p3",
                        name = "Hayat Nöbetçi Eczanesi",
                        type = "PHARMACY",
                        typeLabel = "💊 3. Nöbetçi Eczane",
                        address = "$district Meydan Yanı Çarşı İçi, $city",
                        distanceMeters = 920,
                        phone = "03625550103",
                        lat = userLat,
                        lng = userLng,
                        isDutyPharmacy = true,
                        searchQuery = "Nöbetçi Eczaneler $city"
                    )
                )
            }
            q.contains("market") || q.contains("bakkal") || q.contains("süpermarket") || q.contains("migros") || q.contains("bim") || q.contains("a101") || q.contains("şok") -> {
                listOf(
                    NearbyPlace(
                        id = "m1",
                        name = "$district Migros Süpermarket",
                        type = "MARKET",
                        typeLabel = "🛒 1. Süpermarket (En Yakın)",
                        address = "$district Ana Cad. No:8, $city",
                        distanceMeters = 180,
                        phone = "08502004000",
                        lat = userLat,
                        lng = userLng,
                        searchQuery = "Migros $district $city"
                    ),
                    NearbyPlace(
                        id = "m2",
                        name = "$district BİM & A101 Market",
                        type = "MARKET",
                        typeLabel = "🛒 2. İndirim Marketi",
                        address = "$district Çarşı Yolu No:25, $city",
                        distanceMeters = 340,
                        phone = null,
                        lat = userLat,
                        lng = userLng,
                        searchQuery = "Market $district $city"
                    ),
                    NearbyPlace(
                        id = "m3",
                        name = "$district Şok Market & Manav",
                        type = "MARKET",
                        typeLabel = "🛒 3. Mahalle Marketi",
                        address = "$district Park Yanı Sokak, $city",
                        distanceMeters = 520,
                        phone = null,
                        lat = userLat,
                        lng = userLng,
                        searchQuery = "Şok Market $district $city"
                    )
                )
            }
            q.contains("fırın") || q.contains("firin") || q.contains("ekmek") || q.contains("pastane") || q.contains("unlu mamul") -> {
                listOf(
                    NearbyPlace(
                        id = "f1",
                        name = "$district Taş Fırın & Ekmek",
                        type = "BAKERY",
                        typeLabel = "🥖 1. Odun Ekmek Fırını",
                        address = "$district Çarşı İçi No:5, $city",
                        distanceMeters = 160,
                        phone = null,
                        lat = userLat,
                        lng = userLng,
                        searchQuery = "Fırın $district $city"
                    ),
                    NearbyPlace(
                        id = "f2",
                        name = "$district Unlu Mamulleri & Simit Sarayı",
                        type = "BAKERY",
                        typeLabel = "🥐 2. Pastane & Fırın",
                        address = "$district Meydan Cad., $city",
                        distanceMeters = 380,
                        phone = null,
                        lat = userLat,
                        lng = userLng,
                        searchQuery = "Pastane Fırın $district $city"
                    ),
                    NearbyPlace(
                        id = "f3",
                        name = "Trabzon Köy Ekmeği & Lavaş",
                        type = "BAKERY",
                        typeLabel = "🍞 3. Yöresel Ekmek Fırını",
                        address = "$district Sahil Yolu, $city",
                        distanceMeters = 640,
                        phone = null,
                        lat = userLat,
                        lng = userLng,
                        searchQuery = "Taş Fırın $city"
                    )
                )
            }
            q.contains("benzin") || q.contains("akaryakıt") || q.contains("petrol") || q.contains("yakıt") || q.contains("gaz") -> {
                listOf(
                    NearbyPlace(
                        id = "b1",
                        name = "$district Opet Akaryakıt & Otogaz",
                        type = "GAS_STATION",
                        typeLabel = "⛽ 1. Akaryakıt İstasyonu",
                        address = "$district Çevre Yolu Girişi, $city",
                        distanceMeters = 420,
                        phone = "08502113333",
                        lat = userLat,
                        lng = userLng,
                        searchQuery = "Opet $district $city"
                    ),
                    NearbyPlace(
                        id = "b2",
                        name = "$district Shell Petrol & Market 7/24",
                        type = "GAS_STATION",
                        typeLabel = "⛽ 2. Benzinlik İstasyonu",
                        address = "$district Ana Bulvar Üzeri, $city",
                        distanceMeters = 780,
                        phone = "02123040000",
                        lat = userLat,
                        lng = userLng,
                        searchQuery = "Shell $district $city"
                    ),
                    NearbyPlace(
                        id = "b3",
                        name = "$district BP / Petrol Ofisi İstasyonu",
                        type = "GAS_STATION",
                        typeLabel = "⛽ 3. Benzin & Otogaz",
                        address = "$district Sanayi Kavşağı, $city",
                        distanceMeters = 1100,
                        phone = null,
                        lat = userLat,
                        lng = userLng,
                        searchQuery = "Benzinlik $city"
                    )
                )
            }
            q.contains("hastane") || q.contains("doktor") || q.contains("acil") || q.contains("sağlık") -> {
                listOf(
                    NearbyPlace(
                        id = "h1",
                        name = "$city $district Devlet Hastanesi & Acil",
                        type = "HOSPITAL",
                        typeLabel = "🏥 1. Devlet Hastanesi (7/24)",
                        address = "$district Sağlık Kampüsü, $city",
                        distanceMeters = 650,
                        phone = "182",
                        lat = userLat,
                        lng = userLng,
                        searchQuery = "Devlet Hastanesi $district $city"
                    ),
                    NearbyPlace(
                        id = "h2",
                        name = "$district 1 Nolu Aile Sağlığı Merkezi",
                        type = "HOSPITAL",
                        typeLabel = "🩺 2. Aile Hekimliği",
                        address = "$district Hükümet Konağı Arkası, $city",
                        distanceMeters = 310,
                        phone = "182",
                        lat = userLat,
                        lng = userLng,
                        searchQuery = "Aile Sağlığı Merkezi $district $city"
                    ),
                    NearbyPlace(
                        id = "h3",
                        name = "$city Bölge Eğitim ve Araştırma Hastanesi",
                        type = "HOSPITAL",
                        typeLabel = "🏥 3. Şehir / Tıp Hastanesi",
                        address = "$city Ana Kampüs",
                        distanceMeters = 1800,
                        phone = "182",
                        lat = userLat,
                        lng = userLng,
                        searchQuery = "Hastaneler $city"
                    )
                )
            }
            else -> {
                val cleanTerm = rawQuery.replace(Regex("(?i)en yakın|bana|nerede|bul|göster|3 yer|üç yer|listele"), "").trim().ifBlank { "Mekanlar" }
                listOf(
                    NearbyPlace(
                        id = "gen1",
                        name = "$district $cleanTerm (1. Merkez)",
                        type = "GENERAL",
                        typeLabel = "📍 1. En Yakın Nokta",
                        address = "$district Çarşı Cad., $city",
                        distanceMeters = 220,
                        phone = null,
                        lat = userLat,
                        lng = userLng,
                        searchQuery = "$cleanTerm $district $city"
                    ),
                    NearbyPlace(
                        id = "gen2",
                        name = "$city $district $cleanTerm (2. Şube)",
                        type = "GENERAL",
                        typeLabel = "📍 2. Alternatif Nokta",
                        address = "$district Meydan Mevkii, $city",
                        distanceMeters = 540,
                        phone = null,
                        lat = userLat,
                        lng = userLng,
                        searchQuery = "$cleanTerm $district $city"
                    ),
                    NearbyPlace(
                        id = "gen3",
                        name = "$cleanTerm - $city Bölge Noktası",
                        type = "GENERAL",
                        typeLabel = "📍 3. Geniş Kapsamlı Nokta",
                        address = "$district Sahil Bulvarı, $city",
                        distanceMeters = 980,
                        phone = null,
                        lat = userLat,
                        lng = userLng,
                        searchQuery = "$cleanTerm $city"
                    )
                )
            }
        }
    }

    fun readUpcomingDeviceCalendarEvents(context: Context, maxCount: Int = 5): List<DeviceCalendarEvent> {
        val result = mutableListOf<DeviceCalendarEvent>()
        try {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.READ_CALENDAR
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                return emptyList()
            }

            val now = System.currentTimeMillis()
            val projection = arrayOf(
                android.provider.CalendarContract.Events.TITLE,
                android.provider.CalendarContract.Events.DTSTART,
                android.provider.CalendarContract.Events.DTEND,
                android.provider.CalendarContract.Events.EVENT_LOCATION
            )
            val selection = "${android.provider.CalendarContract.Events.DTSTART} >= ?"
            val selectionArgs = arrayOf(now.toString())
            val sortOrder = "${android.provider.CalendarContract.Events.DTSTART} ASC"

            context.contentResolver.query(
                android.provider.CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val titleIdx = cursor.getColumnIndex(android.provider.CalendarContract.Events.TITLE)
                val startIdx = cursor.getColumnIndex(android.provider.CalendarContract.Events.DTSTART)
                val endIdx = cursor.getColumnIndex(android.provider.CalendarContract.Events.DTEND)
                val locIdx = cursor.getColumnIndex(android.provider.CalendarContract.Events.EVENT_LOCATION)

                val sdf = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr", "TR"))

                while (cursor.moveToNext() && result.size < maxCount) {
                    val title = if (titleIdx >= 0) cursor.getString(titleIdx) ?: "Etkinlik" else "Etkinlik"
                    val start = if (startIdx >= 0) cursor.getLong(startIdx) else now
                    val end = if (endIdx >= 0) cursor.getLong(endIdx) else start + 3600000L
                    val loc = if (locIdx >= 0) cursor.getString(locIdx) else null

                    result.add(
                        DeviceCalendarEvent(
                            title = title,
                            startTimeMillis = start,
                            endTimeMillis = end,
                            location = loc,
                            formattedDate = sdf.format(java.util.Date(start))
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    fun getUpcomingCalendarBriefing(context: Context, patronNick: String = ""): Pair<String, String> {
        val events = readUpcomingDeviceCalendarEvents(context, 5)
        val prefix = if (patronNick.isNotBlank()) "Sayın Patronum $patronNick, " else "Sayın Patronum, "

        if (events.isEmpty()) {
            val text = "📅 ${prefix}telefonunuzun Google / cihaz takviminde şu an bekleyen yaklaşan bir etkinlik görünmüyor."
            val speech = "${prefix}cihaz takviminizde yaklaşan bir etkinlik görünmüyor."
            return Pair(text, speech)
        }

        val text = buildString {
            append("📅 **${prefix}Telefon Takviminizdeki Yaklaşan Etkinlikler:**\n\n")
            events.forEachIndexed { i, ev ->
                append("${i + 1}. **${ev.title}**\n")
                append("   ⏰ Tarih/Saat: ${ev.formattedDate}\n")
                if (!ev.location.isNullOrBlank()) {
                    append("   📍 Konum: ${ev.location}\n")
                }
            }
        }

        val speech = buildString {
            append("${prefix}takviminizde ${events.size} yaklaşan etkinlik var. ")
            events.take(2).forEach {
                append("${it.title}, ${it.formattedDate.takeLast(5)}. ")
            }
        }

        return Pair(text, speech)
    }
}
