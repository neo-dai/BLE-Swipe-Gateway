# MBBridge Controller - 项目结构

```
MBBridgeController/
├── app/
│   ├── src/main/
│   │   ├── java/com/mbbridge/controller/
│   │   │   ├── MainActivity.kt       # Compose UI
│   │   │   ├── MainViewModel.kt      # 状态管理与逻辑
│   │   │   ├── MBBridgeService.kt    # Foreground Service
│   │   │   ├── MBBridgeHttpServer.kt # NanoHTTPD 服务器
│   │   │   ├── CommandModel.kt       # 模型与 Token 存储
│   │   │   └── BootReceiver.kt       # 可选开机自启
│   │   ├── res/values/strings.xml
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── README.md
└── QUICKSTART.md
```
