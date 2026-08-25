package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HatirlaGitAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, HatirlaGitAppWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }

        private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_hatirlagit)

            // Click Root -> Open Main App
            val appIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val appPendingIntent = PendingIntent.getActivity(
                context, 0, appIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, appPendingIntent)

            // Click Hızlı Not -> Open App
            val quickNoteIntent = Intent(context, MainActivity::class.java).apply {
                putExtra("action", "QUICK_NOTE")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val quickNotePendingIntent = PendingIntent.getActivity(
                context, 1, quickNoteIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_quick_note, quickNotePendingIntent)

            // Click Sesli Not -> Open App
            val voiceNoteIntent = Intent(context, MainActivity::class.java).apply {
                putExtra("action", "VOICE_NOTE")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val voiceNotePendingIntent = PendingIntent.getActivity(
                context, 2, voiceNoteIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_voice_note, voiceNotePendingIntent)

            // Async Query Room database for next reminder
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val all = db.reminderDao().getAllRemindersList()
                    val now = System.currentTimeMillis()
                    val nextReminder = all
                        .filter { it.dueDateMillis > now }
                        .minByOrNull { it.dueDateMillis }

                    if (nextReminder != null) {
                        val sdf = SimpleDateFormat("dd MMM HH:mm", Locale("tr"))
                        val timeStr = if (nextReminder.dueDateMillis > 0) sdf.format(Date(nextReminder.dueDateMillis)) else nextReminder.dueDatetime
                        views.setTextViewText(R.id.widget_upcoming_title, "📌 ${nextReminder.title}")
                        views.setTextViewText(R.id.widget_upcoming_time, "🗓️ $timeStr • ${nextReminder.category}")
                    } else {
                        views.setTextViewText(R.id.widget_upcoming_title, "Yaklaşan randevunuz yok")
                        views.setTextViewText(R.id.widget_upcoming_time, "Planlarınızı organize edin")
                    }

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                } catch (e: Exception) {
                    e.printStackTrace()
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }
    }
}
