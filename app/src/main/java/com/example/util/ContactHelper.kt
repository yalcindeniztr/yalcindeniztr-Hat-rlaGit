package com.example.util

import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import java.util.Locale

data class DeviceContact(
    val name: String,
    val phoneNumber: String
)

object ContactHelper {

    /**
     * Rehber izninin kullanıcı tarafından verilip verilmediğini kontrol eder.
     */
    fun hasContactsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Rehberden verilen isme en yakın kişiyi bulur.
     * Kullanıcı gizliliği gereği sadece kullanıcı izin verdiğinde çalışır.
     */
    fun findContactByName(context: Context, searchName: String): DeviceContact? {
        if (!hasContactsPermission(context) || searchName.isBlank()) return null

        val cleanSearch = searchName.lowercase(Locale.forLanguageTag("tr-TR")).trim()
        val resolver = context.contentResolver
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        var cursor: Cursor? = null
        try {
            cursor = resolver.query(uri, projection, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                var bestMatch: DeviceContact? = null
                var exactMatch: DeviceContact? = null

                do {
                    val name = cursor.getString(nameIdx) ?: continue
                    val phone = cursor.getString(numberIdx) ?: continue
                    val lowerName = name.lowercase(Locale.forLanguageTag("tr-TR"))

                    if (lowerName == cleanSearch) {
                        exactMatch = DeviceContact(name = name, phoneNumber = phone)
                        break
                    } else if (lowerName.contains(cleanSearch) || cleanSearch.contains(lowerName)) {
                        if (bestMatch == null) {
                            bestMatch = DeviceContact(name = name, phoneNumber = phone)
                        }
                    }
                } while (cursor.moveToNext())

                return exactMatch ?: bestMatch
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }
        return null
    }
}
