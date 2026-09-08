package com.example.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class InAppSpeechRecognizerManager(
    private val context: Context,
    private val onFinalText: (String) -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null

    var isListening by mutableStateOf(false)
    var remainingSeconds by mutableIntStateOf(10)
    var partialText by mutableStateOf("")
    var rmsDb by mutableFloatStateOf(0f)
    var bestResultText by mutableStateOf("")

    private var countdownJob: Job? = null
    private var silenceFinishJob: Job? = null
    private var currentScope: CoroutineScope? = null
    private var hasSpokenAnyWord by mutableStateOf(false)

    fun startListening(coroutineScope: CoroutineScope) {
        TtsHelper.stop()

        currentScope = coroutineScope
        isListening = true
        remainingSeconds = 10
        partialText = ""
        bestResultText = ""
        rmsDb = 0f
        hasSpokenAnyWord = false

        countdownJob?.cancel()
        silenceFinishJob?.cancel()

        countdownJob = coroutineScope.launch {
            while (remainingSeconds > 0 && isListening) {
                delay(1000L)
                if (isListening) {
                    remainingSeconds--
                }
            }
            if (isListening) {
                finishAndSubmit()
            }
        }

        mainHandler.post {
            initAndStartNativeRecognizer()
        }
    }

    private fun triggerPostSpeechCountdown() {
        if (!isListening) return
        hasSpokenAnyWord = true
        // Konuşma bittikten sonra tam 3 saniye içinde otomatik olarak metin işlenip asistana aktarılır
        if (remainingSeconds > 3) {
            remainingSeconds = 3
        }

        silenceFinishJob?.cancel()
        currentScope?.let { scope ->
            silenceFinishJob = scope.launch {
                delay(3000L)
                if (isListening && hasSpokenAnyWord) {
                    val currentTxt = when {
                        bestResultText.isNotBlank() -> bestResultText
                        partialText.isNotBlank() -> partialText
                        else -> ""
                    }
                    if (currentTxt.isNotBlank()) {
                        finishAndSubmit()
                    }
                }
            }
        }
    }

    private fun initAndStartNativeRecognizer() {
        try {
            speechRecognizer?.destroy()
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                Log.w("InAppSpeech", "SpeechRecognizer not available on this device")
                return
            }

            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        Log.d("InAppSpeech", "Ready for speech")
                    }

                    override fun onBeginningOfSpeech() {
                        Log.d("InAppSpeech", "Speech started")
                        hasSpokenAnyWord = true
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        this@InAppSpeechRecognizerManager.rmsDb = rmsdB.coerceIn(0f, 10f)
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        Log.d("InAppSpeech", "End of speech segment")
                        triggerPostSpeechCountdown()
                    }

                    override fun onError(error: Int) {
                        Log.d("InAppSpeech", "onError: $error (remaining=$remainingSeconds)")
                        if (isListening && remainingSeconds > 1) {
                            mainHandler.postDelayed({
                                if (isListening && remainingSeconds > 1) {
                                    startNativeListeningIntent()
                                }
                            }, 250L)
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()?.trim() ?: ""
                        if (text.isNotBlank()) {
                            bestResultText = text
                            partialText = text
                            triggerPostSpeechCountdown()
                        }
                        if (isListening && remainingSeconds > 1) {
                            mainHandler.postDelayed({
                                if (isListening && remainingSeconds > 1) {
                                    startNativeListeningIntent()
                                }
                            }, 200L)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()?.trim() ?: ""
                        if (text.isNotBlank()) {
                            partialText = text
                            bestResultText = text
                            triggerPostSpeechCountdown()
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            startNativeListeningIntent()
        } catch (e: Exception) {
            Log.e("InAppSpeech", "Error initializing SpeechRecognizer", e)
        }
    }

    private fun startNativeListeningIntent() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 10000L)
            }
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e("InAppSpeech", "startListening intent error", e)
        }
    }

    fun finishAndSubmit() {
        isListening = false
        countdownJob?.cancel()
        silenceFinishJob?.cancel()
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (_: Exception) {}
        }

        val finalText = when {
            bestResultText.isNotBlank() -> bestResultText
            partialText.isNotBlank() -> partialText
            else -> ""
        }
        onFinalText(finalText)
    }

    fun cancel() {
        isListening = false
        countdownJob?.cancel()
        mainHandler.post {
            try {
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (_: Exception) {}
        }
        partialText = ""
        bestResultText = ""
    }
}

@Composable
fun InAppListeningDialog(
    manager: InAppSpeechRecognizerManager,
    assistantName: String = "Usta"
) {
    if (!manager.isListening) return

    Dialog(onDismissRequest = { manager.cancel() }) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF0F172A))
                    )
                )
                .border(2.dp, Color(0xFF00E5FF).copy(alpha = 0.6f), RoundedCornerShape(24.dp))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Başlık & Kalan Süre Rozeti
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = assistantName + " Dinliyor...",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E5FF)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFFFF9100).copy(alpha = 0.2f))
                            .border(1.dp, Color(0xFFFF9100), CircleShape)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${manager.remainingSeconds} sn",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFF9100)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Animasyonlu Ses Dalgası Küresi
                val pulse = 1f + (manager.rmsDb / 10f) * 0.35f
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .scale(pulse)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(Color(0xFF00E5FF), Color(0xFF7C4DFF), Color(0xFF0F172A))
                            )
                        )
                        .shadow(16.dp, CircleShape, spotColor = Color(0xFF00E5FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Algılanan Konuşma Metni
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF090D16))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val displayCaption = if (manager.partialText.isNotBlank()) {
                        manager.partialText
                    } else {
                        "Sözünüzü bekliyorum, en az 10 saniye boyunca kesintisiz dinlemedeyim..."
                    }
                    Text(
                        text = displayCaption,
                        fontSize = 13.sp,
                        color = if (manager.partialText.isNotBlank()) Color.White else Color(0xFF94A3B8),
                        textAlign = TextAlign.Center,
                        fontWeight = if (manager.partialText.isNotBlank()) FontWeight.SemiBold else FontWeight.Normal
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Kontrol Butonları (Tamam / İptal)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { manager.cancel() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("İptal", fontSize = 13.sp)
                    }

                    Button(
                        onClick = { manager.finishAndSubmit() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Gönder", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
