# MBBridge Controller - 快速开始

## 1. 导入与编译
```bash
./gradlew assembleDebug
```

## 2. 安装到设备
```bash
./gradlew installDebug
```

## 3. 启动服务器
打开 App，点击 **“启动服务器”**，通知栏应显示：
`Listening on 127.0.0.1:27123`

## 4. 快速测试
### App 内模拟
点击 **“模拟 PREV / 模拟 NEXT”**，观察统计与日志更新。

### ADB 测试
```bash
adb shell curl -X POST http://127.0.0.1:27123/cmd \
  -H "Content-Type: application/json" \
  -d '{"v":1,"ts":1730000000000,"source":"test"}'

adb shell curl http://127.0.0.1:27123/health
```

## 5. Token（可选）
1. 在 App 中设置 Token 并保存
2. 请求带 Header：
```bash
X-MBBridge-Token: your_token
```
