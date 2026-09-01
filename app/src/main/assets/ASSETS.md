[English](ASSETS_EN.md)

# 可选素材替换说明

仓库不包含赛车运动图片、车队标志、赛道图或专有字体。

## 目录结构

如已有替换素材，可根据需要在 `app/src/main/assets/` 下建立以下目录：

```text
cars/
car_numbers/
driver_headshots/
driver_headshots_square/
race_cards/
team_logos/
tracks/
track_thumbnails/
```

以下代码文件定义了素材名称与数据标识之间的映射：

- `core/CarAssets.kt`
- `core/RaceCardAssets.kt`
- `core/TeamAssets.kt`
- `ui/components/DriverAssets.kt`
- `widget/CountdownWidgetLayoutPolicy.kt`

下列路径均相对于 `app/src/main/assets/`。

## 文件格式与命名

- 车手半身图：`driver_headshots/<driver-name>.webp`
- 车手方形头像：`driver_headshots_square/<driver-name>.webp`
- 车队标志：`team_logos/<constructor-id>.webp`
- 赛车图片：`cars/<constructor-id>.webp`
- 比赛卡片图片：`race_cards/<circuit-id>.webp`
- 赛道图：`tracks/<circuit-id>.webp` 或 `.svg`
- 小部件赛道缩略图：`track_thumbnails/<circuit-id>.svg`

车队标志和赛车图片建议使用透明背景。图片尺寸应尽量接近实际显示尺寸，以免增加 APK 体积和小部件内存占用。

## 字体替换

公开副本默认使用 Android 系统字体。如需替换为自定义字体，将字体文件放入 `app/src/main/res/font/`，并修改：

- `ui/theme/F1Fonts.kt`
- `widget/WidgetTextRenderer.kt`

## 重新构建

添加素材后运行：

```bash
./gradlew clean testDebugUnitTest assembleDebug lintDebug
```

构建完成后，应检查 APK 体积，并在设备或模拟器上测试桌面小部件的最小和最大尺寸。
