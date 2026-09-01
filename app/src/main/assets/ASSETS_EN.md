[简体中文](ASSETS.md)

# Optional asset replacement guide

The repository does not include motorsport photography, team marks, circuit artwork, or proprietary fonts.

## Directory layout

If replacement assets are available, create the following directories under `app/src/main/assets/` as needed:

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

The following source files define the mappings between asset names and data identifiers:

- `core/CarAssets.kt`
- `core/RaceCardAssets.kt`
- `core/TeamAssets.kt`
- `ui/components/DriverAssets.kt`
- `widget/CountdownWidgetLayoutPolicy.kt`

All paths below are relative to `app/src/main/assets/`.

## File formats and names

- Driver portraits: `driver_headshots/<driver-name>.webp`
- Square driver portraits: `driver_headshots_square/<driver-name>.webp`
- Team logos: `team_logos/<constructor-id>.webp`
- Car images: `cars/<constructor-id>.webp`
- Race-card images: `race_cards/<circuit-id>.webp`
- Circuit maps: `tracks/<circuit-id>.webp` or `.svg`
- Widget circuit thumbnails: `track_thumbnails/<circuit-id>.svg`

Transparent backgrounds are recommended for team logos and car images. Keep image dimensions close to their rendered size to limit APK growth and widget memory use.

## Font replacement

The public copy uses Android system fonts by default. To replace them with custom fonts, place the font files in `app/src/main/res/font/` and update:

- `ui/theme/F1Fonts.kt`
- `widget/WidgetTextRenderer.kt`

## Rebuild

After adding assets, run:

```bash
./gradlew clean testDebugUnitTest assembleDebug lintDebug
```

After the build, inspect the APK size and test the smallest and largest widget sizes on a device or emulator.
