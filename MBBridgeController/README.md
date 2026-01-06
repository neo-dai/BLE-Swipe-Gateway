# MBBridge Controller

Android 应用程序，用于接收来自 Zepp App 容器内 Side Service 通过 HTTP 转发的命令。

## 功能特性

- **本地 HTTP 服务器**：监听 127.0.0.1:27123
- **前台服务**：保持服务器持续运行
- **命令接收**：接收 PREV/NEXT 命令并记录
- **统计功能**：计数器显示命令接收次数
- **模拟测试**：内置按钮模拟命令（无需网络）
- **Token 认证**：可选的请求头 Token 验证

## 技术栈

- **语言**：Kotlin
- **UI**：Jetpack Compose
- **HTTP 服务器**：NanoHTTPD 2.3.1
- **架构**：MVVM + Repository

## 安装

1. 使用 Android Studio 打开项目
2. 同步 Gradle 依赖
3. 连接 Android 设备或启动模拟器（Android 10+）
4. 点击 Run 按钮

## 使用方法

### 启动服务器

1. 打开应用
2. 点击 "启动服务器" 按钮
3. 确认通知栏显示 "Listening on 127.0.0.1:27123"

### 接收命令

#### 命令格式

**请求**：
```bash
POST http://127.0.0.1:27123/cmd
Content-Type: application/json

{
  "v": 1,
  "ts": 1730000000000,
  "source": "mbbridge"
}
```

**参数说明**：
- `v`: 命令类型
  - `1` = PREV
  - `2` = NEXT
- `ts`: 时间戳（毫秒）
- `source`: 来源标识

**响应**：
```json
{
  "ok": 1
}
```

**错误响应**：
```json
{
  "ok": 0,
  "err": "错误信息"
}
```

#### Token 认证（可选）

1. 在 App 设置中输入 Token
2. 发送请求时添加 Header：
```bash
X-MBBridge-Token: your_token_here
```

如果 Token 不匹配或缺失，服务器返回 401 Unauthorized。

### 健康检查

```bash
GET http://127.0.0.1:27123/health
```

**响应**：
```json
{
  "ok": 1,
  "app": "MBBridgeCtrl"
}
```

## 测试方法

### 方法 1：App 内模拟测试

1. 启动服务器
2. 点击 "模拟 PREV" 或 "模拟 NEXT" 按钮
3. 查看 UI 更新和统计变化

### 方法 2：ADB Shell（推荐）

```bash
# 发送 PREV 命令
adb shell curl -X POST http://127.0.0.1:27123/cmd \
  -H "Content-Type: application/json" \
  -d '{"v":1,"ts":1730000000000,"source":"test"}'

# 发送 NEXT 命令
adb shell curl -X POST http://127.0.0.1:27123/cmd \
  -H "Content-Type: application/json" \
  -d '{"v":2,"ts":1730000000000,"source":"test"}'

# 健康检查
adb shell curl http://127.0.0.1:27123/health

# 带 Token 的请求
adb shell curl -X POST http://127.0.0.1:27123/cmd \
  -H "Content-Type: application/json" \
  -H "X-MBBridge-Token: your_token" \
  -d '{"v":1,"ts":1730000000000,"source":"test"}'
```

### 方法 3：Postman/Apifox

1. 新建请求：
   - Method: POST
   - URL: `http://127.0.0.1:27123/cmd`
2. Headers：
   - `Content-Type: application/json`
   - `X-MBBridge-Token: your_token`（可选）
3. Body（raw JSON）：
   ```json
   {
     "v": 1,
     "ts": 1730000000000,
     "source": "test"
   }
   ```

### 方法 4：Python 脚本

```python
import requests
import json
import time

url = "http://127.0.0.1:27123/cmd"
headers = {
    "Content-Type": "application/json",
    # "X-MBBridge-Token": "your_token"  # 可选
}

# 发送 PREV 命令
data = {
    "v": 1,
    "ts": int(time.time() * 1000),
    "source": "python_test"
}

response = requests.post(url, headers=headers, data=json.dumps(data))
print(f"Status: {response.status_code}")
print(f"Response: {response.json()}")
```

## 项目结构

```
app/src/main/java/com/mbbridge/controller/
├── MainActivity.kt              # 主 Activity（Compose UI）
├── MainViewModel.kt            # ViewModel（状态管理）
├── CommandModel.kt             # 数据模型
├── MBBridgeService.kt          # 前台服务
├── MBBridgeHttpServer.kt       # HTTP 服务器
├── BootReceiver.kt             # 开机启动接收器（可选）
└── ui/theme/                   # Compose 主题
    ├── Theme.kt
    └── Type.kt
```

## 权限说明

- `INTERNET`：网络通信（即使监听本地也需要）
- `FOREGROUND_SERVICE`：前台服务
- `FOREGROUND_SERVICE_SPECIAL_USE`：Android 14+ 前台服务类型
- `RECEIVE_BOOT_COMPLETED`：开机自启（可选，默认禁用）
- `POST_NOTIFICATIONS`：Android 13+ 通知权限

## 后续扩展

### 无障碍服务集成

1. 点击 App 内 "打开无障碍设置" 按钮
2. 启用对应的无障碍服务
3. 接收到命令后执行对应动作（如音量控制、媒体切换等）

### 开机自启

1. 在 `AndroidManifest.xml` 中启用 `BootReceiver`：
```xml
<receiver
    android:name=".BootReceiver"
    android:enabled="true"  <!-- 改为 true -->
    android:exported="false">
```

2. 首次启动后需要手动启动一次服务，之后开机会自动启动

## 常见问题

### Q: 无法启动服务器？

A: 检查是否有其他应用占用了 27123 端口。使用以下命令查看：
```bash
adb shell netstat -an | grep 27123
```

### Q: ADB Shell 没有 curl 命令？

A: 使用 `wget` 代替：
```bash
adb shell wget --post-data='{"v":1,"ts":1730000000000,"source":"test"}' \
  --header='Content-Type: application/json' \
  -O - http://127.0.0.1:27123/cmd
```

### Q: Token 验证总是失败？

A: 检查 Header 名称是否正确（`X-MBBridge-Token`），区分大小写。

## 许可证

本项目仅供学习和研究使用。
