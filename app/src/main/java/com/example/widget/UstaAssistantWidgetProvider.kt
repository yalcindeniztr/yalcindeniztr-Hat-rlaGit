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

class UstaAssistantWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, UstaAssistantWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }

        private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_usta_assistant)

            // 1. Root / Genel Tıklama -> Usta Asistan Ekranına Aç
            val rootIntent = Intent(context, MainActivity::class.java).apply {
                putExtra("action", "OPEN_AI_ASSISTANT")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val rootPendingIntent = PendingIntent.getActivity(
                context, 101, rootIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_usta_root, rootPendingIntent)

            // 2. Mikrofon Butonu -> Doğrudan Sesli Dinlemeyi Başlat
            val micIntent = Intent(context, MainActivity::class.java).apply {
                putExtra("action", "OPEN_AI_ASSISTANT")
                putExtra("auto_listen", true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val micPendingIntent = PendingIntent.getActivity(
                context, 102, micIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_usta_mic, micPendingIntent)

            // 3. Ders Planı Butonu -> Maarif Ders Planı Hazırlığı
            val lessonIntent = Intent(context, MainActivity::class.java).apply {
                putExtra("action", "OPEN_AI_ASSISTANT")
                putExtra("ai_prompt", "Günlük ders planı hazırla")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val lessonPendingIntent = PendingIntent.getActivity(
                context, 103, lessonIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_lesson_plan, lessonPendingIntent)

            // 4. Park Yeri Butonu -> Arabam Nerede
            val parkIntent = Intent(context, MainActivity::class.java).apply {
                putExtra("action", "OPEN_AI_ASSISTANT")
                putExtra("ai_prompt", "Arabam nerede")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val parkPendingIntent = PendingIntent.getActivity(
                context, 104, parkIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_park, parkPendingIntent)

            // 5. Gazeteler Butonu -> Gazete Manşetleri
            val newsIntent = Intent(context, MainActivity::class.java).apply {
                putExtra("action", "OPEN_AI_ASSISTANT")
                putExtra("ai_prompt", "Günün gazete manşetlerini ve gündemi özetle")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val newsPendingIntent = PendingIntent.getActivity(
                context, 105, newsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_news, newsPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
