package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.regex.Pattern
import kotlin.coroutines.resume

data class OcrScanResult(
    val title: String,
    val amount: String?,
    val dateMillis: Long?,
    val rawText: String,
    val suggestedCategory: String
)

object OcrScannerHelper {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun scanImage(context: Context, imageUri: Uri): OcrScanResult? = suspendCancellableCoroutine { continuation ->
        try {
            val image = InputImage.fromFilePath(context, imageUri)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val result = parseVisionText(visionText.text)
                    continuation.resume(result)
                }
                .addOnFailureListener {
                    continuation.resume(null)
                }
        } catch (e: Exception) {
            continuation.resume(null)
        }
    }

    suspend fun scanBitmap(bitmap: Bitmap): OcrScanResult? = suspendCancellableCoroutine { continuation ->
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val result = parseVisionText(visionText.text)
                    continuation.resume(result)
                }
                .addOnFailureListener {
                    continuation.resume(null)
                }
        } catch (e: Exception) {
            continuation.resume(null)
        }
    }

    fun parseVisionText(rawText: String): OcrScanResult {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
        
        // 1. Tutar Tespiti (Örn: 450,50 TL, 1.250,00 TL, 350.00 TL)
        val amountPattern = Pattern.compile("""(?i)(?:tutar|toplam|öde(?:necek|me)|bedel|fiyat|tl|₺)?\s*[:\s]*(\d{1,3}(?:[.,]\d{3})*(?:[.,]\d{2}))\s*(?:tl|₺)?""")
        var detectedAmount: String? = null
        for (line in lines) {
            val matcher = amountPattern.matcher(line)
            if (matcher.find()) {
                val found = matcher.group(1)
                if (found != null && found.length >= 2) {
                    detectedAmount = "$found TL"
                    break
                }
            }
        }

        // 2. Tarih Tespiti (Örn: 28.08.2026, 28/08/2026, 28-08-2026)
        val datePattern = Pattern.compile("""(\b\d{1,2})[./\-](\d{1,2})[./\-](\d{2,4}\b)""")
        var detectedDateMillis: Long? = null
        for (line in lines) {
            val matcher = datePattern.matcher(line)
            if (matcher.find()) {
                val day = matcher.group(1)
                val month = matcher.group(2)
                var year = matcher.group(3)
                if (year != null && year.length == 2) {
                    year = "20$year"
                }
                try {
                    val dateStr = "$day.$month.$year"
                    val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                    val parsed = sdf.parse(dateStr)
                    if (parsed != null) {
                        val cal = Calendar.getInstance()
                        cal.time = parsed
                        cal.set(Calendar.HOUR_OF_DAY, 10)
                        cal.set(Calendar.MINUTE, 0)
                        detectedDateMillis = cal.timeInMillis
                        break
                    }
                } catch (_: Exception) {}
            }
        }

        // 3. Kurum / Başlık Tespiti ve Kategori Önerisi
        var detectedTitle = "Fatura / Randevu"
        var suggestedCategory = "BILLS_CARDS"

        val lowerText = rawText.lowercase(Locale("tr"))
        when {
            lowerText.contains("hastane") || lowerText.contains("doktor") || lowerText.contains("sağlık") || lowerText.contains("reçete") || lowerText.contains("tahlil") -> {
                detectedTitle = lines.firstOrNull { it.contains("Hastane", true) || it.contains("Dr.", true) } ?: "Doktor Randevusu"
                suggestedCategory = "HEALTH"
            }
            lowerText.contains("elektrik") || lowerText.contains("enerji") || lowerText.contains("bedaş") || lowerText.contains("gediz") || lowerText.contains("ayedaş") -> {
                detectedTitle = "Elektrik Faturası"
                suggestedCategory = "BILLS_CARDS"
            }
            lowerText.contains("su faturası") || lowerText.contains("iski") || lowerText.contains("izsu") || lowerText.contains("aski") -> {
                detectedTitle = "Su Faturası"
                suggestedCategory = "BILLS_CARDS"
            }
            lowerText.contains("doğalgaz") || lowerText.contains("igdaş") || lowerText.contains("gaz") -> {
                detectedTitle = "Doğalgaz Faturası"
                suggestedCategory = "BILLS_CARDS"
            }
            lowerText.contains("türk telekom") || lowerText.contains("turkcell") || lowerText.contains("vodafone") || lowerText.contains("internet") -> {
                detectedTitle = "İletişim / İnternet Faturası"
                suggestedCategory = "BILLS_CARDS"
            }
            lowerText.contains("vergi") || lowerText.contains("gib") || lowerText.contains("belediye") -> {
                detectedTitle = "Resmi Ödeme / Vergi"
                suggestedCategory = "OFFICIAL"
            }
            else -> {
                if (lines.isNotEmpty()) {
                    detectedTitle = lines.take(2).joinToString(" - ")
                }
            }
        }

        return OcrScanResult(
            title = detectedTitle.take(60),
            amount = detectedAmount,
            dateMillis = detectedDateMillis,
            rawText = rawText,
            suggestedCategory = suggestedCategory
        )
    }
}
