# 弹幕功能维护 - 修复记录

## 修复内容

### 问题1：切换影视剧后弹幕对话框状态未清理

**问题描述**：
- 切换到新的影视剧后，再次打开弹幕搜索对话框时，仍然显示上一部剧的搜索结果和剧集列表
- 导致用户体验混乱，可能加载错误的弹幕

**根本原因**：
- `DanmakuSearchState` 是单例模式，状态在整个应用生命周期中保持
- 切换影视剧时没有清理弹幕搜索状态

**解决方案**：
在切换影视剧时调用 `DanmakuSearchState.getInstance().clear()` 清理状态

**修改文件**：

1. **Leanback 版本** - `app/src/leanback/java/com/fongmi/android/tv/ui/activity/VideoActivity.java`
   - 位置：`getDetail(Vod item)` 方法（第417行）
   - 添加：`DanmakuSearchState.getInstance().clear();`
   - 添加 import：`import com.fongmi.android.tv.ui.dialog.DanmakuSearchState;`

2. **Mobile 版本** - `app/src/mobile/java/com/fongmi/android/tv/ui/activity/VideoActivity.java`
   - 位置：`getDetail(Vod item)` 方法（第528行）
   - 添加：`DanmakuSearchState.getInstance().clear();`
   - 添加 import：`import com.fongmi.android.tv.ui.dialog.DanmakuSearchState;`

**代码示例**：
```java
private void getDetail(Vod item) {
    getIntent().putExtra("key", item.getSiteKey());
    getIntent().putExtra("pic", item.getVodPic());
    getIntent().putExtra("id", item.getVodId());
    mBinding.scroll.scrollTo(0, 0);
    mClock.setCallback(null);
    mPlayers.reset();
    mPlayers.stop();
    // 清理弹幕搜索状态，避免切换影视剧后状态混乱
    DanmakuSearchState.getInstance().clear();
    getDetail();
}
```

---

### 问题2：弹幕检索日志未输出到 log.html

**问题描述**：
- 弹幕搜索和匹配过程中的日志使用 `android.util.Log` 输出
- 这些日志只能通过 `adb logcat` 查看，无法在应用内的 log.html 中查看
- 不利于用户调试和问题排查

**根本原因**：
- 项目使用 `com.orhanobut.logger.Logger` 来输出日志到 log.html
- `DanmakuSearchDialog` 中使用的是 Android 原生的 `android.util.Log`

**解决方案**：
将所有 `android.util.Log` 替换为 `Logger.t(TAG)`

**修改文件**：

**DanmakuSearchDialog.java** - `app/src/main/java/com/fongmi/android/tv/ui/dialog/DanmakuSearchDialog.java`

1. **添加 import**：
   ```java
   import com.orhanobut.logger.Logger;
   ```

2. **添加 TAG 常量**：
   ```java
   private static final String TAG = DanmakuSearchDialog.class.getSimpleName();
   ```

3. **替换所有日志调用**：
   - `android.util.Log.d("DanmakuSearch", message)` → `Logger.t(TAG).d(message)`
   - `android.util.Log.e("DanmakuSearch", message, exception)` → `Logger.t(TAG).e(message, exception)`

**修改统计**：
- 替换了约 60+ 处日志调用
- 涉及的方法：
  - `findCurrentEpisodePosition()` - 集数匹配日志
  - `extractEpisodeNumber()` - 集数提取日志
  - `showEpisodesView()` - 剧集列表显示日志
  - `autoLoadCurrentEpisodeDanmaku()` - 自动加载弹幕日志
  - `scrollToPositionWithCenter()` - 滚动定位日志

**日志示例**：
```java
// 修改前
android.util.Log.d("DanmakuSearch", "=== 开始查找当前集数位置 ===");
android.util.Log.d("DanmakuSearch", "当前播放标题: " + currentTitle);
android.util.Log.e("DanmakuSearch", "匹配过程出错", e);

// 修改后
Logger.t(TAG).d("=== 开始查找当前集数位置 ===");
Logger.t(TAG).d("当前播放标题: " + currentTitle);
Logger.t(TAG).e("匹配过程出错", e);
```

---

## 测试验证

### 测试场景1：切换影视剧
1. 打开影视剧A，搜索弹幕
2. 切换到影视剧B
3. 再次打开弹幕搜索对话框
4. **预期结果**：显示空白状态或影视剧B的默认搜索关键词，不显示影视剧A的搜索结果

### 测试场景2：查看弹幕日志
1. 打开影视剧，点击弹幕搜索
2. 搜索并选择番剧
3. 打开应用内的 log.html 页面
4. **预期结果**：可以看到弹幕搜索、匹配、加载的详细日志

---

## 日志输出示例

在 log.html 中可以看到的日志：

```
[DanmakuSearchDialog] === 开始查找当前集数位置 ===
[DanmakuSearchDialog] 当前播放标题: 涌江龙宫
[DanmakuSearchDialog] 当前播放Artist: 正在播放：第 1 季|涌江龙宫|14集
[DanmakuSearchDialog] 从Artist提取的剧集名称: 涌江龙宫
[DanmakuSearchDialog] 从Artist提取的集数: 14
[DanmakuSearchDialog] 最终提取的集数: 14
[DanmakuSearchDialog] 剧集列表 (共24集):
[DanmakuSearchDialog]   [0] episodeNumber=1, displayTitle=第1集
[DanmakuSearchDialog]   [1] episodeNumber=2, displayTitle=第2集
[DanmakuSearchDialog] 尝试方法1: episodeNumber字段匹配
[DanmakuSearchDialog] ✓ 方法1匹配成功! 位置: 13
[DanmakuSearchDialog] === 开始滚动到位置: 13 ===
[DanmakuSearchDialog] 策略1: scrollToPositionWithOffset, offset=500
[DanmakuSearchDialog] 策略2: smoothScrollToPosition
[DanmakuSearchDialog] 显示提示: 第 14 集
[DanmakuSearchDialog] === autoLoadCurrentEpisodeDanmaku 被调用 ===
[DanmakuSearchDialog] 剧集信息: 第14集
[DanmakuSearchDialog] 弹幕URL: https://api.dandanplay.net/api/v2/comment/...
[DanmakuSearchDialog] 弹幕已设置到播放器
```

---

## 相关文件

### 修改的文件
1. `app/src/leanback/java/com/fongmi/android/tv/ui/activity/VideoActivity.java`
2. `app/src/mobile/java/com/fongmi/android/tv/ui/activity/VideoActivity.java`
3. `app/src/main/java/com/fongmi/android/tv/ui/dialog/DanmakuSearchDialog.java`

### 相关类
- `DanmakuSearchState` - 弹幕搜索状态管理
- `DanmakuSearchDialog` - 弹幕搜索对话框
- `Logger` - 日志输出工具（com.orhanobut.logger）

---

## 注意事项

1. **状态清理时机**
   - 只在切换影视剧时清理状态
   - 同一部剧的不同集数切换时不清理，保持搜索状态

2. **日志输出**
   - 使用 `Logger.t(TAG)` 可以在日志中显示类名标签
   - 便于在 log.html 中过滤和查找弹幕相关日志

3. **向后兼容**
   - 修改不影响现有功能
   - 只是增强了状态管理和日志输出

---

## 修改统计

```
app/src/leanback/java/.../VideoActivity.java      |   3 +
app/src/mobile/java/.../VideoActivity.java        |   3 +
app/src/main/java/.../DanmakuSearchDialog.java    | 123 +++---
3 files changed, 69 insertions(+), 60 deletions(-)
```

- 新增代码：6 行（状态清理）
- 修改代码：123 行（日志替换）
- 总计：129 行修改
