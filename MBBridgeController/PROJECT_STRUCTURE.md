# MBBridge Controller - 项目结构与核心代码说明

## 项目目录结构

```
MBBridgeController/
├── app/
│   ├── src/main/
│   │   ├── java/com/mbbridge/controller/
│   │   │   ├── MainActivity.kt              # 主 Activity（Jetpack Compose UI）
│   │   │   ├── MainViewModel.kt            # ViewModel（状态管理）
│   │   │   ├── CommandModel.kt             # 数据模型（Command, HttpResponse, Stats）
│   │   │   ├── MBBridgeService.kt          # 前台服务（保持服务器运行）
│   │   │   ├── MBBridgeHttpServer.kt       # HTTP 服务器（NanoHTTPD 实现）
│   │   │   ├── BootReceiver.kt             # 开机启动接收器（可选）
│   │   │   └── ui/theme/
│   │   │       ├── Theme.kt                # Compose 主题配置
│   │   │       └── Type.kt                 # 字体样式
│   │   ├── res/
│   │   │   ├── values/
│   │   │   │   ├── strings.xml             # 字符串资源
│   │   │   │   ├── colors.xml              # 颜色定义
│   │   │   │   └── themes.xml              # 主题样式
│   │   │   ├── xml/
│   │   │   │   ├── backup_rules.xml        # 备份规则
│   │   │   │   └── data_extraction_rules.xml
│   │   │   ├── layout/                     # 空目录（使用 Compose）
│   │   │   └── mipmap-*/                   # 应用图标（待添加）
│   │   └── AndroidManifest.xml             # 应用清单文件
│   ├── build.gradle.kts                    # 应用级 Gradle 配置
│   └── proguard-rules.pro                  # ProGuard 混淆规则
├── gradle/
│   └── wrapper/
│       └── gradle-wrapper.properties       # Gradle Wrapper 配置
├── build.gradle.kts                        # 项目级 Gradle 配置
├── settings.gradle.kts                     # Gradle 设置
├── gradle.properties                       # Gradle 属性
├── gradlew                                 # Gradle Wrapper 脚本（Unix）
├── gradlew.bat                             # Gradle Wrapper 脚本（Windows，待添加）
├── README.md                               # 项目说明文档
├── QUICKSTART.md                           # 快速开始指南
├── test.sh                                 # Bash 测试脚本
├── test.py                                 # Python 测试脚本
└── .gitignore                              # Git 忽略文件
```

## 核心代码说明

### 1. HTTP 服务器实现 (`MBBridgeHttpServer.kt`)

#### 核心功能
- 使用 **NanoHTTPD** 库实现轻量级 HTTP 服务器
- 监听 `127.0.0.1:27123`
- 支持两个路由：`POST /cmd` 和 `GET /health`

#### 关键代码段

**启动服务器**：
```kotlin
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
```

**处理命令请求**：
```kotlin
private fun handleCommand(session: IHTTPSession): Response {
    // 验证 Token
    if (!verifyToken(session)) {
        return newFixedLengthResponse(
            Response.Status.UNAUTHORIZED,
            "application/json",
            HttpResponse.error("Unauthorized: Invalid or missing token").toJson()
        )
    }

    // 读取并解析请求体
    val body = parseRequestBody(session)
    val command = Command.fromJson(body) ?: run {
        return newFixedLengthResponse(
            Response.Status.BAD_REQUEST,
            "application/json",
            HttpResponse.error("Bad request: Invalid JSON format").toJson()
        )
    }

    // 通知监听器
    commandListener?.onCommandReceived(command)

    // 返回成功
    return newFixedLengthResponse(
        Response.Status.OK,
        "application/json",
        HttpResponse.success().toJson()
    )
}
```

**Token 验证**：
```kotlin
private fun verifyToken(session: IHTTPSession): Boolean {
    val expectedToken = getConfiguredToken() ?: return true // 未配置则跳过
    val providedToken = session.headers.get("x-mbbridge-token")
        ?: session.headers.get("X-MBBridge-Token")
    return providedToken == expectedToken
}
```

---

### 2. 前台服务实现 (`MBBridgeService.kt`)

#### 核心功能
- 保持 HTTP 服务器持续运行（即使用户离开应用）
- 显示常驻通知
- 支持绑定和启动两种模式

#### 关键代码段

**启动服务器**：
```kotlin
private fun startHttpServer() {
    if (isRunning) return

    val server = httpServer ?: return
    if (server.startServer()) {
        isRunning = true
        startForeground(NOTIFICATION_ID, createNotification())
        Log.i(TAG, "HTTP server started successfully")
    }
}
```

**创建前台通知**：
```kotlin
private fun createNotification(): Notification {
    val notificationIntent = Intent(this, MainActivity::class.java)
    val pendingIntent = PendingIntent.getActivity(
        this, 0, notificationIntent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    return NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle(getString(R.string.notification_title))
        .setContentText(getString(R.string.notification_text))
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentIntent(pendingIntent)
        .setOngoing(true)
        .build()
}
```

---

### 3. 数据模型 (`CommandModel.kt`)

#### 数据类定义

**命令模型**：
```kotlin
data class Command(
    val v: Int,           // 命令类型：1=PREV, 2=NEXT
    val ts: Long,         // 时间戳（毫秒）
    val source: String    // 来源标识
) {
    fun getCommandType(): CommandType {
        return when (v) {
            1 -> CommandType.PREV
            2 -> CommandType.NEXT
            else -> CommandType.UNKNOWN(v)
        }
    }

    companion object {
        fun fromJson(jsonString: String): Command? {
            return try {
                val json = JSONObject(jsonString)
                Command(
                    v = json.getInt("v"),
                    ts = json.getLong("ts"),
                    source = json.getString("source")
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
```

**HTTP 响应模型**：
```kotlin
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
```

---

### 4. 状态管理 (`MainViewModel.kt`)

#### 核心功能
- 使用 `StateFlow` 管理 UI 状态
- 处理命令接收并更新 UI
- 管理与服务器的连接

#### 关键代码段

**UI 状态**：
```kotlin
data class UiState(
    val isServerRunning: Boolean = false,
    val lastCommand: Command? = null,
    val stats: CommandStats = CommandStats(),
    val logs: List<String> = emptyList(),
    val token: String = ""
)
```

**接收命令**：
```kotlin
override fun onCommandReceived(command: Command) {
    viewModelScope.launch {
        val currentState = _uiState.value
        val newStats = currentState.stats.apply {
            increment(command.getCommandType())
        }

        val logEntry = buildLogEntry(command)
        val newLogs = (listOf(logEntry) + currentState.logs).take(MAX_LOGS)

        _uiState.value = currentState.copy(
            lastCommand = command,
            stats = newStats,
            logs = newLogs
        )
    }
}
```

**服务绑定**：
```kotlin
private val serviceConnection = object : ServiceConnection {
    override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
        val localBinder = binder as? MBBridgeService.LocalBinder
        service = localBinder?.getService()
        service?.setCommandListener(this@MainViewModel)
        updateServerStatus()
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        service = null
        updateServerStatus()
    }
}
```

---

### 5. UI 实现 (`MainActivity.kt` - Jetpack Compose)

#### 核心组件

**服务器控制卡片**：
```kotlin
@Composable
fun ServerControlCard(
    isRunning: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isRunning)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = context.getString(R.string.server_status),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (isRunning) context.getString(R.string.server_running)
                    else context.getString(R.string.server_stopped),
                color = if (isRunning) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error
            )
            Button(
                onClick = if (isRunning) onStop else onStart,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isRunning) context.getString(R.string.stop_server)
                    else context.getString(R.string.start_server))
            }
        }
    }
}
```

**统计数据卡片**：
```kotlin
@Composable
fun StatsCard(stats: CommandStats) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem("PREV", stats.prevCount.toString(), primaryColor)
            StatItem("NEXT", stats.nextCount.toString(), secondaryColor)
            StatItem("Total", stats.totalCount.toString(), tertiaryColor)
        }
    }
}
```

---

## 数据流说明

### 命令接收流程

```
Zepp App
    │
    │ HTTP POST /cmd
    ↓
MBBridgeHttpServer (NanoHTTPD)
    │
    │ 解析 JSON → 验证 Token
    ↓
CommandListener.onCommandReceived()
    │
    ↓
MainViewModel.onCommandReceived()
    │
    │ 更新 StateFlow
    ↓
MainActivity (Compose UI)
    │
    │ collectAsState()
    ↓
UI 自动更新
```

### 服务启动流程

```
MainActivity
    │
    │ startServer()
    ↓
MBBridgeService.startService()
    │
    │ onStartCommand(ACTION_START)
    ↓
startHttpServer()
    │
    │ MBBridgeHttpServer.start()
    ↓
startForeground(NOTIFICATION_ID, notification)
    │
    ↓
服务器运行中（通知栏显示）
```

---

## 关键技术点

### 1. NanoHTTPD 使用

- **优点**：轻量级、纯 Java、支持 HTTPS
- **适用场景**：本地 HTTP 服务器、嵌入式系统
- **路由处理**：在 `serve()` 方法中根据 URI 分发

### 2. Jetpack Compose 状态管理

- **StateFlow**：持有不可变的 UI 状态
- **collectAsState()**：将 Flow 转换为 Compose State
- **remember**：在 Composable 中缓存对象

### 3. Android 前台服务

- **必需条件**：创建通知渠道、显示常驻通知
- **生命周期**：独立于 Activity，适合长时间运行
- **API 兼容**：Android 14+ 需要指定 `foregroundServiceType`

### 4. Service 绑定通信

- **Binder**：Activity 与 Service 之间的通信桥梁
- **ServiceConnection**：管理绑定生命周期
- **回调模式**：通过 Listener 接收命令

---

## 扩展建议

### 1. 添加 WebSocket 支持

```kotlin
// 可使用 OkHttp WebSocket 或 Java-WebSocket
// 实现双向通信、推送更新
```

### 2. 数据持久化

```kotlin
// 使用 Room Database 存储历史命令
@Entity
data class CommandHistory(
    @PrimaryKey val id: Long,
    val commandType: Int,
    val timestamp: Long,
    val source: String
)
```

### 3. 无障碍功能集成

```kotlin
class MBBridgeAccessibilityService : AccessibilityService() {
    fun performAction(action: Int) {
        // 执行全局操作
        performGlobalAction(GLOBAL_ACTION_BACK)
        // 或模拟按键
    }
}
```

### 4. 添加 Web UI

```kotlin
// 在 HTTP 服务器中添加静态资源路由
// 使用 Compose for Web 或纯 HTML
```

---

## 调试技巧

### Logcat 过滤

```bash
# 只看应用日志
adb logcat MBBridgeCtrl *:S

# 过滤特定标签
adb logcat | grep -E "(MBBridgeCtrl|NanoHTTPD)"
```

### 网络调试

```bash
# 监听端口
adb shell netstat -an | grep 27123

# 抓包
adb shell tcpdump -i any -n port 27123
```

### 数据查看

```bash
# 导出 SharedPreferences
adb shell run-as com.mbbridge.controller \
  cat /data/data/com.mbbridge.controller/shared_prefs/mbbridge_prefs.xml
```

---

## 依赖说明

| 依赖 | 版本 | 用途 |
|------|------|------|
| NanoHTTPD | 2.3.1 | HTTP 服务器 |
| Jetpack Compose | BOM 2024.02.00 | 现代 UI 框架 |
| Lifecycle | 2.7.0 | 生命周期管理 |
| Material 3 | 2024.02.00 | Material Design 3 组件 |

---

## 安全考虑

1. **仅监听本地**：127.0.0.1 避免外网访问
2. **Token 验证**：可选的请求头认证
3. **输入验证**：JSON 解析失败返回 400
4. **权限最小化**：只申请必要权限

---

## 性能优化

1. **协程使用**：网络请求使用 viewModelScope
2. **日志限制**：最多保留 50 条记录
3. **不可变状态**：UiState 使用 data class
4. **懒加载**：按需初始化服务器

---

## 总结

本项目实现了一个完整的 Android HTTP 服务器应用，具备：

- ✅ 本地 HTTP 服务器（NanoHTTPD）
- ✅ 前台服务常驻
- ✅ 命令接收与处理
- ✅ 现代 UI（Jetpack Compose）
- ✅ 状态管理（MVVM）
- ✅ 可选 Token 认证
- ✅ 日志与统计
- ✅ 模拟测试功能
- ✅ 完整的测试脚本

代码结构清晰、易于扩展，适合作为 Zepp 手表与 Android 手机交互的基础框架。
