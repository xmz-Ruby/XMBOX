<h1 align="center"> 📱 XMBOX - Android资源播放器
  </h1>
<div align="center">

![Version](https://img.shields.io/badge/version-3.0.9-blue.svg)
![Android](https://img.shields.io/badge/platform-Android-green.svg)
![License](https://img.shields.io/badge/license-GPL--3.0-orange.svg)
![Build](https://img.shields.io/badge/build-passing-brightgreen.svg)

一个操作方便、界面简洁的Android视频播放器盒子，需自行添源，支持TV和手机双平台。

[下载APK](https://github.com/Tosencen/XMBOX-Release/tree/main/apk/release) • [功能特性](#-功能特性) • [构建指南](#-构建指南) • [API文档](#-api文档)
<img width="1920" height="864" alt="Group 15" src="https://github.com/user-attachments/assets/e69741bd-a21d-417e-ad85-e747032f6daf" />

</div>

## 🎯 功能特性

### 📺 多平台支持
- **Android TV版本** - 针对电视、盒子优化的遥控器界面
- **手机版本** - 触屏友好的移动端界面
- **多架构支持** - ARM64-V8A 和 ARM V7A 双架构

### 🎬 强大的播放功能
- 🎵 **多格式支持** - 支持主流视频格式播放
- 📡 **直播观看** - 支持各种直播源协议
- 🔍 **智能搜索** - 全局搜索和换源功能
- 📚 **收藏管理** - 视频收藏和历史记录
- 🎨 **自定义界面** - 丰富的主题和布局选项

### ⚡ 技术特色
- 🚀 **高性能播放** - 基于ExoPlayer播放内核
- 🔧 **模块化架构** - 清晰的模块分层设计
- 🛡️ **稳定可靠** - 完善的错误处理和崩溃防护
- 🌐 **网络优化** - 智能代理和DNS解析
- 📱 **Material Design** - 现代化UI设计

## 📥 下载安装

### 最新版本: v3.0.9

| 平台 | ARM64-V8A | ARM V7A |
|------|-----------|---------|
| **📱 手机版** | [下载 (35.8MB)](https://github.com/Tosencen/XMBOX-Release/raw/main/apk/release/v3.0.9/mobile-arm64_v8a-v3.0.9.apk) | [下载 (31.6MB)](https://github.com/Tosencen/XMBOX-Release/raw/main/apk/release/v3.0.9/mobile-armeabi_v7a-v3.0.9.apk) |
| **📺 TV版** | [下载 (35.9MB)](https://github.com/Tosencen/XMBOX-Release/raw/main/apk/release/v3.0.9/leanback-arm64_v8a-v3.0.9.apk) | [下载 (31.7MB)](https://github.com/Tosencen/XMBOX-Release/raw/main/apk/release/v3.0.9/leanback-armeabi_v7a-v3.0.9.apk) |

### 📁 版本历史
- **v3.0.9**: [查看v3.0.9版本](https://github.com/Tosencen/XMBOX-Release/tree/main/apk/release/v3.0.9) - 新增直播开关控制和UI交互优化
- **v3.0.8**: [查看v3.0.8版本](https://github.com/Tosencen/XMBOX-Release/tree/main/apk/release/v3.0.8) - UI交互体验全面优化
- **v3.0.7**: [查看v3.0.7版本](https://github.com/Tosencen/XMBOX-Release/tree/main/apk/release/v3.0.7) - 全面优化稳定性和用户体验

### 📦 下载说明
- **最新版本**: 根目录的 `mobile.json` 和 `leanback.json` 包含最新版本信息
- **历史版本**: 每个版本都有独立的文件夹，包含完整的APK文件和版本信息
- **文件结构**: 按版本号组织，便于管理和下载
- **签名保护**: 所有APK均使用v1/v2/v3/v4多重签名保护

TV版基于 [FongMi/TV](https://github.com/FongMi/TV) 原项目就改了些配色，想要嘿稳定的可去原项目体验
### 📋 系统要求
- Android 5.0 (API 21) 及以上
- ARM64-V8A: 推荐新设备使用，性能更优
- ARM V7A: 兼容老设备，适配性更强

## 🏗️ 构建指南

### 📋 环境要求
- Android Studio Arctic Fox 或更高版本
- JDK 11 或更高版本
- Android SDK API 35
- Gradle 8.10.2

### 🔧 快速开始

1. **克隆项目**
```bash
git clone https://github.com/yourusername/XMBOX.git
cd XMBOX
```

2. **配置签名**

项目已包含签名配置，签名文件位于 `keystore/release.jks`

**默认签名信息：**
- 密钥库密码：`xmbox123`
- 密钥别名：`xmbox`
- 密钥密码：`xmbox123`
- 签名算法：RSA 2048位 + SHA256
- 有效期：10000天（约27年）

**如需自定义签名：**

方法一：替换签名文件
```bash
# 生成新的签名文件
keytool -genkeypair -v -keystore keystore/release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias your_alias \
  -storepass your_store_password \
  -keypass your_key_password \
  -dname "CN=YourName, OU=YourUnit, O=YourOrg, L=YourCity, ST=YourState, C=YourCountry"
```

方法二：修改配置文件
```gradle
// 编辑 app/build.gradle 中的 signingConfigs
signingConfigs {
    release {
        storeFile file("../keystore/release.jks")
        storePassword "your_store_password"
        keyAlias "your_alias"
        keyPassword "your_key_password"
    }
}
```

3. **构建项目**

**构建单架构版本：**
```bash
# 手机版
./gradlew assembleArm64_v8aMobileRelease    # ARM64 手机版
./gradlew assembleArmeabi_v7aMobileRelease  # ARM V7A 手机版
./gradlew assembleX86_64MobileRelease       # x86_64 手机版

# TV版
./gradlew assembleArm64_v8aLeanbackRelease    # ARM64 TV版
./gradlew assembleArmeabi_v7aLeanbackRelease  # ARM V7A TV版
./gradlew assembleX86_64LeanbackRelease       # x86_64 TV版
```

**构建全架构通用版本（推荐）：**
```bash
# 包含 ARM64-V8A + ARM V7A + x86_64 三个架构
./gradlew assembleUniversalMobileRelease    # 手机版全架构通用APK
./gradlew assembleUniversalLeanbackRelease  # TV版全架构通用APK
```

**构建AAB格式（用于Google Play）：**
```bash
./gradlew bundleUniversalMobileRelease    # 手机版AAB
./gradlew bundleUniversalLeanbackRelease  # TV版AAB
```

**构建所有版本：**
```bash
./gradlew assembleRelease  # 构建所有架构和平台的Release版本
```

4. **生成的APK位置**

**单架构版本：**
```
app/build/outputs/apk/
├── arm64_v8a/mobile/release/arm64_v8a-mobile.apk
├── arm64_v8a/leanback/release/arm64_v8a-leanback.apk
├── armeabi_v7a/mobile/release/armeabi_v7a-mobile.apk
├── armeabi_v7a/leanback/release/armeabi_v7a-leanback.apk
├── x86_64/mobile/release/x86_64-mobile.apk
└── x86_64/leanback/release/x86_64-leanback.apk
```

**全架构通用版本：**
```
app/build/outputs/apk/
├── universal/mobile/release/universal-mobile.apk      # 约60-70MB
└── universal/leanback/release/universal-leanback.apk  # 约60-70MB
```

**AAB格式：**
```
app/build/outputs/bundle/
├── universalMobileRelease/app-universal-mobile-release.aab
└── universalLeanbackRelease/app-universal-leanback-release.aab
```

### 📦 版本选择建议

| 版本类型 | 体积 | 兼容性 | 适用场景 |
|---------|------|--------|---------|
| **单架构版本** | 小（30-35MB） | 特定架构 | 明确知道设备架构，追求最小体积 |
| **全架构通用版** | 大（60-70MB） | 所有设备 | 不确定设备架构，追求最大兼容性 |
| **AAB格式** | 动态 | 所有设备 | Google Play上架，自动优化分发 |

### 🔐 签名验证

验证APK签名信息：
```bash
# 查看签名信息
keytool -printcert -jarfile app/build/outputs/apk/universal/mobile/release/universal-mobile.apk

# 验证签名
jarsigner -verify -verbose -certs app/build/outputs/apk/universal/mobile/release/universal-mobile.apk
```

## 🏛️ 项目架构

### 📂 模块说明
```
XMBOX/
├── app/                # 主应用模块
│   ├── src/main/      # 通用代码
│   ├── src/mobile/    # 手机版特定代码
│   └── src/leanback/  # TV版特定代码
├── catvod/            # 视频点播核心
├── quickjs/           # JavaScript引擎
├── forcetech/         # 强制技术模块
├── thunder/           # 迅雷下载模块
├── hook/              # 钩子功能
├── jianpian/          # 视频剪辑模块
├── tvbus/             # TV总线功能
└── zlive/             # 直播功能模块
```

### 🔧 技术栈
- **开发语言**: Java
- **UI框架**: Android Views + Material Components
- **播放器**: ExoPlayer
- **网络库**: OkHttp
- **JSON解析**: Gson
- **异步处理**: EventBus
- **数据库**: Room

## 📝 更新日志

### v3.0.9 (2025-10-24)

#### ✨ 新功能
* **直播开关控制** - 新增直播tab显示/隐藏开关，用户可根据需要控制直播功能
* **实时倍速显示** - 播放控制对话框新增实时倍速数值显示，提升用户体验

#### 🎨 UI优化
* **刻度显示改进** - 改进滑杆刻度显示，非激活轨道显示刻度，激活轨道保持干净
* **播放进度条增强** - 增强播放进度条动态大小调整功能，修复圆球跳回问题


### v3.0.8 (2025-10-14)

#### 🎨 UI交互体验全面优化
* **修复按钮点击效果** - 解决按钮点击效果过于明显的问题
* **统一自定义背景** - 使用自定义背景替代系统selectableItemBackgroundBorderless
* **移除文字阴影** - 清理Control.Action样式中的文字阴影效果
* **优化直播页面** - 选择按钮颜色统一为主题黄色
* **调整页面布局** - 许可协议页面按钮区域上间距调整为8dp
* **修复文字重叠** - 解决跨类和换源按钮的文字重叠问题
* **提升视觉一致性** - 整体UI视觉一致性和用户体验优化

### v3.0.5 (2025-08-20)
#### 🎨 界面优化
- 优化导航栏历史记录图标，采用 Material Design 3 规范的列表图标
- 改进设置页面的图标显示效果
- 优化用户界面视觉体验

### v3.0.4 (2025-07-30)
#### 🐛 修复
- 修复设置页面源管理模块中切换视频源时的随机闪退问题
- 增强VodConfig.setHome()方法的空指针异常处理
- 改进Fragment生命周期检查以防止崩溃
- 优化HistoryDialog中源切换的安全性
- 增强并发加载的线程安全性

#### ⚡ 优化
- 提升应用启动速度
- 优化内存使用
- 增强网络请求稳定性

#### 🆕 新增
- 新增自动缓存清理功能
- 添加更完善的错误处理机制
- 增强崩溃保护功能

### v3.0.3 及更早版本
查看 [完整更新日志](CHANGELOG.md)

## 🔌 API 文档

### 刷新操作
```http
# 刷新详情
GET http://127.0.0.1:9978/action?do=refresh&type=detail

# 刷新播放
GET http://127.0.0.1:9978/action?do=refresh&type=player

# 刷新直播
GET http://127.0.0.1:9978/action?do=refresh&type=live
```

### 推送功能
```http
# 推送字幕
GET http://127.0.0.1:9978/action?do=refresh&type=subtitle&path=http://xxx

# 推送弹幕
GET http://127.0.0.1:9978/action?do=refresh&type=danmaku&path=http://xxx
```

### 缓存管理
```http
# 新增缓存
GET http://127.0.0.1:9978/cache?do=set&key=xxx&value=xxx

# 获取缓存
GET http://127.0.0.1:9978/cache?do=get&key=xxx

# 删除缓存
GET http://127.0.0.1:9978/cache?do=del&key=xxx
```

更多API文档请查看 [API参考手册](docs/API.md)

## 📖 配置说明

### 点播字段配置
| 字段名 | 默认值 | 说明 | 备注 |
|--------|--------|------|------|
| searchable | 1 | 是否支持搜索 | 0:关闭 1:启用 |
| changeable | 1 | 是否支持换源 | 0:关闭 1:启用 |
| quickSearch | 1 | 是否快速搜索 | 0:关闭 1:启用 |
| timeout | 15 | 播放超时时间 | 单位:秒 |

### 直播字段配置
| 字段名 | 默认值 | 说明 | 备注 |
|--------|--------|------|------|
| ua | none | 用户代理 | |
| origin | none | 来源 | |
| referer | none | 引用地址 | |
| timeout | 15 | 播放超时 | 单位:秒 |

详细配置说明请查看 [配置文档](docs/CONFIG.md)

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

### 🔄 提交规范
- feat: 新功能
- fix: 修复bug
- docs: 文档更新
- style: 代码格式调整
- refactor: 代码重构
- test: 测试相关
- chore: 构建配置等

### 🧪 开发流程
1. Fork 本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

### ⚖️ 许可协议
XMBOX软件许可协议：
- 以下是对[GPL-3.0](LICENSE.md)开源协议的补充，如有冲突，以以下协议为准。
- 词语约定： 本协议中的“本软件”指“XMBOX软件”，“用户”指签署本协议的使用者，“版权数据”指包括但不限于视频、图像、音频、名字等在内的他人拥有所属版权的数据。
1. 本软件仅为技术性多媒体播放器外壳（“空壳播放器”），核心功能限于基础媒体文件解析与播放。
2. 本软件自身不包含、不预装、不内置、不集成、不主动推荐、不直接或间接提供任何音视频、直播、图文等媒体资源内容。软件播放的任何资源均非由本软件或其开发者提供。
3. 用户通过本软件播放的任何内容均完全来源于用户自行配置、输入、添加、获取或选择的第三方来源（如网络地址、本地文件、用户安装的插件/扩展/配置源等）。本软件仅作为访问用户自行指定内容的技术工具。
4. 本软件无法控制、筛选、审查或保证用户访问的任何第三方内容的合法性、版权状态、准确性、安全性或适宜性。用户对其播放的内容负全部责任。
5. 关于用户责任与风险承担：
   *  用户必须确保其通过本软件配置、访问或播放的所有内容均已获相关权利人合法授权，或属于法律允许的自由使用范畴。
   * 用户理解并同意，使用本软件访问第三方资源可能涉及侵犯版权、传播非法信息、隐私泄露、网络安全等风险。因用户使用本软件访问、播放或传播内容产生的一切法律责任、纠纷、损失及后果（包括法律诉讼、行政处罚、民事赔偿等），均由用户自行承担，与本软件及其开发者无涉。
   * 开发者不认可、不支持任何利用本软件规避技术保护措施（如DRM）的行为，此类行为导致的侵权责任由用户全权承担。
6. 用户承诺并保证不利用本软件从事任何侵犯他人知识产权或其他合法权益的活动，或进行任何违反法律法规的行为。严禁使用本软件播放、传播盗版、色情、暴力、赌博、诈骗、危害国家安全、危害社会稳定等非法或侵权内容。
7. 在任何情况下，本软件开发者均不就因用户使用或无法使用本软件、用户配置或访问的第三方资源、用户违反本协议或法律法规的行为导致的任何直接、间接、偶然、特殊、惩罚性或结果性损害（包括利润损失、数据丢失、业务中断、声誉损害等）承担任何责任（无论基于合同、侵权、严格责任或其他法律理论）。
8. 本软件运行可能依赖第三方库、服务或技术。开发者不对这些第三方组件的可用性、准确性、功能或合法性负责。
9. 用户理解并同意，使用本软件（包括下载、安装、运行）存在固有技术风险（如软件缺陷、兼容性问题、系统不稳定等），用户应自行承担此风险。
10. 本软件仅用于对技术可行性的探索及研究，不接受任何商业（包括但不限于广告等）合作及捐赠。
11. 本软件内使用的部分包括但不限于字体、图片等资源来源于互联网。如果出现侵权可联系开发者移除。
12. 使用本软件的过程中可能会产生版权数据。对于这些版权数据，本软件不拥有它们的所有权。为了避免侵权，用户务必在 24 小时内 清除使用本项目的过程中所产生的版权数据。
13. 本协议受中华人民共和国法律管辖并据其解释。若用户所在地法律强制规定特定责任条款，应以当地法律要求为准，但其他条款仍保持有效。任何由本协议或使用本软件引起的争议，应首先通过友好协商解决。
14. 若你使用了本软件，即代表你接受本协议。

## ⚖️ 免责声明

1. **学习用途**: 本项目仅供学习和技术交流使用，不得用于商业用途
2. **内容来源**: 项目中的内容来源于网络，如有侵权请联系删除
3. **使用责任**: 使用本项目产生的一切后果由使用者自行承担
4. **法律合规**: 请确保在当地法律法规允许的范围内使用本软件

## 📄 开源协议

本项目基于 [GPL-3.0](LICENSE.md) 协议开源

## 🙏 致谢

- 基于 [FongMi/TV](https://github.com/FongMi/TV) 项目开发
- 感谢 [CatVodTVOfficial](https://github.com/CatVodTVOfficial) 提供的核心技术
- 感谢所有为项目做出贡献的开发者

## 📞 联系方式

- GitHub Issues: [提交问题](../../issues)
- 讨论区: [Discussions](../../discussions)

---

<div align="center">

**⭐ 如果这个项目对你有帮助，请给我们一个 Star！**

Made with ❤️ by XMBOX Team

</div>
