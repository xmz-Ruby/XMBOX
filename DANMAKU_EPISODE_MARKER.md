# 弹幕检索自动滚动选中机制 - 集数标记方案

## 实现方案

在 `Players.setMetadata()` 中追加集数标记到 artist 字段，这样不会影响播放，只会影响弹幕匹配逻辑。

## 修改内容

### 1. VideoActivity (Leanback版本)
**文件**: `app/src/leanback/java/com/fongmi/android/tv/ui/activity/VideoActivity.java:1102`

```java
private void setMetadata() {
    String title = mHistory.getVodName();
    String episode = getEpisode().getName();
    String artist = title.equals(episode) ? "" : getString(R.string.play_now, episode);

    // 追加集数标记到 artist 字段，用于弹幕检索自动匹配
    // 格式：正在播放：第 1 季|涌江龙宫 -> 正在播放：第 1 季|涌江龙宫|14集
    if (!artist.isEmpty() && getEpisode().getIndex() > 0) {
        artist = artist + "|" + getEpisode().getIndex() + "集";
    }

    mPlayers.setMetadata(title, artist, mHistory.getVodPic(), mBinding.exo.getDefaultArtwork());
}
```

### 2. VideoActivity (Mobile版本)
**文件**: `app/src/mobile/java/com/fongmi/android/tv/ui/activity/VideoActivity.java:1298`

```java
private void setMetadata() {
    String title = mHistory.getVodName();
    String episode = getEpisode().getName();
    String artist = title.equals(episode) ? "" : getString(R.string.play_now, episode);

    // 追加集数标记到 artist 字段，用于弹幕检索自动匹配
    // 格式：正在播放：第 1 季|涌江龙宫 -> 正在播放：第 1 季|涌江龙宫|14集
    if (!artist.isEmpty() && getEpisode().getIndex() > 0) {
        artist = artist + "|" + getEpisode().getIndex() + "集";
    }

    mPlayers.setMetadata(title, artist, mHistory.getVodPic(), mBinding.exo.getDefaultArtwork());
}
```

## 工作原理

### 数据流程

1. **Episode.index 字段**
   - 在 `Flag.createEpisode()` 中设置（Flag.java:98）
   - 值为剧集序号，从1开始（第1集 = 1，第14集 = 14）

2. **Artist 字段格式**
   - 原格式：`正在播放：第 1 季|涌江龙宫`
   - 新格式：`正在播放：第 1 季|涌江龙宫|14集`
   - 追加的 `|14集` 用于弹幕匹配

3. **弹幕匹配逻辑**
   - `DanmakuSearchDialog.findCurrentEpisodePosition()` 从 `player.getArtist()` 提取集数
   - `extractEpisodeNumber()` 使用正则 `(\\d+)集` 匹配 `14集`
   - 成功提取集数后，与弹幕剧集列表进行匹配

### 匹配优先级

DanmakuSearchDialog 的匹配策略（按优先级）：

0. **剧集名称匹配**（最准确）
   - 从 artist 提取剧集名称（`|` 后的部分）
   - 与 DanmakuEpisode.displayTitle 进行匹配

1. **episodeNumber 字段匹配**
   - 使用 DanmakuEpisode.episodeNumber 直接匹配

2. **完全匹配剧集名称**
   - title 与 displayTitle 完全相同

3. **模糊匹配剧集名称**
   - 清理特殊字符后进行包含匹配

4. **集数数字匹配**
   - 从 displayTitle 提取集数进行匹配
   - 如果没有完全匹配，找最接近的集数

## 优势

1. **不影响播放**
   - artist 字段仅用于显示和元数据，不影响播放逻辑
   - MediaSession 元数据中的 METADATA_KEY_ARTIST 字段

2. **精确匹配**
   - 使用 Episode.index 确保集数准确
   - 避免从剧集名称中提取集数可能出现的误差

3. **兼容性好**
   - 只在 artist 不为空且 index > 0 时追加
   - 不会破坏现有的显示逻辑

4. **易于调试**
   - 格式清晰：`|14集`
   - 日志中可以直接看到完整的 artist 字段

## 测试场景

### 场景1：正常剧集
- 剧名：涌江龙宫
- 剧集：第 1 季|涌江龙宫
- 当前播放：第14集
- Artist: `正在播放：第 1 季|涌江龙宫|14集`
- 匹配结果：✓ 成功定位到第14集

### 场景2：单集视频
- 剧名：电影名称
- 剧集：电影名称（title == episode）
- Artist: `""`（空字符串）
- 匹配结果：不追加集数标记

### 场景3：特殊命名
- 剧名：某动漫
- 剧集：EP01
- 当前播放：第1集（index=1）
- Artist: `正在播放：EP01|1集`
- 匹配结果：✓ 通过 `(\\d+)集` 正则提取到 1

## 相关文件

- `Episode.java` - 剧集数据模型，包含 index 字段
- `Flag.java` - 剧集列表管理，设置 Episode.index
- `Players.java` - 播放器，存储 title 和 artist
- `VideoActivity.java` - 调用 setMetadata 设置元数据
- `DanmakuSearchDialog.java` - 弹幕搜索对话框，实现智能匹配

## 注意事项

1. **index 字段必须正确设置**
   - Flag.createEpisode() 中已正确设置
   - 从1开始计数

2. **格式一致性**
   - 使用 `|` 作为分隔符
   - 使用 `集` 作为单位标识

3. **日志调试**
   - DanmakuSearchDialog 中有详细的日志输出
   - 可以通过 `adb logcat | grep DanmakuSearch` 查看匹配过程
