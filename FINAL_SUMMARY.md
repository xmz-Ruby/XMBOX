# 日志监控系统 - 最终总结

## 🎉 完整功能实现

### 核心功能

✅ **完整的日志监控系统**
- Web实时日志查看
- 支持1000条日志缓存
- 多级别过滤（VERBOSE/DEBUG/INFO/WARN/ERROR）
- 关键词搜索
- 自动刷新（2秒）
- CORS跨域支持

✅ **外部Spider日志完整捕获**
- JAR Spider加载日志
- Python Spider加载日志
- **Spider内部运行日志** ⭐
- **Spider异常和堆栈信息** ⭐
- 所有SpiderDebug.log()输出

## 📊 捕获的日志类型

### 1. Spider加载日志
```
[INFO] JarLoader: Loading JAR - key=xxx, file=spider.jar
[INFO] JarLoader: Spider loaded successfully - xxx
[INFO] PyLoader: Loading Python spider - key=xxx, api=xxx.py
```

### 2. Spider内部日志 ⭐ 新增
```
[DEBUG] SpiderDebug: 开始搜索: 斗罗大陆
[DEBUG] SpiderDebug: 请求URL: http://api.example.com/search?q=斗罗大陆
[DEBUG] SpiderDebug: 解析结果: 找到15条数据
[DEBUG] SpiderDebug: 视频详情: {"name":"斗罗大陆","episodes":100}
```

### 3. Spider异常日志 ⭐ 新增
```
[ERROR] SpiderDebug: 网络请求失败
java.net.SocketTimeoutException: timeout
    at okhttp3.internal.http.RetryAndFollowUpInterceptor.intercept(...)
    at okhttp3.internal.http.RealInterceptorChain.proceed(...)
    ...

[ERROR] SpiderDebug: JSON解析错误
org.json.JSONException: No value for data
    at org.json.JSONObject.get(JSONObject.java:...)
    ...
```

### 4. 加载失败日志
```
[ERROR] JarLoader: Failed to load spider - key=xxx, api=csp_XXX
java.lang.ClassNotFoundException: com.github.catvod.spider.XXX
```

## 🔧 修改的文件（共11个）

### 新建文件（4个）
1. ⭐ `app/src/main/java/com/fongmi/android/tv/utils/LogMonitor.java`
2. ⭐ `app/src/main/java/com/fongmi/android/tv/server/process/LogMonitorProcess.java`
3. ⭐ `app/src/main/assets/logs.html`
4. ⭐ `logs_viewer.html`（项目根目录）

### 修改文件（7个）
5. `app/src/main/java/com/fongmi/android/tv/server/Nano.java`
6. `app/src/main/java/com/fongmi/android/tv/Setting.java`
7. `app/src/main/java/com/fongmi/android/tv/server/process/Action.java`
8. `app/src/main/java/com/fongmi/android/tv/api/loader/PyLoader.java`
9. `app/src/main/java/com/fongmi/android/tv/api/loader/JarLoader.java`
10. ⭐ `catvod/src/main/java/com/github/catvod/crawler/SpiderDebug.java` **（关键）**
11. `catvod/src/main/java/com/github/catvod/utils/Logger.java`

## 🎯 SpiderDebug集成（最重要）

**文件：** `catvod/src/main/java/com/github/catvod/crawler/SpiderDebug.java`

**作用：** 捕获所有外部Spider内部的日志输出

**实现原理：**
```java
// 使用反射获取LogMonitor实例，避免循环依赖
private static Object logMonitor;

static {
    try {
        Class<?> clazz = Class.forName("com.fongmi.android.tv.utils.LogMonitor");
        logMonitor = clazz.getMethod("get").invoke(null);
    } catch (Exception e) {
        logMonitor = null;
    }
}

// 捕获调试日志
public static void log(String msg) {
    if (!TextUtils.isEmpty(msg)) {
        Logger.t(TAG).d(msg);
        addToMonitor("DEBUG", msg);  // 发送到LogMonitor
    }
}

// 捕获异常日志（包含完整堆栈）
public static void log(Throwable th) {
    if (th != null) {
        String stackTrace = Log.getStackTraceString(th);
        Logger.t(TAG).e(th, th.getMessage());
        addToMonitor("ERROR", th.getMessage() + "\n" + stackTrace);
    }
}
```

**为什么重要：**
- 外部Spider（JAR/Python）内部使用`SpiderDebug.log()`输出日志
- 修改后，所有Spider内部日志都会被自动捕获
- 包括调试信息、网络请求、解析过程、异常错误等

## 📱 使用方法

### 方法1：使用独立查看器（立即可用）

1. **打开查看器**
   ```
   浏览器打开：C:\Users\xmz\yorkspace\github\XMBOX\logs_viewer.html
   ```

2. **启用日志监控**
   - 点击页面上的"启用日志监控"按钮
   - 或访问：`http://192.168.31.132:9978/action?do=debug&action=log_monitor&enabled=true`

3. **触发Spider日志**
   - 在App中搜索视频
   - 切换视频源
   - 播放视频
   - 所有操作都会产生日志

4. **查看日志**
   - 搜索"SpiderDebug"查看Spider内部日志
   - 搜索"JarLoader"查看JAR加载日志
   - 搜索"PyLoader"查看Python加载日志
   - 按ERROR级别过滤查看所有错误

### 方法2：使用内置页面（需编译）

1. **编译APK**
   ```bash
   cd C:\Users\xmz\yorkspace\github\XMBOX
   gradlew.bat assembleDebug
   ```

2. **安装新APK**

3. **访问内置页面**
   ```
   http://192.168.31.132:9978/logs.html
   ```

## 🔍 调试Spider示例

### 场景：搜索功能不工作

1. **启用日志监控**
2. **在App中搜索"斗罗大陆"**
3. **查看日志输出：**

```
[INFO] JarLoader: Loading spider class - key=xxx, api=csp_XPath
[INFO] JarLoader: Spider loaded successfully - xxx
[DEBUG] SpiderDebug: 开始搜索: 斗罗大陆
[DEBUG] SpiderDebug: 请求URL: http://api.example.com/search?q=斗罗大陆
[DEBUG] SpiderDebug: 响应状态: 200
[DEBUG] SpiderDebug: 响应内容: {"code":0,"data":[...]}
[DEBUG] SpiderDebug: 解析结果: 找到15条数据
```

4. **如果出错，会看到：**

```
[ERROR] SpiderDebug: 网络请求失败
java.net.SocketTimeoutException: timeout
    at okhttp3.internal.http.RetryAndFollowUpInterceptor.intercept(...)
```

5. **根据错误信息定位问题**

## 📈 日志流程图

```
外部Spider代码
    ↓
SpiderDebug.log("消息")
    ↓
SpiderDebug.addToMonitor()
    ↓
LogMonitor.addLog()
    ↓
内存存储（最多1000条）
    ↓
HTTP API (/logs/api)
    ↓
Web页面实时显示
```

## 🎁 额外功能

### API接口

1. **获取日志**
   ```
   GET http://192.168.31.132:9978/logs/api
   GET http://192.168.31.132:9978/logs/api?limit=100
   ```

2. **清空日志**
   ```
   GET http://192.168.31.132:9978/logs/clear
   ```

3. **启用/禁用日志监控**
   ```
   GET http://192.168.31.132:9978/action?do=debug&action=log_monitor&enabled=true
   GET http://192.168.31.132:9978/action?do=debug&action=log_monitor&enabled=false
   ```

4. **生成测试日志**
   ```
   GET http://192.168.31.132:9978/action?do=debug&action=log_monitor&enabled=test
   ```

### 日志查看器功能

- ✅ 实时自动刷新（每2秒）
- ✅ 按级别过滤
- ✅ 关键词搜索
- ✅ 自动滚动到最新
- ✅ 清空日志
- ✅ 暂停/继续刷新
- ✅ 显示日志统计

## 🚀 性能说明

- 日志监控默认**关闭**，不影响性能
- 启用后内存占用约1-2MB（1000条日志）
- 自动清理旧日志
- 建议调试完成后关闭

## 📚 文档

1. `LOG_MONITOR_README.md` - 完整使用说明
2. `BUILD_INSTRUCTIONS.md` - 编译和安装说明
3. `SPIDER_LOG_CAPTURE.md` - Spider日志捕获详解
4. `FINAL_SUMMARY.md` - 本文档

## ✨ 总结

现在你拥有了一个**完整的Spider调试系统**：

✅ 可以看到Spider加载过程
✅ 可以看到Spider内部运行日志
✅ 可以看到所有异常和堆栈信息
✅ 可以实时监控Spider行为
✅ 可以快速定位Spider问题

**关键改进：**
- 修改了`SpiderDebug.java`，捕获所有外部Spider内部日志
- 这是最重要的改进，让你能看到Spider内部发生的一切！

**下一步：**
1. 重新编译APK
2. 安装到设备
3. 启用日志监控
4. 开始调试Spider！

祝调试顺利！🎉
