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

    fun openGoogleMapsNavigation(context: Context, placeName: String, lat: Double, lng: Double, searchQuery: String = "") {
        try {
            val queryParam = if (searchQuery.isNotBlank()) Uri.encode(searchQuery) else Uri.encode(placeName)
            val uri = if (lat != 0.0 && lng != 0.0) {
                Uri.parse("geo:$lat,$lng?q=$queryParam")
            } else {
                Uri.parse("geo:0,0?q=$queryParam")
            }
            val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.google.android.apps.maps")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
            } else {
                val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$queryParam")
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
