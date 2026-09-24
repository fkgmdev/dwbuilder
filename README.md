# Deepwoken Builder (Android)

A fully offline Android port of [deepwoken.co/builder](https://deepwoken.co/builder) —
Kotlin + Jetpack Compose, Material You themed. All game data is bundled as
minified JSON and parsed in memory: **zero network at runtime**, no login, no
cloud save.

---

## Project layout

```
dwbuilder/
├── android/                       # the Android app (a Gradle project)
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── java/com/dwbuilder/app/
│   │   │   │   ├── MainActivity.kt        # entry point: theme + bottom-nav + tabs
│   │   │   │   ├── data/                  # loading bundled JSON → GameData
│   │   │   │   ├── domain/                # pure Kotlin game rules (JS ports)
│   │   │   │   └── ui/                    # Compose screens, theme, ViewModel
│   │   │   ├── assets/data/*.json         # minified game data (checked in, ships in APK)
│   │   │   └── res/                       # launcher icon, manifest theme
│   │   ├── src/test/                      # JVM unit tests (no device needed)
│   │   └── build.gradle.kts
│   ├── gradle/, gradlew, gradlew.bat     # the Gradle build tool + wrapper
│   └── keystore/                          # RELEASE SIGNING KEY — gitignored, keep safe
├── data/raw/                    # human-readable source JSON (edit these)
├── scripts/build_assets.py      # minify data/raw → app assets + test resources
├── www/                         # captured deepwoken.co site bundles (re-capture reference)
└── docs/site-reference.md       # extracts of the site's JS engines we ported
```

## How the app works (three layers)

1. **`data/`** — `DataProvider` loads every bundled JSON file once at startup into
   `GameData`, an immutable in-memory catalog (1155 talents, 267 mantras, 271
   weapons, 35 damage mods, …). No Room, no network.
2. **`domain/`** — pure, unit-tested rules with **no Android imports**:
   `Points` (power/points math), `TalentRules`, `MantraRules`, `Requirements`,
   `DamageRules` (weapon damage breakdown), `PveRules` (PvE calculator).
   These are exact ports of the site's JS so numbers match the live builder.
3. **`ui/`** — Compose screens. `BuilderViewModel` holds the working `Build`
   (an immutable data class); every tap produces a new `Build`, which
   recomposes the screen. Tabs are wired in `MainActivity`.

---

## Prerequisites

- JDK 17+ (this machine builds on JDK 25 with a Java 17 target)
- Android SDK with platform 36 + build-tools (set `ANDROID_HOME`)
- A phone (USB or wireless `adb`) or an emulator to install on

## Building

On this machine, first:

```bash
export ANDROID_HOME=/home/yamana/android-sdk
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk
cd android
```

Key commands (all via the Gradle wrapper — never install Gradle globally):

| What | Command |
| --- | --- |
| Build debug APK | `./gradlew :app:assembleDebug` |
| Build + install debug on a device | `./gradlew :app:installDebug` |
| Run the unit tests | `./gradlew :app:testDebugUnitTest` |
| Build the release APK (R8 + signed) | `./gradlew :app:assembleRelease` |
| Nuke all build outputs | `./gradlew clean` |

Outputs:

- Debug: `app/build/outputs/apk/debug/app-debug.apk` (~14 MB)
- Release: `app/build/outputs/apk/release/app-release.apk` (~1.6 MB, optimized)

Working with your wireless device:

```bash
adb connect 192.168.1.5:36437
adb install -r app/build/outputs/apk/release/app-release.apk
adb logcat                                      # live logs (crash traces here)
```

## Release signing — read this

- The release APK is signed with `android/keystore/release.jks`; the passwords
  live in `android/keystore.properties`.
- **Both files are gitignored.** Back them up — Android only lets you update an
  app with the *same* signature, so losing the keystore means losing the ability
  to ship updates over your installed app.
- If `keystore.properties` is missing, `assembleRelease` still builds but signs
  with the debug key (fine for sideloading, not for the Play Store).

## Updating game data after a game patch

1. Refresh `data/raw/*.json` (see `scripts/` and `www/` for the capture source).
2. Run `python3 scripts/build_assets.py` — minifies and copies into
   `android/app/src/main/assets/data/` and the JVM test resources.
3. If a brand-new catalog file is added, register it in `DataProvider.bundleNames`.
4. Rebuild + re-run tests.

## Common tasks

- **Add a tab**: add it to `TABS` in `MainActivity.kt` and extend the `when (selectedTab)`.
- **New screen**: create `ui/screens/XScreen.kt`, give it a `BuilderViewModel`
  parameter, wire it into the tab.
- **Change a rule** (e.g. power curve): edit the matching object in `domain/`,
  then `./gradlew :app:testDebugUnitTest` — all engine behavior is pinned by tests.
- **Theme/colors**: `ui/theme/Theme.kt` and `ui/Colors.kt` (attunement/rarity palettes).
- **Reusable UI**: `ui/components/Components.kt` (steppers, dropdowns, cards, chips).