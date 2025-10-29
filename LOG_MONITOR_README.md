# 日志监控功能使用说明

## 功能概述

在9978端口服务上新增了实时日志监控功能，可以通过Web页面实时查看App的所有日志输出，包括：
- App主程序日志
- Spider爬虫日志
- Chaquo加载的Python Spider日志
- 所有使用Logger工具类的日志

## 功能特性

- ✅ 实时日志展示，每2秒自动刷新
- ✅ 支持按日志级别过滤（VERBOSE/DEBUG/INFO/WARN/ERROR）
- ✅ 支持关键词搜索
- ✅ 自动滚动到最新日志
- ✅ 日志清空功能
- ✅ 最多保存1000条日志记录
- ✅ 调试开关控制，默认关闭以节省性能

## 访问方式

### 1. 访问日志监控页面

在浏览器中访问：
```
http://<设备IP>:9978/logs.html
```

例如：
```
http://192.168.1.100:9978/logs.html
```

### 2. API接口

#### 获取日志数据
```
GET http://<设备IP>:9978/logs/api
GET http://<设备IP>:9978/logs/api?limit=100
```

返回JSON格式的日志数据。

#### 清空日志
```
GET http://<设备IP>:9978/logs/clear
```

#### 启用日志监控
```
GET http://<设备IP>:9978/action?do=debug&action=log_monitor&enabled=true
```

#### 禁用日志监控
```
GET http://<设备IP>:9978/action?do=debug&action=log_monitor&enabled=false
```

#### 测试日志输出
```
GET http://<设备IP>:9978/action?do=debug&action=log_monitor&enabled=test
```

## 使用步骤

### 第一步：启用日志监控

默认情况下，日志监控功能是关闭的（为了节省性能）。需要先启用：

**方法1：通过浏览器访问**
```
http://192.168.1.100:9978/action?do=debug&action=log_monitor&enabled=true
```

**方法2：使用curl命令**
```bash
curl "http://192.168.1.100:9978/action?do=debug&action=log_monitor&enabled=true"
```

### 第二步：访问日志监控页面

在浏览器中打开：
```
http://192.168.1.100:9978/logs.html
```

### 第三步：查看日志

- 页面会每2秒自动刷新显示最新日志
- 可以使用顶部的过滤按钮按级别筛选
- 可以使用搜索框搜索关键词
- 点击"清空"按钮可以清除所有日志
- 点击"暂停"可以停止自动刷新

## 日志级别说明

| 级别 | 说明 | 颜色 |
|------|------|------|
| VERBOSE | 详细日志 | 紫色 |
| DEBUG | 调试日志 | 蓝色 |
| INFO | 信息日志 | 绿色 |
| WARN | 警告日志 | 橙色 |
| ERROR | 错误日志 | 红色 |

## 性能说明

- 日志监控默认**关闭**，不会影响App性能
- 启用后会在内存中保存最多1000条日志
- 超过1000条会自动删除最旧的日志
- 建议调试完成后关闭日志监控功能

## 关闭日志监控

调试完成后，建议关闭日志监控以节省性能：

```
http://192.168.1.100:9978/action?do=debug&action=log_monitor&enabled=false
```

## 代码集成

如果需要在代码中输出日志到监控系统，使用以下方式：

```java
import com.fongmi.android.tv.utils.LogMonitor;

// 输出不同级别的日志
LogMonitor.get().d("TAG", "Debug message");
LogMonitor.get().i("TAG", "Info message");
LogMonitor.get().w("TAG", "Warning message");
LogMonitor.get().e("TAG", "Error message");
LogMonitor.get().v("TAG", "Verbose message");

// 输出带异常的日志
LogMonitor.get().e("TAG", "Error occurred", exception);
```

或者使用已有的Logger工具类（已自动集成）：

```java
import com.github.catvod.utils.Logger;

Logger.d("Debug message");
Logger.i("Info message");
Logger.w("Warning message");
Logger.e("Error message");
```

## 文件说明

### 新增文件

1. **LogMonitor.java** - 日志收集器
   - 路径：`app/src/main/java/com/fongmi/android/tv/utils/LogMonitor.java`
   - 功能：收集和存储日志数据

2. **LogMonitorProcess.java** - 日志API处理器
   - 路径：`app/src/main/java/com/fongmi/android/tv/server/process/LogMonitorProcess.java`
   - 功能：提供日志查询和清空的HTTP接口

3. **logs.html** - 日志监控页面
   - 路径：`app/src/main/assets/logs.html`
   - 功能：实时展示日志的Web界面

### 修改文件

1. **Nano.java** - 添加LogMonitorProcess注册
2. **Setting.java** - 添加日志监控开关配置
3. **Action.java** - 添加调试控制接口
4. **Logger.java** - 集成LogMonitor自动收集日志

## 故障排查

### 问题1：无法访问日志页面

**解决方案：**
- 确认设备IP地址正确
- 确认9978端口服务已启动
- 确认设备和访问设备在同一局域网

### 问题2：页面显示"暂无日志数据"

**解决方案：**
- 先启用日志监控功能
- 执行测试日志命令生成测试数据
- 确认App有日志输出

### 问题3：日志不更新

**解决方案：**
- 检查是否点击了"暂停"按钮
- 刷新页面重新加载
- 确认日志监控功能已启用

## 安全说明

- 日志监控服务只允许局域网访问（10.x.x.x, 172.16-31.x.x, 192.168.x.x）
- 不允许外网IP访问，确保安全性
- 日志数据仅保存在内存中，重启App后清空

## 更新日志

### v1.1 (2025-10-29)
- ✅ 添加外部Spider日志捕获
- ✅ PyLoader日志集成（Python Spider）
- ✅ JarLoader日志集成（JAR Spider）
- ✅ Spider加载过程日志记录
- ✅ Spider错误详细信息捕获

### v1.0 (2025-10-29)
- ✅ 实现基础日志收集功能
- ✅ 实现Web日志监控页面
- ✅ 添加日志级别过滤
- ✅ 添加关键词搜索
- ✅ 添加调试开关控制
- ✅ 集成到现有Logger系统
- ✅ 添加CORS跨域支持
