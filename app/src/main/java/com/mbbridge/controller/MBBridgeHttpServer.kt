package com.mbbridge.controller

import android.content.Context
import android.util.Log
import fi.iki.elonen.NanoHTTPD
import java.util.concurrent.atomic.AtomicBoolean

/**
 * HTTP 服务器实现（NanoHTTPD 方案）
 * 监听 127.0.0.1:27123
 */
class MBBridgeHttpServer(
    private val context: Context,
    private val port: Int = DEFAULT_PORT
) : NanoHTTPD(HOST, port) {

    companion object {
        private const val TAG = "MBBridgeCtrl"
        const val HOST = "127.0.0.1"
        const val DEFAULT_PORT = 27123
    }

    private val running = AtomicBoolean(false)
    private var commandListener: CommandListener? = null
    private var logListener: LogListener? = null

    interface CommandListener {
        fun onCommandReceived(command: Command)
    }

    interface LogListener {
        fun onLog(level: LogLevel, message: String)
    }

    fun setCommandListener(listener: CommandListener?) {
        this.commandListener = listener
    }

    fun setLogListener(listener: LogListener?) {
        this.logListener = listener
    }

    fun isRunning(): Boolean = running.get()

    override fun serve(session: IHTTPSession): Response {
        return try {
            when {
                session.method == Method.POST && session.uri == "/cmd" -> handleCommand(session)
                session.method == Method.GET && session.uri == "/health" -> handleHealth()
                else -> jsonResponse(Response.Status.NOT_FOUND, HttpResponse.error("Not found"))
            }
        } catch (e: Exception) {
            log(LogLevel.ERROR, "HTTP error: ${e.message}")
            jsonResponse(Response.Status.INTERNAL_ERROR, HttpResponse.error("Internal error"))
        }
    }

    private fun handleHealth(): Response {
        log(LogLevel.INFO, "GET /health")
        return jsonResponse(Response.Status.OK, HttpResponse.success(app = "MBBridgeCtrl"))
    }

    private fun handleCommand(session: IHTTPSession): Response {
        log(LogLevel.INFO, "POST /cmd")

        if (!TokenStore(context).verify(session.headers)) {
            log(LogLevel.WARN, "Token 校验失败")
            return jsonResponse(
                Response.Status.UNAUTHORIZED,
                HttpResponse.error("Unauthorized: Invalid or missing token")
            )
        }

        val body = parseBodyText(session)
        if (body.isNullOrBlank()) {
            return jsonResponse(Response.Status.BAD_REQUEST, HttpResponse.error("Bad request: Empty body"))
        }

        val command = Command.fromJson(body)
            ?: return jsonResponse(Response.Status.BAD_REQUEST, HttpResponse.error("Bad request: Invalid JSON"))

        commandListener?.onCommandReceived(command)
        log(
            LogLevel.INFO,
            "Command: ${command.getCommandType()} v=${command.v} ts=${command.ts} source=${command.source}"
        )
        return jsonResponse(Response.Status.OK, HttpResponse.success())
    }

    private fun parseBodyText(session: IHTTPSession): String? {
        return try {
            val files = HashMap<String, String>()
            session.parseBody(files)
            files["postData"]
        } catch (e: Exception) {
            log(LogLevel.ERROR, "Parse body failed: ${e.message}")
            null
        }
    }

    private fun jsonResponse(status: Response.Status, body: HttpResponse): Response {
        return newFixedLengthResponse(status, "application/json", body.toJson())
    }

    fun startServer(): Boolean {
        return try {
            start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
            running.set(true)
            log(LogLevel.INFO, "HTTP server started on $HOST:$port")
            true
        } catch (e: Exception) {
            log(LogLevel.ERROR, "Start server failed: ${e.message}")
            false
        }
    }

    fun stopServer() {
        if (!running.get()) {
            return
        }
        try {
            stop()
        } finally {
            running.set(false)
            log(LogLevel.INFO, "HTTP server stopped")
        }
    }

    private fun log(level: LogLevel, message: String) {
        when (level) {
            LogLevel.VERBOSE -> Log.v(TAG, message)
            LogLevel.DEBUG -> Log.d(TAG, message)
            LogLevel.INFO -> Log.i(TAG, message)
            LogLevel.WARN -> Log.w(TAG, message)
            LogLevel.ERROR -> Log.e(TAG, message)
        }
        logListener?.onLog(level, message)
    }
}
