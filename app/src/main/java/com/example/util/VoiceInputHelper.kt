package com.example.util

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoiseSecondary
import java.util.Locale

/**
 * Android Native Speech Recognizer helper for elderly and voice-typing accessibility.
 */
@Composable
fun rememberVoiceRecognizer(
    onResult: (String) -> Unit
): (prompt: String) -> Unit {
    val context = LocalContext.current
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                onResult(spokenText)
            }
        }
    }

    return { prompt ->
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
            putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("tr-TR", "tr"))
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "tr-TR")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "tr-TR")
            putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
        }
        try {
            speechLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                context,
                "Cihazınızda Google Sesli Yazma servisi bulunamadı veya etkin değil.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}

/**
 * Trailing Microphone Icon for text fields so elderly users can speak into any field.
 */
@Composable
fun VoiceInputTrailingIcon(
    prompt: String = "Lütfen mikrofona konuşun...",
    onSpeechResult: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val startVoice = rememberVoiceRecognizer(onSpeechResult)

    IconButton(
        onClick = { startVoice(prompt) },
        modifier = modifier.size(40.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFF3E0)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Mic,
                contentDescription = "Sesle Yazdır",
                tint = OrangePrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Large, accessible Voice Mic Button designed specially for seniors & elderly users.
 */
@Composable
fun ElderlyVoiceActionButton(
    label: String = "Sesli Not Al / Konuş",
    prompt: String = "Lütfen hatırlatmanızı veya notunuzu söyleyin...",
    onSpeechResult: (String) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    val startVoice = rememberVoiceRecognizer(onSpeechResult)

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Row(
        modifier = modifier
            .scale(pulseScale)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(OrangePrimary, Color(0xFFFF5722))
                )
            )
            .clickable { startVoice(prompt) }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Mic,
                contentDescription = "Mikrofon",
                tint = OrangePrimary,
                modifier = Modifier.size(size * 0.58f)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            color = Color.White
        )
    }
}
