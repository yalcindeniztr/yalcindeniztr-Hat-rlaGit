package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

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
    val isDutyPharmacy: Boolean = false
)

object NearbyPlacesHelper {

    fun getRecommendedPlaces(userLat: Double, userLng: Double, queryType: String): List<NearbyPlace> {
        val q = queryType.lowercase()
        return when {
            q.contains("eczane") || q.contains("nobetci") || q.contains("nöbetçi") -> {
                listOf(
                    NearbyPlace(
                        id = "p1",
                        name = "Şifa Nöbetçi Eczanesi",
                        type = "PHARMACY",
                        typeLabel = "💊 Nöbetçi Eczane",
                        address = "Atatürk Cad. No: 42 (Hastane Karşısı)",
                        distanceMeters = 350,
                        phone = "02125550101",
                        lat = userLat + 0.0031,
                        lng = userLng + 0.0022,
                        isDutyPharmacy = true
                    ),
                    NearbyPlace(
                        id = "p2",
                        name = "Merkez Hayat Eczanesi",
                        type = "PHARMACY",
                        typeLabel = "💊 Eczane",
                        address = "Cumhuriyet Meydanı No: 12",
                        distanceMeters = 720,
                        phone = "02125550102",
                        lat = userLat + 0.0055,
                        lng = userLng - 0.0041
                    ),
                    NearbyPlace(
                        id = "p3",
                        name = "Güneş Nöbetçi Eczanesi",
                        type = "PHARMACY",
                        typeLabel = "💊 Nöbetçi Eczane",
                        address = "İnönü Bulvarı No: 88/A",
                        distanceMeters = 1100,
                        phone = "02125550103",
                        lat = userLat - 0.0072,
                        lng = userLng + 0.0065,
                        isDutyPharmacy = true
                    )
                )
            }
            q.contains("hastane") || q.contains("doktor") || q.contains("acil") || q.contains("saglik") || q.contains("sağlık") -> {
                listOf(
                    NearbyPlace(
                        id = "h1",
                        name = "Devlet Hastanesi & Acil Servis",
                        type = "HOSPITAL",
                        typeLabel = "🏥 Devlet Hastanesi",
                        address = "Sağlık Caddesi No: 1",
                        distanceMeters = 850,
                        phone = "182",
                        lat = userLat + 0.0068,
                        lng = userLng + 0.0045
                    ),
                    NearbyPlace(
                        id = "h2",
                        name = "Merkez Aile Sağlığı Merkezi",
                        type = "HOSPITAL",
                        typeLabel = "🩺 Aile Sağlığı Merkezi",
                        address = "Gazi Sokak No: 14",
                        distanceMeters = 400,
                        phone = "02125550202",
                        lat = userLat - 0.0035,
                        lng = userLng + 0.0015
                    ),
                    NearbyPlace(
                        id = "h3",
                        name = "Özel Yaşam Tıp Merkezi",
                        type = "HOSPITAL",
                        typeLabel = "🏥 Tıp Merkezi",
                        address = "Fevzi Çakmak Cad. No: 29",
                        distanceMeters = 1450,
                        phone = "02125550203",
                        lat = userLat + 0.0110,
                        lng = userLng - 0.0080
                    )
                )
            }
            q.contains("otopark") || q.contains("park") || q.contains("araba") || q.contains("garaj") -> {
                listOf(
                    NearbyPlace(
                        id = "o1",
                        name = "Belediye Kapalı Otoparkı (7/24)",
                        type = "PARKING",
                        typeLabel = "🅿️ Kapalı Otopark",
                        address = "Kent Meydanı Kat Altı",
                        distanceMeters = 200,
                        phone = "02125550301",
                        lat = userLat + 0.0018,
                        lng = userLng + 0.0010
                    ),
                    NearbyPlace(
                        id = "o2",
                        name = "Meydan Açık Otopark Alanı",
                        type = "PARKING",
                        typeLabel = "🅿️ Açık Otopark",
                        address = "İstasyon Yanı",
                        distanceMeters = 550,
                        phone = null,
                        lat = userLat - 0.0042,
                        lng = userLng - 0.0030
                    ),
                    NearbyPlace(
                        id = "o3",
                        name = "Güven Katlı Otopark",
                        type = "PARKING",
                        typeLabel = "🅿️ Katlı Otopark",
                        address = "Çarşı Yolu No: 5",
                        distanceMeters = 900,
                        phone = "02125550303",
                        lat = userLat + 0.0075,
                        lng = userLng + 0.0050
                    )
                )
            }
            else -> {
                // Market / Süpermarket
                listOf(
                    NearbyPlace(
                        id = "m1",
                        name = "Merkez Süpermarket",
                        type = "MARKET",
                        typeLabel = "🛒 Süpermarket",
                        address = "Ana Cadde No: 18",
                        distanceMeters = 150,
                        phone = "02125550401",
                        lat = userLat + 0.0012,
                        lng = userLng - 0.0008
                    ),
                    NearbyPlace(
                        id = "m2",
                        name = "Mahalle Bakkalı & Şarküteri",
                        type = "MARKET",
                        typeLabel = "🥖 Bakkal / Market",
                        address = "Gül Sokak No: 7",
                        distanceMeters = 280,
                        phone = "02125550402",
                        lat = userLat - 0.0022,
                        lng = userLng + 0.0018
                    ),
                    NearbyPlace(
                        id = "m3",
                        name = "7/24 Açık Ekspres Market",
                        type = "MARKET",
                        typeLabel = "🏪 7/24 Market",
                        address = "Köşe Başı No: 50",
                        distanceMeters = 480,
                        phone = "02125550403",
                        lat = userLat + 0.0039,
                        lng = userLng + 0.0031
                    )
                )
            }
        }
    }

    fun openGoogleMapsNavigation(context: Context, placeName: String, lat: Double, lng: Double) {
        try {
            // Intent for Google Maps navigation or search
            val uri = Uri.parse("google.navigation:q=$lat,$lng&mode=d")
            val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.google.android.apps.maps")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
            } else {
                // Fallback to browser Google Maps
                val webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lng")
                val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
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

    fun insertEventIntoCalendar(context: Context, title: String, description: String, startTimeMillis: Long, endTimeMillis: Long = startTimeMillis + 3600000L) {
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
