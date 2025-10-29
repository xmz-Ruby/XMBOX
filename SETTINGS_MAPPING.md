# XMBOX 设置菜单映射文档

本文档详细记录了 XMBOX 应用中所有设置菜单项与 SharedPreferences 或数据库中存储值的对应关系。

## 1. 设置菜单 UI 文件

### Mobile 版本 (手机/平板)
- **主设置界面**: `app/src/mobile/java/com/fongmi/android/tv/ui/fragment/SettingFragment.java`
- **布局文件**: `app/src/mobile/res/layout/fragment_setting.xml`
- **播放器设置界面**: `app/src/mobile/java/com/fongmi/android/tv/ui/activity/SettingPlayerActivity.java`
- **播放器设置布局**: `app/src/mobile/res/layout/activity_setting_player.xml`

### Leanback 版本 (TV)
- **主设置界面**: `app/src/leanback/java/com/fongmi/android/tv/ui/activity/SettingActivity.java`
- **布局文件**: `app/src/leanback/res/layout/activity_setting.xml`
- **播放器设置界面**: `app/src/leanback/java/com/fongmi/android/tv/ui/activity/SettingPlayerActivity.java`
- **播放器设置布局**: `app/src/leanback/res/layout/activity_setting_player.xml`

---

## 2. SharedPreferences 键值映射

**存储管理类**: `app/src/main/java/com/fongmi/android/tv/Setting.java`
**底层存储类**: `catvod/src/main/java/com/github/catvod/utils/Prefers.java`
**存储类型**: Android SharedPreferences (通过 PreferenceManager 获取默认实例)

### 2.1 网络设置

| UI 设置项 | SharedPreferences 键 | 数据类型 | 默认值 | 存储方法 |
|-----------|---------------------|---------|--------|----------|
| DoH (DNS over HTTPS) | `doh` | String | "" | `Setting.putDoh(String)` |
| 代理 | `proxy` | String | "" | `Setting.putProxy(String)` |

### 2.2 播放器设置

| UI 设置项 | SharedPreferences 键 | 数据类型 | 默认值 | 存储方法 |
|-----------|---------------------|---------|--------|----------|
| User Agent | `ua` | String | "" | `Setting.putUa(String)` |
| 渲染模式 | `render` | int | 0 | `Setting.putRender(int)` |
| 视频缩放 | `scale` | int | 0 | `Setting.putScale(int)` |
| 直播视频缩放 | `scale_live` | int | scale 值 | `Setting.putLiveScale(int)` |
| 缓冲倍数 | `buffer` | int | 1 (范围: 1-10) | `Setting.putBuffer(int)` |
| 播放速度 | `speed` | float | 3.0 (范围: 2-5) | `Setting.putSpeed(float)` |
| 后台播放模式 | `background` | int | 0 (0=关闭, 1=开启, 2=画中画) | `Setting.putBackground(int)` |
| 字幕/Caption | `caption` | boolean | false | `Setting.putCaption(boolean)` |
| 隧道模式 | `tunnel` | boolean | false | `Setting.putTunnel(boolean)` |
| 音频解码优先 | `audio_prefer` | boolean | false | `Setting.putAudioPrefer(boolean)` |
| 优先 AAC | `prefer_aac` | boolean | false | `Setting.putPreferAAC(boolean)` |
| 弹幕加载 | `danmaku_load` | boolean | false | `Setting.putDanmakuLoad(boolean)` |
| 弹幕显示 | `danmaku_show` | boolean | false | `Setting.putDanmakuShow(boolean)` |
| 解码模式 | `decode` | int | Players.HARD | `Setting.putDecode(int)` |

### 2.3 弹幕设置

| UI 设置项 | SharedPreferences 键 | 数据类型 | 默认值 | 存储方法 |
|-----------|---------------------|---------|--------|----------|
| 弹幕密度 | `danmaku_density` | int | 30 | `Setting.putDanmakuDensity(int)` |
| 弹幕透明度 | `danmaku_alpha` | float | 0.8f | `Setting.putDanmakuAlpha(float)` |
| 弹幕文字大小 | `danmaku_text_size` | float | 0.75f | `Setting.putDanmakuTextSize(float)` |
| 弹幕速度 | `danmaku_speed` | float | 1.2f | `Setting.putDanmakuSpeed(float)` |
| 弹幕描边 | `danmaku_stroke` | boolean | false | `Setting.putDanmakuStroke(boolean)` |

### 2.4 字幕设置

| UI 设置项 | SharedPreferences 键 | 数据类型 | 默认值 | 存储方法 |
|-----------|---------------------|---------|--------|----------|
| 字幕文字大小 | `subtitle_text_size` | float | 0f | `Setting.putSubtitleTextSize(float)` |
| 字幕位置 | `subtitle_position` | float | 0f | `Setting.putSubtitlePosition(float)` |

### 2.5 应用设置

| UI 设置项 | SharedPreferences 键 | 数据类型 | 默认值 | 存储方法 |
|-----------|---------------------|---------|--------|----------|
| 无痕模式 | `incognito` | boolean | false | `Setting.putIncognito(boolean)` |
| 直播标签可见 | `live_tab_visible` | boolean | true | `Setting.putLiveTabVisible(boolean)` |
| 图片质量 | `quality` | int | 2 | `Setting.putQuality(int)` |
| 图片大小 | `size` | int | 2 | `Setting.putSize(int)` |
| 视图类型 | `viewType` | int | 不定 | `Setting.putViewType(int)` |
| 站点模式 | `site_mode` | int | 0 | `Setting.putSiteMode(int)` |
| 同步模式 | `sync_mode` | int | 0 | `Setting.putSyncMode(int)` |
| 启动到直播 | `boot_live` | boolean | false | `Setting.putBootLive(boolean)` |
| 反转 | `invert` | boolean | false | `Setting.putInvert(boolean)` |
| 横向 | `across` | boolean | true | `Setting.putAcross(boolean)` |
| 切换 | `change` | boolean | true | `Setting.putChange(boolean)` |
| 注音 | `zhuyin` | boolean | false | `Setting.putZhuyin(boolean)` |
| 隐私协议已同意 | `privacy_agreed_v1` | boolean | false | `Setting.setPrivacyAgreed(boolean)` |
| 日志监控启用 | `log_monitor_enabled` | boolean | false | `Setting.putLogMonitorEnabled(boolean)` |

### 2.6 其他设置

| UI 设置项 | SharedPreferences 键 | 数据类型 | 默认值 | 存储方法 |
|-----------|---------------------|---------|--------|----------|
| 壁纸 | `wall` | int | 4 | `Setting.putWall(int)` |
| 重置计数器 | `reset` | int | 0 | `Setting.putReset(int)` |
| 搜索关键词 | `keyword` | String | "" | `Setting.putKeyword(String)` |
| 热搜 | `hot` | String | "" | `Setting.putHot(String)` |

---

## 3. 数据库表/字段

**数据库类**: `app/src/main/java/com/fongmi/android/tv/db/AppDatabase.java`

应用使用 Room Database，包含以下实体（不直接用于设置，但与内容相关）：

### 数据库实体

- **Config** (`app/src/main/java/com/fongmi/android/tv/bean/Config.java`) - 存储 VOD/Live/Wall 配置 URL
- **Site** (`app/src/main/java/com/fongmi/android/tv/bean/Site.java`) - 存储视频站点配置
- **Live** (`app/src/main/java/com/fongmi/android/tv/bean/Live.java`) - 存储直播电视配置
- **History** (`app/src/main/java/com/fongmi/android/tv/bean/History.java`) - 存储观看历史
- **Keep** (`app/src/main/java/com/fongmi/android/tv/bean/Keep.java`) - 存储收藏/书签
- **Track** (`app/src/main/java/com/fongmi/android/tv/bean/Track.java`) - 存储播放轨道
- **Device** (`app/src/main/java/com/fongmi/android/tv/bean/Device.java`) - 存储设备信息
- **Backup** (`app/src/main/java/com/fongmi/android/tv/bean/Backup.java`) - 存储备份元数据

**注意**: 设置项不存储在数据库中，全部使用 SharedPreferences。数据库用于内容配置、历史记录和用户数据。

---

## 4. 完整 UI 到存储的映射

### 4.1 主设置界面 (Mobile)

| UI 元素 | 操作 | 存储键 | 存储类型 |
|---------|------|--------|----------|
| VOD 源输入 | 点击添加/编辑 | 由 VodConfig 管理 | 数据库 (Config 表) |
| VOD 主页按钮 | 设置默认站点 | 由 VodConfig 管理 | 数据库 (Site 表) |
| VOD 历史按钮 | 查看历史 | N/A | 数据库 (History 表) |
| Live 源输入 | 点击添加/编辑 | 由 LiveConfig 管理 | 数据库 (Config 表) |
| Live 主页按钮 | 设置默认频道 | 由 LiveConfig 管理 | 数据库 (Live 表) |
| Live 历史按钮 | 查看历史 | N/A | 数据库 (History 表) |
| 播放器设置按钮 | 导航到播放器设置 | N/A | 导航 |
| 无痕模式开关 | 切换无痕模式 | `incognito` | SharedPreferences (boolean) |
| 直播标签可见开关 | 显示/隐藏直播标签 | `live_tab_visible` | SharedPreferences (boolean) |
| 图片大小选择器 | 选择图片大小 | `size` | SharedPreferences (int, 0-4) |
| DoH 选择器 | 选择 DNS 提供商 | `doh` | SharedPreferences (String) |
| 代理输入 | 设置代理 URL | `proxy` | SharedPreferences (String) |
| 缓存显示/清除 | 清除缓存 | N/A | 文件系统操作 |
| 备份按钮 | 备份数据库 | N/A | 数据库导出 |
| 恢复按钮 | 恢复数据库 | N/A | 数据库导入 |
| 版本显示 | 显示版本 | N/A | 只读 (BuildConfig) |

### 4.2 播放器设置界面 (Mobile & Leanback)

| UI 元素 | 操作 | 存储键 | 存储类型 |
|---------|------|--------|----------|
| 渲染模式选择器 | 循环渲染模式 | `render` | SharedPreferences (int, 0-2) |
| 缩放选择器 | 选择视频缩放 | `scale` | SharedPreferences (int, 0-7) |
| Caption 切换 | 启用/禁用字幕 | `caption` | SharedPreferences (boolean) |
| 缓冲输入 | 设置缓冲倍数 | `buffer` | SharedPreferences (int, 1-10) |
| 速度输入 | 设置播放速度 | `speed` | SharedPreferences (float, 2-5) |
| 隧道开关 | 切换隧道模式 | `tunnel` | SharedPreferences (boolean) |
| 音频解码开关 | 切换音频解码 | `audio_prefer` | SharedPreferences (boolean) |
| AAC 开关 | 优先 AAC 编解码器 | `prefer_aac` | SharedPreferences (boolean) |
| 弹幕加载开关 | 自动加载弹幕 | `danmaku_load` | SharedPreferences (boolean) |
| 后台选择器 | 后台播放模式 | `background` | SharedPreferences (int, 0-2) |
| User Agent 输入 | 设置自定义 UA | `ua` | SharedPreferences (String) |

---

## 5. 数组资源 (选择选项)

设置使用 XML 资源中定义的字符串数组：

- **`R.array.select_scale`** - 视频缩放选项 (默认, 16:9, 4:3, 填充等)
- **`R.array.select_render`** - 渲染模式 (Surface, Texture 等)
- **`R.array.select_caption`** - Caption 选项 (关闭, 开启)
- **`R.array.select_background`** - 后台模式 (关闭, 开启, 画中画)
- **`R.array.select_size`** - 图片大小选项 (小, 中, 大等)
- **`R.array.select_quality`** - 图片质量选项 (低, 中, 高等)

---

## 总结

XMBOX Android TV/移动应用使用**混合存储方式**：

- **SharedPreferences** - 用于所有用户设置（通过 `Setting.java` 封装）
- **Room Database** - 用于内容配置、历史记录和用户数据
- **无 XML preference 文件** - 所有设置都是基于代码的
- **两种 UI 变体**: Mobile（基于 Fragment）和 Leanback（基于 Activity，用于 TV）

所有设置都使用 Android 默认的 SharedPreferences 持久化，通过 `Setting` 类中的类型安全的 getter/setter 方法进行操作，该类将实际存储操作委托给 `Prefers` 工具类。

---

**文档生成日期**: 2025-10-29
**项目**: XMBOX
**维护**: 请在修改设置相关代码时同步更新此文档
