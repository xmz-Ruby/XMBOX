# Chaquo 到 Pyramid 模块迁移说明

## 迁移概述

已成功将 Python 爬虫支持从 `chaquo` 模块迁移到独立的 `pyramid` 模块。

## 迁移原因

1. **模块化设计**：pyramid 是独立的 library 模块，不在 chaquo 中编写
2. **更好的维护性**：Python 代码和 Java 代码分离清晰
3. **参考成熟项目**：基于 TV 项目的成熟实现
4. **功能完整性**：支持所有 Spider 接口方法和丰富的工具库

## 已完成的更改

### 1. 移除 chaquo 模块依赖

**app/build.gradle:**
```gradle
// 旧代码（已注释）
// implementation project(':chaquo')

// 新代码
implementation project(':pyramid') // Python爬虫支持
```

**settings.gradle:**
```gradle
// 移除
// include ':chaquo'

// 保留
include ':pyramid'
```

### 2. 简化 PyLoader.java

**旧代码（有回退逻辑）：**
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

**新代码（仅使用 pyramid）：**
```java
private void init() {
    try {
        loader = Class.forName("com.github.xmbox.pyramid.Loader").newInstance();
        Logger.i("PyLoader: Pyramid loader initialized successfully");
    } catch (Throwable e) {
        Logger.e("PyLoader: Failed to initialize pyramid loader", e);
    }
}
```

### 3. 创建 pyramid 模块

完整的 pyramid 模块结构：
```
pyramid/
├── build.gradle
├── proguard-rules.pro
├── src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/github/xmbox/pyramid/
│   │   ├── Loader.java
│   │   └── Spider.java
│   └── python/
│       ├── app.py
│       ├── base/
│       │   ├── __init__.py
│       │   └── spider.py
│       └── test_spider.py
```

## 功能对比

| 功能 | chaquo | pyramid |
|------|--------|---------|
| Python 版本 | 3.8 | 3.8 |
| 独立模块 | ❌ | ✅ |
| 完整 Spider 接口 | ⚠️ | ✅ |
| 工具方法 | 基础 | 丰富 |
| 缓存支持 | ❌ | ✅ |
| 日志支持 | 基础 | 完善 |
| Python 库 | 基础 | 完整（requests, lxml, pyquery, beautifulsoup4, pycryptodome, ujson, jsonpath） |
| 代码维护性 | 一般 | 优秀 |

## pyramid 模块优势

1. **独立模块**：作为独立的 library 模块，不与其他代码耦合
2. **完整功能**：实现所有 Spider 接口方法
3. **丰富工具**：
   - HTTP 请求（fetch, post）
   - HTML 解析（html, regStr, removeHtmlTags）
   - 缓存操作（getCache, setCache, delCache）
   - 日志输出（log）
   - 代理支持（getProxyUrl）
4. **Python 库支持**：
   - requests - HTTP 请求
   - lxml - XML/HTML 解析
   - pyquery - jQuery 风格解析
   - beautifulsoup4 - HTML 解析
   - pycryptodome - 加密解密
   - ujson - 快速 JSON
   - jsonpath - JSON 路径查询
5. **易于扩展**：可以轻松添加新的 Python 库

## 兼容性

### Python 爬虫代码
现有的 Python 爬虫代码**无需修改**，因为：
- Spider 基类接口保持一致
- 所有方法签名相同
- 工具方法更加丰富

### 配置文件
配置文件**无需修改**，使用方式完全相同：
```json
{
  "key": "py_spider",
  "name": "Python爬虫",
  "type": 3,
  "api": "py_爬虫名称",
  "searchable": 1
}
```

## 注意事项

1. **minSdk 要求**：pyramid 模块要求 minSdk 24（与 app 模块一致）
2. **架构支持**：仅支持 armeabi-v7a 和 arm64-v8a
3. **APK 体积**：Python 环境会增加约 20-30MB
4. **首次加载**：首次加载 Python 环境需要一定时间

## chaquo 模块处理建议

### 选项 1：保留但不使用（推荐）
- 保留 chaquo 目录和代码
- 不在 settings.gradle 和 app/build.gradle 中引用
- 优点：如果需要可以快速恢复
- 缺点：占用磁盘空间

### 选项 2：完全删除
- 删除整个 chaquo 目录
- 优点：项目更简洁
- 缺点：无法快速恢复

**当前状态**：已采用选项 1，chaquo 模块已从构建配置中移除但保留在磁盘上。

## 测试建议

1. ✅ 编译项目验证 pyramid 模块
2. ⏳ 测试加载本地 Python 爬虫
3. ⏳ 测试加载远程 Python 爬虫
4. ⏳ 测试所有 Spider 接口方法
5. ⏳ 测试缓存功能
6. ⏳ 测试日志输出

## 回滚方案

如果需要回滚到 chaquo：

1. 恢复 app/build.gradle：
```gradle
implementation project(':chaquo')
// implementation project(':pyramid')
```

2. 恢复 settings.gradle：
```gradle
include ':chaquo'
// include ':pyramid'
```

3. 恢复 PyLoader.java 的 init() 方法使用 chaquo

## 总结

✅ **迁移已完成**
- chaquo 模块已从构建配置中移除
- pyramid 模块已成功集成
- PyLoader 已简化为仅使用 pyramid
- 所有代码已更新并修复编译错误

✅ **优势明显**
- 更好的模块化设计
- 更完整的功能支持
- 更丰富的工具库
- 更易于维护和扩展

✅ **兼容性好**
- Python 爬虫代码无需修改
- 配置文件无需修改
- 使用方式完全相同

## 相关文档

- [Pyramid 模块详细说明](PYRAMID_MODULE_README.md)
- [Chaquopy 官方文档](https://chaquo.com/chaquopy/doc/current/)
