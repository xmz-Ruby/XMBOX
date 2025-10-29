# 弹幕搜索状态切换问题 - 最终修复方案

## 问题根因

通过日志分析发现：从《现在就出发》切换到《牧神记》时，`DanmakuSearchState.clear()` **没有被调用**。

### 原因分析

用户切换影视剧的方式有两种：

1. **在同一个 Activity 内切换**（调用 `getDetail(Vod item)`）
   - 这种情况下，我们已经添加了 `clear()` 调用
   - ✅ 可以正确清理状态

2. **关闭 Activity 并打开新的 Activity**（常见场景）
   - 从首页点击不同的影视剧
   - 从搜索结果点击不同的影视剧
   - 从历史记录点击不同的影视剧
   - ❌ `getDetail(Vod item)` 不会被调用
   - ❌ `DanmakuSearchState` 作为单例保留旧状态

## 最终解决方案

### 方案：在对话框打开时自动检测并清理

在 `DanmakuSearchDialog.restoreSearchState()` 中添加智能检测：

1. **检查当前播放的影视剧标题**
2. **与保存的搜索关键词进行匹配**
3. **如果不匹配，自动清理旧状态**

### 实现细节

#### 1. DanmakuSearchState.java

添加匹配检查方法：

```java
/**
 * 检查当前关键词是否匹配指定的标题
 * 用于判断是否需要清理状态
 */
public boolean isKeywordMatchTitle(String title) {
    if (lastKeyword.isEmpty() || title == null || title.isEmpty()) {
        return false;
    }
    // 清理标题中的集数信息
    String cleanTitle = title.replaceAll("第\\d+集", "")
                             .replaceAll("\\d+集", "")
                             .replaceAll("EP\\d+", "")
                             .replaceAll("\\[.*?\\]", "")
                             .replaceAll("\\(.*?\\)", "")
                             .trim();
    return cleanTitle.contains(lastKeyword) || lastKeyword.contains(cleanTitle);
}
```

#### 2. DanmakuSearchDialog.java

在 `restoreSearchState()` 中添加检测逻辑：

```java
private void restoreSearchState() {
    Logger.t(TAG).d("恢复搜索状态 - hasKeyword: " + !searchState.getLastKeyword().isEmpty() +
                   ", hasSearchResults: " + searchState.hasSearchResults() +
                   ", hasEpisodes: " + searchState.hasEpisodes());

    // 检查当前播放的影视剧是否与保存的关键词匹配
    String currentTitle = player != null ? player.getTitle() : "";
    if (!searchState.getLastKeyword().isEmpty() && !currentTitle.isEmpty()) {
        boolean isMatch = searchState.isKeywordMatchTitle(currentTitle);
        Logger.t(TAG).d("关键词匹配检查 - 当前标题: " + currentTitle +
                       ", 保存关键词: " + searchState.getLastKeyword() +
                       ", 匹配: " + isMatch);

        if (!isMatch) {
            Logger.t(TAG).d("检测到影视剧切换，清理旧状态");
            searchState.clear();
        }
    }

    // ... 其他代码
}
```

## 工作流程

### 场景1：同一部剧切换集数

```
《现在就出发》第1集 → 搜索弹幕 → 切换到第2集 → 打开弹幕对话框

日志输出：
[DanmakuSearchDialog] 恢复搜索状态 - hasKeyword: true, hasSearchResults: true, hasEpisodes: true
[DanmakuSearchDialog] 关键词匹配检查 - 当前标题: 现在就出发, 保存关键词: 现在就出发, 匹配: true
[DanmakuSearchDialog] 恢复关键词: 现在就出发
[DanmakuSearchDialog] 显示剧集列表

结果：✅ 保持搜索状态，显示剧集列表
```

### 场景2：切换到不同的影视剧

```
《现在就出发》→ 搜索弹幕 → 切换到《牧神记》→ 打开弹幕对话框

日志输出：
[DanmakuSearchDialog] 恢复搜索状态 - hasKeyword: true, hasSearchResults: true, hasEpisodes: true
[DanmakuSearchDialog] 关键词匹配检查 - 当前标题: 牧神记, 保存关键词: 现在就出发, 匹配: false
[DanmakuSearchDialog] 检测到影视剧切换，清理旧状态
[DanmakuSearchState] 清理弹幕搜索状态 - keyword: 现在就出发, searchResults: 5, episodes: 24
[DanmakuSearchState] 状态已清理
[DanmakuSearchDialog] 使用默认关键词: 牧神记
[DanmakuSearchDialog] 显示空白状态

结果：✅ 自动清理旧状态，显示新剧的默认关键词
```

## 优势

### 1. 自动检测，无需手动清理

- 不依赖特定的切换方式
- 无论用户如何切换影视剧，都能正确处理

### 2. 智能匹配

- 自动清理标题中的集数信息
- 支持双向匹配（标题包含关键词 或 关键词包含标题）
- 避免误判

### 3. 保持同剧集状态

- 同一部剧切换集数时，保持搜索状态
- 提升用户体验

### 4. 详细日志

- 每一步都有日志输出
- 便于调试和问题排查

## 测试用例

### 测试1：切换影视剧（不同剧）

1. 打开《现在就出发》
2. 搜索弹幕，选择番剧
3. 返回首页，打开《牧神记》
4. 点击弹幕按钮
5. **预期**：显示"牧神记"的默认关键词

### 测试2：切换集数（同一剧）

1. 打开《现在就出发》第1集
2. 搜索弹幕，选择番剧
3. 切换到第2集
4. 点击弹幕按钮
5. **预期**：保持搜索状态，显示剧集列表

### 测试3：相似剧名

1. 打开《现在就出发 第一季》
2. 搜索弹幕（关键词：现在就出发）
3. 切换到《现在就出发 第二季》
4. 点击弹幕按钮
5. **预期**：保持搜索状态（因为关键词匹配）

### 测试4：完全不同的剧

1. 打开《现在就出发》
2. 搜索弹幕
3. 切换到《斗罗大陆》
4. 点击弹幕按钮
5. **预期**：清理状态，显示"斗罗大陆"的默认关键词

## 修改文件

1. `DanmakuSearchState.java`
   - 添加 `isKeywordMatchTitle()` 方法
   - 添加日志输出

2. `DanmakuSearchDialog.java`
   - 在 `restoreSearchState()` 中添加匹配检测
   - 添加详细日志

3. `VideoActivity.java` (leanback & mobile)
   - 在 `getDetail(Vod item)` 中添加 `clear()` 调用（已完成）
   - 作为双重保险

## 日志示例

完整的日志输出（切换影视剧场景）：

```
[DanmakuSearchDialog] 恢复搜索状态 - hasKeyword: true, hasSearchResults: true, hasEpisodes: true
[DanmakuSearchDialog] 关键词匹配检查 - 当前标题: 牧神记, 保存关键词: 现在就出发, 匹配: false
[DanmakuSearchDialog] 检测到影视剧切换，清理旧状态
[DanmakuSearchState] 清理弹幕搜索状态 - keyword: 现在就出发, searchResults: 5, episodes: 24
[DanmakuSearchState] 状态已清理
[DanmakuSearchDialog] 使用默认关键词: 牧神记
[DanmakuSearchDialog] 显示空白状态
```

## 总结

这个方案通过**智能检测**的方式，在对话框打开时自动判断是否需要清理状态，完美解决了切换影视剧后状态混乱的问题。

**核心思想**：不依赖特定的切换方式，而是在使用时检测状态是否有效。
