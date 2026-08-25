package com.example.util

import android.graphics.Bitmap
import android.graphics.Color
import com.example.data.ReminderEntity
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

data class HatirlaGitBackupPayload(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val reminders: List<ReminderEntity>
)

object QrSyncHelper {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter(HatirlaGitBackupPayload::class.java)

    fun createBackupJson(reminders: List<ReminderEntity>): String {
        val payload = HatirlaGitBackupPayload(
            reminders = reminders
        )
        return adapter.toJson(payload)
    }

    fun parseBackupJson(jsonStr: String): List<ReminderEntity>? {
        return try {
            val payload = adapter.fromJson(jsonStr)
            payload?.reminders
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun generateQrBitmap(content: String, size: Int = 512): Bitmap? {
        return try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
