package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R

/**
 * Patron'un doğrudan akıllı telefonuna köprülenmiş otonom baş sekreteri,
 * pedagojik danışmanı ve özel ajanı Jarvis (ATİLA) için 3D bas-konuş masaüstü widget'ı.
 * Tıklandığı anda MainActivity'yi sesli dinleme modunda başlatır.
 */
class JarvisAssistantWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, JarvisAssistantWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                for (appWidgetId in appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, appWidgetId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            try {
                val views = RemoteViews(context.packageName, R.layout.widget_jarvis_assistant)

                // Tek Tıkla Bas-Konuş -> Doğrudan Jarvis'i sesli dinleme (auto_listen) modunda açar
                val micIntent = Intent(context, MainActivity::class.java).apply {
                    putExtra("action", "OPEN_AI_ASSISTANT")
                    putExtra("auto_listen", true)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val micPendingIntent = PendingIntent.getActivity(
                    context, 105, micIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_jarvis_root, micPendingIntent)
                views.setOnClickPendingIntent(R.id.widget_btn_jarvis_mic, micPendingIntent)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
