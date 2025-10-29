# 弹幕搜索状态调试指南

## 问题描述

在剧A中选择过弹幕后，切换到剧B，再次打开弹幕搜索对话框时，不显示剧B的默认关键词。

## 调试日志

我们已经在关键位置添加了详细的日志输出，所有日志都会输出到 log.html 中。

### 日志位置

1. **DanmakuSearchState.clear()** - 状态清理日志
2. **DanmakuSearchDialog.restoreSearchState()** - 状态恢复日志
3. **VideoActivity.getDetail(Vod item)** - 切换影视剧时的调用

### 日志示例

#### 正常流程

```
[VideoActivity] 切换影视剧: 从《剧A》到《剧B》
[DanmakuSearchState] 清理弹幕搜索状态 - keyword: 剧A, searchResults: 5, episodes: 24
[DanmakuSearchState] 状态已清理
[DanmakuSearchDialog] 恢复搜索状态 - hasKeyword: false, hasSearchResults: false, hasEpisodes: false
[DanmakuSearchDialog] 使用默认关键词: 剧B
[DanmakuSearchDialog] 显示空白状态
```

#### 异常流程（如果状态未清理）

```
[VideoActivity] 切换影视剧: 从《剧A》到《剧B》
[DanmakuSearchDialog] 恢复搜索状态 - hasKeyword: true, hasSearchResults: true, hasEpisodes: true
[DanmakuSearchDialog] 恢复关键词: 剧A
[DanmakuSearchDialog] 显示搜索结果
```

## 调试步骤

### 步骤1：重现问题

1. 打开剧A
2. 点击弹幕按钮，搜索并选择弹幕
3. 返回，切换到剧B
4. 再次点击弹幕按钮
5. **观察**：是否显示剧B的默认关键词？

### 步骤2：查看日志

1. 打开应用内的 log.html 页面
2. 搜索关键词：`DanmakuSearchState` 或 `DanmakuSearchDialog`
3. 查看日志输出

### 步骤3：分析日志

#### 检查点1：clear() 是否被调用？

在切换影视剧时，应该看到：
```
[DanmakuSearchState] 清理弹幕搜索状态 - keyword: xxx, searchResults: x, episodes: x
[DanmakuSearchState] 状态已清理
```

**如果没有看到这些日志**：
- 说明 `clear()` 没有被调用
- 可能是 `getDetail(Vod item)` 没有被执行
- 或者切换的不是影视剧，而是同一部剧的不同集数

#### 检查点2：restoreSearchState() 的状态

打开弹幕对话框时，应该看到：
```
[DanmakuSearchDialog] 恢复搜索状态 - hasKeyword: false, hasSearchResults: false, hasEpisodes: false
```

**如果看到的是**：
```
[DanmakuSearchDialog] 恢复搜索状态 - hasKeyword: true, hasSearchResults: true, hasEpisodes: true
```

说明状态没有被清理，或者清理后又被重新设置了。

#### 检查点3：默认关键词

应该看到：
```
[DanmakuSearchDialog] 使用默认关键词: 剧B
```

**如果看到的是空字符串**：
```
[DanmakuSearchDialog] 使用默认关键词:
```

说明 `getDefaultSearchKeyword()` 没有正确提取剧名。

## 可能的问题原因

### 原因1：切换的是集数而不是影视剧

**现象**：
- 在同一部剧内切换不同集数
- `getDetail(Vod item)` 不会被调用
- 状态不会被清理

**解决方案**：
- 这是预期行为，同一部剧切换集数时应该保持搜索状态
- 如果需要清理，应该在切换集数时也调用 `clearEpisodes()`

### 原因2：clear() 调用时机问题

**现象**：
- `clear()` 被调用了
- 但之后又有代码重新设置了状态

**解决方案**：
- 检查 `clear()` 之后是否有其他代码设置了 `searchState`
- 确保 `clear()` 在正确的时机被调用

### 原因3：单例状态未正确清理

**现象**：
- `clear()` 被调用了
- 但 `hasSearchResults()` 或 `hasEpisodes()` 仍然返回 true

**解决方案**：
- 检查 `clear()` 方法是否正确清空了所有字段
- 确保 `searchResults.clear()` 和 `episodes.clear()` 被执行

## 临时解决方案

如果问题仍然存在，可以尝试以下临时方案：

### 方案1：在对话框打开时强制清理

在 `DanmakuSearchDialog.initView()` 中添加：

```java
@Override
protected void initView() {
    mainHandler = new Handler(Looper.getMainLooper());

    // 检查是否需要清理状态（根据播放器的 title 判断）
    String currentTitle = player != null ? player.getTitle() : "";
    String lastKeyword = searchState.getLastKeyword();

    if (!currentTitle.isEmpty() && !lastKeyword.isEmpty() && !currentTitle.contains(lastKeyword)) {
        Logger.t(TAG).d("检测到影视剧切换，清理状态");
        searchState.clear();
    }

    searchState = DanmakuSearchState.getInstance();
    // ... 其他代码
}
```

### 方案2：添加影视剧ID追踪

在 `DanmakuSearchState` 中添加：

```java
private String currentVodId = "";

public void setCurrentVodId(String vodId) {
    if (!this.currentVodId.equals(vodId)) {
        Logger.t(TAG).d("影视剧ID变化: " + this.currentVodId + " -> " + vodId);
        clear();
        this.currentVodId = vodId;
    }
}
```

在 `VideoActivity.getDetail(Vod item)` 中调用：

```java
DanmakuSearchState.getInstance().setCurrentVodId(item.getVodId());
```

## 测试验证

### 测试用例1：切换影视剧

1. 打开剧A（ID: 001）
2. 搜索弹幕，选择番剧
3. 切换到剧B（ID: 002）
4. 打开弹幕对话框
5. **预期**：显示剧B的默认关键词

### 测试用例2：切换集数

1. 打开剧A第1集
2. 搜索弹幕，选择番剧
3. 切换到剧A第2集
4. 打开弹幕对话框
5. **预期**：保持搜索状态，显示剧集列表

### 测试用例3：返回同一部剧

1. 打开剧A
2. 搜索弹幕，选择番剧
3. 切换到剧B
4. 再切换回剧A
5. 打开弹幕对话框
6. **预期**：显示剧A的默认关键词（状态已清理）

## 日志收集

如果问题仍然存在，请提供以下信息：

1. **完整的日志输出**（从 log.html 复制）
2. **操作步骤**（详细描述如何重现问题）
3. **影视剧信息**（剧名、ID、集数）
4. **预期行为 vs 实际行为**

## 相关代码位置

- `DanmakuSearchState.java:80` - clear() 方法
- `DanmakuSearchDialog.java:142` - restoreSearchState() 方法
- `VideoActivity.java:426` (leanback) - 切换影视剧时调用 clear()
- `VideoActivity.java:539` (mobile) - 切换影视剧时调用 clear()
