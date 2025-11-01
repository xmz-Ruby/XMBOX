# XMBOX 项目文档

欢迎查阅XMBOX项目文档。本目录包含了项目各个模块的详细文档。

## 文档索引

### 弹幕系统

| 文档 | 说明 | 最后更新 |
|------|------|----------|
| [弹幕系统架构](./danmaku-system.md) | 弹幕系统整体架构、核心组件、API接口等 | 2024-10-27 |
| [弹幕弹层UI维护](./danmaku-dialog-ui.md) | 弹幕弹层界面实现、样式系统、焦点处理等 | 2025-11-01 |

### 快速导航

#### 弹幕相关
- **新手入门**: 先阅读 [弹幕系统架构](./danmaku-system.md) 了解整体设计
- **UI开发**: 查看 [弹幕弹层UI维护](./danmaku-dialog-ui.md) 了解界面实现细节
- **焦点问题**: 直接跳转到 [焦点处理机制](./danmaku-dialog-ui.md#焦点处理机制)
- **样式修改**: 参考 [样式系统](./danmaku-dialog-ui.md#样式系统)
- **问题排查**: 查看 [常见问题](./danmaku-dialog-ui.md#常见问题)

## 文档贡献

如需添加或更新文档，请遵循以下规范：

1. 使用Markdown格式
2. 保持文档结构清晰，使用合理的标题层级
3. 代码示例需要标注语言类型
4. 更新文档后修改"最后更新"时间
5. 在本README中添加索引

## 技术栈

- **Android**: Java, XML
- **UI框架**: Material Components
- **TV框架**: Android Leanback
- **弹幕引擎**: DanmakuFlameMaster

## 项目结构

```
XMBOX/
├── app/
│   ├── src/
│   │   ├── main/          # 共享代码
│   │   ├── mobile/        # 手机版资源
│   │   └── leanback/      # TV版资源
│   └── build.gradle
├── docs/                   # 📚 项目文档
│   ├── README.md          # 文档索引（本文件）
│   ├── danmaku-system.md  # 弹幕系统架构
│   └── danmaku-dialog-ui.md # 弹幕弹层UI
└── README.md              # 项目README
```

## 相关链接

- [Android TV开发指南](https://developer.android.com/training/tv)
- [Material Design](https://material.io/design)
- [DanmakuFlameMaster](https://github.com/bilibili/DanmakuFlameMaster)

---

最后更新: 2025-11-01
