package com.example.util

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AtillaAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or AccessibilityEvent.TYPE_VIEW_CLICKED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // İhtiyaç duyulduğunda pencere geçişlerini dinler
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) instance = null
    }

    companion object {
        var instance: AtillaAccessibilityService? = null
            private set

        val isServiceActive: Boolean
            get() = instance != null

        fun performBack(): Boolean {
            return instance?.performGlobalAction(GLOBAL_ACTION_BACK) ?: false
        }

        fun performHome(): Boolean {
            return instance?.performGlobalAction(GLOBAL_ACTION_HOME) ?: false
        }

        fun performNotifications(): Boolean {
            return instance?.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS) ?: false
        }

        fun getRawScreenText(): String {
            val root = instance?.rootInActiveWindow ?: return ""
            val sb = StringBuilder()
            extractNodeText(root, sb, 0)
            return sb.toString().trim()
        }

        fun getScreenContentSummary(): String {
            val raw = getRawScreenText()
            return if (raw.isNotBlank()) {
                "📱 **Ekranda Görünen İçerik:**\n\n$raw"
            } else {
                "Ekranda okunabilir bir metin tespit edilemedi efendim."
            }
        }

        private fun extractNodeText(node: AccessibilityNodeInfo?, sb: StringBuilder, depth: Int) {
            if (node == null || depth > 10) return
            val text = node.text?.toString()?.trim()
            val desc = node.contentDescription?.toString()?.trim()
            if (!text.isNullOrBlank()) {
                sb.append("• ").append(text).append("\n")
            } else if (!desc.isNullOrBlank()) {
                sb.append("• [").append(desc).append("]\n")
            }
            for (i in 0 until node.childCount) {
                extractNodeText(node.getChild(i), sb, depth + 1)
            }
        }
    }
}
