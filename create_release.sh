#!/bin/bash

# GitHub CLI 创建 Release 脚本
# 使用前请先运行: gh auth login

echo "创建 XMBOX v3.0.7 Release..."

gh release create v3.0.7 \
  --title "XMBOX v3.0.7 - 全面优化稳定性和用户体验" \
  --notes-file RELEASE_NOTES_v3.0.7.md \
  --draft \
  ~/Desktop/mobile-arm64_v8a-v3.0.7.apk \
  ~/Desktop/mobile-armeabi_v7a-v3.0.7.apk \
  ~/Desktop/leanback-arm64_v8a-v3.0.7.apk \
  ~/Desktop/leanback-armeabi_v7a-v3.0.7.apk

echo "Release 创建完成（草稿状态）"
echo "请在 GitHub 上检查并发布"
