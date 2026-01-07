package com.mbbridge.controller

import android.content.Context
import android.util.Log

data class Command(
    val v: Int,
    val ts: Long = 0L,
    val source: String = "bridge"
) {
    companion object {
        private const val TAG = "MBBridgeCtrl"
        private const val LOG_ENABLED = false

        fun fromBytes(bytes: ByteArray): Command? {
            if (bytes.isEmpty()) {
                return null
            }
            val v = bytes[0].toInt() and 0xFF
            if (v == 0) {
                if (LOG_ENABLED) Log.w(TAG, "Invalid command byte: 0")
                return null
            }
            return Command(v = v)
        }
    }

    fun getCommandType(): CommandType = when (v) {
        1 -> CommandType.PREV
        2 -> CommandType.NEXT
        else -> CommandType.UNKNOWN(v)
    }
}

sealed class CommandType {
    object PREV : CommandType()
    object NEXT : CommandType()
    data class UNKNOWN(val value: Int) : CommandType()

    override fun toString(): String = when (this) {
        is PREV -> "PREV"
        is NEXT -> "NEXT"
        is UNKNOWN -> "UNKNOWN($value)"
    }
}

data class HttpResponse(
    val ok: Int,
    val err: String? = null,
    val app: String? = null
) {
    fun toJson(): String {
        return JSONObject().apply {
            put("ok", ok)
            err?.let { put("err", it) }
            app?.let { put("app", it) }
        }.toString()
    }

    companion object {
        fun success(app: String? = null) = HttpResponse(ok = 1, app = app)
        fun error(message: String) = HttpResponse(ok = 0, err = message)
    }
}

data class CommandStats(
    val prevCount: Int = 0,
    val nextCount: Int = 0,
    val totalCount: Int = 0
) {
    fun increment(commandType: CommandType): CommandStats {
        return when (commandType) {
            is CommandType.PREV -> copy(prevCount = prevCount + 1, totalCount = totalCount + 1)
            is CommandType.NEXT -> copy(nextCount = nextCount + 1, totalCount = totalCount + 1)
            is CommandType.UNKNOWN -> copy(totalCount = totalCount + 1)
        }
    }
}

enum class LogLevel {
    VERBOSE,
    DEBUG,
    INFO,
    WARN,
    ERROR
}

object TapAction {
    const val ACTION_TAP = "com.mbbridge.controller.ACTION_TAP"
    const val ACTION_TAP_RESULT = "com.mbbridge.controller.ACTION_TAP_RESULT"
    const val EXTRA_SIDE = "side"
    const val EXTRA_RESULT = "result"
    const val EXTRA_X = "x"
    const val EXTRA_Y = "y"
    const val SIDE_LEFT = "left"
    const val SIDE_RIGHT = "right"
}

class TokenStore(private val context: Context) {
    companion object {
        private const val PREFS_NAME = "mbbridge_prefs"
        private const val KEY_TOKEN = "auth_token"
        private const val TAG = "MBBridgeCtrl"
    }

    fun getToken(): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_TOKEN, null)
            ?.takeIf { it.isNotBlank() }
    }

    fun saveToken(token: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TOKEN, token.trim())
            .apply()
        Log.i(TAG, "Token updated")
    }

    fun verify(headers: Map<String, String>): Boolean {
        val expected = getToken() ?: return true
        val provided = headers["x-mbbridge-token"] ?: headers["X-MBBridge-Token"]
        return expected == provided
    }
}

class PortStore(private val context: Context) {
    companion object {
        private const val PREFS_NAME = "mbbridge_prefs"
        private const val KEY_PORT = "server_port"
        private const val DEFAULT_PORT = 27123
    }

    fun getPort(): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_PORT, DEFAULT_PORT)
    }

    fun savePort(port: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_PORT, port)
            .apply()
    }
}
