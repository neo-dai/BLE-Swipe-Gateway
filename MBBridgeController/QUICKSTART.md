# MBBridge Controller - 快速开始指南

## 一、项目导入

### 方法 1：Android Studio 导入

1. 打开 Android Studio
2. 选择 `File` -> `Open`
3. 选择 `MBBridgeController` 目录
4. 等待 Gradle 同步完成

### 方法 2：命令行编译

```bash
cd MBBridgeController
./gradlew assembleDebug
```

## 二、安装到设备

### 方法 1：Android Studio

1. 连接 Android 设备（开启 USB 调试）
2. 点击工具栏的 Run 按钮（绿色三角形）
3. 选择目标设备

### 方法 2：命令行安装

```bash
# 检查设备连接
adb devices

# 安装 APK
./gradlew installDebug

# 或手动安装
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 三、快速测试

### 步骤 1：启动应用

1. 在手机上打开 "MBBridge Controller" 应用
2. 点击 **"启动服务器"** 按钮
3. 确认通知栏显示 "Listening on 127.0.0.1:27123"

### 步骤 2：模拟测试（最简单）

1. 在 App 内向下滚动到 **"模拟测试"** 区域
2. 点击 **"模拟 PREV"** 或 **"模拟 NEXT"** 按钮
3. 观察 UI 更新：
   - 统计计数增加
   - 最近命令更新
   - 日志显示新记录

### 步骤 3：网络测试（ADB）

```bash
# 健康检查
adb shell curl http://127.0.0.1:27123/health

# 发送 PREV 命令
adb shell curl -X POST http://127.0.0.1:27123/cmd \
  -H "Content-Type: application/json" \
  -d '{"v":1,"ts":'$(date +%s%3N)',"source":"test"}'

# 发送 NEXT 命令
adb shell curl -X POST http://127.0.0.1:27123/cmd \
  -H "Content-Type: application/json" \
  -d '{"v":2,"ts":'$(date +%s%3N)',"source":"test"}'
```

### 步骤 4：使用测试脚本（可选）

```bash
# Bash 脚本
cd MBBridgeController
./test.sh

# Python 脚本（需要先设置端口转发）
adb forward tcp:27123 tcp:27123
pip install requests
python3 test.py
```

## 四、验证功能

检查以下功能是否正常：

### ✓ 服务器控制
- [ ] 启动服务器后通知栏显示常驻通知
- [ ] 停止服务器后通知消失
- [ ] 状态显示 "运行中" / "已停止"

### ✓ 命令接收
- [ ] 模拟 PREV 按钮后，PREV 计数 +1
- [ ] 模拟 NEXT 按钮后，NEXT 计数 +1
- [ ] 总计数正确累加
- [ ] 最近命令显示正确的类型、时间、来源

### ✓ 日志记录
- [ ] 每次操作后日志自动添加记录
- [ ] 日志格式正确：`[时间] 类型 (v=1, source=xxx)`
- [ ] 清空日志按钮正常工作

### ✓ 设置功能
- [ ] Token 设置可以保存
- [ ] 打开无障碍设置按钮跳转到系统设置页

## 五、常见问题

### Q1: ADB 提示 "device offline"

**解决方案**：
```bash
adb kill-server
adb start-server
adb devices
```

### Q2: 应用启动后崩溃

**检查**：
- Android 版本是否 ≥ Android 10 (API 29)
- 是否授予了通知权限（Android 13+）

**查看日志**：
```bash
adb logcat | grep MBBridgeCtrl
```

### Q3: 无法启动服务器

**可能原因**：
- 端口 27123 已被占用
- 权限不足

**排查**：
```bash
# 检查端口占用
adb shell netstat -an | grep 27123

# 查看日志
adb logcat | grep MBBridgeCtrl
```

### Q4: curl 命令不存在

**替代方案**：使用 `wget`
```bash
adb shell wget --post-data='{"v":1,"ts":1730000000000,"source":"test"}' \
  --header='Content-Type: application/json' \
  -O - http://127.0.0.1:27123/cmd
```

或使用 `Postman` / `Apifox` 等 GUI 工具。

## 六、下一步

### 1. 对接 Zepp Side Service

在 Zepp 手表应用的 Side Service 中添加 HTTP 转发：

```javascript
// 示例：发送 PREV 命令
fetch('http://127.0.0.1:27123/cmd', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'X-MBBridge-Token': 'your_token'  // 可选
  },
  body: JSON.stringify({
    v: 1,  // 1=PREV, 2=NEXT
    ts: Date.now(),
    source: 'zepp'
  })
})
.then(res => res.json())
.then(data => console.log('Response:', data))
.catch(err => console.error('Error:', err));
```

### 2. 添加无障碍功能

创建 `AccessibilityService` 来执行实际操作：

```kotlin
class MBBridgeAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {
        // 服务连接
    }

    fun performGlobalAction(action: Int) {
        // 执行全局操作（如音量控制、媒体切换等）
    }
}
```

### 3. 开机自启

在 `AndroidManifest.xml` 中启用 `BootReceiver`：

```xml
<receiver
    android:name=".BootReceiver"
    android:enabled="true"  <!-- 改为 true -->
    android:exported="false">
```

## 七、调试技巧

### 查看 Logcat

```bash
# 只看本应用日志
adb logcat MBBridgeCtrl *:S

# 过滤特定标签
adb logcat | grep -E "(MBBridgeCtrl|NanoHTTPD)"

# 保存到文件
adb logcat > logcat.txt
```

### 抓取网络包

```bash
# 监听本地端口
adb shell tcpdump -i any -n port 27123
```

### 导出数据库

```bash
# 导出 SharedPreferences
adb shell run-as com.mbbridge.controller cat /data/data/com.mbbridge.controller/shared_prefs/mbbridge_prefs.xml
```

## 八、项目文件说明

| 文件 | 说明 |
|------|------|
| `CommandModel.kt` | 数据模型定义 |
| `MBBridgeHttpServer.kt` | HTTP 服务器核心 |
| `MBBridgeService.kt` | 前台服务 |
| `MainActivity.kt` | 主界面 UI |
| `MainViewModel.kt` | 状态管理 |
| `BootReceiver.kt` | 开机启动（可选） |

## 九、技术支持

- 查看 README.md 获取详细文档
- 运行 `./test.sh` 进行自动化测试
- 检查 logcat 日志排查问题
