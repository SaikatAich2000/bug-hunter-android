# Running Bug Hunter Android

Three deploy paths, plus an optional wireless one.

## Prerequisites

- **Android Studio Koala** or newer — bundles JDK 17, Android SDK 35, and ADB. Get it at <https://developer.android.com/studio>.
- A reachable **Bug Hunter backend** (HTTPS).
- A phone with **Developer options → USB debugging** enabled, or an emulator (Pixel 7 API 35 works well).

> Enable Developer options: **Settings → About phone → Build number** → tap 7 times → **Settings → System → Developer options → USB debugging**.

## 1. Point the app at your backend

Edit `gradle.properties`:

```properties
bh.baseUrl=https://your-bug-hunter-backend.example.com
```

Re-sync in Android Studio (🐘 icon → Sync Now).

You can also change it at runtime via **Settings → Developer → Base URL** in debug builds. The cookie jar clears on URL change.

## Path A — Android Studio + USB (easiest)

1. **File → Open** → pick the project folder → wait for Gradle sync.
2. Plug your phone in. Accept the "Allow USB debugging" prompt.
3. Confirm your device shows in the device dropdown next to the ▶ Run button.
4. Click ▶ **Run**.

Studio compiles, installs, and launches the app.

## Path B — Command line + USB

```bash
# from project root
./gradlew installDebug
```

The APK is built and installed on the connected device in one step.

To launch via ADB:
```bash
adb shell am start -n com.bughunter/.MainActivity
```

## Path C — Build APK, then sideload

```bash
./gradlew assembleDebug
# APK lands at: app/build/outputs/apk/debug/app-debug.apk
```

Transfer the APK to the phone (USB, email, cloud drive). On the phone:
1. Open **Files** → tap the APK.
2. Allow install from this source if prompted.
3. Tap **Install**.

## Path D — Wireless ADB (Android 11+)

Phone and laptop on the same Wi-Fi.

**On the phone:** Developer options → **Wireless debugging** → enable → **Pair device with pairing code**. Note the IP:port and 6-digit code.

**On the laptop:**
```bash
adb pair 192.168.1.42:42137
# enter the 6-digit code
adb connect 192.168.1.42:5555  # use the persistent port from the Wireless debugging screen
```

Then deploy normally (Path A or B). Pairing is one-time; connect once per Wi-Fi session.

## Common gotchas

| Symptom | Fix |
|---|---|
| Gradle sync fails on `compileSdk 35` | SDK Manager → install **Android 15 (API 35)**. |
| `./gradlew` not found | Run `gradle wrapper --gradle-version 8.10.2` once, or open the project in Android Studio (it bootstraps). |
| Phone shows as `unauthorized` in `adb devices` | Unplug + replug, accept the trust prompt, tap **Always allow**. |
| Phone not in device dropdown | Pull down notification shade on phone → USB notification → switch to **File transfer** mode. Use a data cable, not charge-only. |
| Login spins forever | If your backend is on Render free tier or similar, first request takes 30–60s while it cold-starts. |
| Login fails with network/SSL error | Backend not reachable from the phone, or its cert isn't trusted on Android. Test via the phone's browser first. |
| White screen on release build | Default ProGuard rules cover Moshi codegen but may miss runtime adapters. Toggle `isMinifyEnabled = false` to bisect. |

## Build cheat sheet

```bash
./gradlew assembleDebug                # debug APK
./gradlew installDebug                 # install on connected device
./gradlew testDebugUnitTest            # JVM unit tests
./gradlew connectedDebugAndroidTest    # instrumented tests (emulator/device)
./gradlew :app:lintDebug               # Android Lint
./gradlew assembleRelease              # release APK (needs your own signing config)
```

Release builds require a signing config under `app/build.gradle.kts` → `signingConfigs { release { ... } }`. None is shipped in this repo.
