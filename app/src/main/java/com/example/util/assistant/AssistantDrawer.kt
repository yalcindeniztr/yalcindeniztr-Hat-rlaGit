package com.example.util.assistant

import android.content.Context
import com.example.data.AppDatabase
import com.example.data.DataStoreManager
import com.example.ui.tabs.ChatMessage
import com.example.util.NearbyPlace

data class DrawerResult(
    val replyText: String,
    val recommendedPlaces: List<NearbyPlace> = emptyList(),
    val actionSummary: String? = null,
    val isSpeechReady: Boolean = true
)

data class WardrobeSessionData(
    val assistantName: String = "ATİLLA",
    val userNick: String = "",
    val userCity: String = "İstanbul",
    val userDistrict: String = "Merkez",
    val userLat: Double = 0.0,
    val userLng: Double = 0.0,
    val conversationHistory: List<ChatMessage> = emptyList(),
    val db: AppDatabase,
    val dataStoreManager: DataStoreManager,
    val apiKey: String = ""
)

interface AssistantDrawer {
    val drawerName: String
    fun canHandle(query: String, lowerQuery: String): Boolean
    suspend fun handle(
        context: Context,
        query: String,
        lowerQuery: String,
        sessionData: WardrobeSessionData
    ): DrawerResult?
}
