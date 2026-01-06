# MBBridge Controller

用于接收 Zepp App Side Service 转发的本机 HTTP 指令，并更新 UI 与日志。服务以 Foreground Service 常驻，监听 `127.0.0.1:27123`。

## 功能
- 本机 HTTP Server：`POST /cmd`、`GET /health`
- 前台服务常驻：通知显示 `Listening on 127.0.0.1:27123`
- UI 展示：服务器状态、最近命令、计数器、日志
- 模拟测试：App 内按钮直接更新 UI
- Token 校验：可选 `X-MBBridge-Token`

## 技术栈
- Kotlin + Jetpack Compose
- NanoHTTPD 2.3.1
- MVVM（ViewModel + StateFlow）

## 命令协议
`POST http://127.0.0.1:27123/cmd`
```json
{ "v": 1, "ts": 1730000000000, "source": "mbbridge" }
```
- `v`: 1 = PREV, 2 = NEXT
- 成功：`{ "ok": 1 }`
- 失败：`{ "ok": 0, "err": "..." }`

健康检查：
```bash
GET http://127.0.0.1:27123/health
```
响应：
```json
{ "ok": 1, "app": "MBBridgeCtrl" }
```

## 运行与测试
```bash
cd MBBridgeController
./gradlew assembleDebug
./gradlew installDebug
```

### App 内模拟
点击 **“模拟 PREV / 模拟 NEXT”**，无需网络即可验证 UI 与日志。

### ADB 测试
```bash
adb shell curl -X POST http://127.0.0.1:27123/cmd \
  -H "Content-Type: application/json" \
  -d '{"v":1,"ts":1730000000000,"source":"test"}'

adb shell curl http://127.0.0.1:27123/health
```

### Token 校验（可选）
1. 在 App 中设置 Token 并保存
2. 请求携带 Header：
```bash
X-MBBridge-Token: your_token
```

## 目录结构
```
MBBridgeController/app/src/main/java/com/mbbridge/controller/
├── MainActivity.kt
├── MainViewModel.kt
├── MBBridgeService.kt
├── MBBridgeHttpServer.kt
├── CommandModel.kt
└── BootReceiver.kt
```
