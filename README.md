# Qraft - 文本转二维码应用

一个简洁高效的 Android 应用，支持将长文本自动切割生成多个二维码。

## 功能

- 智能切割：短文本单码模式(1500字)，长文本自动分码
- 二维码生成：支持中文、英文、特殊字符
- 纠错等级：中等纠错(M级别)，保证可读性
- 滑动浏览：左右滑动查看多个二维码
- 一键分享：快速分享二维码图片
- 历史记录：保存生成记录
- Material Design 3：现代美观界面

## 技术栈

- Kotlin 2.0+ / Jetpack Compose
- MVVM + Clean Architecture
- Hilt 依赖注入 / Room 数据库
- ZXing 二维码库
- 最小 SDK: Android 15 (API 35)

## 构建

```bash
# Debug
./gradlew assembleDebug

# Release
./gradlew assembleRelease

# 测试
./gradlew test
```

## 安装

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## License

MIT
