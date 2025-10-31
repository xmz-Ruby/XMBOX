# 弹幕投送二维码功能实现

## 功能概述

维护 leanback 播放器的弹幕弹层，将剧集名输入框改为只读，点击后显示局域网投送二维码，使用手机扫描后进入 9978 服务页面进行弹幕检索和投送。

## 修改内容

### 1. DanmakuDialog.java 修改

**文件路径**: `app/src/main/java/com/fongmi/android/tv/ui/dialog/DanmakuDialog.java`

#### 1.1 添加导入

```java
import com.fongmi.android.tv.server.Server;
import com.fongmi.android.tv.utils.QRCode;
import com.github.catvod.Proxy;
import com.github.catvod.utils.Util;
import android.graphics.Bitmap;
```

#### 1.2 修改 initEvent() 方法

将搜索输入框设为只读，但保持可聚焦（支持遥控器选中），并添加点击和按键事件：

```java
@Override
protected void initEvent() {
    // 将搜索输入框设为只读，但保持可聚焦（遥控器可选中）
    searchInput.setFocusable(true);
    searchInput.setFocusableInTouchMode(true);
    searchInput.setCursorVisible(false);
    searchInput.setKeyListener(null); // 禁止键盘输入，保持只读

    // 点击输入框显示投送二维码
    searchInput.setOnClickListener(v -> showCastQRCode());

    // 遥控器按确认键也显示投送二维码，方向键不拦截以便导航
    searchInput.setOnKeyListener((v, keyCode, event) -> {
        // 只处理确认键，方向键返回 false 让系统处理焦点导航
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
                showCastQRCode();
                return true;
            }
            // 方向键不拦截，返回 false 让系统处理焦点切换
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP ||
                keyCode == KeyEvent.KEYCODE_DPAD_DOWN ||
                keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
                keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                return false;
            }
        }
        return false;
    });

    // ... 其他代码保持不变
}
```

#### 1.3 添加 showCastQRCode() 方法

在文件末尾添加新方法，用于显示投送二维码对话框：

```java
/**
 * 显示投送二维码对话框
 */
private void showCastQRCode() {
    try {
        // 获取局域网地址和端口
        String ip = Util.getIp();
        int port = Proxy.getPort();

        if (ip.isEmpty()) {
            android.widget.Toast.makeText(getContext(), "无法获取局域网IP地址", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        // 生成投送URL - 指向弹幕搜索页面
        String castUrl = "http://" + ip + ":" + port + "/danmaku";
        Logger.t(TAG).d("生成投送URL: " + castUrl);

        // 生成二维码
        Bitmap qrBitmap = QRCode.getBitmap(castUrl, 200, 1);
        if (qrBitmap == null) {
            android.widget.Toast.makeText(getContext(), "生成二维码失败", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        // 创建对话框显示二维码
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());

        // 创建自定义布局
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 40);
        layout.setGravity(android.view.Gravity.CENTER);

        // 添加标题文本
        TextView titleText = new TextView(getContext());
        titleText.setText("扫描二维码进行弹幕投送");
        titleText.setTextSize(16);
        titleText.setGravity(android.view.Gravity.CENTER);
        titleText.setPadding(0, 0, 0, 20);
        layout.addView(titleText);

        // 添加二维码图片
        ImageView qrImageView = new ImageView(getContext());
        qrImageView.setImageBitmap(qrBitmap);
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        imageParams.gravity = android.view.Gravity.CENTER;
        qrImageView.setLayoutParams(imageParams);
        layout.addView(qrImageView);

        // 添加URL文本
        TextView urlText = new TextView(getContext());
        urlText.setText(castUrl);
        urlText.setTextSize(12);
        urlText.setGravity(android.view.Gravity.CENTER);
        urlText.setPadding(0, 20, 0, 0);
        urlText.setTextColor(0xFF999999);
        layout.addView(urlText);

        // 添加说明文本
        TextView hintText = new TextView(getContext());
        hintText.setText("使用手机扫描二维码\n在手机上搜索并投送弹幕到电视");
        hintText.setTextSize(12);
        hintText.setGravity(android.view.Gravity.CENTER);
        hintText.setPadding(0, 10, 0, 0);
        hintText.setTextColor(0xFF666666);
        layout.addView(hintText);

        builder.setView(layout);
        builder.setPositiveButton("关闭", null);
        builder.show();

    } catch (Exception e) {
        Logger.t(TAG).e("显示投送二维码失败", e);
        android.widget.Toast.makeText(getContext(), "显示二维码失败: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
    }
}
```

### 2. QRCode.java 使用说明

**位置**: `app/src/leanback/java/com/fongmi/android/tv/utils/QRCode.java`

**说明**: DanmakuDialog 虽然位于 main 源集，但可以直接使用 leanback 源集中的 QRCode 类，因为在 Android 项目中，leanback 源集的代码对 main 源集是可见的。无需复制文件，避免类重复编译错误。

## 功能说明

### 用户体验流程

1. 用户在电视端打开弹幕对话框
2. 点击顶部的搜索输入框（现在是只读的）
3. 弹出二维码对话框，显示：
   - 标题："扫描二维码进行弹幕投送"
   - 二维码图片（200x200 dp）
   - 投送 URL（格式：`http://局域网IP:端口/danmaku`）
   - 使用说明："使用手机扫描二维码\n在手机上搜索并投送弹幕到电视"
4. 用户使用手机扫描二维码
5. 手机浏览器打开 9978 服务的弹幕搜索页面
6. 用户在手机上搜索并选择弹幕
7. 弹幕自动投送到电视端播放器

### 技术实现

- **局域网 IP 获取**: 使用 `Util.getIp()` 方法，优先获取 wlan、eth 网络接口的 IPv4 地址
- **服务端口**: 使用 `Proxy.getPort()` 获取当前 9978 服务的端口号
- **二维码生成**: 使用 `QRCode.getBitmap()` 方法生成二维码位图
- **投送 URL**: 格式为 `http://[局域网IP]:[端口]/danmaku`

### 错误处理

- 无法获取局域网 IP 时，显示提示："无法获取局域网IP地址"
- 二维码生成失败时，显示提示："生成二维码失败"
- 其他异常会捕获并显示详细错误信息

## 依赖关系

### 使用的工具类

1. **Util.getIp()** - 获取局域网 IP 地址
   - 位置: `catvod/src/main/java/com/github/catvod/utils/Util.java`
   - 功能: 依次尝试 wlan、eth、wifi 接口获取 IPv4 地址

2. **Proxy.getPort()** - 获取服务端口
   - 位置: `catvod/src/main/java/com/github/catvod/Proxy.java`
   - 功能: 返回当前 HTTP 服务的端口号（9978-9999 范围）

3. **QRCode.getBitmap()** - 生成二维码
   - 位置: `app/src/main/java/com/fongmi/android/tv/utils/QRCode.java`
   - 功能: 使用 ZXing 库生成二维码位图

4. **Server.get()** - 服务器单例
   - 位置: `app/src/main/java/com/fongmi/android/tv/server/Server.java`
   - 功能: 管理 HTTP 服务器实例

## 注意事项

1. **输入框只读**: 搜索输入框现在是只读的，用户无法直接输入文字
2. **遥控器支持**: 输入框保持可聚焦状态，遥控器可以选中并按确认键显示二维码
3. **触屏支持**: 触屏点击输入框也可以显示二维码
4. **搜索功能保留**: 搜索按钮仍然可用，可以通过其他方式触发搜索
5. **网络要求**: 手机和电视必须在同一局域网内
6. **服务启动**: 确保 9978 HTTP 服务已启动（通常在应用启动时自动启动）

## 交互方式

### 遥控器操作
1. 使用方向键（上下左右）选中搜索输入框
2. 按确认键（DPAD_CENTER 或 ENTER）显示二维码对话框

### 触屏操作
1. 直接点击搜索输入框
2. 显示二维码对话框

## 测试建议

1. 测试局域网 IP 获取是否正常
2. 测试二维码生成和显示
3. 测试手机扫描二维码后能否正常访问
4. 测试弹幕投送功能是否正常工作
5. 测试错误处理（如无网络连接时）

## 相关文件

- `app/src/main/java/com/fongmi/android/tv/ui/dialog/DanmakuDialog.java` - 主要修改文件
- `app/src/leanback/java/com/fongmi/android/tv/utils/QRCode.java` - 二维码生成工具（已存在）
- `app/src/main/res/layout/dialog_danmaku_search.xml` - 对话框布局（未修改）
- `catvod/src/main/java/com/github/catvod/utils/Util.java` - IP 获取工具
- `app/src/main/java/com/fongmi/android/tv/server/Server.java` - HTTP 服务器

## 编译说明

由于 Android 项目的源集结构，leanback 源集中的类对 main 源集是可见的，因此 DanmakuDialog（位于 main）可以直接使用 QRCode（位于 leanback），无需在 main 中重复创建该类。这样可以避免"类重复"的编译错误。

## 修改日期

2025-10-31
