[English](README_EN.md)

# F1 Pulse

F1 Pulse 是一个个人开发的非官方 Android 车迷应用，提供赛历、积分榜、比赛结果、时区转换和桌面小部件等功能。本项目作为软件开发作品集公开，与 Formula 1、FIA、任何车队或车手均无关系。

## 主要功能

- 展示练习赛、排位赛、冲刺赛和正赛日程
- 展示车手积分榜和车队积分榜
- 展示正赛及各场次排名
- 支持设备时区和赛道当地时区
- 使用 Room 缓存数据，并提供明确的刷新状态
- 使用 WorkManager 执行后台刷新
- 提供 7 种可响应尺寸变化的 Jetpack Glance 桌面小部件
- 支持历史赛季浏览

## 技术栈

- Kotlin 2.0.21、Jetpack Compose 与 Material 3
- MVVM、Repository、Coroutines 与 Flow
- Hilt 依赖注入
- Room 与 DataStore
- Retrofit、OkHttp 与 Moshi
- WorkManager 与 Jetpack Glance
- Coil 与 SVG 支持
- 最低 SDK 26，目标 SDK 35

## 数据源

应用使用免费公开的 Jolpica F1 和 OpenF1 公共接口。

## 可选视觉素材

本仓库不包含车手照片、车队标志、赛车图片、赛道图、比赛图片、小部件截图或专有品牌字体。应用使用系统字体和通用占位视觉。

如已有替换素材，请按照 [素材替换说明](app/src/main/assets/ASSETS.md) 中的目录、文件名和格式要求放置，然后重新编译。

## 构建方法

需要 JDK 17 和 Android SDK 35。可以设置 `ANDROID_HOME`，也可以在未纳入 Git 的 `local.properties` 中填写 Android SDK 路径。

```bash
./gradlew testDebugUnitTest assembleDebug lintDebug
```

Windows：

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

## 商标与许可说明

Formula 1、F1、车队名称、车手名称及相关标志归各自权利人所有。本项目中的名称仅用于说明非官方车迷项目所涉及的主题，不代表任何形式的认可或合作。

