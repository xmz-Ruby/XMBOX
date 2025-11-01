# 弹幕弹层UI维护文档

> 最后更新：2025-11-01
> 维护者：Claude Code

## 目录

- [概述](#概述)
- [架构设计](#架构设计)
- [文件结构](#文件结构)
- [样式系统](#样式系统)
- [焦点处理机制](#焦点处理机制)
- [关键代码说明](#关键代码说明)
- [维护指南](#维护指南)
- [常见问题](#常见问题)

---

## 概述

弹幕弹层（DanmakuDialog）是一个跨平台的弹幕搜索和选择界面，支持两种模式：

- **Mobile模式**: 手机/平板触摸界面，使用浅色/深色自适应主题
- **Leanback模式**: 电视/TV遥控器界面，使用深色主题和TV焦点系统

### 主要功能

1. 弹幕搜索：输入剧名搜索弹幕源
2. 番剧选择：从搜索结果中选择番剧
3. 剧集选择：选择具体集数加载弹幕
4. 弹幕配置：密度、透明度、字体大小等设置

---

## 架构设计

### 代码架构

```
DanmakuDialog (Java)
├── UI组件
│   ├── 搜索输入框
│   ├── 功能按钮（反转、跳转、设置、关闭）
│   ├── 番剧列表 (RecyclerView + DanmakuAnimeAdapter)
│   └── 剧集列表 (RecyclerView + DanmakuEpisodeAdapter)
├── 状态管理
│   └── DanmakuSearchState
└── API交互
    └── DanmakuApi
```

### 布局架构

```
LinearLayout (vertical)
├── 搜索栏 (LinearLayout horizontal)
│   ├── EditText (搜索输入)
│   ├── ImageView (搜索按钮)
│   ├── TextView (反转按钮)
│   ├── TextView (跳转按钮)
│   ├── ImageView (设置按钮)
│   └── ImageView (关闭按钮)
├── 加载视图 (FrameLayout)
├── 空状态视图 (LinearLayout)
└── 内容区域 (FrameLayout)
    ├── 番剧列表容器 (LinearLayout)
    │   ├── 标题栏
    │   └── RecyclerView
    └── 剧集列表容器 (LinearLayout)
        ├── 标题栏
        └── RecyclerView
```

---

## 文件结构

### Java代码

| 文件路径 | 说明 |
|---------|------|
| `app/src/main/java/com/fongmi/android/tv/ui/dialog/DanmakuDialog.java` | 主对话框逻辑，包含两个Adapter |

### 布局文件

#### Mobile版本
| 文件路径 | 说明 |
|---------|------|
| `app/src/main/res/layout/dialog_danmaku_search.xml` | 主弹层布局（竖屏） |
| `app/src/main/res/layout-land/dialog_danmaku_search.xml` | 主弹层布局（横屏） |
| `app/src/main/res/layout/item_danmaku_anime.xml` | 番剧列表项 |
| `app/src/main/res/layout/item_danmaku_episode.xml` | 剧集列表项 |

#### Leanback版本
| 文件路径 | 说明 |
|---------|------|
| `app/src/leanback/res/layout/dialog_danmaku_search.xml` | TV主弹层布局（竖屏） |
| `app/src/leanback/res/layout-land/dialog_danmaku_search.xml` | TV主弹层布局（横屏） |
| `app/src/leanback/res/layout/item_danmaku_anime.xml` | TV番剧列表项 |
| `app/src/leanback/res/layout/item_danmaku_episode.xml` | TV剧集列表项 |
| `app/src/leanback/res/layout/dialog_danmaku.xml` | TV老版弹层（已弃用） |

### Drawable资源 (Leanback专用)

#### 按钮样式
| 文件 | 说明 |
|------|------|
| `selector_button.xml` | 按钮状态选择器 |
| `shape_button_normal.xml` | 按钮普通状态 |
| `shape_button_focused.xml` | 按钮焦点状态 |

#### 搜索框样式
| 文件 | 说明 |
|------|------|
| `selector_search_input.xml` | 搜索框状态选择器 |
| `shape_search_input.xml` | 搜索框普通状态 |
| `shape_search_input_focused.xml` | 搜索框焦点状态 |

#### 列表项样式
| 文件 | 说明 |
|------|------|
| `selector_item_danmaku.xml` | 列表项状态选择器 |
| `shape_item_danmaku_normal.xml` | 列表项普通状态 |
| `shape_item_danmaku_focused.xml` | 列表项焦点状态 |
| `shape_item_danmaku_selected.xml` | 列表项选中状态 |

#### 文字样式
| 文件 | 说明 |
|------|------|
| `selector_text.xml` | 老版文字选择器（dialog_danmaku.xml使用） |
| `text_danmaku_item.xml` | 列表项文字颜色选择器 |
| `button_text.xml` | 按钮文字颜色选择器 |

### 颜色资源 (Leanback)

```xml
<!-- app/src/leanback/res/values/colors.xml -->

<!-- 主题色 -->
<color name="primary">#FFEB3B</color>        <!-- 黄色，焦点色 -->
<color name="grey_900">#212121</color>       <!-- 深灰，背景色 -->
<color name="grey_700">#616161</color>       <!-- 中灰，组件背景 -->
<color name="grey_600">#757575</color>       <!-- 浅灰 -->

<!-- 白色系列 -->
<color name="white">#FFFFFF</color>
<color name="white_70">#B3FFFFFF</color>     <!-- 次要文字 -->
<color name="white_50">#80FFFFFF</color>     <!-- 提示文字 -->
<color name="white_30">#4DFFFFFF</color>     <!-- 边框 -->
<color name="white_20">#33FFFFFF</color>     <!-- 淡边框 -->

<!-- 黑色系列 -->
<color name="black_40">#66000000</color>     <!-- 半透明背景 -->
<color name="black_30">#4D000000</color>
```

---

## 样式系统

### 设计原则

1. **一致性**: 所有焦点效果使用统一的黄色(primary)外发光和边框
2. **层次感**: 通过背景色深浅区分不同层级
3. **对比度**: 确保在深色背景上有足够的视觉对比
4. **可访问性**: 焦点指示清晰，支持遥控器导航

### 焦点效果设计

#### 按钮焦点效果

```xml
<!-- shape_button_focused.xml -->
<layer-list>
    <!-- 外层黄色光晕 -->
    <item>
        <shape android:shape="rectangle">
            <solid android:color="@color/primary" />
            <corners android:radius="8dp" />
        </shape>
    </item>

    <!-- 内层按钮主体 -->
    <item android:bottom="2dp" android:left="2dp" android:right="2dp" android:top="2dp">
        <shape android:shape="rectangle">
            <solid android:color="@color/grey_700" />
            <stroke android:width="2dp" android:color="@color/primary" />
            <corners android:radius="6dp" />
        </shape>
    </item>
</layer-list>
```

**效果说明**:
- 外层: 2dp黄色光晕
- 内层: 深灰背景 + 2dp黄色边框
- 圆角: 外层8dp，内层6dp

#### 列表项焦点效果

```xml
<!-- shape_item_danmaku_focused.xml -->
<layer-list>
    <!-- 外层黄色光晕 -->
    <item>
        <shape android:shape="rectangle">
            <solid android:color="@color/primary" />
            <corners android:radius="8dp" />
        </shape>
    </item>

    <!-- 内层主体 -->
    <item android:bottom="2dp" android:left="2dp" android:right="2dp" android:top="2dp">
        <shape android:shape="rectangle">
            <solid android:color="@color/grey_700" />
            <stroke android:width="2dp" android:color="@color/primary" />
            <corners android:radius="6dp" />
        </shape>
    </item>
</layer-list>
```

### 状态优先级

```xml
<!-- selector_item_danmaku.xml -->
<selector>
    <!-- 1. 焦点状态（最高优先级） -->
    <item state_focused="true" drawable="@drawable/shape_item_danmaku_focused" />

    <!-- 2. 焦点+选中组合状态 -->
    <item state_activated="true" state_focused="true"
          drawable="@drawable/shape_item_danmaku_focused" />

    <!-- 3. 选中但无焦点 -->
    <item state_activated="true"
          drawable="@drawable/shape_item_danmaku_selected" />

    <!-- 4. 默认状态 -->
    <item drawable="@drawable/shape_item_danmaku_normal" />
</selector>
```

**优先级说明**:
1. **Focused** (获得遥控器焦点): 黄色高亮
2. **Activated** (被选中): 选中样式
3. **Normal**: 半透明背景

### 文字颜色系统

```xml
<!-- text_danmaku_item.xml -->
<selector>
    <item android:color="@color/primary" android:state_focused="true" />
    <item android:color="@color/primary" android:state_activated="true" />
    <item android:color="@color/white" />
</selector>
```

**规则**:
- 焦点/选中: 黄色（与边框同色）
- 普通状态: 白色
- 使用 `duplicateParentState="true"` 继承父容器状态

---

## 焦点处理机制

### RecyclerView焦点配置

```xml
<androidx.recyclerview.widget.RecyclerView
    android:descendantFocusability="afterDescendants"
    ... />
```

**说明**: `afterDescendants` 允许子项（列表项）优先获取焦点。

### Adapter焦点处理

#### 1. 状态绑定

```java
@Override
public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    // 设置activated状态触发selector
    holder.itemView.setActivated(position == selectedPosition);

    // 添加焦点监听器确保重绘
    holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
        v.invalidate();
    });
}
```

#### 2. 为什么需要焦点监听器?

在RecyclerView中，item获取焦点时状态变化不会自动触发重绘。通过添加 `OnFocusChangeListener` 并调用 `invalidate()`，确保焦点变化时View重新绘制，从而显示正确的焦点样式。

#### 3. 避免直接设置背景色

❌ **错误做法**:
```java
if (position == selectedPosition) {
    holder.itemView.setBackgroundColor(0x3300FF00);
} else {
    holder.itemView.setBackgroundColor(0x00000000);
}
```

✅ **正确做法**:
```java
holder.itemView.setActivated(position == selectedPosition);
```

**原因**: 直接设置背景色会覆盖XML中定义的selector drawable，导致焦点效果失效。

### 布局焦点配置

#### 列表项布局

```xml
<LinearLayout
    android:background="@drawable/selector_item_danmaku"
    android:focusable="true"
    android:focusableInTouchMode="true">

    <TextView
        android:textColor="@color/text_danmaku_item"
        android:duplicateParentState="true" />
</LinearLayout>
```

**关键属性**:
- `focusable="true"`: 可以通过遥控器获取焦点
- `focusableInTouchMode="true"`: 触摸模式下也可获取焦点
- `duplicateParentState="true"`: 子View继承父容器的状态

---

## 关键代码说明

### DanmakuDialog.java

#### Adapter结构

```java
private class DanmakuAnimeAdapter extends RecyclerView.Adapter<...> {
    private int selectedPosition = -1;

    void setSelectedPosition(int position) {
        int oldPosition = selectedPosition;
        selectedPosition = position;
        // 刷新旧位置和新位置
        if (oldPosition >= 0) notifyItemChanged(oldPosition);
        if (selectedPosition >= 0) notifyItemChanged(selectedPosition);
    }
}
```

#### 状态管理

```java
// 搜索状态类
private class DanmakuSearchState {
    private int selectedAnimePosition = -1;
    private int highlightedEpisodePosition = -1;

    void setSelectedAnime(DanmakuAnime anime, int position) {
        this.selectedAnimePosition = position;
    }

    void setHighlightedEpisodePosition(int position) {
        this.highlightedEpisodePosition = position;
    }
}
```

#### Context空指针修复

```java
private void scrollToPositionWithCenter(int position, boolean showToast) {
    // 保存Context引用，避免在延迟执行时Context为null
    final android.content.Context context = getContext();

    episodeResults.postDelayed(() -> {
        if (showToast && context != null) {
            Toast.makeText(context, "已定位到...", Toast.LENGTH_SHORT).show();
        }
    }, 400);
}
```

**原因**: 在 `postDelayed` 中使用 `getContext()` 可能返回null（Dialog已关闭），提前保存引用可以避免崩溃。

---

## 维护指南

### 修改焦点样式

#### 1. 修改焦点颜色

编辑 `app/src/leanback/res/values/colors.xml`:
```xml
<color name="primary">#你的颜色</color>
```

#### 2. 修改焦点边框粗细

编辑各个 `shape_*_focused.xml` 文件:
```xml
<stroke
    android:width="你的宽度dp"
    android:color="@color/primary" />
```

#### 3. 修改外发光大小

调整 layer-list 中的边距:
```xml
<item
    android:bottom="边距dp"
    android:left="边距dp"
    android:right="边距dp"
    android:top="边距dp">
```

### 添加新按钮

1. 在布局文件中添加按钮
2. 设置 `android:background="@drawable/selector_button"`
3. 添加 `android:focusable="true"`
4. 在Java代码中绑定点击事件

示例:
```xml
<ImageView
    android:id="@+id/my_button"
    android:layout_width="48dp"
    android:layout_height="48dp"
    android:padding="12dp"
    android:src="@drawable/my_icon"
    android:background="@drawable/selector_button"
    android:focusable="true"
    android:focusableInTouchMode="true"
    android:contentDescription="我的按钮" />
```

### 修改列表项样式

#### 1. 修改列表项高度/内边距

编辑 `item_danmaku_*.xml`:
```xml
<LinearLayout
    android:paddingStart="你的padding"
    android:paddingTop="你的padding"
    ... >
```

#### 2. 修改文字大小

```xml
<TextView
    android:textSize="你的大小sp"
    ... />
```

#### 3. 添加新的视觉元素

在 `LinearLayout` 或 `FrameLayout` 中添加新的 View，记得添加 `duplicateParentState="true"` 以继承焦点状态。

### 调试技巧

#### 1. 查看焦点状态

在Adapter的 `onBindViewHolder` 中添加日志:
```java
holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
    Log.d("Focus", "Position " + position + " focused: " + hasFocus);
    v.invalidate();
});
```

#### 2. 检查selector是否生效

临时修改 `shape_*_focused.xml` 的颜色为明显的颜色（如红色）测试。

#### 3. 检查状态继承

在TextView上添加背景色测试 `duplicateParentState`:
```xml
<TextView
    android:background="#33FF0000"
    android:duplicateParentState="true" />
```

---

## 常见问题

### Q1: 遥控器移动焦点时列表项没有高亮？

**原因**:
1. 没有添加 `android:focusable="true"`
2. 没有添加焦点监听器调用 `invalidate()`
3. RecyclerView缺少 `android:descendantFocusability="afterDescendants"`

**解决方案**:
```java
holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
    v.invalidate();
});
```

### Q2: 焦点样式不显示，只有选中样式？

**原因**: Selector优先级配置错误，或者在代码中使用了 `setBackgroundColor()` 覆盖了drawable。

**解决方案**:
1. 检查selector中 `state_focused` 是否在最前面
2. 不要使用 `setBackgroundColor()`，使用 `setActivated()` 代替

### Q3: 选中状态和焦点状态冲突？

**原因**: Selector中需要处理组合状态。

**解决方案**:
```xml
<selector>
    <!-- 焦点优先 -->
    <item state_focused="true" drawable="@drawable/focused" />
    <!-- 焦点+选中 -->
    <item state_focused="true" state_activated="true" drawable="@drawable/focused" />
    <!-- 仅选中 -->
    <item state_activated="true" drawable="@drawable/selected" />
    <!-- 默认 -->
    <item drawable="@drawable/normal" />
</selector>
```

### Q4: 搜索框焦点不显示？

**原因**: EditText需要使用selector drawable而不是单一的shape。

**解决方案**:
```xml
<EditText
    android:background="@drawable/selector_search_input"
    ... />
```

### Q5: Toast显示时应用崩溃？

**原因**: 在延迟执行中 `getContext()` 返回null。

**解决方案**:
```java
final Context context = getContext();
view.postDelayed(() -> {
    if (context != null) {
        Toast.makeText(context, "...", Toast.LENGTH_SHORT).show();
    }
}, delay);
```

### Q6: Leanback主题是浅色而不是深色？

**原因**: 主题继承了 `Theme.MaterialComponents.Light`。

**解决方案**:
在 `app/src/leanback/res/values/styles.xml` 中:
```xml
<style name="BaseTheme" parent="Theme.MaterialComponents.NoActionBar">
    <!-- 不要使用 Theme.MaterialComponents.Light.NoActionBar -->
</style>
```

### Q7: 文字颜色不随焦点变化？

**原因**: TextView没有使用ColorStateList或缺少 `duplicateParentState="true"`。

**解决方案**:
```xml
<TextView
    android:textColor="@color/text_danmaku_item"
    android:duplicateParentState="true" />
```

### Q8: 按钮获得焦点但没有视觉反馈？

**原因**: 按钮使用了系统默认的 `?attr/selectableItemBackground`。

**解决方案**:
使用自定义selector:
```xml
<ImageView
    android:background="@drawable/selector_button"
    android:focusable="true"
    ... />
```

---

## 版本历史

### v2.0 (2025-11-01)
- ✅ 统一Leanback焦点样式系统
- ✅ 修复列表项焦点显示问题
- ✅ 修复搜索框焦点显示问题
- ✅ 优化按钮和列表项样式
- ✅ 修复Context空指针崩溃
- ✅ 将主题改为深色

### v1.0 (之前)
- 初始实现
- 支持Mobile和Leanback双模式
- 基本的弹幕搜索和选择功能

---

## 相关文档

- [弹幕系统架构](./danmaku-system.md)
- Android TV焦点系统: https://developer.android.com/training/tv/start/navigation
- Material Design: https://material.io/design

---

## 联系方式

如有问题或建议，请在项目中创建Issue。
