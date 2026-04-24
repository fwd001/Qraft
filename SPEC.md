# QRCodeKit - 文本转二维码应用

## 1. 项目概述

- **项目名称**: QRCodeKit
- **项目类型**: 原生 Android 应用
- **核心功能**: 用户输入文本，自动按800字切割生成多个二维码，支持左右滑动浏览和分享

## 2. 技术栈

- **语言**: Kotlin 2.0+
- **UI框架**: Jetpack Compose (Material Design 3)
- **最小SDK**: Android 15 (API 35)
- **目标SDK**: Android 15 (API 35)
- **架构**: MVVM + Clean Architecture
- **二维码库**: ZXing Android Embedded
- **依赖注入**: Hilt
- **异步处理**: Kotlin Coroutines + Flow

## 3. 功能需求

### 3.1 核心功能

1. **文本输入**
   - 大文本输入框，支持多行输入
   - 实时字数统计显示
   - 支持复制粘贴

2. **二维码生成**
   - 按800字自动切割文本
   - 每段生成独立二维码
   - 纠错级别：中等 (ERROR_CORRECTION_M)
   - 二维码尺寸：屏幕宽度 - 32dp

3. **二维码浏览**
   - 水平ViewPager2实现左右滑动
   - 当前页码/总页数指示器
   - 平滑过渡动画

4. **分享功能**
   - 分享当前二维码图片
   - 使用系统分享面板

### 3.2 UI/UX要求

- **屏幕方向**: 仅竖屏 (android:screenOrientation="portrait")
- **主题**: Material Design 3，暗色/亮色跟随系统
- **配色**: 现代化配色方案

## 4. 页面结构

```
MainScreen
├── TopAppBar (标题)
├── TextInputSection
│   ├── OutlinedTextField (文本输入)
│   └── TextCountIndicator (字数统计)
├── GenerateButton (生成按钮)
└── QRCodesSection (生成后显示)
    ├── ViewPager2 (二维码滑动浏览)
    │   └── QRCodeItem (每个二维码 + 页码)
    └── ShareButton (分享按钮)
```

## 5. 技术实现细节

### 5.1 二维码生成策略
- 每个二维码最大容量约2000+字符（中文字符按1个计算，但实际ZXing中文编码效率较低）
- 考虑到实际编码效率，800个中文字符是安全阈值
- 使用UTF-8编码

### 5.2 二维码尺寸计算
```kotlin
val qrCodeSize = (screenWidthPx - 64.dpToPx()) // 左右各32dp边距
```

### 5.3 文本切割逻辑
```kotlin
fun splitText(text: String, maxChars: Int = 800): List<String> {
    if (text.length <= maxChars) return listOf(text)
    // 按800字切割，返回字符串列表
}
```
