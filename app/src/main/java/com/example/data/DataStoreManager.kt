package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class DataStoreManager(private val context: Context) {
    
    companion object {
        val LEGAL_ACCEPTED = booleanPreferencesKey("legal_accepted")
        val USER_NICK = stringPreferencesKey("user_nick")
        val USER_PIN = stringPreferencesKey("user_pin")
        val USER_AVATAR = stringPreferencesKey("user_avatar")
        val FONT_SCALE_LEVEL = stringPreferencesKey("font_scale_level")
        val HAS_RATED_APP = booleanPreferencesKey("has_rated_app")
        val LAST_RATING_PROMPT_TIME = androidx.datastore.preferences.core.longPreferencesKey("last_rating_prompt_time")
        val LATEST_IGNORED_UPDATE_VERSION = stringPreferencesKey("latest_ignored_update_version")
        val CUSTOM_CATEGORIES = stringPreferencesKey("custom_categories")
        val IS_NOTIFICATIONS_ENABLED = booleanPreferencesKey("is_notifications_enabled")
        val IS_LOCATION_ENABLED = booleanPreferencesKey("is_location_enabled")
        val PRAYER_LOCATION_MODE = stringPreferencesKey("prayer_location_mode")
        val PRAYER_SELECTED_CITY = stringPreferencesKey("prayer_selected_city")
        val PRAYER_NOTIFICATIONS_JSON = stringPreferencesKey("prayer_notifications_json")
        val PRAYER_REMINDER_MINUTES_BEFORE = androidx.datastore.preferences.core.intPreferencesKey("prayer_reminder_minutes_before")
        val PARKED_CAR_LAT = stringPreferencesKey("parked_car_lat")
        val PARKED_CAR_LNG = stringPreferencesKey("parked_car_lng")
        val PARKED_CAR_TIME = androidx.datastore.preferences.core.longPreferencesKey("parked_car_time")
        val FAVORITE_CATEGORIES = androidx.datastore.preferences.core.stringSetPreferencesKey("favorite_categories")
        val HOME_BLOCK_ORDER = stringPreferencesKey("home_block_order")
        val CUSTOM_SUBCATEGORIES = stringPreferencesKey("custom_subcategories")
        val IS_VOICE_SPEAKING_ENABLED = booleanPreferencesKey("is_voice_speaking_enabled")
        val LOCKED_CATEGORIES = androidx.datastore.preferences.core.stringSetPreferencesKey("locked_categories")
        val CATEGORY_PINS = stringPreferencesKey("category_pins")
        val AI_API_KEY_ENCRYPTED = stringPreferencesKey("ai_api_key_encrypted")
        val AI_PROVIDER = stringPreferencesKey("ai_provider") // GEMINI, OPENAI
        val AI_ASSISTANT_NAME = stringPreferencesKey("ai_assistant_name")
        val AI_VOICE_RESPONSES_ENABLED = booleanPreferencesKey("ai_voice_responses_enabled")
        val REQUIRE_VOICE_CONFIRMATION = booleanPreferencesKey("require_voice_confirmation")
    }

    
    val homeBlockOrder: Flow<List<String>> = context.dataStore.data.map { preferences ->
        val json = preferences[HOME_BLOCK_ORDER] ?: "[]"
        val defaultList = listOf("BILLS_CARDS", "MY_CAR", "QUICK_NOTE", "VOICE_NOTE", "PARK", "LOCATIONS", "FAVORITES", "ADD_NEW")
        if (json == "[]") {
            defaultList
        } else {
            try {
                val list = json.removePrefix("[").removeSuffix("]").replace("\"", "").split(",").map { it.trim() }.filter { it.isNotEmpty() && it != "ALL" && it != "REMINDERS" }
                if (list.isEmpty()) defaultList else list
            } catch (e: Exception) {
                defaultList
            }
        }
    }

    suspend fun setHomeBlockOrder(order: List<String>) {
        context.dataStore.edit { preferences ->
            preferences[HOME_BLOCK_ORDER] = order.filter { it != "ALL" && it != "REMINDERS" }.joinToString(separator = ",", prefix = "[", postfix = "]") { "\"$it\"" }
        }
    }

    val customSubCategoriesJson: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[CUSTOM_SUBCATEGORIES] ?: "[]"
    }

    suspend fun saveCustomSubCategoriesJson(json: String) {
        context.dataStore.edit { preferences ->
            preferences[CUSTOM_SUBCATEGORIES] = json
        }
    }
    
    val favoriteCategories: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[FAVORITE_CATEGORIES] ?: emptySet()
    }

    suspend fun setFavoriteCategories(categories: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[FAVORITE_CATEGORIES] = categories
        }
    }

    val isNotificationsEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_NOTIFICATIONS_ENABLED] ?: true
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    val isVoiceSpeakingEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_VOICE_SPEAKING_ENABLED] ?: true
    }

    suspend fun setVoiceSpeakingEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_VOICE_SPEAKING_ENABLED] = enabled
        }
    }

    val isLocationEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_LOCATION_ENABLED] ?: true
    }

    suspend fun setLocationEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_LOCATION_ENABLED] = enabled
        }
    }

    val prayerLocationMode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PRAYER_LOCATION_MODE] ?: "MANUAL"
    }

    suspend fun setPrayerLocationMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[PRAYER_LOCATION_MODE] = mode
        }
    }

    val prayerSelectedCity: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PRAYER_SELECTED_CITY] ?: "İstanbul"
    }

    suspend fun setPrayerSelectedCity(city: String) {
        context.dataStore.edit { preferences ->
            preferences[PRAYER_SELECTED_CITY] = city
        }
    }

    val prayerNotificationsJson: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PRAYER_NOTIFICATIONS_JSON] ?: "{\"İmsak\":true,\"Güneş\":false,\"Öğle\":true,\"İkindi\":true,\"Akşam\":true,\"Yatsı\":true}"
    }

    suspend fun setPrayerNotificationsJson(json: String) {
        context.dataStore.edit { preferences ->
            preferences[PRAYER_NOTIFICATIONS_JSON] = json
        }
    }

    val prayerReminderMinutesBefore: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PRAYER_REMINDER_MINUTES_BEFORE] ?: 15 // Default 15 minutes
    }

    suspend fun setPrayerReminderMinutesBefore(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[PRAYER_REMINDER_MINUTES_BEFORE] = minutes
        }
    }

    val customCategoriesJson: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[CUSTOM_CATEGORIES] ?: "[]"
    }

    val parkedCarLat: Flow<String?> = context.dataStore.data.map { it[PARKED_CAR_LAT] }
    val parkedCarLng: Flow<String?> = context.dataStore.data.map { it[PARKED_CAR_LNG] }
    val parkedCarTime: Flow<Long?> = context.dataStore.data.map { it[PARKED_CAR_TIME] }

    suspend fun saveParkedCarLocation(lat: String, lng: String, time: Long) {
        context.dataStore.edit { preferences ->
            preferences[PARKED_CAR_LAT] = lat
            preferences[PARKED_CAR_LNG] = lng
            preferences[PARKED_CAR_TIME] = time
        }
    }

    suspend fun clearParkedCarLocation() {
        context.dataStore.edit { preferences ->
            preferences.remove(PARKED_CAR_LAT)
            preferences.remove(PARKED_CAR_LNG)
            preferences.remove(PARKED_CAR_TIME)
        }
    }

    suspend fun saveCustomCategoriesJson(json: String) {
        context.dataStore.edit { preferences ->
            preferences[CUSTOM_CATEGORIES] = json
        }
    }
    
    val legalAccepted: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[LEGAL_ACCEPTED] ?: false
    }

    val fontScaleLevel: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[FONT_SCALE_LEVEL] ?: "NORMAL"
    }

    val hasRatedApp: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[HAS_RATED_APP] ?: false
    }

    val lastRatingPromptTime: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[LAST_RATING_PROMPT_TIME] ?: 0L
    }

    suspend fun setHasRatedApp(rated: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HAS_RATED_APP] = rated
        }
    }

    val latestIgnoredUpdateVersion: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[LATEST_IGNORED_UPDATE_VERSION]
    }

    suspend fun setLatestIgnoredUpdateVersion(version: String) {
        context.dataStore.edit { preferences ->
            preferences[LATEST_IGNORED_UPDATE_VERSION] = version
        }
    }

    suspend fun setLastRatingPromptTime(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[LAST_RATING_PROMPT_TIME] = timestamp
        }
    }

    suspend fun setFontScaleLevel(level: String) {
        context.dataStore.edit { preferences ->
            preferences[FONT_SCALE_LEVEL] = level
        }
    }

    suspend fun setLegalAccepted(accepted: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[LEGAL_ACCEPTED] = accepted
        }
    }

    val userNick: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USER_NICK]
    }
    
    val userPin: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USER_PIN]
    }

    val userAvatar: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USER_AVATAR]
    }

    suspend fun saveProfile(nick: String, pin: String, avatar: String? = null) {
        context.dataStore.edit { preferences ->
            preferences[USER_NICK] = CryptoHelper.encrypt(nick)
            preferences[USER_PIN] = CryptoHelper.encrypt(pin)
            if (avatar != null) {
                preferences[USER_AVATAR] = avatar
            }
        }
    }

    suspend fun updateNick(newNick: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_NICK] = CryptoHelper.encrypt(newNick)
        }
    }

    suspend fun updateAvatar(newAvatarUri: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_AVATAR] = newAvatarUri
        }
    }
    
    val lockedCategories: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[LOCKED_CATEGORIES] ?: emptySet()
    }

    suspend fun setCategoryLock(categoryKey: String, pin: String, isLocked: Boolean) {
        context.dataStore.edit { preferences ->
            val currentLocked = preferences[LOCKED_CATEGORIES]?.toMutableSet() ?: mutableSetOf()
            val currentPinsJson = preferences[CATEGORY_PINS] ?: "{}"
            
            // Simple parsing of category to encrypted pin map
            val pinMap = try {
                val moshi = com.squareup.moshi.Moshi.Builder().add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory()).build()
                val mapType = com.squareup.moshi.Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
                val adapter = moshi.adapter<Map<String, String>>(mapType)
                adapter.fromJson(currentPinsJson)?.toMutableMap() ?: mutableMapOf()
            } catch (e: Exception) {
                mutableMapOf()
            }

            if (isLocked && pin.isNotBlank()) {
                currentLocked.add(categoryKey)
                pinMap[categoryKey] = CryptoHelper.encrypt(pin.trim())
            } else {
                currentLocked.remove(categoryKey)
                pinMap.remove(categoryKey)
            }

            val moshi = com.squareup.moshi.Moshi.Builder().add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory()).build()
            val mapType = com.squareup.moshi.Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
            val adapter = moshi.adapter<Map<String, String>>(mapType)
            val newPinsJson = adapter.toJson(pinMap)

            preferences[LOCKED_CATEGORIES] = currentLocked
            preferences[CATEGORY_PINS] = newPinsJson
        }
    }

    suspend fun verifyCategoryPin(categoryKey: String, enteredPin: String): Boolean {
        return try {
            val preferences = context.dataStore.data.first()
            val isLocked = preferences[LOCKED_CATEGORIES]?.contains(categoryKey) ?: false
            if (!isLocked) return true

            val currentPinsJson = preferences[CATEGORY_PINS] ?: "{}"
            val moshi = com.squareup.moshi.Moshi.Builder().add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory()).build()
            val mapType = com.squareup.moshi.Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
            val adapter = moshi.adapter<Map<String, String>>(mapType)
            val pinMap = adapter.fromJson(currentPinsJson) ?: emptyMap()
            
            val encryptedPin = pinMap[categoryKey]
            if (encryptedPin != null) {
                val decryptedPin = CryptoHelper.decrypt(encryptedPin)
                decryptedPin.trim() == enteredPin.trim()
            } else {
                // Fallback to Master user PIN if specific category PIN is not found
                val userMasterPinEncrypted = preferences[USER_PIN]
                if (userMasterPinEncrypted != null) {
                    val userMasterPin = CryptoHelper.decrypt(userMasterPinEncrypted)
                    userMasterPin.trim() == enteredPin.trim()
                } else {
                    true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getDecryptedNick(encryptedNick: String?): String? {
        return encryptedNick?.let { CryptoHelper.decrypt(it) }
    }

    val encryptedAiApiKey: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[AI_API_KEY_ENCRYPTED]
    }

    val aiProvider: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[AI_PROVIDER] ?: "GEMINI"
    }

    val aiAssistantName: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[AI_ASSISTANT_NAME] ?: "ASİSTAN"
    }

    val isAiVoiceResponsesEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[AI_VOICE_RESPONSES_ENABLED] ?: true
    }

    val requireVoiceConfirmation: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[REQUIRE_VOICE_CONFIRMATION] ?: true
    }

    suspend fun saveAiApiKey(plainKey: String) {
        context.dataStore.edit { preferences ->
            if (plainKey.isBlank()) {
                preferences.remove(AI_API_KEY_ENCRYPTED)
            } else {
                preferences[AI_API_KEY_ENCRYPTED] = CryptoHelper.encrypt(plainKey.trim())
            }
        }
    }

    suspend fun saveAiProvider(provider: String) {
        context.dataStore.edit { preferences ->
            preferences[AI_PROVIDER] = provider
        }
    }

    suspend fun saveAiAssistantName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[AI_ASSISTANT_NAME] = if (name.isBlank()) "ASİSTAN" else name.trim()
        }
    }

    suspend fun toggleAiVoiceResponses(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AI_VOICE_RESPONSES_ENABLED] = enabled
        }
    }

    suspend fun toggleRequireVoiceConfirmation(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[REQUIRE_VOICE_CONFIRMATION] = enabled
        }
    }
}
