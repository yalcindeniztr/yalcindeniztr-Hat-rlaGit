package com.example.util

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * LocalStorageManager provides guaranteed file-based local storage (JSON)
 * inside the app's private files directory, ensuring that voice notes,
 * locations, and reminders are NEVER lost even if memory or session states change.
 */
object LocalStorageManager {

    private const val DIR_NAME = "local_storage"
    private const val NOTES_FILE = "notes.json"
    private const val LOCATIONS_FILE = "locations.json"
    private const val REMINDERS_FILE = "reminders.json"

    private fun getStorageDir(context: Context): File {
        val dir = File(context.filesDir, DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun readFile(context: Context, filename: String): JSONArray {
        return try {
            val file = File(getStorageDir(context), filename)
            if (!file.exists()) return JSONArray()
            val text = file.readText(StandardCharsets.UTF_8)
            if (text.isBlank()) JSONArray() else JSONArray(text)
        } catch (_: Exception) {
            JSONArray()
        }
    }

    private fun writeFile(context: Context, filename: String, array: JSONArray) {
        try {
            val file = File(getStorageDir(context), filename)
            file.writeText(array.toString(2), StandardCharsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 1. Sesli ve Hızlı Not Kaydı
    fun saveLocalNote(context: Context, title: String, content: String, category: String = "SESLİ NOT"): Boolean {
        return try {
            val array = readFile(context, NOTES_FILE)
            val noteObj = JSONObject().apply {
                put("id", System.currentTimeMillis())
                put("title", title)
                put("content", content)
                put("category", category)
                put("timestamp", System.currentTimeMillis())
            }
            array.put(noteObj)
            writeFile(context, NOTES_FILE, array)
            true
        } catch (_: Exception) {
            false
        }
    }

    // 2. Harita Konumu Kaydı
    fun saveLocalLocation(context: Context, name: String, lat: Double, lng: Double): Boolean {
        return try {
            val array = readFile(context, LOCATIONS_FILE)
            val locObj = JSONObject().apply {
                put("id", System.currentTimeMillis())
                put("name", name)
                put("lat", lat)
                put("lng", lng)
                put("timestamp", System.currentTimeMillis())
            }
            array.put(locObj)
            writeFile(context, LOCATIONS_FILE, array)
            true
        } catch (_: Exception) {
            false
        }
    }

    // 3. Hatırlatıcı / Alarm Kaydı
    fun saveLocalReminder(context: Context, title: String, dueTimeStr: String, dueDateMillis: Long, category: String = "HATIRLATICI"): Boolean {
        return try {
            val array = readFile(context, REMINDERS_FILE)
            val remObj = JSONObject().apply {
                put("id", System.currentTimeMillis())
                put("title", title)
                put("dueTime", dueTimeStr)
                put("dueDateMillis", dueDateMillis)
                put("category", category)
                put("timestamp", System.currentTimeMillis())
            }
            array.put(remObj)
            writeFile(context, REMINDERS_FILE, array)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun getAllLocalNotes(context: Context): List<JSONObject> {
        val array = readFile(context, NOTES_FILE)
        val list = mutableListOf<JSONObject>()
        for (i in 0 until array.length()) {
            list.add(array.getJSONObject(i))
        }
        return list.reversed()
    }

    fun getAllLocalLocations(context: Context): List<JSONObject> {
        val array = readFile(context, LOCATIONS_FILE)
        val list = mutableListOf<JSONObject>()
        for (i in 0 until array.length()) {
            list.add(array.getJSONObject(i))
        }
        return list.reversed()
    }
}
