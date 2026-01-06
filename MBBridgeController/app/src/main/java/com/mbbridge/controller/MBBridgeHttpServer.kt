package com.mbbridge.controller

import android.content.Context
import android.util.Log
import fi.iki.elonen.NanoHTTPD
import org.json.JSONObject
import java.io.IOException

/**
 * HTTP 服务器实现
 * 监听 127.0.0.1:27123
 *
 * 路由：
 * - POST /cmd：接收命令
 * - GET /health：健康检查
 */
class MBBridgeHttpServer(
    private val context: Context,
    port: Int = PORT
) : NanoHTTPD(HOST, port) {

    companion object {
        private const val TAG = "MBBridgeCtrl"
        const val HOST = "127.0.0.1"
        const val PORT = 27123

        // SharedPreferences 键名
        private const val PREFS_NAME = "mbbridge_prefs"
        private const val KEY_TOKEN = "auth_token"
    }

    private var commandListener: CommandListener? = null

    interface CommandListener {
        fun onCommandReceived(command: Command)
    }

    fun setCommandListener(listener: CommandListener?) {
        this.commandListener = listener
    }

    /**
     * 获取配置的 Token（可能为空）
     */
    private fun getConfiguredToken(): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_TOKEN, null)?.takeIf { it.isNotEmpty() }
    }

    /**
     * 保存 Token
     */
    fun saveToken(token: String?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_TOKEN, token).apply()
        Log.i(TAG, "Token ${if (token.isNullOrEmpty()) "cleared" else "updated"}")
    }

    /**
     * 验证 Token
     */
    private fun verifyToken(session: IHTTPSession): Boolean {
        val expectedToken = getConfiguredToken() ?: return true // 未配置 token 则跳过验证
        val providedToken = session.headers.get("x-mbbridge-token") ?: session.headers.get("X-MBBridge-Token")
        return providedToken == expectedToken
    }

    override fun serve(session: IHTTPSession): Response {
        return try {
            val uri = session.uri
            val method = session.method

            Log.d(TAG, "Request: $method $uri from ${session.remoteIpAddress}")

            when {
                // POST /cmd - 接收命令
                method == Method.POST && uri == "/cmd" -> handleCommand(session)

                // GET /health - 健康检查
                method == Method.GET && uri == "/health" -> handleHealth()

                // 404 Not Found
                else -> handleNotFound()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling request", e)
            newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR,
                "application/json",
                HttpResponse.error("Internal server error: ${e.message}").toJson()
            )
        }
    }

    /**
     * 处理命令请求
     * POST /cmd
     * Body: { "v": 1, "ts": 1730000000000, "source": "mbbridge" }
     */
    private fun handleCommand(session: IHTTPSession): Response {
        // 验证 Token
        if (!verifyToken(session)) {
            Log.w(TAG, "Invalid token from ${session.remoteIpAddress}")
            return newFixedLengthResponse(
                Response.Status.UNAUTHORIZED,
                "application/json",
                HttpResponse.error("Unauthorized: Invalid or missing token").toJson()
            )
        }

        // 读取请求体
        val body = parseRequestBody(session)
        if (body.isNullOrBlank()) {
            Log.w(TAG, "Empty request body")
            return newFixedLengthResponse(
                Response.Status.BAD_REQUEST,
                "application/json",
                HttpResponse.error("Bad request: Empty body").toJson()
            )
        }

        Log.d(TAG, "Received command: $body")

        // 解析 JSON
        val command = Command.fromJson(body)
        if (command == null) {
            Log.w(TAG, "Invalid command JSON: $body")
            return newFixedLengthResponse(
                Response.Status.BAD_REQUEST,
                "application/json",
                HttpResponse.error("Bad request: Invalid JSON format").toJson()
            )
        }

        // 通知监听器
        commandListener?.onCommandReceived(command)

        // 返回成功响应
        return newFixedLengthResponse(
            Response.Status.OK,
            "application/json",
            HttpResponse.success().toJson()
        )
    }

    /**
     * 解析请求体
     */
    private fun parseRequestBody(session: IHTTPSession): String? {
        return try {
            // 读取 Content-Length
            val contentLength = session.headers["content-length"]?.toIntOrNull() ?: 0
            if (contentLength <= 0) return null

            // 读取 body
            val buffer = ByteArray(contentLength)
            session.inputStream.use { it.read(buffer) }
            String(buffer, Charsets.UTF_8)
        } catch (e: IOException) {
            Log.e(TAG, "Failed to read request body", e)
            null
        }
    }

    /**
     * 处理健康检查请求
     * GET /health
     */
    private fun handleHealth(): Response {
        return newFixedLengthResponse(
            Response.Status.OK,
            "application/json",
            HttpResponse.success(app = "MBBridgeCtrl").toJson()
        )
    }

    /**
     * 处理 404
     */
    private fun handleNotFound(): Response {
        return newFixedLengthResponse(
            Response.Status.NOT_FOUND,
            "application/json",
            HttpResponse.error("Not found").toJson()
        )
    }

    /**
     * 启动服务器
     */
    fun startServer(): Boolean {
        return try {
            start()
            Log.i(TAG, "HTTP Server started on $HOST:$PORT")
            true
        } catch (e: IOException) {
            Log.e(TAG, "Failed to start HTTP server", e)
            false
        }
    }

    /**
     * 停止服务器
     */
    fun stopServer() {
        try {
            stop()
            Log.i(TAG, "HTTP Server stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping HTTP server", e)
        }
    }
}
