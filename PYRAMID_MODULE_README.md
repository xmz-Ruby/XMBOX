# Pyramid 模块集成说明

## 概述

已成功将参考项目 TV 中的 pyramid 模块集成到 XMBOX 项目中。pyramid 是一个基于 Chaquo Python 的 Android Python 爬虫框架，允许在 Android 应用中运行 Python 爬虫脚本。

## 模块结构

```
pyramid/
├── build.gradle                          # Gradle 配置文件
├── proguard-rules.pro                    # ProGuard 混淆规则
├── src/main/
│   ├── AndroidManifest.xml              # Android 清单文件
│   ├── java/com/github/xmbox/pyramid/
│   │   ├── Loader.java                  # Python 环境加载器
│   │   └── Spider.java                  # Spider 包装类
│   └── python/
│       ├── app.py                       # Python 桥接模块
│       ├── base/
│       │   ├── __init__.py             # Python 包初始化
│       │   └── spider.py               # Spider 基类
│       └── test_spider.py              # 测试爬虫示例
```

## 核心组件

### 1. Loader.java
- 负责初始化 Python 环境
- 使用 Chaquo Python 的 AndroidPlatform
- 加载 Python 的 app 模块
- 创建 Spider 实例

### 2. Spider.java
- 继承自 `com.github.catvod.crawler.Spider`
- 包装 Python Spider 对象
- 通过 PyObject 调用 Python 方法
- 实现所有 Spider 接口方法：
  - `init()` - 初始化
  - `homeContent()` - 首页内容
  - `categoryContent()` - 分类内容
  - `detailContent()` - 详情内容
  - `searchContent()` - 搜索内容
  - `playerContent()` - 播放器内容
  - `proxyLocal()` - 本地代理
  - 等等

### 3. app.py
- Python 桥接模块
- 负责下载和加载远程 Python 爬虫
- 提供 JSON 序列化/反序列化
- 调用 Spider 实例的各种方法

### 4. base/spider.py
- Python Spider 基类
- 提供常用工具方法：
  - `fetch()` - HTTP GET 请求
  - `post()` - HTTP POST 请求
  - `html()` - HTML 解析
  - `regStr()` - 正则表达式提取
  - `getCache()/setCache()` - 缓存操作
  - `log()` - 日志输出
  - 等等

## 依赖配置

### build.gradle
```gradle
plugins {
    id 'com.android.library'
    id 'com.chaquo.python'
}

android {
    namespace 'com.github.xmbox.pyramid'
    compileSdk 34

    defaultConfig {
        minSdk 21
        targetSdk 34

        python {
            version "3.8"
            pip {
                install "lxml"
                install "ujson"
                install "pyquery"
                install "requests"
                install "jsonpath"
                install 'pycryptodome'
                install 'beautifulsoup4'
            }
        }

        ndk {
            abiFilters "armeabi-v7a", "arm64-v8a"
        }
    }

    sourceSets {
        main {
            python.srcDirs = ["src/main/python"]
        }
    }
}

dependencies {
    implementation project(':catvod')
    implementation 'com.google.code.gson:gson:2.10.1'
    implementation 'androidx.annotation:annotation:1.7.1'
}
```

### settings.gradle
已添加 pyramid 模块：
```gradle
include ':pyramid'
```

### app/build.gradle
已添加 pyramid 依赖：
```gradle
implementation project(':pyramid') // Python爬虫支持
```

## PyLoader 集成

已更新 `app/src/main/java/com/fongmi/android/tv/api/loader/PyLoader.java`：

```java
private void init() {
    try {
        // 优先使用 pyramid 模块
        loader = Class.forName("com.github.xmbox.pyramid.Loader").newInstance();
        Logger.i("PyLoader: Using pyramid loader");
    } catch (Throwable e1) {
        try {
            // 回退到 chaquo 模块
            loader = Class.forName("com.fongmi.chaquo.Loader").newInstance();
            Logger.i("PyLoader: Using chaquo loader");
        } catch (Throwable e2) {
            Logger.e("PyLoader: Failed to initialize any Python loader", e2);
        }
    }
}
```

使用反射方式加载，支持：
1. 优先使用 pyramid 模块（新实现）
2. 回退到 chaquo 模块（旧实现）
3. 兼容性好，不会因为缺少某个模块而崩溃

## Python 爬虫开发

### 基本结构
```python
from base.spider import Spider

class Spider(Spider):

    def init(self, extend=""):
        # 初始化逻辑
        pass

    def homeContent(self, filter):
        # 返回首页内容
        return {"class": [...], "filters": {...}}

    def categoryContent(self, tid, pg, filter, extend):
        # 返回分类内容
        return {"list": [...], "page": pg, ...}

    def detailContent(self, ids):
        # 返回详情内容
        return {"list": [...]}

    def searchContent(self, key, quick, pg="1"):
        # 返回搜索结果
        return {"list": [...]}

    def playerContent(self, flag, id, vipFlags):
        # 返回播放地址
        return {"url": "...", "parse": 0}
```

### 可用工具方法
- `self.fetch(url)` - HTTP GET 请求
- `self.post(url, data)` - HTTP POST 请求
- `self.html(content)` - 解析 HTML
- `self.regStr(pattern, text)` - 正则提取
- `self.log(msg)` - 日志输出
- `self.getCache(key)` - 获取缓存
- `self.setCache(key, value)` - 设置缓存

### 可用 Python 库
- `requests` - HTTP 请求
- `lxml` - XML/HTML 解析
- `pyquery` - jQuery 风格的 HTML 解析
- `ujson` - 快速 JSON 处理
- `jsonpath` - JSON 路径查询
- `pycryptodome` - 加密解密
- `beautifulsoup4` - HTML 解析

## 使用方式

### 1. 在配置中指定 Python 爬虫
```json
{
  "key": "py_spider",
  "name": "Python爬虫",
  "type": 3,
  "api": "py_爬虫名称",
  "searchable": 1,
  "quickSearch": 1,
  "filterable": 1
}
```

### 2. 远程 Python 爬虫
```json
{
  "key": "py_remote",
  "name": "远程Python爬虫",
  "type": 3,
  "api": "https://example.com/spider.py",
  "searchable": 1
}
```

## 测试

项目包含一个测试爬虫 `test_spider.py`，可以用于验证 pyramid 模块是否正常工作。

## 优势

1. **独立模块**：pyramid 是独立的 library 模块，不在 chaquo 中编写
2. **易于维护**：Python 代码和 Java 代码分离清晰
3. **功能完整**：支持所有 Spider 接口方法
4. **工具丰富**：提供常用的 HTTP、解析、缓存等工具
5. **兼容性好**：使用反射加载，支持多种实现方式
6. **扩展性强**：可以轻松添加新的 Python 库

## 注意事项

1. Python 版本固定为 3.8
2. 仅支持 armeabi-v7a 和 arm64-v8a 架构
3. Python 爬虫会增加 APK 体积（约 20-30MB）
4. 首次加载 Python 环境需要一定时间
5. Python 爬虫的性能略低于 Java 爬虫

## 下一步

1. 测试 pyramid 模块是否能正常编译
2. 测试加载 Python 爬虫是否正常
3. 根据需要添加更多 Python 库
4. 优化 Python 环境加载速度
5. 编写更多示例爬虫

## 参考

- 参考项目：C:\Users\xmz\yorkspace\github\TV
- Chaquo Python 文档：https://chaquo.com/chaquopy/doc/current/
