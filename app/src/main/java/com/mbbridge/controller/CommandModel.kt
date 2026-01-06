package com.mbbridge.controller

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

data class Command(
    val v: Int,
    val ts: Long,
    val source: String
) {
    companion object {
        private const val TAG = "MBBridgeCtrl"

        fun fromJson(jsonString: String): Command? {
            val trimmed = jsonString.trim()
            return try {
                if (trimmed.startsWith("{")) {
                    val json = JSONObject(trimmed)
                    Command(
                        v = json.getInt("v"),
                        ts = json.getLong("ts"),
                        source = json.getString("source")
                    )
                } else {
                    parseFormEncoded(trimmed)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Parse command JSON failed", e)
                null
            }
        }

        private fun parseFormEncoded(body: String): Command? {
            val map = body.split("&")
                .mapNotNull { part ->
                    val idx = part.indexOf("=")
                    if (idx <= 0) return@mapNotNull null
                    val key = URLDecoder.decode(part.substring(0, idx), StandardCharsets.UTF_8.name())
                    val value = URLDecoder.decode(part.substring(idx + 1), StandardCharsets.UTF_8.name())
                    key to value
                }
                .toMap()
            val v = map["v"]?.toIntOrNull() ?: return null
            val ts = map["ts"]?.toLongOrNull() ?: return null
            val source = map["source"] ?: "unknown"
            return Command(v = v, ts = ts, source = source)
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
