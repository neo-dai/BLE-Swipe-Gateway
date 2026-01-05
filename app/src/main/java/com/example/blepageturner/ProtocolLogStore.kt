package com.example.blepageturner

import android.content.Context
import java.util.ArrayDeque

object ProtocolLogStore {
    private const val PREFS_NAME = "ble_page_turner"
    const val PREF_LOG_ENABLED = "log_enabled"

    const val ACTION_PROTOCOL_LOG = "com.example.blepageturner.ACTION_PROTOCOL_LOG"
    const val EXTRA_LINE = "line"

    private const val MAX_LINES = 200

    private val lock = Any()
    private val lines = ArrayDeque<String>(MAX_LINES)

    fun isEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(PREF_LOG_ENABLED, false)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(PREF_LOG_ENABLED, enabled)
            .apply()
    }

    fun add(line: String) {
        synchronized(lock) {
            while (lines.size >= MAX_LINES) {
                lines.removeFirst()
            }
            lines.addLast(line)
        }
    }

    fun snapshot(): List<String> {
        return synchronized(lock) {
            lines.toList()
        }
    }

    fun clear() {
        synchronized(lock) {
            lines.clear()
        }
    }
}
