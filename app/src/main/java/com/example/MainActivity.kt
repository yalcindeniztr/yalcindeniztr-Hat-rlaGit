package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.LifeAssistantApp
import com.example.ui.LifeAssistantViewModel
import com.example.ui.theme.LifeAssistantTheme

class MainActivity : ComponentActivity() {
    private val viewModel: LifeAssistantViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIncomingIntent(intent)
        setContent {
            LifeAssistantTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LifeAssistantApp(viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.getStringExtra("action")
        if (action == "OPEN_AI_ASSISTANT") {
            val autoListen = intent.getBooleanExtra("auto_listen", false)
            val prompt = intent.getStringExtra("ai_prompt")
            viewModel.triggerAiFromWidget(autoListen, prompt)
        }
    }
}

