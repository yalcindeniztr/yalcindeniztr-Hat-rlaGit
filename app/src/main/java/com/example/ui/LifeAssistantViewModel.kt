package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.CryptoHelper
import com.example.data.SubCategory
import com.example.data.CustomCategory
import com.example.data.DataStoreManager
import com.example.data.PrayerTimingData
import com.example.data.PrayerTimesRepository
import com.example.data.ReminderEntity
import com.example.data.TurkeyCities
import com.example.util.AlarmHelper
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.UpdateAvailability
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LifeAssistantViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val reminderDao = database.reminderDao()
    private val savedLocationDao = database.savedLocationDao()
    private val aiKnowledgeDao = database.aiKnowledgeDao()
    private val dataStoreManager = DataStoreManager(application)
    private val prayerRepository = PrayerTimesRepository(application)
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val customCategoryListType = Types.newParameterizedType(List::class.java, CustomCategory::class.java)
    private val customCategoryAdapter = moshi.adapter<List<CustomCategory>>(customCategoryListType)
    private val subCategoryListType = Types.newParameterizedType(List::class.java, SubCategory::class.java)
    private val subCategoryAdapter = moshi.adapter<List<SubCategory>>(subCategoryListType)
    private val prayerMapType = Types.newParameterizedType(Map::class.java, String::class.java, Boolean::class.javaObjectType)
    private val prayerMapAdapter = moshi.adapter<Map<String, Boolean>>(prayerMapType)

    val currentVersionName = "1.1.1"
    val currentVersionCode = 63

    // Latest published store release version information
    val latestAvailableVersionName = "1.1.1"
    val latestAvailableVersionCode = 63

    val allAiKnowledge: StateFlow<List<com.example.data.AiKnowledgeEntity>> = aiKnowledgeDao.getAllKnowledge()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val aiAssistantName: StateFlow<String> = dataStoreManager.aiAssistantName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "ASİSTAN")

    val isAiVoiceResponsesEnabled: StateFlow<Boolean> = dataStoreManager.isAiVoiceResponsesEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val requireVoiceConfirmation: StateFlow<Boolean> = dataStoreManager.requireVoiceConfirmation
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val encryptedAiApiKey: StateFlow<String?> = dataStoreManager.encryptedAiApiKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val aiProvider: StateFlow<String> = dataStoreManager.aiProvider
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "GEMINI")

    val lockedCategories: StateFlow<Set<String>> = dataStoreManager.lockedCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun setCategoryLock(categoryKey: String, pin: String, isLocked: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setCategoryLock(categoryKey, pin, isLocked)
        }
    }

    suspend fun verifyCategoryPin(categoryKey: String, enteredPin: String): Boolean {
        return dataStoreManager.verifyCategoryPin(categoryKey, enteredPin)
    }

    val isNotificationsEnabled = dataStoreManager.isNotificationsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isVoiceSpeakingEnabled = dataStoreManager.isVoiceSpeakingEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isLocationEnabled = dataStoreManager.isLocationEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val prayerLocationMode = dataStoreManager.prayerLocationMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "MANUAL")

    val prayerSelectedCity = dataStoreManager.prayerSelectedCity
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "İstanbul")

    val prayerNotifications: StateFlow<Map<String, Boolean>> = dataStoreManager.prayerNotificationsJson
        .map { json ->
            try {
                prayerMapAdapter.fromJson(json) ?: mapOf(
                    "İmsak" to true, "Güneş" to false, "Öğle" to true, "İkindi" to true, "Akşam" to true, "Yatsı" to true
                )
            } catch (e: Exception) {
                mapOf("İmsak" to true, "Güneş" to false, "Öğle" to true, "İkindi" to true, "Akşam" to true, "Yatsı" to true)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), mapOf(
            "İmsak" to true, "Güneş" to false, "Öğle" to true, "İkindi" to true, "Akşam" to true, "Yatsı" to true
        ))

    val prayerReminderMinutesBefore = dataStoreManager.prayerReminderMinutesBefore
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 15)

    private val _prayerTimingData = MutableStateFlow<PrayerTimingData?>(null)
    val prayerTimingData: StateFlow<PrayerTimingData?> = _prayerTimingData

    private val _isPrayerLoading = MutableStateFlow(false)
    val isPrayerLoading: StateFlow<Boolean> = _isPrayerLoading

    val customCategories: StateFlow<List<CustomCategory>> = dataStoreManager.customCategoriesJson
        .map { json ->
            try {
                customCategoryAdapter.fromJson(json) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customSubCategories: StateFlow<List<SubCategory>> = dataStoreManager.customSubCategoriesJson
        .map { json ->
            try {
                subCategoryAdapter.fromJson(json) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    
    val homeBlockOrder = dataStoreManager.homeBlockOrder
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("QUICK_NOTE", "VOICE_NOTE", "PARK", "LOCATIONS", "REMINDERS", "FAVORITES", "ADD_NEW"))

    fun updateHomeBlockOrder(order: List<String>) {
        viewModelScope.launch {
            dataStoreManager.setHomeBlockOrder(order)
        }
    }

    val favoriteCategories = dataStoreManager.favoriteCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun toggleFavoriteCategory(categoryKey: String) {
        viewModelScope.launch {
            val current = favoriteCategories.value.toMutableSet()
            if (current.contains(categoryKey)) {
                current.remove(categoryKey)
            } else {
                current.add(categoryKey)
            }
            dataStoreManager.setFavoriteCategories(current)
        }
    }

    val allReminders = reminderDao.getAllReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteReminders = reminderDao.getFavoriteReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedLocations = savedLocationDao.getAllLocations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addSavedLocation(name: String, lat: Double, lng: Double) {
        viewModelScope.launch {
            savedLocationDao.insertLocation(
                com.example.data.SavedLocationEntity(
                    name = name,
                    lat = lat,
                    lng = lng,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteSavedLocation(location: com.example.data.SavedLocationEntity) {
        viewModelScope.launch {
            savedLocationDao.deleteLocation(location)
        }
    }

    fun updateSavedLocation(location: com.example.data.SavedLocationEntity) {
        viewModelScope.launch {
            savedLocationDao.updateLocation(location)
        }
    }

    val parkedCarLat = dataStoreManager.parkedCarLat
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val parkedCarLng = dataStoreManager.parkedCarLng
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val parkedCarTime = dataStoreManager.parkedCarTime
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun saveParkedCarLocation(lat: String, lng: String) {
        viewModelScope.launch {
            dataStoreManager.saveParkedCarLocation(lat, lng, System.currentTimeMillis())
        }
    }

    fun clearParkedCarLocation() {
        viewModelScope.launch {
            dataStoreManager.clearParkedCarLocation()
        }
    }

    val legalAccepted = dataStoreManager.legalAccepted
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
        
    val userNick = dataStoreManager.userNick
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val userAvatar = dataStoreManager.userAvatar
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val fontScaleLevel = dataStoreManager.fontScaleLevel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "NORMAL")

    val hasRatedApp = dataStoreManager.hasRatedApp
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val lastRatingPromptTime = dataStoreManager.lastRatingPromptTime
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    private val _showRatingDialog = MutableStateFlow(false)
    val showRatingDialog: StateFlow<Boolean> = _showRatingDialog

    private val _showUpdateDialog = MutableStateFlow(false)
    val showUpdateDialog: StateFlow<Boolean> = _showUpdateDialog

    private val _showUpToDateDialog = MutableStateFlow(false)
    val showUpToDateDialog: StateFlow<Boolean> = _showUpToDateDialog

    private val _showInterstitialAd = MutableStateFlow(false)
    val showInterstitialAd: StateFlow<Boolean> = _showInterstitialAd

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    init {
        checkAppPrompts()
    }

    private fun checkAppPrompts() {
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000)
            checkAppUpdate(manualCheck = false)
        }
    }

    private var discoveredUpdateVersionCode = 0

    fun checkAppUpdate(manualCheck: Boolean = false) {
        val appUpdateManager = AppUpdateManagerFactory.create(getApplication())
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            val isAvailable = appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
            val availableVersionCode = appUpdateInfo.availableVersionCode()

            viewModelScope.launch {
                val ignoredVersion = dataStoreManager.latestIgnoredUpdateVersion.first()
                if (isAvailable && availableVersionCode > currentVersionCode) {
                    discoveredUpdateVersionCode = availableVersionCode
                    if (manualCheck || ignoredVersion != availableVersionCode.toString()) {
                        _showUpdateDialog.value = true
                    }
                } else {
                    _showUpdateDialog.value = false
                    if (manualCheck) {
                        _showUpToDateDialog.value = true
                    } else {
                        triggerRatingCheckIfEligible()
                    }
                }
            }
        }.addOnFailureListener {
            // Fallback for devices without Google Play Services
            viewModelScope.launch {
                _showUpdateDialog.value = false
                if (manualCheck) {
                    _showUpToDateDialog.value = true
                }
            }
        }
    }

    fun dismissUpToDateDialog() {
        _showUpToDateDialog.value = false
    }

    fun triggerRatingCheckIfEligible() {
        viewModelScope.launch {
            val hasRated = dataStoreManager.hasRatedApp.first()
            if (!hasRated) {
                val lastPrompt = dataStoreManager.lastRatingPromptTime.first()
                val oneWeekMillis = 7 * 24 * 60 * 60 * 1000L
                val currentTime = System.currentTimeMillis()
                if (lastPrompt == 0L || (currentTime - lastPrompt) >= oneWeekMillis) {
                    _showRatingDialog.value = true
                }
            }
        }
    }

    fun onUserRatedApp() {
        viewModelScope.launch {
            dataStoreManager.setHasRatedApp(true)
            _showRatingDialog.value = false
        }
    }

    fun onDismissRatingPrompt() {
        viewModelScope.launch {
            // Postpone for 1 week
            dataStoreManager.setLastRatingPromptTime(System.currentTimeMillis())
            _showRatingDialog.value = false
        }
    }

    fun dismissUpdateDialog() {
        viewModelScope.launch {
            val versionToIgnore = if (discoveredUpdateVersionCode > 0) discoveredUpdateVersionCode.toString() else latestAvailableVersionName
            dataStoreManager.setLatestIgnoredUpdateVersion(versionToIgnore)
            _showUpdateDialog.value = false
            // Trigger rating check after update dialog is dismissed if eligible
            triggerRatingCheckIfEligible()
        }
    }

    fun showInterstitial() {
        _showInterstitialAd.value = true
    }

    fun dismissInterstitial() {
        _showInterstitialAd.value = false
    }

    fun setFontScaleLevel(level: String) {
        viewModelScope.launch {
            dataStoreManager.setFontScaleLevel(level)
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun acceptLegalTerms() {
        viewModelScope.launch {
            dataStoreManager.setLegalAccepted(true)
        }
    }
    
    fun saveProfile(nick: String, pin: String, avatar: String? = null) {
        viewModelScope.launch {
            dataStoreManager.saveProfile(nick, pin, avatar)
        }
    }

    fun updateNick(newNick: String) {
        viewModelScope.launch {
            dataStoreManager.updateNick(newNick)
        }
    }

    fun updateAvatar(newAvatarUri: String) {
        viewModelScope.launch {
            dataStoreManager.updateAvatar(newAvatarUri)
        }
    }

    fun toggleFavorite(id: Int, currentStatus: Boolean) {
        viewModelScope.launch {
            reminderDao.updateFavoriteStatus(id, !currentStatus)
        }
    }

    fun getRemindersByCategory(category: String): StateFlow<List<ReminderEntity>> {
        return reminderDao.getAllReminders()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
            // Note: properly we should map this to filter, but we'll filter in the UI for simplicity or we can map it here.
    }

    fun updateReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            reminderDao.updateReminder(reminder)
            AlarmHelper.scheduleAlarm(getApplication(), reminder)
        }
    }

    fun deleteReminder(id: Int) {
        viewModelScope.launch {
            reminderDao.deleteReminderById(id)
            AlarmHelper.cancelAlarm(getApplication(), id)
            for (i in 0..10) {
                AlarmHelper.cancelAlarm(getApplication(), (id * 1000) + i)
            }
            AlarmHelper.cancelAlarm(getApplication(), (id * 1000) + 999)
        }
    }

    fun addReminderWithSubAlarms(
        reminder: ReminderEntity,
        earlyDays: Int,
        times: List<String>,
        sound: String
    ) {
        viewModelScope.launch {
            val id = reminderDao.insertReminder(reminder).toInt()
            val savedReminder = reminder.copy(id = id)
            AlarmHelper.scheduleAlarm(getApplication(), savedReminder, sound)
            
            val context = getApplication<android.app.Application>()
            
            // 1. Schedule early warning alarm if requested
            if (earlyDays > 0) {
                val earlyTriggerMillis = savedReminder.dueDateMillis - (earlyDays * 24 * 3600 * 1000L)
                if (earlyTriggerMillis > System.currentTimeMillis()) {
                    val earlyAlarmId = (id * 1000) + 999
                    AlarmHelper.scheduleCustomAlarm(
                        context = context,
                        alarmId = earlyAlarmId,
                        title = "⚠️ [$earlyDays Gün Kaldı] ${savedReminder.title}",
                        category = savedReminder.category,
                        note = "Son işlem tarihine $earlyDays gün kaldı. ${savedReminder.customNote}".trim(),
                        triggerAtMillis = earlyTriggerMillis,
                        alarmSound = sound
                    )
                }
            }

            // 2. Schedule all separate times for this reminder
            times.forEachIndexed { idx, tStr ->
                try {
                    val cal = java.util.Calendar.getInstance()
                    val tParts = tStr.split(":")
                    val hour = tParts[0].toIntOrNull() ?: 9
                    val minute = tParts[1].toIntOrNull() ?: 0
                    cal.set(java.util.Calendar.HOUR_OF_DAY, hour)
                    cal.set(java.util.Calendar.MINUTE, minute)
                    cal.set(java.util.Calendar.SECOND, 0)
                    if (cal.timeInMillis <= System.currentTimeMillis()) {
                        cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
                    }

                    val subAlarmId = (id * 1000) + idx
                    AlarmHelper.scheduleCustomAlarm(
                        context = context,
                        alarmId = subAlarmId,
                        title = "${savedReminder.title} ($tStr)",
                        category = savedReminder.category,
                        note = savedReminder.customNote.trim(),
                        triggerAtMillis = cal.timeInMillis,
                        alarmSound = sound
                    )
                } catch (e: Exception) {}
            }
        }
    }

    fun addReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            val id = reminderDao.insertReminder(reminder).toInt()
            val savedReminder = reminder.copy(id = id)
            AlarmHelper.scheduleAlarm(getApplication(), savedReminder)
        }
    }
    
    fun addCustomCategory(name: String, colorHex: String, iconName: String, fields: List<String> = emptyList()) {
        viewModelScope.launch {
            val currentList = customCategories.value.toMutableList()
            val newId = "custom_${System.currentTimeMillis()}"
            val newCategory = CustomCategory(
                id = newId,
                name = name.trim(),
                colorHex = colorHex,
                iconName = iconName,
                customFields = fields
            )
            currentList.add(newCategory)
            val json = customCategoryAdapter.toJson(currentList)
            dataStoreManager.saveCustomCategoriesJson(json)
        }
    }

    fun deleteCustomCategory(id: String) {
        viewModelScope.launch {
            val currentList = customCategories.value.filter { it.id != id }
            val json = customCategoryAdapter.toJson(currentList)
            dataStoreManager.saveCustomCategoriesJson(json)
        }
    }

    fun addCustomSubCategory(
        categoryKey: String,
        name: String,
        iconName: String = "Alarm",
        defaultDescription: String = "",
        defaultTimes: List<String> = listOf("09:00"),
        suggestedInterval: String = "DAILY"
    ) {
        viewModelScope.launch {
            val newSub = SubCategory(
                id = "custom_sub_${System.currentTimeMillis()}",
                categoryKey = categoryKey,
                name = name.trim(),
                iconName = iconName,
                defaultDescription = defaultDescription.trim(),
                defaultTimes = defaultTimes,
                suggestedInterval = suggestedInterval,
                isCustom = true
            )
            val currentList = customSubCategories.value.toMutableList()
            currentList.add(newSub)
            val json = subCategoryAdapter.toJson(currentList)
            dataStoreManager.saveCustomSubCategoriesJson(json)
        }
    }

    fun deleteCustomSubCategory(id: String) {
        viewModelScope.launch {
            val currentList = customSubCategories.value.filter { it.id != id }
            val json = subCategoryAdapter.toJson(currentList)
            dataStoreManager.saveCustomSubCategoriesJson(json)
        }
    }

    fun addQuickNote(title: String, note: String, categoryName: String = "GENEL") {
        viewModelScope.launch {
            val nowMillis = System.currentTimeMillis() + (30 * 60 * 1000) // Default 30 min from now
            val format = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
            val dateStr = format.format(java.util.Date(nowMillis))
            
            val jsonMetadata = "{}"
            val encryptedMetadata = CryptoHelper.encrypt(jsonMetadata)

            val entity = ReminderEntity(
                category = categoryName,
                title = title.trim(),
                dueDatetime = dateStr,
                dueDateMillis = nowMillis,
                customNote = note.trim(),
                isFavorite = false,
                encryptedMetadata = encryptedMetadata,
                actionStep = "QuickNote"
            )
            addReminder(entity)
        }
    }
    
    fun toggleNotificationsPermission(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setNotificationsEnabled(enabled)
        }
    }

    fun toggleVoiceSpeaking(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setVoiceSpeakingEnabled(enabled)
        }
    }

    fun toggleLocationPermission(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setLocationEnabled(enabled)
            if (!enabled) {
                // If location is disabled, default prayer mode to MANUAL
                dataStoreManager.setPrayerLocationMode("MANUAL")
            }
        }
    }

    fun setPrayerLocationMode(mode: String) {
        viewModelScope.launch {
            dataStoreManager.setPrayerLocationMode(mode)
        }
    }

    fun setPrayerSelectedCity(city: String) {
        viewModelScope.launch {
            dataStoreManager.setPrayerSelectedCity(city)
            loadPrayerTimes(city = city)
        }
    }

    fun togglePrayerNotification(prayerName: String) {
        viewModelScope.launch {
            val currentMap = prayerNotifications.value.toMutableMap()
            val currentVal = currentMap[prayerName] ?: true
            currentMap[prayerName] = !currentVal
            val json = prayerMapAdapter.toJson(currentMap)
            dataStoreManager.setPrayerNotificationsJson(json)
            syncPrayerAlarms(updatedNotifications = currentMap)
        }
    }

    fun setPrayerReminderMinutesBefore(minutes: Int) {
        viewModelScope.launch {
            dataStoreManager.setPrayerReminderMinutesBefore(minutes)
            syncPrayerAlarms(updatedMinutesBefore = minutes)
        }
    }

    fun loadPrayerTimes(city: String? = null, lat: Double? = null, lng: Double? = null) {
        viewModelScope.launch {
            _isPrayerLoading.value = true
            try {
                val targetCity = city ?: prayerSelectedCity.first()
                val timing = prayerRepository.getPrayerTimes(targetCity, lat, lng)
                _prayerTimingData.value = timing
                if (timing != null) {
                    syncPrayerAlarms(updatedTiming = timing)
                }
            } catch (e: Exception) {
                // Ignore or fallback
            } finally {
                _isPrayerLoading.value = false
            }
        }
    }

    private fun syncPrayerAlarms(
        updatedTiming: PrayerTimingData? = null,
        updatedNotifications: Map<String, Boolean>? = null,
        updatedMinutesBefore: Int? = null
    ) {
        val timing = updatedTiming ?: _prayerTimingData.value ?: return
        val notifs = updatedNotifications ?: prayerNotifications.value
        val minutes = updatedMinutesBefore ?: prayerReminderMinutesBefore.value
        AlarmHelper.scheduleAllPrayerAlarms(getApplication(), timing, notifs, minutes)
    }

    fun addAiKnowledge(title: String, content: String, category: String = "USER_NOTE") {
        viewModelScope.launch {
            aiKnowledgeDao.insertKnowledge(
                com.example.data.AiKnowledgeEntity(
                    title = title,
                    content = content,
                    category = category
                )
            )
        }
    }

    fun deleteAiKnowledge(id: Int) {
        viewModelScope.launch {
            aiKnowledgeDao.deleteKnowledgeById(id)
        }
    }

    fun saveAiApiKey(plainKey: String) {
        viewModelScope.launch {
            dataStoreManager.saveAiApiKey(plainKey)
        }
    }

    fun saveAiAssistantName(name: String) {
        viewModelScope.launch {
            dataStoreManager.saveAiAssistantName(name)
        }
    }

    fun toggleAiVoiceResponses(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.toggleAiVoiceResponses(enabled)
        }
    }

    fun toggleRequireVoiceConfirmation(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.toggleRequireVoiceConfirmation(enabled)
        }
    }

    fun getDecryptedUserNick(): String {
        return userNick.value?.let { CryptoHelper.decrypt(it) } ?: ""
    }

    fun deleteAllData() {
        viewModelScope.launch {
            reminderDao.deleteAll()
            savedLocationDao.deleteAll()
            aiKnowledgeDao.deleteAll()
        }
    }

    override fun onCleared() {
        super.onCleared()
        com.example.util.TtsHelper.stop()
    }
}
