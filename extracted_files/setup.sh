#!/bin/bash

# XMBOX项目设置脚本
# 这个脚本会自动复制提取的文件到正确的位置

echo "开始设置XMBOX项目..."

# 确保我们在正确的目录
cd "$(dirname "$0")"
EXTRACT_DIR=$(pwd)
PROJECT_DIR=$(dirname "$EXTRACT_DIR")

echo "提取目录: $EXTRACT_DIR"
echo "项目目录: $PROJECT_DIR"

# 创建必要的目录
echo "创建必要的目录..."
mkdir -p "$PROJECT_DIR/app/libs/arm64-v8a"
mkdir -p "$PROJECT_DIR/app/src/main/java/com/fongmi/android/tv/event"
mkdir -p "$PROJECT_DIR/app/src/main/res/values"

# 复制native库文件
echo "复制native库文件..."
if [ -d "$EXTRACT_DIR/libs/arm64-v8a" ]; then
  cp -R "$EXTRACT_DIR/libs/arm64-v8a/"* "$PROJECT_DIR/app/libs/arm64-v8a/" || echo "复制native库失败"
  echo "Native库复制完成"
else
  echo "libs/arm64-v8a 目录不存在"
fi

# 复制EventIndex类
echo "复制EventIndex类..."
if [ -f "$EXTRACT_DIR/java/com/fongmi/android/tv/event/EventIndex.java" ]; then
  cp "$EXTRACT_DIR/java/com/fongmi/android/tv/event/EventIndex.java" "$PROJECT_DIR/app/src/main/java/com/fongmi/android/tv/event/" || echo "复制EventIndex类失败"
  echo "EventIndex类复制完成"
else
  echo "EventIndex.java 文件不存在"
fi

# 复制颜色资源文件
echo "复制颜色资源文件..."
if [ -f "$EXTRACT_DIR/colors.xml" ]; then
  cp "$EXTRACT_DIR/colors.xml" "$PROJECT_DIR/app/src/main/res/values/" || echo "复制颜色资源文件失败"
  echo "颜色资源文件复制完成"
else
  echo "colors.xml 文件不存在"
fi

# 备份原有的build.gradle文件
echo "备份原有的build.gradle文件..."
if [ -f "$PROJECT_DIR/app/build.gradle" ]; then
  cp "$PROJECT_DIR/app/build.gradle" "$PROJECT_DIR/app/build.gradle.bak" || echo "备份build.gradle失败"
  echo "build.gradle备份完成"
else
  echo "app/build.gradle 文件不存在"
fi

# 将修改后的build.gradle复制到项目中
echo "复制修改后的build.gradle文件..."
if [ -f "$EXTRACT_DIR/modified_build.gradle" ]; then
  cp "$EXTRACT_DIR/modified_build.gradle" "$PROJECT_DIR/app/build.gradle" || echo "复制build.gradle失败"
  echo "build.gradle复制完成"
else
  echo "modified_build.gradle 文件不存在"
fi

# 修改gradle.properties文件
echo "修改gradle.properties文件..."
if [ -f "$PROJECT_DIR/gradle.properties" ]; then
  # 添加Java兼容性配置
  echo "" >> "$PROJECT_DIR/gradle.properties"
  echo "# 允许访问JDK内部API" >> "$PROJECT_DIR/gradle.properties"
  echo "android.injected.testOnly=false" >> "$PROJECT_DIR/gradle.properties"
  echo "android.enableR8.fullMode=false" >> "$PROJECT_DIR/gradle.properties"
  echo "org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8 --add-exports=java.base/sun.nio.ch=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.lang.reflect=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED --add-exports=jdk.unsupported/sun.misc=ALL-UNNAMED" >> "$PROJECT_DIR/gradle.properties"
  echo "gradle.properties修改完成"
else
  echo "gradle.properties 文件不存在"
fi

echo "设置完成！您现在可以尝试构建项目了。"
echo "请运行: ./gradlew assembleMobileArm64_v8aDebug --info" 