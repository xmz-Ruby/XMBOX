# Spider日志捕获说明

## 概述

已成功集成外部Spider（JAR和Python）的日志捕获功能，所有Spider加载、初始化、执行过程中的日志都会被自动收集到日志监控系统中。

## 捕获的日志类型

### 1. JAR Spider日志（JarLoader）

**捕获位置：** `app/src/main/java/com/fongmi/android/tv/api/loader/JarLoader.java`

**捕获的日志：**
- ✅ JAR文件加载过程
- ✅ Spider类初始化
- ✅ Init类调用
- ✅ Proxy方法注册
- ✅ Spider实例创建
- ✅ 所有异常和错误信息

**日志示例：**
```
[INFO] JarLoader: Loading JAR - key=xxx, file=spider.jar
[INFO] JarLoader: Init invoked successfully - xxx
[INFO] JarLoader: Proxy method registered - xxx
[INFO] JarLoader: JAR loaded successfully - xxx
[INFO] JarLoader: Loading spider class - key=xxx, api=csp_XXX
[INFO] JarLoader: Spider loaded successfully - xxx
[ERROR] JarLoader: Failed to load spider - key=xxx, api=csp_XXX
```

### 2. Python Spider日志（PyLoader）

**捕获位置：** `app/src/main/java/com/fongmi/android/tv/api/loader/PyLoader.java`

**捕获的日志：**
- ✅ Python Spider加载过程
- ✅ Chaquo Python环境初始化
- ✅ Spider实例创建
- ✅ 所有异常和错误信息

**日志示例：**
```
[INFO] PyLoader: Loading Python spider - key=xxx, api=xxx.py
[INFO] PyLoader: Python spider loaded successfully - xxx
[ERROR] PyLoader: Failed to load Python spider - xxx
[ERROR] PyLoader: proxyInvoke failed
```

### 3. Spider内部日志（SpiderDebug）

**捕获位置：** `catvod/src/main/java/com/github/catvod/crawler/SpiderDebug.java`

**捕获的日志：**
- ✅ Spider内部调试日志
- ✅ Spider运行时异常
- ✅ 完整的异常堆栈信息
- ✅ 所有使用SpiderDebug.log()的日志

**日志示例：**
```
[DEBUG] SpiderDebug: 开始搜索: 关键词
[DEBUG] SpiderDebug: 请求URL: http://...
[DEBUG] SpiderDebug: 解析结果: 找到10条数据
[ERROR] SpiderDebug: 网络请求失败
java.net.SocketTimeoutException: timeout
    at okhttp3.internal.http.RetryAndFollowUpInterceptor.intercept(...)
    ...
```

### 4. 通用日志（Logger）

**捕获位置：** `catvod/src/main/java/com/github/catvod/utils/Logger.java`

**捕获的日志：**
- ✅ 所有使用Logger工具类的日志
- ✅ 自动集成到LogMonitor
- ✅ 支持所有日志级别（VERBOSE/DEBUG/INFO/WARN/ERROR）

## 修改的文件

### 1. SpiderDebug.java ⭐ 重要
```java
// 添加LogMonitor集成
private static Object logMonitor;

static {
    try {
        Class<?> clazz = Class.forName("com.fongmi.android.tv.utils.LogMonitor");
        logMonitor = clazz.getMethod("get").invoke(null);
    } catch (Exception e) {
        logMonitor = null;
    }
}

// 捕获Spider内部所有日志
public static void log(Throwable th) {
    if (th != null) {
        String stackTrace = Log.getStackTraceString(th);
        Logger.t(TAG).e(th, th.getMessage());
        addToMonitor("ERROR", th.getMessage() + "\n" + stackTrace);
    }
}

public static void log(String msg) {
    if (!TextUtils.isEmpty(msg)) {
        Logger.t(TAG).d(msg);
        addToMonitor("DEBUG", msg);
    }
}
```

### 2. PyLoader.java
```java
// 添加Logger导入
import com.github.catvod.utils.Logger;

// 替换所有 e.printStackTrace() 为 Logger.e()
Logger.i("PyLoader: Loading Python spider - key=" + key + ", api=" + api);
Logger.e("PyLoader: Failed to load Python spider - " + key, e);
```

### 3. JarLoader.java
```java
// 添加Logger导入
import com.github.catvod.utils.Logger;

// 在关键位置添加日志
Logger.i("JarLoader: Loading JAR - key=" + key + ", file=" + file.getName());
Logger.i("JarLoader: JAR loaded successfully - " + key);
Logger.e("JarLoader: Failed to load spider - key=" + key + ", api=" + api, e);
```

## 使用方法

### 1. 启用日志监控

```bash
curl "http://192.168.31.132:9978/action?do=debug&action=log_monitor&enabled=true"
```

### 2. 打开日志查看器

**方法A：使用独立查看器**
- 浏览器打开：`C:\Users\xmz\yorkspace\github\XMBOX\logs_viewer.html`

**方法B：使用内置页面（需重新编译）**
- 浏览器访问：`http://192.168.31.132:9978/logs.html`

### 3. 触发Spider加载

在App中执行以下操作会触发Spider日志：
- 切换视频源
- 搜索视频
- 播放视频
- 加载配置文件

### 4. 查看Spider日志

在日志监控页面中：
- 搜索 "JarLoader" 查看JAR Spider日志
- 搜索 "PyLoader" 查看Python Spider日志
- 按ERROR级别过滤查看所有错误
- 按时间顺序查看Spider加载流程

## 调试Spider问题

### 场景1：Spider加载失败

**查看日志：**
```
[ERROR] JarLoader: Failed to load spider - key=xxx, api=csp_XXX
java.lang.ClassNotFoundException: com.github.catvod.spider.XXX
```

**解决方案：**
- 检查JAR文件是否包含对应的Spider类
- 检查类名是否正确
- 检查JAR文件是否损坏

### 场景2：Python Spider初始化失败

**查看日志：**
```
[ERROR] PyLoader: Failed to load Python spider - xxx
com.chaquo.python.PyException: ModuleNotFoundError: No module named 'xxx'
```

**解决方案：**
- 检查Python文件是否存在
- 检查Python依赖是否完整
- 检查Chaquo配置是否正确

### 场景3：Spider执行错误

**查看日志：**
```
[ERROR] JarLoader: proxyInvoke failed
java.lang.NullPointerException: ...
```

**解决方案：**
- 查看完整堆栈信息
- 检查Spider代码逻辑
- 检查传入参数是否正确

## 日志级别说明

| 级别 | 用途 | 示例 |
|------|------|------|
| INFO | Spider加载成功 | `JarLoader: Spider loaded successfully` |
| WARN | 可选功能缺失 | `JarLoader: No Proxy class found` |
| ERROR | 加载或执行失败 | `JarLoader: Failed to load spider` |

## 性能影响

- ✅ 日志收集对性能影响极小
- ✅ 仅在启用日志监控时收集
- ✅ 最多保存1000条日志
- ✅ 自动清理旧日志
- ✅ 建议调试完成后关闭

## 最佳实践

### 1. 开发调试时

```bash
# 启用日志监控
curl "http://192.168.31.132:9978/action?do=debug&action=log_monitor&enabled=true"

# 打开日志查看器
# 浏览器打开 logs_viewer.html

# 在App中操作触发Spider加载

# 在日志查看器中搜索关键词
```

### 2. 生产环境

```bash
# 关闭日志监控以节省性能
curl "http://192.168.31.132:9978/action?do=debug&action=log_monitor&enabled=false"
```

### 3. 问题排查

1. 启用日志监控
2. 清空现有日志
3. 重现问题
4. 按时间顺序查看日志
5. 搜索ERROR级别日志
6. 分析堆栈信息

## 常见问题

### Q: 为什么看不到Spider日志？

**A:** 确保：
1. 已启用日志监控
2. 已触发Spider加载（切换源、搜索等）
3. 刷新日志查看器

### Q: 日志太多怎么办？

**A:** 使用过滤功能：
1. 按级别过滤（只看ERROR）
2. 搜索关键词（JarLoader、PyLoader）
3. 清空旧日志重新开始

### Q: 如何查看Python Spider的print输出？

**A:** Python的print输出会通过Chaquo重定向到Android日志系统，会被Logger捕获并显示在日志监控中。

## 技术细节

### 日志流程

```
Spider加载
    ↓
JarLoader/PyLoader
    ↓
Logger.i/e/w()
    ↓
LogMonitor.addLog()
    ↓
内存存储（最多1000条）
    ↓
HTTP API (/logs/api)
    ↓
Web页面显示
```

### 异常捕获

所有Spider加载和执行过程中的异常都会被捕获并记录：
- ClassNotFoundException
- NoSuchMethodException
- InvocationTargetException
- PyException（Python异常）
- 其他运行时异常

### 堆栈信息

错误日志会包含完整的堆栈信息，便于定位问题：
```
[ERROR] JarLoader: Failed to load spider - key=xxx, api=csp_XXX
java.lang.ClassNotFoundException: com.github.catvod.spider.XXX
    at dalvik.system.BaseDexClassLoader.findClass(...)
    at java.lang.ClassLoader.loadClass(...)
    ...
```

## 总结

通过集成Spider日志捕获功能，现在可以：
- ✅ 实时监控所有Spider加载过程
- ✅ 快速定位Spider加载失败原因
- ✅ 查看详细的错误堆栈信息
- ✅ 追踪Spider执行流程
- ✅ 调试Python和JAR Spider问题

这大大提升了Spider开发和调试的效率！
