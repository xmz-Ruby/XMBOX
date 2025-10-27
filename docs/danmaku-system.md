# 弹幕系统开发文档

## 目录
1. [系统架构](#系统架构)
2. [核心组件](#核心组件)
3. [数据模型](#数据模型)
4. [API接口](#api接口)
5. [配置管理](#配置管理)
6. [UI组件](#ui组件)
7. [扩展指南](#扩展指南)

---

## 系统架构

### 整体架构图
```
┌─────────────────────────────────────────────────────────┐
│                      UI Layer                            │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ DanmakuDialog│  │ VideoActivity│  │ LiveActivity │  │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  │
└─────────┼──────────────────┼──────────────────┼─────────┘
          │                  │                  │
┌─────────┼──────────────────┼──────────────────┼─────────┐
│         │         Player Layer                │          │
│  ┌──────▼───────┐  ┌──────▼───────┐  ┌───────▼──────┐  │
│  │   Players    │──│   DanPlayer  │──│ DanmakuView  │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
└─────────┼──────────────────┼──────────────────┼─────────┘
          │                  │                  │
┌─────────┼──────────────────┼──────────────────┼─────────┐
│         │         Data Layer               │             │
│  ┌──────▼───────┐  ┌──────▼───────┐  ┌───▼──────────┐  │
│  │  DanmakuApi  │  │    Parser    │  │    Loader    │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
└──────────────────────────────────────────────────────────┘
```

### 技术栈
- **播放器**: ExoPlayer + master.flame.danmaku
- **网络**: OkHttp
- **数据解析**: Gson
- **UI**: Material Design + ViewBinding
- **存储**: SharedPreferences

---

## 核心组件

### 1. DanPlayer (弹幕播放器)
**路径**: `app/src/main/java/com/fongmi/android/tv/player/danmaku/DanPlayer.java`

**职责**:
- 弹幕渲染配置
- 生命周期管理
- 与视频播放器同步

**关键方法**:
```java
// 初始化配置（从Setting读取用户设置）
private void initConfig()

// 更新配置并重启弹幕
public void updateConfig()

// 设置弹幕数据
public void setDanmaku(Danmaku item)

// 播放控制
public void play()
public void pause()
public void seekTo(long time)
```

**配置参数**:
```java
// 最大行数
maxLines.put(BaseDanmaku.TYPE_FIX_TOP, 2);      // 顶部固定
maxLines.put(BaseDanmaku.TYPE_SCROLL_RL, 3);    // 右到左滚动
maxLines.put(BaseDanmaku.TYPE_SCROLL_LR, 2);    // 左到右滚动
maxLines.put(BaseDanmaku.TYPE_FIX_BOTTOM, 2);   // 底部固定

// 性能优化
setScrollSpeedFactor(speed)              // 滚动速度
setDanmakuTransparency(alpha)            // 透明度
setDuplicateMergingEnabled(true)         // 合并重复弹幕
preventOverlapping(overlapping)          // 防止重叠
setMaximumVisibleSizeInScreen(density)   // 最大显示数量
setCacheStuffer(...)                     // 启用缓存
```

### 2. Parser (弹幕解析器)
**路径**: `app/src/main/java/com/fongmi/android/tv/player/danmaku/Parser.java`

**支持格式**:

**XML格式** (Bilibili风格):
```xml
<i>
  <d p="时间,类型,字号,颜色,时间戳,弹幕池,用户ID,弹幕ID">弹幕文本</d>
</i>
```

**TXT格式**:
```
[时间,类型,字号,颜色]弹幕文本
```

**参数说明**:
- `时间`: 秒数（支持小数）
- `类型`: 1=右到左滚动, 4=底部固定, 5=顶部固定, 6=左到右滚动
- `字号`: 通常25左右
- `颜色`: 十进制RGB值（16777215=白色）

**正则表达式**:
```java
// XML: <d p="参数">文本</d>
Pattern XML = Pattern.compile("<d p=\"([^\"]+)\"[^>]*>([^<]+)</d>");

// TXT: [参数]文本
Pattern TXT = Pattern.compile("\\[(.*?)\\](.*)");
```

### 3. Loader (弹幕加载器)
**路径**: `app/src/main/java/com/fongmi/android/tv/player/danmaku/Loader.java`

**功能**:
- 支持HTTP/HTTPS URL加载
- 支持本地文件路径加载
- 自动处理file://协议转换

**使用示例**:
```java
// URL加载
Danmaku danmaku = Danmaku.from("https://example.com/danmaku.xml");

// 本地文件
Danmaku danmaku = Danmaku.from("/storage/emulated/0/danmaku.xml");
```

---

## 数据模型

### Bean类结构

#### 1. Danmaku (弹幕源)
**路径**: `app/src/main/java/com/fongmi/android/tv/bean/Danmaku.java`
```java
public class Danmaku {
    private String name;      // 显示名称
    private String url;       // 弹幕URL或文件路径
    private boolean selected; // 是否选中
}
```

#### 2. DanmakuData (弹幕条目)
**路径**: `app/src/main/java/com/fongmi/android/tv/bean/DanmakuData.java`
```java
public class DanmakuData {
    private String text;   // 弹幕文本
    private int type;      // 弹幕类型
    private int color;     // 文字颜色
    private int shadow;    // 阴影颜色（自动计算）
    private long time;     // 显示时间（毫秒）
    private float size;    // 字体大小
}
```

#### 3. DanmakuAnime (番剧信息)
**路径**: `app/src/main/java/com/fongmi/android/tv/bean/DanmakuAnime.java`
```java
public class DanmakuAnime {
    private int animeId;          // 番剧ID
    private String animeTitle;    // 番剧标题
    private String type;          // 类型（动画/综艺等）
    private int episodeCount;     // 集数
}
```

#### 4. DanmakuEpisode (剧集信息)
**路径**: `app/src/main/java/com/fongmi/android/tv/bean/DanmakuEpisode.java`
```java
public class DanmakuEpisode {
    private int episodeId;        // 剧集ID
    private String episodeTitle;  // 剧集标题
    private String episodeNumber; // 集数编号
}
```

---

## API接口

### DanmakuApi
**路径**: `app/src/main/java/com/fongmi/android/tv/api/DanmakuApi.java`

**基础URL**: `https://danmu.mangzhexuexi.com/mangzhexuexi/api/v2`

### 接口列表

#### 1. 搜索番剧
```java
DanmakuApi.searchAnime(String keyword, Callback<List<DanmakuAnime>> callback)
```
**请求**: `GET /search/anime?keyword={keyword}`

**响应示例**:
```json
{
  "success": true,
  "animes": [
    {
      "animeId": 293297,
      "animeTitle": "现在就出发 第3季(2025)【综艺】from 360",
      "type": "综艺",
      "episodeCount": 16
    }
  ]
}
```

#### 2. 获取剧集列表
```java
DanmakuApi.getBangumiEpisodes(int animeId, Callback<List<DanmakuEpisode>> callback)
```
**请求**: `GET /bangumi/{animeId}`

**响应示例**:
```json
{
  "success": true,
  "bangumi": {
    "episodes": [
      {
        "episodeId": 10287,
        "episodeTitle": "【qq】 先导片上：显眼包们开启沈腾模仿大赛",
        "episodeNumber": "1"
      }
    ]
  }
}
```

#### 3. 获取弹幕URL
```java
String url = DanmakuApi.getDanmakuUrl(int episodeId)
```
**返回**: `https://danmu.mangzhexuexi.com/mangzhexuexi/api/v2/comment/{episodeId}?format=xml`

---

## 配置管理

### Setting类
**路径**: `app/src/main/java/com/fongmi/android/tv/Setting.java`

### 配置项

| 配置项 | 方法 | 默认值 | 说明 |
|--------|------|--------|------|
| 弹幕密度 | `getDanmakuDensity()` | 30 | 同屏最大弹幕数 |
| 透明度 | `getDanmakuAlpha()` | 0.8f | 0.0-1.0 |
| 字体大小 | `getDanmakuTextSize()` | 0.75f | 相对缩放比例 |
| 滚动速度 | `getDanmakuSpeed()` | 1.2f | 速度倍数 |
| 描边效果 | `getDanmakuStroke()` | false | 是否启用描边 |

### 使用示例
```java
// 读取配置
int density = Setting.getDanmakuDensity();
float alpha = Setting.getDanmakuAlpha();

// 保存配置
Setting.putDanmakuDensity(50);
Setting.putDanmakuAlpha(0.6f);

// 应用配置
player.getDanPlayer().updateConfig();
```

---

## UI组件

### DanmakuDialog
**路径**: `app/src/main/java/com/fongmi/android/tv/ui/dialog/DanmakuDialog.java`

### 布局文件
- Mobile: `app/src/mobile/res/layout/dialog_danmaku.xml`
- Leanback: `app/src/leanback/res/layout/dialog_danmaku.xml`

### UI结构
```
┌─────────────────────────────────────────┐
│ 弹幕选择        🔍  ⚙️  📁              │
├─────────────────────────────────────────┤
│ □ 弹幕源1                               │
│ ☑ 弹幕源2 (当前)                        │
│ □ 弹幕源3                               │
└─────────────────────────────────────────┘
```

### 按钮功能

#### 🔍 搜索按钮 (`binding.search`)
```java
private void showSearchDialog(View view)
```
- 自动填充当前视频标题
- 智能清理集数标识
- 搜索 → 选择番剧 → 选择集数 → 加载弹幕

#### ⚙️ 设置按钮 (`binding.settings`)
```java
private void showSettings(View view)
```
- 弹幕密度（10/20/30/50/100）
- 透明度（20%/40%/60%/80%/100%）
- 字体大小（50%/65%/75%/85%/100%）
- 滚动速度（0.8x/1.0x/1.2x/1.5x/2.0x）
- 描边效果（关闭/开启）

#### 📁 选择按钮 (`binding.choose`)
```java
private void showChooser(View view)
```
- 支持MIME类型：`text/*`, `application/xml`, `application/json`
- 调用系统文件选择器

### 关键方法

#### 获取默认搜索词
```java
private String getDefaultSearchKeyword() {
    String title = player.getTitle();
    // 移除集数标识
    return title.replaceAll("第\\d+集", "")
                .replaceAll("\\d+集", "")
                .replaceAll("EP\\d+", "")
                .replaceAll("\\[.*?\\]", "")
                .replaceAll("\\(.*?\\)", "")
                .trim();
}
```

#### 搜索流程
```java
showSearchDialog()
  ↓
searchAnime(keyword)
  ↓
showAnimeList(animes)
  ↓
loadEpisodes(animeId)
  ↓
showEpisodeList(episodes)
  ↓
loadDanmaku(episode)
```

---

## 扩展指南

### 1. 添加新的弹幕源

#### 步骤1: 创建API接口
```java
// DanmakuApi.java
public static void searchFromNewSource(String keyword, Callback callback) {
    String url = "https://new-source.com/api/search?q=" + keyword;
    // 实现网络请求
}
```

#### 步骤2: 添加数据模型
```java
// NewSourceAnime.java
public class NewSourceAnime {
    private int id;
    private String title;
    // 其他字段
}
```

#### 步骤3: 修改DanmakuDialog
```java
// 在showSettings中添加新选项
.setItems(new String[]{"在线搜索", "新弹幕源", "弹幕密度", ...})
```

### 2. 支持新的弹幕格式

#### 步骤1: 添加正则表达式
```java
// Parser.java
private static final Pattern JSON = Pattern.compile("正则表达式");
```

#### 步骤2: 修改parse()方法
```java
if (line.startsWith("{")) {
    pattern = JSON;
}
```

#### 步骤3: 解析参数
```java
// DanmakuData.java
private void parseJson(String json) {
    // JSON解析逻辑
}
```

### 3. 添加新的配置项

#### 步骤1: 在Setting中添加getter/setter
```java
// Setting.java
public static int getDanmakuNewOption() {
    return Prefers.getInt("danmaku_new_option", 默认值);
}

public static void putDanmakuNewOption(int value) {
    Prefers.put("danmaku_new_option", value);
}
```

#### 步骤2: 在DanPlayer中使用
```java
// DanPlayer.java initConfig()
int newOption = Setting.getDanmakuNewOption();
context.setNewOption(newOption);
```

#### 步骤3: 在DanmakuDialog中添加UI
```java
private void showNewOptionDialog() {
    new AlertDialog.Builder(getContext())
        .setTitle("新选项")
        .setItems(new String[]{"选项1", "选项2"}, ...)
        .show();
}
```

### 4. 优化性能

#### 减少弹幕密度
```java
Setting.putDanmakuDensity(20); // 降低到20条
```

#### 关闭描边
```java
Setting.putDanmakuStroke(false); // 描边消耗性能
```

#### 调整透明度
```java
Setting.putDanmakuAlpha(0.6f); // 降低透明度
```

#### 减少行数
```java
// DanPlayer.java
maxLines.put(BaseDanmaku.TYPE_SCROLL_RL, 2); // 从3降到2
```

### 5. 调试技巧

#### 启用日志
```java
// Parser.java
Logger.d("解析到 " + items.size() + " 条弹幕");

// DanPlayer.java
Logger.t(TAG).d("弹幕准备完成 - 播放状态:" + playing);
```

#### 查看logcat
```bash
adb logcat | grep -i "danmaku\|DanPlayer\|Parser"
```

#### 测试弹幕文件
```xml
<!-- test.xml -->
<i>
<d p="0.1,1,25,16777215">测试弹幕1</d>
<d p="1.0,1,25,16711680">测试弹幕2</d>
<d p="2.0,5,25,65280">顶部弹幕</d>
<d p="3.0,4,25,255">底部弹幕</d>
</i>
```

---

## 常见问题

### Q1: 弹幕不显示？
**排查步骤**:
1. 检查logcat是否有"解析到 X 条弹幕"
2. 检查视频位置是否在弹幕时间范围内
3. 检查DanmakuView的visibility
4. 确认弹幕格式是否正确

### Q2: 弹幕卡顿？
**解决方案**:
1. 降低弹幕密度（30 → 20）
2. 关闭描边效果
3. 减少最大行数
4. 降低透明度

### Q3: 搜索不到弹幕？
**排查步骤**:
1. 检查网络连接
2. 查看API返回的错误信息
3. 尝试修改搜索关键词
4. 检查API地址是否可访问

### Q4: 本地文件无法选择？
**解决方案**:
1. 确认文件MIME类型正确
2. 检查文件权限
3. 尝试将文件移到Download目录
4. 确认文件编码为UTF-8

---

## 版本历史

### v1.0 (当前版本)
- ✅ 支持XML和TXT格式弹幕
- ✅ 在线搜索功能
- ✅ 本地文件加载
- ✅ 5项可调节配置
- ✅ 性能优化（缓存、防重叠、合并）
- ✅ Mobile和Leanback双版本支持

### 未来计划
- [ ] 支持ASS/SSA字幕格式
- [ ] 弹幕发送功能
- [ ] 弹幕屏蔽关键词
- [ ] 弹幕时间轴调整
- [ ] 更多弹幕源接入

---

## 联系方式

如有问题或建议，请提交Issue到项目仓库。
