# 编译和安装说明

## 快速编译

### Windows环境

```bash
cd C:\Users\xmz\yorkspace\github\XMBOX
gradlew.bat assembleDebug
```

### Linux/Mac环境

```bash
cd /path/to/XMBOX
./gradlew assembleDebug
```

## 编译输出位置

编译完成后，APK文件位于：

```
app/build/outputs/apk/leanback/debug/app-universal-leanback-debug.apk
app/build/outputs/apk/mobile/debug/app-universal-mobile-debug.apk
```

或者根据你的构建变体：
```
app/build/outputs/apk/
```

## 安装到设备

### 通过ADB安装

```bash
adb install -r app/build/outputs/apk/leanback/debug/app-universal-leanback-debug.apk
```

### 通过网络安装

1. 将APK文件复制到设备
2. 在设备上打开文件管理器
3. 点击APK文件进行安装

## 验证日志监控功能

安装新APK后：

1. **启用日志监控**
   ```
   http://192.168.31.132:9978/action?do=debug&action=log_monitor&enabled=true
   ```

2. **打开日志查看器**
   - 浏览器打开：`C:\Users\xmz\yorkspace\github\XMBOX\logs_viewer.html`
   - 或访问：`http://192.168.31.132:9978/logs.html`（编译后可用）

3. **生成测试日志**
   ```
   http://192.168.31.132:9978/action?do=debug&action=log_monitor&enabled=test
   ```

## 本次更新内容

### 新增功能
- ✅ 日志监控系统
- ✅ Web日志查看界面
- ✅ CORS跨域支持
- ✅ 调试开关控制

### 修改的文件
1. `app/src/main/java/com/fongmi/android/tv/utils/LogMonitor.java` (新建)
2. `app/src/main/java/com/fongmi/android/tv/server/process/LogMonitorProcess.java` (新建)
3. `app/src/main/assets/logs.html` (新建)
4. `app/src/main/java/com/fongmi/android/tv/server/Nano.java` (修改)
5. `app/src/main/java/com/fongmi/android/tv/Setting.java` (修改)
6. `app/src/main/java/com/fongmi/android/tv/server/process/Action.java` (修改 + CORS)
7. `catvod/src/main/java/com/github/catvod/utils/Logger.java` (修改)

### CORS支持
- LogMonitorProcess - 所有日志API接口支持跨域
- Action - debug接口支持跨域
- 允许从本地HTML文件访问API

## 故障排查

### 编译失败

1. **清理项目**
   ```bash
   gradlew clean
   ```

2. **重新同步**
   ```bash
   gradlew --refresh-dependencies
   ```

3. **检查Java版本**
   ```bash
   java -version
   ```
   需要JDK 11或更高版本

### 安装失败

1. **卸载旧版本**
   ```bash
   adb uninstall com.fongmi.android.tv
   ```

2. **重新安装**
   ```bash
   adb install app/build/outputs/apk/xxx.apk
   ```

### 日志监控不工作

1. 确认已启用日志监控
2. 检查服务器地址是否正确
3. 查看logcat确认服务是否启动
   ```bash
   adb logcat | grep XMBOX
   ```

## 临时使用方案（无需编译）

如果暂时不想编译，可以使用独立的日志查看器：

1. 用浏览器打开：`C:\Users\xmz\yorkspace\github\XMBOX\logs_viewer.html`
2. 确认服务器地址正确
3. 点击"启用日志监控"按钮
4. 点击"生成测试日志"查看效果

**注意：** 独立查看器需要重新编译APK后才能支持CORS跨域访问。
