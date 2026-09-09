[简体中文](README.md)

# paddock-pulse

paddock-pulse is a personal, unofficial Android fan app for race schedules, standings, session results, time-zone conversion, and home-screen widgets. It is published as a software-development portfolio project and has no relationship with Formula 1, the FIA, any team, or any driver.

## Features

- Practice, qualifying, sprint, and race schedules
- Driver and constructor standings
- Race and session classifications
- Device-time and circuit-local-time display modes
- Room-backed caching with explicit refresh states
- WorkManager background refresh
- Seven size-responsive Jetpack Glance home-screen widgets
- Historical-season browsing

## Tech stack

- Kotlin 2.0.21, Jetpack Compose, and Material 3
- MVVM, Repository, Coroutines, and Flow
- Hilt dependency injection
- Room and DataStore
- Retrofit, OkHttp, and Moshi
- WorkManager and Jetpack Glance
- Coil with SVG support
- Minimum SDK 26; target SDK 35

## Data sources

The app uses the free, public Jolpica F1 and OpenF1 APIs.

## Optional visual assets

This repository does not include driver photos, team logos, car images, circuit artwork, race photography, widget screenshots, or proprietary brand fonts. The app uses system fonts and generic visual placeholders.

If replacement assets are available, place them according to the directories, file names, and formats in the [asset replacement guide](app/src/main/assets/ASSETS_EN.md), then rebuild the app.

## Build

JDK 17 and Android SDK 35 are required. Set `ANDROID_HOME`, or specify the Android SDK path in an untracked `local.properties` file.

```bash
./gradlew testDebugUnitTest assembleDebug lintDebug
```

Windows:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

## Trademark and licensing notice

Formula 1, F1, team names, driver names, and related marks belong to their respective owners. Their use here identifies the subject matter of an unofficial fan project and does not imply endorsement or affiliation.
